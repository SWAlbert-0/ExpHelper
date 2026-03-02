package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.auth.UserFieldValidator;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;
import fjnu.edu.notify.service.NotificationService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/notification")
public class NotificationCtrl {
    private static final Logger log = LoggerFactory.getLogger(NotificationCtrl.class);

    @Autowired
    NotificationService notificationService;
    @Autowired
    PlatMgrService platMgrService;

    @GetMapping("/profile")
    public Map<String, Object> getProfile(HttpServletRequest request) {
        UserInfo user = currentUser(request);
        if (user == null) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        UserNotifyProfile profile = notificationService.getProfile(user.getUserId(), user.getEmail());
        return ApiResponse.ok(request, profile);
    }

    @PutMapping("/profile")
    public Map<String, Object> saveProfile(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        UserInfo user = currentUser(request);
        if (user == null) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        UserNotifyProfile update = new UserNotifyProfile();
        update.setEmail(stringValue(payload.get("email")));
        update.setTimezone(stringValue(payload.get("timezone")));
        update.setQuietHoursStart(stringValue(payload.get("quietHoursStart")));
        update.setQuietHoursEnd(stringValue(payload.get("quietHoursEnd")));
        update.setEmailEnabled(boolValue(payload.get("emailEnabled")));
        update.setEventPlanDoneEnabled(boolValue(payload.get("eventPlanDoneEnabled")));
        update.setQuietHoursEnabled(boolValue(payload.get("quietHoursEnabled")));

        String emailMsg = UserFieldValidator.validateEmail(update.getEmail());
        if (emailMsg != null) {
            return ApiResponse.failed(request, 400, emailMsg, ErrorCode.USER_EMAIL_INVALID.code());
        }
        if (Boolean.TRUE.equals(update.getQuietHoursEnabled())) {
            if (!validHm(update.getQuietHoursStart()) || !validHm(update.getQuietHoursEnd())) {
                return ApiResponse.failed(request, 400, "静默时段格式必须为HH:mm", ErrorCode.INVALID_ARGUMENT.code());
            }
        }
        UserNotifyProfile saved = notificationService.saveProfile(user.getUserId(), update, user.getEmail());
        return ApiResponse.ok(request, saved, "保存成功");
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String planId,
                                    @RequestParam(required = false) String executionId,
                                    @RequestParam(required = false) String userId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false, defaultValue = "0") long fromTs,
                                    @RequestParam(required = false, defaultValue = "0") long toTs,
                                    @RequestParam(required = false, defaultValue = "1") int pageNum,
                                    @RequestParam(required = false, defaultValue = "20") int pageSize,
                                    HttpServletRequest request) {
        AuthUser auth = currentAuth(request);
        String authUserId = resolveAuthUserId(auth);
        if (!StringUtils.hasText(authUserId)) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        boolean admin = auth.getRole() != null && auth.getRole() == 1;
        String effectiveUserId = admin ? userId : authUserId;
        List<NotificationOutbox> items = notificationService.listOutbox(planId, executionId, effectiveUserId, status, fromTs, toTs, pageNum, pageSize);
        long total = notificationService.countOutbox(planId, executionId, effectiveUserId, status, fromTs, toTs);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("pageNum", Math.max(1, pageNum));
        data.put("pageSize", pageSize <= 0 ? 20 : Math.min(pageSize, 200));
        data.put("userScope", admin ? "all" : "self");
        return ApiResponse.ok(request, data);
    }

    @PostMapping("/resend")
    public Map<String, Object> resend(@RequestParam String notificationId, HttpServletRequest request) {
        AuthUser auth = currentAuth(request);
        String authUserId = resolveAuthUserId(auth);
        if (!StringUtils.hasText(authUserId)) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        NotificationOutbox target = notificationService.getOutboxById(notificationId);
        if (target == null) {
            return ApiResponse.failed(request, 404, "通知记录不存在", ErrorCode.INVALID_ARGUMENT.code());
        }
        boolean admin = auth.getRole() != null && auth.getRole() == 1;
        if (!admin && !authUserId.equals(target.getUserId())) {
            return ApiResponse.failed(request, 403, "无权限操作该通知记录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        NotificationOutbox created = notificationService.manualResend(notificationId);
        if (created == null) {
            return ApiResponse.failed(request, 500, "补发任务创建失败", ErrorCode.INTERNAL_ERROR.code());
        }
        log.info("traceId={} userId={} path={} notificationId={} createdId={}",
                TraceContext.getTraceId(request), authUserId, "/api/notification/resend", notificationId, created.getNotificationId());
        return ApiResponse.ok(request, created, "补发任务已创建");
    }

    @PostMapping("/resendByExecution")
    public Map<String, Object> resendByExecution(@RequestParam String planId,
                                                 @RequestParam String executionId,
                                                 HttpServletRequest request) {
        AuthUser auth = currentAuth(request);
        String authUserId = resolveAuthUserId(auth);
        if (!StringUtils.hasText(authUserId)) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        boolean admin = auth.getRole() != null && auth.getRole() == 1;
        String userScope = admin ? null : authUserId;
        int count = notificationService.manualResendByExecution(planId, executionId, userScope);
        Map<String, Object> data = new HashMap<>();
        data.put("createdCount", count);
        return ApiResponse.ok(request, data, "补发任务创建完成");
    }

    @PostMapping("/testSend")
    public Map<String, Object> testSend(HttpServletRequest request) {
        UserInfo user = currentUser(request);
        if (user == null) {
            return ApiResponse.failed(request, 401, "未登录", ErrorCode.AUTH_UNAUTHORIZED.code());
        }
        fjnu.edu.mail.MailSendResult result = notificationService.sendProfileTestMail(user.getUserId(), user.getEmail(), user.getUserName());
        if (result != null && result.isSent()) {
            Map<String, Object> data = new HashMap<>();
            data.put("reasonCode", result.getReasonCode());
            data.put("message", result.getMessage());
            return ApiResponse.ok(request, data, "测试邮件发送成功");
        }
        String reasonCode = result == null ? "MAIL_SEND_FAILED" : result.getReasonCode();
        String message = result == null ? "测试邮件发送失败" : result.getMessage();
        return ApiResponse.failed(request, 400, message, reasonCode);
    }

    private AuthUser currentAuth(HttpServletRequest request) {
        Object auth = request.getAttribute("authUser");
        if (auth instanceof AuthUser) {
            return (AuthUser) auth;
        }
        return null;
    }

    private UserInfo currentUser(HttpServletRequest request) {
        AuthUser auth = currentAuth(request);
        if (auth == null) {
            return null;
        }
        UserInfo user = null;
        if (StringUtils.hasText(auth.getUserId())) {
            user = platMgrService.getUserById(auth.getUserId());
        }
        if (user == null && StringUtils.hasText(auth.getUserName())) {
            user = platMgrService.getUserByName(auth.getUserName());
        }
        return user;
    }

    private String resolveAuthUserId(AuthUser auth) {
        if (auth == null) {
            return "";
        }
        if (StringUtils.hasText(auth.getUserId())) {
            return auth.getUserId().trim();
        }
        if (StringUtils.hasText(auth.getUserName())) {
            UserInfo user = platMgrService.getUserByName(auth.getUserName().trim());
            if (user != null && StringUtils.hasText(user.getUserId())) {
                return user.getUserId().trim();
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean boolValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private boolean validHm(String value) {
        if (!StringUtils.hasText(value) || value.length() != 5 || value.charAt(2) != ':') {
            return false;
        }
        try {
            int hour = Integer.parseInt(value.substring(0, 2));
            int minute = Integer.parseInt(value.substring(3, 5));
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
