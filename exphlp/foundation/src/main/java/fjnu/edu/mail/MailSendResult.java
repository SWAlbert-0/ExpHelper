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

    public static MailSendResult notConfigured(String message) {
        return new MailSendResult(false, "MAIL_NOT_CONFIGURED", message);
    }

    public static MailSendResult failed(String message) {
        return new MailSendResult(false, "MAIL_SEND_FAILED", message);
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

