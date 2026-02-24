package fjnu.edu.auth;

import fjnu.edu.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("traceId={} path={} errorCode={} message={}",
                TraceContext.getTraceId(request),
                request == null ? null : request.getRequestURI(),
                ErrorCode.INVALID_ARGUMENT.code(),
                ex.getMessage());
        return ApiResponse.failed(request, 400, ex.getMessage(), ErrorCode.INVALID_ARGUMENT.code());
    }

    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("traceId={} path={} errorCode={} message={}",
                TraceContext.getTraceId(request),
                request == null ? null : request.getRequestURI(),
                ErrorCode.INTERNAL_ERROR.code(),
                ex.getMessage());
        return ApiResponse.failed(request, 500, ex.getMessage(), ErrorCode.INTERNAL_ERROR.code());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Map<String, Object> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "请求参数缺失: " + ex.getParameterName();
        log.warn("traceId={} path={} errorCode={} message={}",
                TraceContext.getTraceId(request),
                request == null ? null : request.getRequestURI(),
                ErrorCode.INVALID_ARGUMENT.code(),
                msg);
        return ApiResponse.failed(request, 400, msg, ErrorCode.INVALID_ARGUMENT.code());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "请求参数类型错误: " + ex.getName();
        log.warn("traceId={} path={} errorCode={} message={}",
                TraceContext.getTraceId(request),
                request == null ? null : request.getRequestURI(),
                ErrorCode.INVALID_ARGUMENT.code(),
                msg);
        return ApiResponse.failed(request, 400, msg, ErrorCode.INVALID_ARGUMENT.code());
    }

    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleAny(Exception ex, HttpServletRequest request) {
        log.error("traceId={} path={} errorCode={} message={}",
                TraceContext.getTraceId(request),
                request == null ? null : request.getRequestURI(),
                ErrorCode.INTERNAL_ERROR.code(),
                ex.getMessage(), ex);
        return ApiResponse.failed(request, 500, "系统内部错误", ErrorCode.INTERNAL_ERROR.code());
    }
}
