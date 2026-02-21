package fjnu.edu.controller;

import fjnu.edu.auth.PasswordService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlatMgrCtrlTest {

    private PlatMgrCtrl controller;
    private PlatMgrService platMgrService;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        controller = new PlatMgrCtrl();
        platMgrService = mock(PlatMgrService.class);
        passwordService = mock(PasswordService.class);
        ReflectionTestUtils.setField(controller, "platMgrService", platMgrService);
        ReflectionTestUtils.setField(controller, "passwordService", passwordService);
    }

    @Test
    void addUserRejectsInvalidEmail() {
        UserInfo user = new UserInfo();
        user.setUserName("tester");
        user.setPassword("123456");
        user.setEmail("bad-email");

        Map<String, Object> response = controller.addUser(user, new MockHttpServletRequest());

        assertEquals(400, response.get("code"));
        assertEquals("USER_FIELD_INVALID", response.get("errorCode"));
        verifyNoInteractions(platMgrService);
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        UserInfo payload = new UserInfo();
        payload.setUserId("u-1");
        payload.setPassword("123");
        UserInfo current = new UserInfo();
        current.setUserId("u-1");
        when(platMgrService.getUserById("u-1")).thenReturn(current);

        Map<String, Object> response = controller.resetUserPassword(payload, new MockHttpServletRequest());

        assertEquals(400, response.get("code"));
        assertEquals("PASSWORD_LENGTH_INVALID", response.get("errorCode"));
        verify(platMgrService, never()).updateUserById(any(UserInfo.class));
    }
}

