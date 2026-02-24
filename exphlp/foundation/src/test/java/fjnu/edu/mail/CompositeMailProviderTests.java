package fjnu.edu.mail;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeMailProviderTests {

    @Test
    void shouldFallbackToTencentWhenSmtpConfigInvalid() {
        MailProvider smtp = new FixedProvider(MailSendResult.configInvalid("smtp invalid"));
        MailProvider tencent = new FixedProvider(MailSendResult.sent());
        CompositeMailProvider provider = new CompositeMailProvider(smtp, tencent);
        ReflectionTestUtils.setField(provider, "strategy", "smtp_primary_ses_fallback");
        ReflectionTestUtils.setField(provider, "tencentEnabled", true);

        MailSendResult result = provider.sendMail("to@example.com", "s", "c");

        assertTrue(result.isSent());
        assertEquals("MAIL_PROVIDER_FALLBACK_USED", result.getReasonCode());
    }

    @Test
    void shouldUseSmtpOnlyWhenConfigured() {
        MailProvider smtp = new FixedProvider(MailSendResult.sent());
        MailProvider tencent = new FixedProvider(MailSendResult.tencentSendFailed("tx failed"));
        CompositeMailProvider provider = new CompositeMailProvider(smtp, tencent);
        ReflectionTestUtils.setField(provider, "strategy", "smtp_only");
        ReflectionTestUtils.setField(provider, "tencentEnabled", true);

        MailSendResult result = provider.sendMail("to@example.com", "s", "c");

        assertTrue(result.isSent());
        assertEquals("MAIL_SENT", result.getReasonCode());
    }

    @Test
    void shouldReturnAllFailedWhenBothProviderFailed() {
        MailProvider smtp = new FixedProvider(MailSendResult.failed("smtp down"));
        MailProvider tencent = new FixedProvider(MailSendResult.tencentSendFailed("tx down"));
        CompositeMailProvider provider = new CompositeMailProvider(smtp, tencent);
        ReflectionTestUtils.setField(provider, "strategy", "smtp_primary_ses_fallback");
        ReflectionTestUtils.setField(provider, "tencentEnabled", true);

        MailSendResult result = provider.sendMail("to@example.com", "s", "c");

        assertEquals("MAIL_PROVIDER_ALL_FAILED", result.getReasonCode());
    }

    private static final class FixedProvider implements MailProvider {
        private final MailSendResult result;

        private FixedProvider(MailSendResult result) {
            this.result = result;
        }

        @Override
        public MailSendResult sendMail(String to, String subject, String text) {
            return result;
        }

        @Override
        public String providerName() {
            return "FIXED";
        }
    }
}
