package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.JwtUtil;
import fjnu.edu.auth.LoginRequest;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {
    private final PlatMgrService platMgrService;
    private final PasswordService passwordService;
    private final JwtUtil jwtUtil;

    public AuthController(PlatMgrService platMgrService, PasswordService passwordService, JwtUtil jwtUtil) {
        this.platMgrService = platMgrService;
        this.passwordService = passwordService;
        this.jwtUtil = jwtUtil;
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
        AuthUser authUser = (AuthUser) request.getAttribute("authUser");
        if (authUser == null) {
            return unauthorized("未登录");
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", authUser.getUserName());
        user.put("avatar", "");
        user.put("unread_msg_count", 0);

        Map<String, Object> data = new HashMap<>();
        data.put("roles", Collections.singletonList("ROLE_DEFAULT"));
        data.put("permissions", new ArrayList<>());
        data.put("user", user);
        return ok(data);
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
}
