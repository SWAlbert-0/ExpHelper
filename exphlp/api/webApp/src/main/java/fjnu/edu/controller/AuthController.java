package fjnu.edu.controller;

import fjnu.edu.Main;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.JwtUtil;
import fjnu.edu.auth.LoginRequest;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.auth.UserFieldValidator;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final PlatMgrService platMgrService;
    private final PasswordService passwordService;
    private final JwtUtil jwtUtil;
    private final Path avatarDir;

    public AuthController(
            PlatMgrService platMgrService,
            PasswordService passwordService,
            JwtUtil jwtUtil,
            @Value("${auth.avatar-dir:./data/uploads/avatar}") String avatarDir
    ) {
        this.platMgrService = platMgrService;
        this.passwordService = passwordService;
        this.jwtUtil = jwtUtil;
        this.avatarDir = Paths.get(avatarDir).toAbsolutePath().normalize();
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String traceId = TraceContext.getTraceId(httpRequest);
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            log.warn("traceId={} path={} errorCode={}", traceId, "/api/auth/login", ErrorCode.AUTH_BAD_CREDENTIAL_PAYLOAD.code());
            return ApiResponse.failed(httpRequest, 400, "用户名或密码不能为空", ErrorCode.AUTH_BAD_CREDENTIAL_PAYLOAD.code());
        }
        UserInfo userInfo = platMgrService.getUserByName(request.getUsername().trim());
        if (userInfo == null || !passwordService.matches(request.getPassword(), userInfo.getPassword())) {
            log.warn("traceId={} path={} username={} errorCode={}", traceId, "/api/auth/login", request.getUsername(), ErrorCode.AUTH_INVALID_CREDENTIALS.code());
            return ApiResponse.failed(httpRequest, 401, "用户名或密码错误", ErrorCode.AUTH_INVALID_CREDENTIALS.code());
        }

        // Password migration: plain-text passwords are transparently upgraded to bcrypt.
        if (!passwordService.isBcrypt(userInfo.getPassword())) {
            userInfo.setPassword(passwordService.encode(request.getPassword()));
            platMgrService.updateUserById(userInfo);
        }

        AuthUser authUser = new AuthUser(userInfo.getUserId(), userInfo.getUserName(), userInfo.getRole());
        String token = jwtUtil.generateToken(authUser);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresAt", System.currentTimeMillis() + 24L * 60 * 60 * 1000);
        log.info("traceId={} userId={} path={} action=login status=success", traceId, userInfo.getUserId(), "/api/auth/login");
        return ApiResponse.ok(httpRequest, data);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            log.warn("traceId={} path={} errorCode={}", TraceContext.getTraceId(request), "/api/auth/me", ErrorCode.AUTH_UNAUTHORIZED.code());
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", userInfo.getUserName());
        user.put("avatar", userInfo.getAvatar() == null ? "" : userInfo.getAvatar());
        user.put("unread_msg_count", 0);

        Map<String, Object> data = new HashMap<>();
        data.put("roles", Collections.singletonList("ROLE_DEFAULT"));
        data.put("permissions", new ArrayList<>());
        data.put("user", user);
        return ApiResponse.ok(request, data);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            log.warn("traceId={} path={} errorCode={}", TraceContext.getTraceId(request), "/api/auth/profile", ErrorCode.AUTH_UNAUTHORIZED.code());
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfo.getUserId());
        data.put("username", userInfo.getUserName());
        data.put("email", userInfo.getEmail());
        data.put("wechat", userInfo.getWechat());
        data.put("mobile", userInfo.getMobile());
        data.put("qq", userInfo.getQq());
        data.put("avatar", userInfo.getAvatar() == null ? "" : userInfo.getAvatar());
        return ApiResponse.ok(request, data);
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            log.warn("traceId={} path={} errorCode={}", TraceContext.getTraceId(request), "/api/auth/profile", ErrorCode.AUTH_UNAUTHORIZED.code());
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        String userName = stringValue(payload.get("username"));
        if (!StringUtils.hasText(userName)) {
            userName = stringValue(payload.get("userName"));
        }
        String email = stringValue(payload.get("email"));
        String wechat = stringValue(payload.get("wechat"));
        String mobile = stringValue(payload.get("mobile"));
        String qq = stringValue(payload.get("qq"));

        if (!StringUtils.hasText(userName)) {
            log.warn("traceId={} userId={} path={} errorCode={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/profile", ErrorCode.USER_NAME_REQUIRED.code());
            return ApiResponse.failed(request, 400, "用户名不能为空", ErrorCode.USER_NAME_REQUIRED.code());
        }
        String emailMsg = UserFieldValidator.validateEmail(email);
        if (emailMsg != null) {
            log.warn("traceId={} userId={} path={} errorCode={} reason={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/profile", ErrorCode.USER_EMAIL_INVALID.code(), emailMsg);
            return ApiResponse.failed(request, 400, emailMsg, ErrorCode.USER_EMAIL_INVALID.code());
        }
        String mobileMsg = UserFieldValidator.validateMobile(mobile);
        if (mobileMsg != null) {
            log.warn("traceId={} userId={} path={} errorCode={} reason={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/profile", ErrorCode.USER_MOBILE_INVALID.code(), mobileMsg);
            return ApiResponse.failed(request, 400, mobileMsg, ErrorCode.USER_MOBILE_INVALID.code());
        }
        String qqMsg = UserFieldValidator.validateQq(qq);
        if (qqMsg != null) {
            log.warn("traceId={} userId={} path={} errorCode={} reason={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/profile", ErrorCode.USER_QQ_INVALID.code(), qqMsg);
            return ApiResponse.failed(request, 400, qqMsg, ErrorCode.USER_QQ_INVALID.code());
        }
        userInfo.setUserName(userName.trim());
        userInfo.setEmail(email == null ? null : email.trim());
        userInfo.setWechat(wechat == null ? null : wechat.trim());
        userInfo.setMobile(mobile == null ? null : mobile.trim());
        userInfo.setQq(qq == null ? null : qq.trim());
        platMgrService.updateUserById(userInfo);
        log.info("traceId={} userId={} path={} action=updateProfile status=success", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/profile");
        return ApiResponse.ok(request, null);
    }

    @PutMapping("/password")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        String oldPassword = stringValue(payload.get("oldPassword"));
        String newPassword = stringValue(payload.get("newPassword"));
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            log.warn("traceId={} userId={} path={} errorCode={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/password", ErrorCode.PASSWORD_EMPTY.code());
            return ApiResponse.failed(request, 400, "旧密码和新密码不能为空", ErrorCode.PASSWORD_EMPTY.code());
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            log.warn("traceId={} userId={} path={} errorCode={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/password", ErrorCode.PASSWORD_LENGTH_INVALID.code());
            return ApiResponse.failed(request, 400, "新密码长度需在6到50之间", ErrorCode.PASSWORD_LENGTH_INVALID.code());
        }
        if (!passwordService.matches(oldPassword, userInfo.getPassword())) {
            log.warn("traceId={} userId={} path={} errorCode={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/password", ErrorCode.PASSWORD_OLD_INVALID.code());
            return ApiResponse.failed(request, 401, "旧密码错误", ErrorCode.PASSWORD_OLD_INVALID.code());
        }
        userInfo.setPassword(passwordService.encode(newPassword));
        platMgrService.updateUserById(userInfo);
        log.info("traceId={} userId={} path={} action=updatePassword status=success", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/password");
        return ApiResponse.ok(request, null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.failed(request, 400, "上传文件不能为空", ErrorCode.AVATAR_FILE_EMPTY.code());
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        Set<String> allowed = new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".webp"));
        if (!allowed.contains(ext)) {
            return ApiResponse.failed(request, 400, "仅支持png/jpg/jpeg/gif/webp格式图片", ErrorCode.AVATAR_FILE_TYPE_INVALID.code());
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.createDirectories(avatarDir);
            Path target = avatarDir.resolve(filename).normalize();
            if (!target.startsWith(avatarDir)) {
                return ApiResponse.failed(request, 400, "非法文件路径", ErrorCode.AVATAR_PATH_INVALID.code());
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String avatarPath = "/api/auth/avatar/" + filename;
            userInfo.setAvatar(avatarPath);
            platMgrService.updateUserById(userInfo);
            Map<String, Object> data = new HashMap<>();
            data.put("avatar", avatarPath);
            log.info("traceId={} userId={} path={} action=uploadAvatar status=success avatar={}", TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/avatar", avatarPath);
            return ApiResponse.ok(request, data);
        } catch (IOException e) {
            log.error("traceId={} userId={} path={} action=uploadAvatar status=failed errorCode={} message={}",
                    TraceContext.getTraceId(request), userInfo.getUserId(), "/api/auth/avatar", ErrorCode.AVATAR_UPLOAD_FAILED.code(), e.getMessage());
            return ApiResponse.failed(request, 500, "头像上传失败", ErrorCode.AVATAR_UPLOAD_FAILED.code());
        }
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String filename) throws MalformedURLException {
        Path file = avatarDir.resolve(filename).normalize();
        if (!file.startsWith(avatarDir) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        String contentType = "application/octet-stream";
        try {
            String detected = Files.probeContentType(file);
            if (detected != null) {
                contentType = detected;
            }
        } catch (IOException ignored) {
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        return ApiResponse.ok(request, null);
    }

    @GetMapping("/captcha")
    public Map<String, Object> captcha(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("image_url", "");
        data.put("key", UUID.randomUUID().toString());
        return ApiResponse.ok(request, data);
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("artifactVersion", resolveArtifactVersion());
        data.put("buildTime", resolveBuildTime());
        return ApiResponse.ok(request, data);
    }

    private UserInfo currentUser(HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute("authUser");
        if (authUser == null || !StringUtils.hasText(authUser.getUserId())) {
            return null;
        }
        UserInfo userInfo = platMgrService.getUserById(authUser.getUserId());
        if (userInfo == null && StringUtils.hasText(authUser.getUserName())) {
            // Compatibility fallback for historical data where _id mapping is inconsistent.
            userInfo = platMgrService.getUserByName(authUser.getUserName());
        }
        return userInfo;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveArtifactVersion() {
        String version = null;
        try {
            Properties props = new Properties();
            try (var in = Main.class.getResourceAsStream("/META-INF/maven/fjnu.edu/webApp/pom.properties")) {
                if (in != null) {
                    props.load(in);
                    version = props.getProperty("version");
                }
            }
        } catch (Exception ignored) {
        }
        if (!StringUtils.hasText(version)) {
            Package pkg = getClass().getPackage();
            version = pkg == null ? null : pkg.getImplementationVersion();
        }
        return StringUtils.hasText(version) ? version : "unknown";
    }

    private String resolveBuildTime() {
        try {
            Path path = resolveJarPath();
            if (!Files.exists(path)) {
                return "unknown";
            }
            long timestamp = Files.getLastModifiedTime(path).toMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private Path resolveJarPath() {
        String cmd = System.getProperty("sun.java.command", "");
        if (StringUtils.hasText(cmd)) {
            String mainToken = Pattern.compile("\\s+").split(cmd.trim(), 2)[0];
            if (mainToken.endsWith(".jar")) {
                return Paths.get(mainToken).toAbsolutePath().normalize();
            }
        }
        try {
            URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
            return Paths.get(location.toURI()).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }
}
