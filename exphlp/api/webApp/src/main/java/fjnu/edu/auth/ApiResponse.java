package fjnu.edu.auth;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponse {
    private ApiResponse() {
    }

    public static Map<String, Object> ok(HttpServletRequest request, Object data) {
        return ok(request, data, "success");
    }

    public static Map<String, Object> ok(HttpServletRequest request, Object data, String msg) {
        Map<String, Object> response = base(request, 200, msg);
        response.put("data", data);
        return response;
    }

    public static Map<String, Object> failed(HttpServletRequest request, int code, String msg, String errorCode) {
        return failed(request, code, msg, errorCode, null);
    }

    public static Map<String, Object> failed(HttpServletRequest request, int code, String msg, String errorCode, Object data) {
        Map<String, Object> response = base(request, code, msg);
        response.put("errorCode", errorCode);
        response.put("data", data);
        return response;
    }

    private static Map<String, Object> base(HttpServletRequest request, int code, String msg) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("msg", msg);
        response.put("message", msg);
        response.put("traceId", TraceContext.getTraceId(request));
        response.put("path", request == null ? null : request.getRequestURI());
        response.put("timestamp", Instant.now().toEpochMilli());
        return response;
    }
}

