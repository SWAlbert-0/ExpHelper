package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.JwtUtil;
import fjnu.edu.auth.LoginRequest;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {
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
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return failed("用户名或密码不能为空");
        }
        UserInfo userInfo = platMgrService.getUserByName(request.getUsername().trim());
        if (userInfo == null || !passwordService.matches(request.getPassword(), userInfo.getPassword())) {
            return unauthorized("用户名或密码错误");
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
        return ok(data);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return unauthorized("未登录");
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", userInfo.getUserName());
        user.put("avatar", userInfo.getAvatar() == null ? "" : userInfo.getAvatar());
        user.put("unread_msg_count", 0);

        Map<String, Object> data = new HashMap<>();
        data.put("roles", Collections.singletonList("ROLE_DEFAULT"));
        data.put("permissions", new ArrayList<>());
        data.put("user", user);
        return ok(data);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return unauthorized("未登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfo.getUserId());
        data.put("username", userInfo.getUserName());
        data.put("email", userInfo.getEmail());
        data.put("wechat", userInfo.getWechat());
        data.put("mobile", userInfo.getMobile());
        data.put("qq", userInfo.getQq());
        data.put("avatar", userInfo.getAvatar() == null ? "" : userInfo.getAvatar());
        return ok(data);
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return unauthorized("未登录");
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
            return failed("用户名不能为空");
        }
        userInfo.setUserName(userName.trim());
        userInfo.setEmail(email == null ? null : email.trim());
        userInfo.setWechat(wechat == null ? null : wechat.trim());
        userInfo.setMobile(mobile == null ? null : mobile.trim());
        userInfo.setQq(qq == null ? null : qq.trim());
        platMgrService.updateUserById(userInfo);
        return ok(null);
    }

    @PutMapping("/password")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return unauthorized("未登录");
        }
        String oldPassword = stringValue(payload.get("oldPassword"));
        String newPassword = stringValue(payload.get("newPassword"));
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return failed("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            return failed("新密码长度需在6到50之间");
        }
        if (!passwordService.matches(oldPassword, userInfo.getPassword())) {
            return unauthorized("旧密码错误");
        }
        userInfo.setPassword(passwordService.encode(newPassword));
        platMgrService.updateUserById(userInfo);
        return ok(null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        UserInfo userInfo = currentUser(request);
        if (userInfo == null) {
            return unauthorized("未登录");
        }
        if (file == null || file.isEmpty()) {
            return failed("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        Set<String> allowed = new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".webp"));
        if (!allowed.contains(ext)) {
            return failed("仅支持png/jpg/jpeg/gif/webp格式图片");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.createDirectories(avatarDir);
            Path target = avatarDir.resolve(filename).normalize();
            if (!target.startsWith(avatarDir)) {
                return failed("非法文件路径");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String avatarPath = "/api/auth/avatar/" + filename;
            userInfo.setAvatar(avatarPath);
            platMgrService.updateUserById(userInfo);
            Map<String, Object> data = new HashMap<>();
            data.put("avatar", avatarPath);
            return ok(data);
        } catch (IOException e) {
            return failed("头像上传失败");
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
    public Map<String, Object> logout() {
        return ok(null);
    }

    @GetMapping("/captcha")
    public Map<String, Object> captcha() {
        Map<String, Object> data = new HashMap<>();
        data.put("image_url", "");
        data.put("key", UUID.randomUUID().toString());
        return ok(data);
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        return ok("ok");
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> unauthorized(String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 401);
        response.put("msg", msg);
        return response;
    }

    private Map<String, Object> failed(String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("msg", msg);
        return response;
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
}
