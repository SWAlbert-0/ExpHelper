package fjnu.edu.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiResponseTest {

    @Test
    void failedRequiresErrorCode() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ApiResponse.failed(new MockHttpServletRequest(), 500, "err", ""));
        assertEquals("errorCode must not be empty", ex.getMessage());
    }

    @Test
    void failedContainsErrorCodeWhenProvided() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/demo");
        request.setAttribute(TraceContext.ATTR_TRACE_ID, "t-1");

        Object errorCode = ApiResponse.failed(request, 500, "err", ErrorCode.INTERNAL_ERROR.code()).get("errorCode");

        assertEquals(ErrorCode.INTERNAL_ERROR.code(), errorCode);
    }
}

