package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.JwtUtil;
import fjnu.edu.auth.LoginRequest;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private PlatMgrService platMgrService;
    private PasswordService passwordService;
    private JwtUtil jwtUtil;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        platMgrService = mock(PlatMgrService.class);
        passwordService = mock(PasswordService.class);
        jwtUtil = mock(JwtUtil.class);
        controller = new AuthController(platMgrService, passwordService, jwtUtil, "./target/test-avatar");
    }

    @Test
    void loginReturns401WhenCredentialInvalid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrong");
        when(platMgrService.getUserByName("admin")).thenReturn(null);

        Map<String, Object> response = controller.login(loginRequest, new MockHttpServletRequest());

        assertEquals(401, response.get("code"));
        assertEquals("AUTH_INVALID_CREDENTIALS", response.get("errorCode"));
    }

    @Test
    void updateProfileRejectsInvalidMobile() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-1", "admin", 1));
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("u-1");
        userInfo.setUserName("admin");
        when(platMgrService.getUserById("u-1")).thenReturn(userInfo);

        Map<String, Object> response = controller.updateProfile(Map.of(
                "username", "admin",
                "mobile", "123"
        ), request);

        assertEquals(400, response.get("code"));
        assertEquals("USER_MOBILE_INVALID", response.get("errorCode"));
    }
}

