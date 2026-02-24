package fjnu.edu.notify.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class NotificationOutbox {
    @Id
    private String notificationId;
    private String bizKey;
    private String planId;
    private String executionId;
    private String userId;
    private String channel;
    private String eventType;
    private String status;
    private String source;

    private String toEmail;
    private String subject;
    private String content;
    private String contentHtml;
    private String contentMode;
    private long renderedAt;
    private String renderedAtText;

    private int retryCount;
    private long nextRetryAt;
    private String lastErrorCode;
    private String lastErrorMsg;

    private long createdAt;
    private long updatedAt;
}
