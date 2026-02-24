package fjnu.edu.mail;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("mailProvider")
public class CompositeMailProvider implements MailProvider {
    private final MailProvider smtpMailProvider;
    private final MailProvider tencentSesMailProvider;

    @Value("${notify.mail.provider.strategy:smtp_primary_ses_fallback}")
    private String strategy;
    @Value("${notify.mail.tencent.enabled:false}")
    private boolean tencentEnabled;

    public CompositeMailProvider(@Qualifier("smtpMailProvider") MailProvider smtpMailProvider,
                                 @Qualifier("tencentSesMailProvider") MailProvider tencentSesMailProvider) {
        this.smtpMailProvider = smtpMailProvider;
        this.tencentSesMailProvider = tencentSesMailProvider;
    }

    @Override
    public MailSendResult sendMail(String to, String subject, String text) {
        return sendMail(to, subject, text, null);
    }

    @Override
    public MailSendResult sendMail(String to, String subject, String text, String html) {
        String mode = safe(strategy);
        if ("tencent_only".equalsIgnoreCase(mode)) {
            return sendByTencent(to, subject, text, html);
        }
        if ("smtp_only".equalsIgnoreCase(mode)) {
            return smtpMailProvider.sendMail(to, subject, text, html);
        }

        MailSendResult smtpResult = smtpMailProvider.sendMail(to, subject, text, html);
        if (smtpResult != null && smtpResult.isSent()) {
            return smtpResult;
        }
        if (!shouldFallback(smtpResult) || !tencentEnabled) {
            return smtpResult == null ? MailSendResult.failed("SMTP发送失败") : smtpResult;
        }

        MailSendResult tencentResult = sendByTencent(to, subject, text, html);
        if (tencentResult != null && tencentResult.isSent()) {
            return MailSendResult.fallbackUsed("SMTP失败后已切换腾讯SES发送成功");
        }
        String smtpMsg = smtpResult == null ? "SMTP发送失败" : safe(smtpResult.getMessage());
        String txMsg = tencentResult == null ? "腾讯SES发送失败" : safe(tencentResult.getMessage());
        return MailSendResult.allProviderFailed("SMTP失败(" + smtpMsg + "), 腾讯SES失败(" + txMsg + ")");
    }

    @Override
    public String providerName() {
        return "COMPOSITE";
    }

    private MailSendResult sendByTencent(String to, String subject, String text, String html) {
        if (!tencentEnabled) {
            return MailSendResult.tencentConfigInvalid("腾讯SES未启用，请设置notify.mail.tencent.enabled=true");
        }
        return tencentSesMailProvider.sendMail(to, subject, text, html);
    }

    private boolean shouldFallback(MailSendResult smtpResult) {
        if (smtpResult == null) {
            return true;
        }
        String code = safe(smtpResult.getReasonCode());
        return "MAIL_NOT_CONFIGURED".equals(code)
                || "MAIL_CONFIG_INVALID".equals(code)
                || "MAIL_FROM_EMPTY".equals(code)
                || "MAIL_FROM_INVALID".equals(code)
                || "MAIL_SEND_FAILED".equals(code);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
