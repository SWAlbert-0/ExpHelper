package fjnu.edu.notify.entity;

public final class NotificationConstants {
    private NotificationConstants() {
    }

    public static final String CHANNEL_MAIL = "MAIL";
    public static final String EVENT_PLAN_DONE = "PLAN_DONE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED_RETRY = "FAILED_RETRY";
    public static final String STATUS_FAILED_FINAL = "FAILED_FINAL";

    public static final String SOURCE_AUTO = "AUTO";
    public static final String SOURCE_MANUAL_RESEND = "MANUAL_RESEND";

    public static final String CONTENT_MODE_TEXT_ONLY = "TEXT_ONLY";
    public static final String CONTENT_MODE_TEXT_HTML = "TEXT_HTML";
}
