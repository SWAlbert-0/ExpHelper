package fjnu.edu.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailServiceTests {

    @Test
    void shouldReturnNotConfiguredWhenMailSenderMissing() {
        EmailService service = new EmailService();

        MailSendResult result = service.sendMail("to@example.com", "subject", "body");

        assertFalse(result.isSent());
        assertEquals("MAIL_NOT_CONFIGURED", result.getReasonCode());
    }

    @Test
    void shouldReturnConfigInvalidWhenHostOrPortMissing() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "javaMailSender", org.mockito.Mockito.mock(JavaMailSender.class));
        ReflectionTestUtils.setField(service, "host", "");
        ReflectionTestUtils.setField(service, "port", "");

        MailSendResult result = service.sendMail("to@example.com", "subject", "body");

        assertFalse(result.isSent());
        assertEquals("MAIL_CONFIG_INVALID", result.getReasonCode());
    }

    @Test
    void shouldReturnFromEmptyWhenUsernameAndFromBothEmpty() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "javaMailSender", org.mockito.Mockito.mock(JavaMailSender.class));
        ReflectionTestUtils.setField(service, "host", "smtp.qq.com");
        ReflectionTestUtils.setField(service, "port", "465");
        ReflectionTestUtils.setField(service, "configuredFrom", "");
        ReflectionTestUtils.setField(service, "username", "");

        MailSendResult result = service.sendMail("to@example.com", "subject", "body");

        assertFalse(result.isSent());
        assertEquals("MAIL_FROM_EMPTY", result.getReasonCode());
    }

    @Test
    void shouldReturnFromInvalidWhenFromAddressMalformed() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "javaMailSender", org.mockito.Mockito.mock(JavaMailSender.class));
        ReflectionTestUtils.setField(service, "host", "smtp.qq.com");
        ReflectionTestUtils.setField(service, "port", "465");
        ReflectionTestUtils.setField(service, "configuredFrom", "bad-from");

        MailSendResult result = service.sendMail("to@example.com", "subject", "body");

        assertFalse(result.isSent());
        assertEquals("MAIL_FROM_INVALID", result.getReasonCode());
    }

    @Test
    void shouldReturnReceiverInvalidWhenReceiverMalformed() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "javaMailSender", org.mockito.Mockito.mock(JavaMailSender.class));
        ReflectionTestUtils.setField(service, "host", "smtp.qq.com");
        ReflectionTestUtils.setField(service, "port", "465");
        ReflectionTestUtils.setField(service, "configuredFrom", "from@example.com");

        MailSendResult result = service.sendMail("bad-to", "subject", "body");

        assertFalse(result.isSent());
        assertEquals("MAIL_INVALID_RECEIVER", result.getReasonCode());
    }
}
