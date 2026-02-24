package fjnu.edu.notify.service;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;

import java.util.List;

public interface NotificationService {
    UserNotifyProfile getProfile(String userId, String fallbackEmail);

    UserNotifyProfile saveProfile(String userId, UserNotifyProfile update, String fallbackEmail);

    int enqueuePlanDoneNotifications(ExePlan exePlan, boolean success);

    List<NotificationOutbox> listOutbox(String planId, String executionId, String userId, String status,
                                        long fromTs, long toTs, int pageNum, int pageSize);

    long countOutbox(String planId, String executionId, String userId, String status, long fromTs, long toTs);

    NotificationOutbox getOutboxById(String notificationId);

    NotificationOutbox manualResend(String notificationId);

    int manualResendByExecution(String planId, String executionId, String userId);
}

