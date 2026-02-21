package fjnu.edu.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TraceLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startAt = System.currentTimeMillis();
        TraceContext.ensureTraceId(request, response);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latency = System.currentTimeMillis() - startAt;
            AuthUser authUser = (AuthUser) request.getAttribute("authUser");
            String userId = authUser == null ? null : authUser.getUserId();
            String traceId = TraceContext.getTraceId(request);
            if (response.getStatus() >= 400) {
                log.warn("traceId={} userId={} method={} path={} status={} latencyMs={}",
                        traceId, userId, request.getMethod(), request.getRequestURI(), response.getStatus(), latency);
            } else {
                log.info("traceId={} userId={} method={} path={} status={} latencyMs={}",
                        traceId, userId, request.getMethod(), request.getRequestURI(), response.getStatus(), latency);
            }
        }
    }
}

