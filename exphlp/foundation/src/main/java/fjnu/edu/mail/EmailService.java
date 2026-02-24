package fjnu.edu.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @ClassName EmailService
 * @Author zhh
 * @Date 2021/12/30 18:18
 **/
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String from;
    /**
     * 发送纯文本邮件.
     *
     * @param to      目标email 地址
     * @param subject 邮件主题
     * @param text    纯文本内容
     */
    public MailSendResult sendMail(String to, String subject, String text) {
        if (javaMailSender == null) {
            log.warn("Skip email notification because JavaMailSender is not configured. to={}, subject={}", to, subject);
            return MailSendResult.notConfigured("JavaMailSender未配置");
        }
        if (!StringUtils.hasText(to)) {
            return MailSendResult.failed("收件人邮箱为空");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
            return MailSendResult.sent();
        } catch (Exception ex) {
            log.warn("Send email failed. to={}, subject={}, reason={}", to, subject, ex.getMessage());
            return MailSendResult.failed(ex.getMessage() == null ? "邮件发送失败" : ex.getMessage());
        }
    }
}
