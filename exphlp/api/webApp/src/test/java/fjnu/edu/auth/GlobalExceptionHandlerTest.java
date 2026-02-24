package fjnu.edu.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingRequestParamReturns400() throws Exception {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("planId", "String");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/AlgRltSaveController/getAlgSaveByAlgName");

        Map<String, Object> resp = handler.handleMissingParam(ex, request);

        assertEquals(400, resp.get("code"));
        assertEquals(ErrorCode.INVALID_ARGUMENT.code(), resp.get("errorCode"));
    }

    @Test
    void typeMismatchReturns400() throws Exception {
        Method method = Probe.class.getDeclaredMethod("probe", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "bad", Long.class, "planId", parameter, new IllegalArgumentException("bad type"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ExePlanController/getPlanLogs");

        Map<String, Object> resp = handler.handleTypeMismatch(ex, request);

        assertEquals(400, resp.get("code"));
        assertEquals(ErrorCode.INVALID_ARGUMENT.code(), resp.get("errorCode"));
    }

    static class Probe {
        @SuppressWarnings("unused")
        public void probe(Long value) {
        }
    }
}
