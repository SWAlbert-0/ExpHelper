package fjnu.edu.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * @ClassName EmailService
 * @Author zhh
 * @Date 2021/12/30 18:18
 **/
@Service("smtpMailProvider")
public class EmailService implements MailProvider {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String username;
    @Value("${spring.mail.from:}")
    private String configuredFrom;
    @Value("${spring.mail.host:}")
    private String host;
    @Value("${spring.mail.port:}")
    private String port;
    /**
     * 发送纯文本邮件.
     *
     * @param to      目标email 地址
     * @param subject 邮件主题
     * @param text    纯文本内容
     */
    @Override
    public MailSendResult sendMail(String to, String subject, String text) {
        return sendMail(to, subject, text, null);
    }

    @Override
    public MailSendResult sendMail(String to, String subject, String text, String html) {
        if (javaMailSender == null) {
            log.warn("Skip email notification because JavaMailSender is not configured. to={}, subject={}", to, subject);
            return MailSendResult.notConfigured("JavaMailSender未配置");
        }
        if (!StringUtils.hasText(host) || !StringUtils.hasText(port)) {
            return MailSendResult.configInvalid("SMTP配置不完整：spring.mail.host/port不能为空");
        }
        String from = resolveFromAddress();
        if (!StringUtils.hasText(from)) {
            return MailSendResult.fromEmpty("发件人邮箱为空，请配置spring.mail.from或spring.mail.username");
        }
        if (!isValidEmail(from)) {
            return MailSendResult.fromInvalid("发件人邮箱格式不正确: " + from);
        }
        if (!StringUtils.hasText(to)) {
            return MailSendResult.receiverInvalid("收件人邮箱为空");
        }
        if (!isValidEmail(to)) {
            return MailSendResult.receiverInvalid("收件人邮箱格式不正确: " + to);
        }
        try {
            if (StringUtils.hasText(html)) {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(to.trim());
                helper.setSubject(subject);
                helper.setText(text == null ? "" : text, html);
                javaMailSender.send(mimeMessage);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to.trim());
                message.setSubject(subject);
                message.setText(text);
                javaMailSender.send(message);
            }
            return MailSendResult.sent();
        } catch (Exception ex) {
            log.warn("Send email failed. to={}, subject={}, reason={}", to, subject, ex.getMessage());
            return MailSendResult.failed(ex.getMessage() == null ? "邮件发送失败" : ex.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "SMTP";
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(configuredFrom)) {
            return configuredFrom.trim();
        }
        if (StringUtils.hasText(username)) {
            return username.trim();
        }
        return "";
    }

    private boolean isValidEmail(String value) {
        try {
            InternetAddress address = new InternetAddress(value);
            address.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
}
