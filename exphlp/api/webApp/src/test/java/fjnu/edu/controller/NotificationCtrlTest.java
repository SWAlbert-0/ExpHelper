package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
import fjnu.edu.mail.MailSendResult;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;
import fjnu.edu.notify.service.NotificationService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class NotificationCtrlTest {
    private NotificationCtrl controller;
    private NotificationService notificationService;
    private PlatMgrService platMgrService;

    @BeforeEach
    void setUp() {
        controller = new NotificationCtrl();
        notificationService = mock(NotificationService.class);
        platMgrService = mock(PlatMgrService.class);
        ReflectionTestUtils.setField(controller, "notificationService", notificationService);
        ReflectionTestUtils.setField(controller, "platMgrService", platMgrService);
    }

    @Test
    void getProfileRequiresLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> response = controller.getProfile(request);
        assertEquals(401, response.get("code"));
    }

    @Test
    void getProfileReturnsData() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-1", "admin", 1));
        UserInfo user = new UserInfo();
        user.setUserId("u-1");
        user.setEmail("admin@example.com");
        when(platMgrService.getUserById("u-1")).thenReturn(user);
        UserNotifyProfile profile = new UserNotifyProfile();
        profile.setUserId("u-1");
        when(notificationService.getProfile("u-1", "admin@example.com")).thenReturn(profile);

        Map<String, Object> response = controller.getProfile(request);
        assertEquals(200, response.get("code"));
        assertNotNull(response.get("data"));
    }

    @Test
    void listUsesSelfScopeForNormalUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-2", "user", 0));
        when(notificationService.listOutbox(null, null, "u-2", null, 0, 0, 1, 20))
                .thenReturn(Collections.emptyList());
        when(notificationService.countOutbox(null, null, "u-2", null, 0, 0)).thenReturn(0L);
        Map<String, Object> response = controller.list(null, null, null, null, 0, 0, 1, 20, request);
        assertEquals(200, response.get("code"));
        verify(notificationService, times(1)).listOutbox(null, null, "u-2", null, 0, 0, 1, 20);
    }

    @Test
    void resendChecksOwnership() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-3", "user", 0));
        NotificationOutbox target = new NotificationOutbox();
        target.setNotificationId("n-1");
        target.setUserId("u-9");
        when(notificationService.getOutboxById("n-1")).thenReturn(target);
        Map<String, Object> response = controller.resend("n-1", request);
        assertEquals(403, response.get("code"));
    }

    @Test
    void testSendRequiresLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> response = controller.testSend(request);
        assertEquals(401, response.get("code"));
    }

    @Test
    void testSendReturnsSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-1", "admin", 1));
        UserInfo user = new UserInfo();
        user.setUserId("u-1");
        user.setUserName("admin");
        user.setEmail("admin@example.com");
        when(platMgrService.getUserById("u-1")).thenReturn(user);
        when(notificationService.sendProfileTestMail("u-1", "admin@example.com", "admin"))
                .thenReturn(MailSendResult.sent());

        Map<String, Object> response = controller.testSend(request);
        assertEquals(200, response.get("code"));
    }
}
