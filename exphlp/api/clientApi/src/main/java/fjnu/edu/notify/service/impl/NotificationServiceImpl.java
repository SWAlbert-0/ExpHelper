package fjnu.edu.notify.service.impl;

import fjnu.edu.exePlanMgr.dao.ExePlanMgrDao;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.mail.EmailService;
import fjnu.edu.mail.MailSendResult;
import fjnu.edu.notify.dao.NotificationDao;
import fjnu.edu.notify.entity.NotificationConstants;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;
import fjnu.edu.notify.service.NotificationService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    NotificationDao notificationDao;
    @Autowired
    PlatMgrService platMgrService;
    @Autowired
    EmailService emailService;
    @Autowired
    ExePlanMgrDao exePlanMgrDao;

    @org.springframework.beans.factory.annotation.Value("${notify.dispatch.max-per-cycle:20}")
    private int maxPerCycle;
    @org.springframework.beans.factory.annotation.Value("${notify.retry.max:3}")
    private int retryMax;
    @org.springframework.beans.factory.annotation.Value("${notify.retention.days:90}")
    private int retentionDays;

    @PostConstruct
    public void initIndexes() {
        notificationDao.ensureIndexes(retentionDays);
    }

    @Scheduled(fixedDelayString = "${notify.dispatch.interval-ms:10000}")
    public void dispatchPending() {
        int safeMax = Math.max(1, Math.min(maxPerCycle, 100));
        for (int i = 0; i < safeMax; i++) {
            NotificationOutbox task = notificationDao.claimOneReady(System.currentTimeMillis());
            if (task == null) {
                break;
            }
            dispatchSingle(task);
        }
    }

    @Override
    public UserNotifyProfile getProfile(String userId, String fallbackEmail) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        UserNotifyProfile saved = notificationDao.getProfile(userId);
        return mergeWithDefaults(userId.trim(), saved, fallbackEmail);
    }

    @Override
    public UserNotifyProfile saveProfile(String userId, UserNotifyProfile update, String fallbackEmail) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        UserNotifyProfile base = getProfile(userId, fallbackEmail);
        if (base == null) {
            return null;
        }
        if (update != null) {
            if (update.getEmail() != null) {
                base.setEmail(update.getEmail().trim());
            }
            if (update.getEmailEnabled() != null) {
                base.setEmailEnabled(update.getEmailEnabled());
            }
            if (update.getEventPlanDoneEnabled() != null) {
                base.setEventPlanDoneEnabled(update.getEventPlanDoneEnabled());
            }
            if (update.getQuietHoursEnabled() != null) {
                base.setQuietHoursEnabled(update.getQuietHoursEnabled());
            }
            if (update.getQuietHoursStart() != null) {
                base.setQuietHoursStart(update.getQuietHoursStart().trim());
            }
            if (update.getQuietHoursEnd() != null) {
                base.setQuietHoursEnd(update.getQuietHoursEnd().trim());
            }
            if (update.getTimezone() != null) {
                base.setTimezone(update.getTimezone().trim());
            }
        }
        base.setUpdatedAt(System.currentTimeMillis());
        return notificationDao.saveProfile(base);
    }

    @Override
    public int enqueuePlanDoneNotifications(ExePlan exePlan, boolean success) {
        if (exePlan == null || !StringUtils.hasText(exePlan.getPlanId())) {
            return 0;
        }
        List<String> userIds = exePlan.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                    "未配置通知用户，跳过通知任务创建", "reasonCode=MAIL_NO_RECEIVERS");
            return 0;
        }
        int created = 0;
        for (String userId : userIds) {
            if (!StringUtils.hasText(userId)) {
                continue;
            }
            UserInfo user = platMgrService.getUserById(userId);
            if (user == null) {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "WARN", "MAIL_NOTIFY",
                        "邮件未发送：通知用户不存在", "userId=" + userId + ";reasonCode=MAIL_USER_NOT_FOUND");
                continue;
            }
            UserNotifyProfile profile = getProfile(userId, user.getEmail());
            if (profile == null || Boolean.FALSE.equals(profile.getEmailEnabled()) || Boolean.FALSE.equals(profile.getEventPlanDoneEnabled())) {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "用户未启用计划结束通知，跳过", "userId=" + userId + ";reasonCode=MAIL_USER_DISABLED");
                continue;
            }
            String targetEmail = profileEmail(profile, user.getEmail());
            if (!StringUtils.hasText(targetEmail)) {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "WARN", "MAIL_NOTIFY",
                        "邮件未发送：收件人邮箱为空", "userId=" + userId + ";reasonCode=MAIL_INVALID_RECEIVER");
                continue;
            }
            String bizKey = buildBizKey(exePlan.getPlanId(), exePlan.getExecutionId(), userId, NotificationConstants.EVENT_PLAN_DONE);
            if (notificationDao.findByBizKey(bizKey) != null) {
                continue;
            }
            NotificationOutbox task = new NotificationOutbox();
            long now = System.currentTimeMillis();
            task.setBizKey(bizKey);
            task.setPlanId(exePlan.getPlanId());
            task.setExecutionId(exePlan.getExecutionId());
            task.setUserId(userId);
            task.setChannel(NotificationConstants.CHANNEL_MAIL);
            task.setEventType(NotificationConstants.EVENT_PLAN_DONE);
            task.setStatus(NotificationConstants.STATUS_PENDING);
            task.setSource(NotificationConstants.SOURCE_AUTO);
            task.setToEmail(targetEmail.trim());
            task.setSubject(buildSubject(exePlan));
            task.setContent(buildContent(exePlan, success));
            task.setRetryCount(0);
            task.setLastErrorCode("");
            task.setLastErrorMsg("");
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            long nextTs = computeNextRetryAtWithQuietHours(profile, now);
            task.setNextRetryAt(nextTs);
            notificationDao.insertOutbox(task);
            created++;

            if (nextTs > now) {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "命中静默时段，通知已延迟投递",
                        "userId=" + userId + ";to=" + targetEmail + ";reasonCode=MAIL_QUIET_HOURS_DELAYED");
            } else {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "通知任务已创建，等待异步发送",
                        "userId=" + userId + ";to=" + targetEmail + ";reasonCode=MAIL_OUTBOX_CREATED");
            }
        }
        return created;
    }

    @Override
    public List<NotificationOutbox> listOutbox(String planId, String executionId, String userId, String status,
                                               long fromTs, long toTs, int pageNum, int pageSize) {
        return notificationDao.listOutbox(planId, executionId, userId, status, fromTs, toTs, pageNum, pageSize);
    }

    @Override
    public long countOutbox(String planId, String executionId, String userId, String status, long fromTs, long toTs) {
        return notificationDao.countOutbox(planId, executionId, userId, status, fromTs, toTs);
    }

    @Override
    public NotificationOutbox getOutboxById(String notificationId) {
        return notificationDao.getOutboxById(notificationId);
    }

    @Override
    public NotificationOutbox manualResend(String notificationId) {
        NotificationOutbox origin = notificationDao.getOutboxById(notificationId);
        if (origin == null) {
            return null;
        }
        NotificationOutbox copy = new NotificationOutbox();
        long now = System.currentTimeMillis();
        copy.setBizKey(origin.getBizKey() + ":manual:" + now + ":" + UUID.randomUUID().toString().replace("-", ""));
        copy.setPlanId(origin.getPlanId());
        copy.setExecutionId(origin.getExecutionId());
        copy.setUserId(origin.getUserId());
        copy.setChannel(origin.getChannel());
        copy.setEventType(origin.getEventType());
        copy.setStatus(NotificationConstants.STATUS_PENDING);
        copy.setSource(NotificationConstants.SOURCE_MANUAL_RESEND);
        copy.setToEmail(origin.getToEmail());
        copy.setSubject(origin.getSubject());
        copy.setContent(origin.getContent());
        copy.setRetryCount(0);
        copy.setNextRetryAt(now);
        copy.setLastErrorCode("");
        copy.setLastErrorMsg("");
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        NotificationOutbox inserted = notificationDao.insertOutbox(copy);
        appendPlanLog(copy.getPlanId(), copy.getExecutionId(), "INFO", "MAIL_NOTIFY",
                "已创建手动补发通知任务", "userId=" + copy.getUserId() + ";reasonCode=MAIL_MANUAL_RESEND_CREATED");
        return inserted;
    }

    @Override
    public int manualResendByExecution(String planId, String executionId, String userId) {
        List<NotificationOutbox> failed = notificationDao.listOutbox(planId, executionId, userId,
                NotificationConstants.STATUS_FAILED_FINAL, 0, 0, 1, 500);
        int count = 0;
        for (NotificationOutbox item : failed) {
            NotificationOutbox created = manualResend(item.getNotificationId());
            if (created != null) {
                count++;
            }
        }
        return count;
    }

    private void dispatchSingle(NotificationOutbox task) {
        long now = System.currentTimeMillis();
        String reasonCode = "";
        String reasonMsg = "";
        try {
            MailSendResult result = emailService.sendMail(task.getToEmail(), task.getSubject(), task.getContent());
            if (result != null && result.isSent()) {
                notificationDao.markSent(task.getNotificationId(), now);
                appendPlanLog(task.getPlanId(), task.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "邮件发送成功", "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode=MAIL_SENT");
                return;
            }
            reasonCode = result == null ? "MAIL_SEND_FAILED" : safe(result.getReasonCode());
            reasonMsg = result == null ? "邮件发送失败" : safe(result.getMessage());
        } catch (Exception ex) {
            reasonCode = "MAIL_SEND_EXCEPTION";
            reasonMsg = ex.getMessage() == null ? "邮件发送异常" : ex.getMessage();
        }

        int nextRetryCount = Math.max(0, task.getRetryCount()) + 1;
        if (isFinalFailure(reasonCode) || nextRetryCount > Math.max(1, retryMax)) {
            notificationDao.markFinalFailed(task.getNotificationId(), nextRetryCount, reasonCode, reasonMsg, now);
            appendPlanLog(task.getPlanId(), task.getExecutionId(), "WARN", "MAIL_NOTIFY",
                    "邮件未发送: " + reasonMsg,
                    "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode=" + reasonCode);
            return;
        }
        long delayMs = retryDelayMs(nextRetryCount);
        long nextRetryAt = now + delayMs;
        notificationDao.markRetry(task.getNotificationId(), nextRetryCount, nextRetryAt, reasonCode, reasonMsg, now);
        appendPlanLog(task.getPlanId(), task.getExecutionId(), "WARN", "MAIL_NOTIFY",
                "邮件发送失败，已加入重试队列: " + reasonMsg,
                "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode=" + reasonCode +
                        ";retry=" + nextRetryCount);
    }

    private boolean isFinalFailure(String reasonCode) {
        return "MAIL_NOT_CONFIGURED".equals(reasonCode)
                || "MAIL_INVALID_RECEIVER".equals(reasonCode)
                || "MAIL_USER_NOT_FOUND".equals(reasonCode);
    }

    private long retryDelayMs(int retryCount) {
        if (retryCount <= 1) {
            return 30_000L;
        }
        if (retryCount == 2) {
            return 120_000L;
        }
        return 600_000L;
    }

    private UserNotifyProfile mergeWithDefaults(String userId, UserNotifyProfile saved, String fallbackEmail) {
        UserNotifyProfile profile = saved == null ? new UserNotifyProfile() : saved;
        profile.setUserId(userId);
        if (!StringUtils.hasText(profile.getEmail()) && StringUtils.hasText(fallbackEmail)) {
            profile.setEmail(fallbackEmail.trim());
        }
        if (profile.getEmailEnabled() == null) {
            profile.setEmailEnabled(true);
        }
        if (profile.getEventPlanDoneEnabled() == null) {
            profile.setEventPlanDoneEnabled(true);
        }
        if (profile.getQuietHoursEnabled() == null) {
            profile.setQuietHoursEnabled(false);
        }
        if (!StringUtils.hasText(profile.getQuietHoursStart())) {
            profile.setQuietHoursStart("23:00");
        }
        if (!StringUtils.hasText(profile.getQuietHoursEnd())) {
            profile.setQuietHoursEnd("08:00");
        }
        if (!StringUtils.hasText(profile.getTimezone())) {
            profile.setTimezone("Asia/Shanghai");
        }
        if (profile.getUpdatedAt() <= 0) {
            profile.setUpdatedAt(System.currentTimeMillis());
        }
        return profile;
    }

    private String profileEmail(UserNotifyProfile profile, String fallbackEmail) {
        if (profile != null && StringUtils.hasText(profile.getEmail())) {
            return profile.getEmail().trim();
        }
        if (StringUtils.hasText(fallbackEmail)) {
            return fallbackEmail.trim();
        }
        return "";
    }

    private String buildSubject(ExePlan exePlan) {
        return "执行计划通知: " + exePlan.getPlanName();
    }

    private String buildContent(ExePlan exePlan, boolean success) {
        String status = success ? "正常结束" : "异常结束";
        StringBuilder sb = new StringBuilder();
        sb.append("计划[").append(exePlan.getPlanName()).append("]执行").append(status).append("\n");
        sb.append("planId=").append(exePlan.getPlanId()).append("\n");
        sb.append("executionId=").append(exePlan.getExecutionId()).append("\n");
        sb.append("结束时间=").append(System.currentTimeMillis()).append("\n");
        if (StringUtils.hasText(exePlan.getLastError())) {
            sb.append("异常原因=").append(exePlan.getLastError()).append("\n");
        }
        sb.append("请登录系统查看执行日志与执行结果。");
        return sb.toString();
    }

    private String buildBizKey(String planId, String executionId, String userId, String eventType) {
        return safe(planId) + ":" + safe(executionId) + ":" + safe(userId) + ":" + safe(eventType);
    }

    private long computeNextRetryAtWithQuietHours(UserNotifyProfile profile, long nowMs) {
        if (profile == null || Boolean.FALSE.equals(profile.getQuietHoursEnabled())) {
            return nowMs;
        }
        try {
            ZoneId zoneId = ZoneId.of(StringUtils.hasText(profile.getTimezone()) ? profile.getTimezone().trim() : "Asia/Shanghai");
            LocalTime start = LocalTime.parse(profile.getQuietHoursStart());
            LocalTime end = LocalTime.parse(profile.getQuietHoursEnd());
            ZonedDateTime now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMs), zoneId);
            LocalTime nowTime = now.toLocalTime();
            if (!inQuietHours(nowTime, start, end)) {
                return nowMs;
            }
            ZonedDateTime release;
            if (start.equals(end)) {
                release = now.plusDays(1).with(LocalTime.MIN);
            } else if (start.isBefore(end)) {
                LocalDate base = now.toLocalDate();
                release = ZonedDateTime.of(base, end, zoneId);
            } else {
                LocalDate base = now.toLocalDate();
                if (nowTime.isBefore(end)) {
                    release = ZonedDateTime.of(base, end, zoneId);
                } else {
                    release = ZonedDateTime.of(base.plusDays(1), end, zoneId);
                }
            }
            return release.toInstant().toEpochMilli();
        } catch (RuntimeException ex) {
            return nowMs;
        }
    }

    private boolean inQuietHours(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private void appendPlanLog(String planId, String executionId, String level, String stage, String message, String details) {
        if (!StringUtils.hasText(planId)) {
            return;
        }
        ExePlanLog logItem = new ExePlanLog();
        logItem.setPlanId(planId);
        logItem.setExecutionId(executionId);
        logItem.setLevel(level);
        logItem.setStage(stage);
        logItem.setMessage(message);
        logItem.setDetails(details);
        logItem.setTs(System.currentTimeMillis());
        exePlanMgrDao.appendPlanLog(logItem);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
