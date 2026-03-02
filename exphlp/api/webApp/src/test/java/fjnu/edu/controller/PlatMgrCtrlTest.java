package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
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

        Map<String, Object> response = controller.addUser(user, adminRequest());

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

        Map<String, Object> response = controller.resetUserPassword(payload, adminRequest());

        assertEquals(400, response.get("code"));
        assertEquals("PASSWORD_LENGTH_INVALID", response.get("errorCode"));
        verify(platMgrService, never()).updateUserById(any(UserInfo.class));
    }

    @Test
    void addUserClearsBlankUserIdBeforeInsert() {
        UserInfo user = new UserInfo();
        user.setUserId("   ");
        user.setUserName("new-user");
        user.setPassword("123456");

        when(passwordService.encode("123456")).thenReturn("encoded");

        Map<String, Object> response = controller.addUser(user, adminRequest());

        assertEquals(200, response.get("code"));
        verify(platMgrService).addUser(argThat(saved ->
                saved != null
                        && saved.getUserId() == null
                        && "encoded".equals(saved.getPassword())
                        && "new-user".equals(saved.getUserName())
        ));
    }

    @Test
    void getUsersAllowsNonAdminReadOnlyAccess() {
        controller.getUsers(nonAdminRequest());
        verify(platMgrService, times(1)).getUsers();
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-admin", "admin", 1));
        return request;
    }

    private MockHttpServletRequest nonAdminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-user", "user", 0));
        return request;
    }
}
