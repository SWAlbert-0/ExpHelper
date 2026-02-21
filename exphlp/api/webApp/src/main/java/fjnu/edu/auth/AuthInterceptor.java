package fjnu.edu.auth;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = TraceContext.ensureTraceId(request, response);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/captcha")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/auth/avatar/")) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("traceId={} path={} method={} errorCode={}", traceId, path, request.getMethod(), "AUTH_MISSING_TOKEN");
            writeUnauthorized(request, response, "未登录或登录已过期", "AUTH_MISSING_TOKEN");
            return false;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        AuthUser authUser = jwtUtil.verify(token);
        if (authUser == null) {
            log.warn("traceId={} path={} method={} errorCode={}", traceId, path, request.getMethod(), "AUTH_INVALID_TOKEN");
            writeUnauthorized(request, response, "登录信息无效", "AUTH_INVALID_TOKEN");
            return false;
        }
        request.setAttribute("authUser", authUser);
        return true;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String msg, String errorCode) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(ApiResponse.failed(request, 401, msg, errorCode)));
    }
}
