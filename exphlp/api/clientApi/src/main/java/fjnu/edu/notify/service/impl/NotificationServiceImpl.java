package fjnu.edu.notify.service.impl;

import fjnu.edu.exePlanMgr.dao.ExePlanMgrDao;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.entity.ExeResultAggregate;
import fjnu.edu.entity.ExeResultDetail;
import fjnu.edu.entity.ExeResultRunDetail;
import fjnu.edu.mail.MailProvider;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    NotificationDao notificationDao;
    @Autowired
    PlatMgrService platMgrService;
    @Autowired
    @Qualifier("mailProvider")
    MailProvider mailProvider;
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
    public int enqueuePlanDoneNotifications(ExePlan exePlan, boolean success, Map<String, ExeResultDetail> resultDetailsByAlgId) {
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
            String timezone = resolveTimezone(profile);
            String renderedAtText = formatEpochMillis(now, timezone);
            String content = buildTextContent(exePlan, success, resultDetailsByAlgId, now, timezone);
            String contentHtml = buildHtmlContent(exePlan, success, resultDetailsByAlgId, now, timezone);
            task.setContent(content);
            task.setContentHtml(contentHtml);
            task.setContentMode(StringUtils.hasText(contentHtml)
                    ? NotificationConstants.CONTENT_MODE_TEXT_HTML
                    : NotificationConstants.CONTENT_MODE_TEXT_ONLY);
            task.setRenderedAt(now);
            task.setRenderedAtText(renderedAtText);
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
                        "userId=" + userId + ";to=" + targetEmail + ";reasonCode=MAIL_QUIET_HOURS_DELAYED"
                                + ";contentMode=" + safe(task.getContentMode())
                                + ";renderedAt=" + safe(task.getRenderedAtText())
                                + ";timezone=" + timezone);
            } else {
                appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "通知任务已创建，等待异步发送",
                        "userId=" + userId + ";to=" + targetEmail + ";reasonCode=MAIL_OUTBOX_CREATED"
                                + ";contentMode=" + safe(task.getContentMode())
                                + ";renderedAt=" + safe(task.getRenderedAtText())
                                + ";timezone=" + timezone);
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
        copy.setContentHtml(origin.getContentHtml());
        copy.setContentMode(origin.getContentMode());
        copy.setRenderedAt(origin.getRenderedAt());
        copy.setRenderedAtText(origin.getRenderedAtText());
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

    @Override
    public MailSendResult sendProfileTestMail(String userId, String fallbackEmail, String userName) {
        if (!StringUtils.hasText(userId)) {
            return MailSendResult.receiverInvalid("用户未登录");
        }
        UserNotifyProfile profile = getProfile(userId, fallbackEmail);
        String targetEmail = profileEmail(profile, fallbackEmail);
        if (!StringUtils.hasText(targetEmail)) {
            return MailSendResult.receiverInvalid("未配置通知邮箱，请先在通知设置中填写邮箱");
        }
        String safeName = StringUtils.hasText(userName) ? userName.trim() : "用户";
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now());
        String subject = "实验助手通知测试";
        String content = "您好，" + safeName + "：\n"
                + "这是一封来自实验助手的测试邮件。\n"
                + "接收邮箱：" + targetEmail + "\n"
                + "发送时间：" + now + "\n"
                + "若收到此邮件，说明通知通道配置可用。";
        return mailProvider.sendMail(targetEmail, subject, content);
    }

    private void dispatchSingle(NotificationOutbox task) {
        long now = System.currentTimeMillis();
        String reasonCode = "";
        String reasonMsg = "";
        try {
            MailSendResult result = mailProvider.sendMail(task.getToEmail(), task.getSubject(), task.getContent(), task.getContentHtml());
            if (result != null && result.isSent()) {
                notificationDao.markSent(task.getNotificationId(), now);
                String sentReasonCode = safe(result.getReasonCode());
                appendPlanLog(task.getPlanId(), task.getExecutionId(), "INFO", "MAIL_NOTIFY",
                        "邮件发送成功", "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode="
                                + (StringUtils.hasText(sentReasonCode) ? sentReasonCode : "MAIL_SENT")
                                + ";" + buildSendMeta(task));
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
                    "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode=" + reasonCode
                            + ";" + buildSendMeta(task));
            return;
        }
        long delayMs = retryDelayMs(nextRetryCount);
        long nextRetryAt = now + delayMs;
        notificationDao.markRetry(task.getNotificationId(), nextRetryCount, nextRetryAt, reasonCode, reasonMsg, now);
        appendPlanLog(task.getPlanId(), task.getExecutionId(), "WARN", "MAIL_NOTIFY",
                "邮件发送失败，已加入重试队列: " + reasonMsg,
                "userId=" + task.getUserId() + ";to=" + task.getToEmail() + ";reasonCode=" + reasonCode +
                        ";retry=" + nextRetryCount + ";" + buildSendMeta(task));
    }

    private boolean isFinalFailure(String reasonCode) {
        return "MAIL_NOT_CONFIGURED".equals(reasonCode)
                || "MAIL_INVALID_RECEIVER".equals(reasonCode)
                || "MAIL_USER_NOT_FOUND".equals(reasonCode)
                || "MAIL_CONFIG_INVALID".equals(reasonCode)
                || "MAIL_FROM_EMPTY".equals(reasonCode)
                || "MAIL_FROM_INVALID".equals(reasonCode)
                || "MAIL_TENCENT_CONFIG_INVALID".equals(reasonCode)
                || "MAIL_PROVIDER_ALL_FAILED".equals(reasonCode);
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

    private String buildTextContent(ExePlan exePlan, boolean success, Map<String, ExeResultDetail> resultDetailsByAlgId,
                                    long finishedAt, String timezone) {
        String status = success ? "正常结束" : "异常结束";
        StringBuilder sb = new StringBuilder();
        sb.append("计划[").append(exePlan.getPlanName()).append("]执行").append(status).append("\n");
        sb.append("planId=").append(exePlan.getPlanId()).append("\n");
        sb.append("executionId=").append(exePlan.getExecutionId()).append("\n");
        sb.append("结束时间=").append(formatEpochMillis(finishedAt, timezone)).append("\n");
        if (StringUtils.hasText(exePlan.getLastError())) {
            sb.append("异常原因=").append(exePlan.getLastError()).append("\n");
        }
        appendMetricText(sb, exePlan, resultDetailsByAlgId);
        sb.append("请登录系统查看执行日志与执行结果。");
        return sb.toString();
    }

    private String buildHtmlContent(ExePlan exePlan, boolean success, Map<String, ExeResultDetail> resultDetailsByAlgId,
                                    long finishedAt, String timezone) {
        String status = success ? "正常结束" : "异常结束";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial,\\'Microsoft YaHei\\',sans-serif;font-size:14px;color:#303133;'>");
        sb.append("<h3 style='margin-bottom:8px;'>执行计划通知</h3>");
        sb.append("<p style='margin:4px 0;'>计划名称：").append(escapeHtml(exePlan.getPlanName())).append("</p>");
        sb.append("<p style='margin:4px 0;'>状态：").append(escapeHtml(status)).append("</p>");
        sb.append("<p style='margin:4px 0;'>planId：").append(escapeHtml(exePlan.getPlanId())).append("</p>");
        sb.append("<p style='margin:4px 0;'>executionId：").append(escapeHtml(exePlan.getExecutionId())).append("</p>");
        sb.append("<p style='margin:4px 0;'>结束时间：").append(escapeHtml(formatEpochMillis(finishedAt, timezone))).append("</p>");
        if (StringUtils.hasText(exePlan.getLastError())) {
            sb.append("<p style='margin:4px 0;color:#F56C6C;'>异常原因：").append(escapeHtml(exePlan.getLastError())).append("</p>");
        }
        appendMetricHtml(sb, exePlan, resultDetailsByAlgId);
        sb.append("<p style='margin-top:12px;color:#909399;'>请登录系统查看完整执行日志与执行结果。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendMetricText(StringBuilder sb, ExePlan exePlan, Map<String, ExeResultDetail> resultDetailsByAlgId) {
        if (exePlan == null || exePlan.getAlgRunInfos() == null || exePlan.getAlgRunInfos().isEmpty()) {
            return;
        }
        sb.append("指标汇总:\n");
        for (fjnu.edu.exePlanMgr.entity.AlgRunInfo info : exePlan.getAlgRunInfos()) {
            if (info == null) {
                continue;
            }
            ExeResultDetail detail = findDetail(resultDetailsByAlgId, info.getAlgId());
            sb.append("- 算法=").append(safe(info.getAlgName())).append(" (algId=").append(safe(info.getAlgId())).append(")\n");
            if (detail == null) {
                sb.append("  状态=MISSING, 原因=RESULT_NOT_FOUND\n");
                continue;
            }
            ExeResultAggregate agg = detail.getAggregate();
            sb.append("  状态=").append(safe(detail.getStatus()))
                    .append(", 原因=").append(safe(detail.getReasonCode()))
                    .append(", runCount=").append(agg == null ? 0 : safeInt(agg.getRunCount()))
                    .append(", Runtime(ms)=").append(formatMetric(agg == null ? null : agg.getRuntimeMsMean()))
                    .append(", HV=").append(formatMetric(agg == null ? null : agg.getHvMean()))
                    .append(", IGD+=").append(formatMetric(agg == null ? null : agg.getIgdPlusMean()))
                    .append(", GD=").append(formatMetric(agg == null ? null : agg.getGdMean()))
                    .append(", Coverage=").append(formatMetric(agg == null ? null : agg.getCoverageMean()))
                    .append(", Spread=").append(formatMetric(agg == null ? null : agg.getSpreadDeltaMean()))
                    .append(", Spacing=").append(formatMetric(agg == null ? null : agg.getSpacingMean()))
                    .append("\n");
        }
    }

    private void appendMetricHtml(StringBuilder sb, ExePlan exePlan, Map<String, ExeResultDetail> resultDetailsByAlgId) {
        if (exePlan == null || exePlan.getAlgRunInfos() == null || exePlan.getAlgRunInfos().isEmpty()) {
            return;
        }
        for (fjnu.edu.exePlanMgr.entity.AlgRunInfo info : exePlan.getAlgRunInfos()) {
            if (info == null) {
                continue;
            }
            ExeResultDetail detail = findDetail(resultDetailsByAlgId, info.getAlgId());
            sb.append("<h4 style='margin:16px 0 8px;'>算法：").append(escapeHtml(info.getAlgName()))
                    .append("（").append(escapeHtml(info.getAlgId())).append("）</h4>");
            sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;margin-bottom:8px;'>");
            sb.append("<tr style='background:#F5F7FA;'><th>状态</th><th>原因</th><th>运行条数</th><th>Runtime(ms)</th><th>HV</th><th>IGD+</th><th>GD</th><th>Coverage</th><th>Spread(Δ)</th><th>Spacing</th><th>指标版本</th></tr>");
            if (detail == null) {
                sb.append("<tr><td>MISSING</td><td>RESULT_NOT_FOUND</td><td>0</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td></tr>");
            } else {
                ExeResultAggregate agg = detail.getAggregate();
                sb.append("<tr>")
                        .append("<td>").append(escapeHtml(detail.getStatus())).append("</td>")
                        .append("<td>").append(escapeHtml(detail.getReasonCode())).append("</td>")
                        .append("<td>").append(agg == null ? "0" : String.valueOf(safeInt(agg.getRunCount()))).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getRuntimeMsMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getHvMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getIgdPlusMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getGdMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getCoverageMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getSpreadDeltaMean())).append("</td>")
                        .append("<td>").append(formatMetric(agg == null ? null : agg.getSpacingMean())).append("</td>")
                        .append("<td>").append(escapeHtml(detail.getMetricVersion())).append("</td>")
                        .append("</tr>");
            }
            sb.append("</table>");

            sb.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>");
            sb.append("<tr style='background:#F5F7FA;'><th>run</th><th>问题实例</th><th>Runtime(ms)</th><th>Pareto点数</th><th>HV</th><th>IGD+</th><th>GD</th><th>Coverage</th><th>Spread(Δ)</th><th>Spacing</th><th>指标状态</th><th>原因码</th></tr>");
            if (detail == null || detail.getRuns() == null || detail.getRuns().isEmpty()) {
                sb.append("<tr><td colspan='12' style='text-align:center;'>暂无明细</td></tr>");
            } else {
                int total = detail.getRuns().size();
                int limit = Math.min(10, total);
                for (int i = 0; i < limit; i++) {
                    ExeResultRunDetail run = detail.getRuns().get(i);
                    sb.append("<tr>")
                            .append("<td>").append(run == null || run.getRunIndex() == null ? String.valueOf(i + 1) : String.valueOf(run.getRunIndex())).append("</td>")
                            .append("<td>").append(escapeHtml(run == null ? "" : run.getProbInstName())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getRuntimeMs())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getParetoSize())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getHv())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getIgdPlus())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getGd())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getCoverage())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getSpreadDelta())).append("</td>")
                            .append("<td>").append(formatMetric(run == null ? null : run.getSpacing())).append("</td>")
                            .append("<td>").append(escapeHtml(run == null ? "" : run.getMetricStatus())).append("</td>")
                            .append("<td>").append(escapeHtml(run == null ? "" : run.getReasonCode())).append("</td>")
                            .append("</tr>");
                }
                if (total > limit) {
                    sb.append("<tr><td colspan='12' style='text-align:right;color:#909399;'>共").append(total).append("条，邮件仅展示前").append(limit).append("条</td></tr>");
                }
            }
            sb.append("</table>");
        }
    }

    private ExeResultDetail findDetail(Map<String, ExeResultDetail> resultDetailsByAlgId, String algId) {
        if (resultDetailsByAlgId == null || !StringUtils.hasText(algId)) {
            return null;
        }
        return resultDetailsByAlgId.get(algId);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatMetric(Number number) {
        if (number == null) {
            return "-";
        }
        return String.format(Locale.US, "%.6f", number.doubleValue());
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String buildSendMeta(NotificationOutbox task) {
        int textLength = task == null || task.getContent() == null ? 0 : task.getContent().length();
        int htmlLength = task == null || task.getContentHtml() == null ? 0 : task.getContentHtml().length();
        return "contentMode=" + safe(task == null ? "" : task.getContentMode())
                + ";textLength=" + textLength
                + ";htmlLength=" + htmlLength
                + ";renderedAt=" + safe(task == null ? "" : task.getRenderedAtText())
                + ";executionId=" + safe(task == null ? "" : task.getExecutionId());
    }

    private String resolveTimezone(UserNotifyProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getTimezone())) {
            return profile.getTimezone().trim();
        }
        return DEFAULT_TIMEZONE;
    }

    private String formatEpochMillis(long ts, String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(StringUtils.hasText(timezone) ? timezone.trim() : DEFAULT_TIMEZONE);
            return DT_FMT.format(Instant.ofEpochMilli(ts).atZone(zoneId));
        } catch (RuntimeException ex) {
            return DT_FMT.format(Instant.ofEpochMilli(ts).atZone(ZoneId.of(DEFAULT_TIMEZONE)));
        }
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
