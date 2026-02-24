package fjnu.edu.mail;

public interface MailProvider {
    MailSendResult sendMail(String to, String subject, String text);

    default MailSendResult sendMail(String to, String subject, String text, String html) {
        return sendMail(to, subject, text);
    }

    String providerName();
}
