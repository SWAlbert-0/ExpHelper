package fjnu.edu.notify.dao;

import fjnu.edu.notify.entity.NotificationConstants;
import fjnu.edu.notify.entity.NotificationOutbox;
import fjnu.edu.notify.entity.UserNotifyProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationDao {
    private static final String COL_PROFILE = "userNotifyProfile";
    private static final String COL_OUTBOX = "notificationOutbox";

    @Autowired
    MongoTemplate mongoTemplate;

    public void ensureIndexes(int retentionDays) {
        mongoTemplate.indexOps(COL_OUTBOX)
                .ensureIndex(new Index().on("bizKey", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(COL_OUTBOX)
                .ensureIndex(new Index().on("status", Sort.Direction.ASC).on("nextRetryAt", Sort.Direction.ASC));
        mongoTemplate.indexOps(COL_OUTBOX)
                .ensureIndex(new Index().on("planId", Sort.Direction.ASC).on("executionId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        mongoTemplate.indexOps(COL_OUTBOX)
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        if (retentionDays > 0) {
            long expireSeconds = retentionDays * 24L * 3600L;
            mongoTemplate.indexOps(COL_OUTBOX)
                    .ensureIndex(new Index().on("createdAt", Sort.Direction.ASC).expire(expireSeconds));
        }
    }

    public UserNotifyProfile getProfile(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return mongoTemplate.findById(userId.trim(), UserNotifyProfile.class, COL_PROFILE);
    }

    public UserNotifyProfile saveProfile(UserNotifyProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getUserId())) {
            return null;
        }
        profile.setUserId(profile.getUserId().trim());
        mongoTemplate.save(profile, COL_PROFILE);
        return profile;
    }

    public NotificationOutbox findByBizKey(String bizKey) {
        if (!StringUtils.hasText(bizKey)) {
            return null;
        }
        Query query = new Query(Criteria.where("bizKey").is(bizKey.trim()));
        return mongoTemplate.findOne(query, NotificationOutbox.class, COL_OUTBOX);
    }

    public NotificationOutbox insertOutbox(NotificationOutbox outbox) {
        if (outbox == null) {
            return null;
        }
        return mongoTemplate.insert(outbox, COL_OUTBOX);
    }

    public NotificationOutbox claimOneReady(long nowMs) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").in(Arrays.asList(NotificationConstants.STATUS_PENDING, NotificationConstants.STATUS_FAILED_RETRY)),
                Criteria.where("nextRetryAt").lte(nowMs)
        );
        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.asc("nextRetryAt"), Sort.Order.asc("createdAt")))
                .limit(1);
        Update update = new Update()
                .set("status", NotificationConstants.STATUS_SENDING)
                .set("updatedAt", nowMs);
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), NotificationOutbox.class, COL_OUTBOX);
    }

    public void markSent(String notificationId, long nowMs) {
        if (!StringUtils.hasText(notificationId)) {
            return;
        }
        Query query = new Query(Criteria.where("_id").is(notificationId));
        Update update = new Update()
                .set("status", NotificationConstants.STATUS_SENT)
                .set("updatedAt", nowMs)
                .set("lastErrorCode", "")
                .set("lastErrorMsg", "");
        mongoTemplate.updateFirst(query, update, NotificationOutbox.class, COL_OUTBOX);
    }

    public void markRetry(String notificationId, int retryCount, long nextRetryAt, String errorCode, String errorMsg, long nowMs) {
        if (!StringUtils.hasText(notificationId)) {
            return;
        }
        Query query = new Query(Criteria.where("_id").is(notificationId));
        Update update = new Update()
                .set("status", NotificationConstants.STATUS_FAILED_RETRY)
                .set("retryCount", retryCount)
                .set("nextRetryAt", nextRetryAt)
                .set("lastErrorCode", safe(errorCode))
                .set("lastErrorMsg", safe(errorMsg))
                .set("updatedAt", nowMs);
        mongoTemplate.updateFirst(query, update, NotificationOutbox.class, COL_OUTBOX);
    }

    public void markFinalFailed(String notificationId, int retryCount, String errorCode, String errorMsg, long nowMs) {
        if (!StringUtils.hasText(notificationId)) {
            return;
        }
        Query query = new Query(Criteria.where("_id").is(notificationId));
        Update update = new Update()
                .set("status", NotificationConstants.STATUS_FAILED_FINAL)
                .set("retryCount", retryCount)
                .set("lastErrorCode", safe(errorCode))
                .set("lastErrorMsg", safe(errorMsg))
                .set("updatedAt", nowMs);
        mongoTemplate.updateFirst(query, update, NotificationOutbox.class, COL_OUTBOX);
    }

    public NotificationOutbox getOutboxById(String notificationId) {
        if (!StringUtils.hasText(notificationId)) {
            return null;
        }
        return mongoTemplate.findById(notificationId.trim(), NotificationOutbox.class, COL_OUTBOX);
    }

    public List<NotificationOutbox> listOutbox(String planId, String executionId, String userId, String status,
                                               long fromTs, long toTs, int pageNum, int pageSize) {
        Query query = new Query();
        if (StringUtils.hasText(planId)) {
            query.addCriteria(Criteria.where("planId").is(planId.trim()));
        }
        if (StringUtils.hasText(executionId)) {
            query.addCriteria(Criteria.where("executionId").is(executionId.trim()));
        }
        if (StringUtils.hasText(userId)) {
            query.addCriteria(Criteria.where("userId").is(userId.trim()));
        }
        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status.trim()));
        }
        if (fromTs > 0 || toTs > 0) {
            Criteria tsCriteria = Criteria.where("createdAt");
            if (fromTs > 0) {
                tsCriteria = tsCriteria.gte(fromTs);
            }
            if (toTs > 0) {
                tsCriteria = tsCriteria.lte(toTs);
            }
            query.addCriteria(tsCriteria);
        }
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        query.skip((long) (safePageNum - 1) * safePageSize);
        query.limit(safePageSize);
        query.with(Sort.by(Sort.Order.desc("createdAt")));
        List<NotificationOutbox> list = mongoTemplate.find(query, NotificationOutbox.class, COL_OUTBOX);
        return list == null ? Collections.emptyList() : list;
    }

    public long countOutbox(String planId, String executionId, String userId, String status, long fromTs, long toTs) {
        Query query = new Query();
        if (StringUtils.hasText(planId)) {
            query.addCriteria(Criteria.where("planId").is(planId.trim()));
        }
        if (StringUtils.hasText(executionId)) {
            query.addCriteria(Criteria.where("executionId").is(executionId.trim()));
        }
        if (StringUtils.hasText(userId)) {
            query.addCriteria(Criteria.where("userId").is(userId.trim()));
        }
        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status.trim()));
        }
        if (fromTs > 0 || toTs > 0) {
            Criteria tsCriteria = Criteria.where("createdAt");
            if (fromTs > 0) {
                tsCriteria = tsCriteria.gte(fromTs);
            }
            if (toTs > 0) {
                tsCriteria = tsCriteria.lte(toTs);
            }
            query.addCriteria(tsCriteria);
        }
        return mongoTemplate.count(query, NotificationOutbox.class, COL_OUTBOX);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
