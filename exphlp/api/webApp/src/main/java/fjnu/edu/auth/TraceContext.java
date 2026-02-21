package fjnu.edu.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public final class TraceContext {
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String ATTR_TRACE_ID = "traceId";

    private TraceContext() {
    }

    public static String ensureTraceId(HttpServletRequest request, HttpServletResponse response) {
        Object attr = request.getAttribute(ATTR_TRACE_ID);
        if (attr instanceof String) {
            String traceId = (String) attr;
            if (!traceId.trim().isEmpty()) {
                if (response != null) {
                    response.setHeader(HEADER_TRACE_ID, traceId);
                }
                return traceId;
            }
        }
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        request.setAttribute(ATTR_TRACE_ID, traceId);
        if (response != null) {
            response.setHeader(HEADER_TRACE_ID, traceId);
        }
        return traceId;
    }

    public static String getTraceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(ATTR_TRACE_ID);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }
}

