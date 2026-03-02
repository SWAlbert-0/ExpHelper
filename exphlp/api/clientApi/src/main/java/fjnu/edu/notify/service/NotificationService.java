package fjnu.edu.notify.service;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.entity.ExeResultDetail;
import fjnu.edu.mail.MailSendResult;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    UserNotifyProfile getProfile(String userId, String fallbackEmail);

    UserNotifyProfile saveProfile(String userId, UserNotifyProfile update, String fallbackEmail);

    int enqueuePlanDoneNotifications(ExePlan exePlan, boolean success, Map<String, ExeResultDetail> resultDetailsByAlgId);

    int enqueuePlanProgressNotifications(ExePlan exePlan, String progressMessage);

    int enqueuePlanExceptionNotifications(ExePlan exePlan, String errorMessage);

    List<NotificationOutbox> listOutbox(String planId, String executionId, String userId, String status,
                                        long fromTs, long toTs, int pageNum, int pageSize);

    long countOutbox(String planId, String executionId, String userId, String status, long fromTs, long toTs);

    NotificationOutbox getOutboxById(String notificationId);

    NotificationOutbox manualResend(String notificationId);

    int manualResendByExecution(String planId, String executionId, String userId);

    MailSendResult sendProfileTestMail(String userId, String fallbackEmail, String userName);
}
