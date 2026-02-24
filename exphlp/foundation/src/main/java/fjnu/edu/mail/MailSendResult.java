package fjnu.edu.mail;

public class MailSendResult {
    private final boolean sent;
    private final String reasonCode;
    private final String message;

    private MailSendResult(boolean sent, String reasonCode, String message) {
        this.sent = sent;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public static MailSendResult sent() {
        return new MailSendResult(true, "MAIL_SENT", "邮件发送成功");
    }

    public static MailSendResult fallbackUsed(String message) {
        return new MailSendResult(true, "MAIL_PROVIDER_FALLBACK_USED", message);
    }

    public static MailSendResult notConfigured(String message) {
        return new MailSendResult(false, "MAIL_NOT_CONFIGURED", message);
    }

    public static MailSendResult failed(String message) {
        return new MailSendResult(false, "MAIL_SEND_FAILED", message);
    }

    public static MailSendResult configInvalid(String message) {
        return new MailSendResult(false, "MAIL_CONFIG_INVALID", message);
    }

    public static MailSendResult fromEmpty(String message) {
        return new MailSendResult(false, "MAIL_FROM_EMPTY", message);
    }

    public static MailSendResult fromInvalid(String message) {
        return new MailSendResult(false, "MAIL_FROM_INVALID", message);
    }

    public static MailSendResult receiverInvalid(String message) {
        return new MailSendResult(false, "MAIL_INVALID_RECEIVER", message);
    }

    public static MailSendResult tencentConfigInvalid(String message) {
        return new MailSendResult(false, "MAIL_TENCENT_CONFIG_INVALID", message);
    }

    public static MailSendResult tencentSendFailed(String message) {
        return new MailSendResult(false, "MAIL_TENCENT_SEND_FAILED", message);
    }

    public static MailSendResult allProviderFailed(String message) {
        return new MailSendResult(false, "MAIL_PROVIDER_ALL_FAILED", message);
    }

    public boolean isSent() {
        return sent;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getMessage() {
        return message;
    }
}
