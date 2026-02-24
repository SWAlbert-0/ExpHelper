package fjnu.edu.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service("tencentSesMailProvider")
public class TencentSesMailProvider implements MailProvider {
    private static final Logger log = LoggerFactory.getLogger(TencentSesMailProvider.class);
    private static final String SERVICE = "ses";
    private static final String VERSION = "2020-10-02";
    private static final String ACTION = "SendEmail";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";

    @Value("${notify.mail.tencent.endpoint:ses.tencentcloudapi.com}")
    private String endpoint;
    @Value("${notify.mail.tencent.region:ap-guangzhou}")
    private String region;
    @Value("${notify.mail.tencent.secret-id:}")
    private String secretId;
    @Value("${notify.mail.tencent.secret-key:}")
    private String secretKey;
    @Value("${notify.mail.tencent.from-email:}")
    private String fromEmail;
    @Value("${notify.mail.tencent.from-name:}")
    private String fromName;

    @Override
    public MailSendResult sendMail(String to, String subject, String text) {
        return sendMail(to, subject, text, null);
    }

    @Override
    public MailSendResult sendMail(String to, String subject, String text, String html) {
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey)) {
            return MailSendResult.tencentConfigInvalid("腾讯SES配置不完整：secret-id/secret-key不能为空");
        }
        if (!StringUtils.hasText(fromEmail)) {
            return MailSendResult.tencentConfigInvalid("腾讯SES配置不完整：from-email不能为空");
        }
        if (!StringUtils.hasText(to)) {
            return MailSendResult.receiverInvalid("收件人邮箱为空");
        }
        try {
            long timestamp = Instant.now().getEpochSecond();
            String date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC).toString();
            String host = resolveHost();
            String payload = buildPayload(to.trim(), subject, text, html);
            String authorization = buildAuthorization(host, payload, timestamp, date);

            HttpPost post = new HttpPost("https://" + host);
            post.setHeader("Authorization", authorization);
            post.setHeader("Content-Type", "application/json; charset=utf-8");
            post.setHeader("Host", host);
            post.setHeader("X-TC-Action", ACTION);
            post.setHeader("X-TC-Version", VERSION);
            post.setHeader("X-TC-Timestamp", String.valueOf(timestamp));
            post.setHeader("X-TC-Region", region);
            post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post)) {
                String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return mapResponse(response.getStatusLine().getStatusCode(), body);
            }
        } catch (Exception ex) {
            log.warn("Send email by Tencent SES failed. to={}, reason={}", to, ex.getMessage());
            String msg = ex.getMessage() == null ? "腾讯SES发送异常" : ex.getMessage();
            return MailSendResult.tencentSendFailed(msg);
        }
    }

    @Override
    public String providerName() {
        return "TENCENT_SES";
    }

    private MailSendResult mapResponse(int statusCode, String body) {
        if (statusCode != 200) {
            return MailSendResult.tencentSendFailed("腾讯SES HTTP状态异常: " + statusCode);
        }
        if (!StringUtils.hasText(body)) {
            return MailSendResult.tencentSendFailed("腾讯SES返回为空");
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject resp = root.getAsJsonObject("Response");
            if (resp == null) {
                return MailSendResult.tencentSendFailed("腾讯SES响应缺少Response字段");
            }
            JsonObject err = resp.getAsJsonObject("Error");
            if (err != null) {
                String code = err.has("Code") ? err.get("Code").getAsString() : "UNKNOWN";
                String msg = err.has("Message") ? err.get("Message").getAsString() : "腾讯SES返回错误";
                return MailSendResult.tencentSendFailed(code + ": " + msg);
            }
            return MailSendResult.sent();
        } catch (RuntimeException ex) {
            return MailSendResult.tencentSendFailed("腾讯SES响应解析失败: " + ex.getMessage());
        }
    }

    private String resolveHost() {
        String value = StringUtils.hasText(endpoint) ? endpoint.trim() : "ses.tencentcloudapi.com";
        if (!value.contains("://")) {
            return value;
        }
        return URI.create(value).getHost();
    }

    private String buildPayload(String to, String subject, String text, String html) {
        JsonObject root = new JsonObject();
        root.addProperty("FromEmailAddress", fromEmail.trim());
        if (StringUtils.hasText(fromName)) {
            root.addProperty("FromEmailAddressIdentity", fromName.trim());
        }
        JsonObject destination = new JsonObject();
        JsonArray toAddresses = new JsonArray();
        toAddresses.add(to);
        destination.add("ToAddresses", toAddresses);
        root.add("Destination", destination);

        JsonObject simple = new JsonObject();
        simple.addProperty("Subject", StringUtils.hasText(subject) ? subject : "(无主题)");
        simple.addProperty("Text", StringUtils.hasText(text) ? text : "");
        if (StringUtils.hasText(html)) {
            simple.addProperty("Html", html);
        }
        root.add("Simple", simple);
        return root.toString();
    }

    private String buildAuthorization(String host, String payload, long timestamp, String date) throws Exception {
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + host + "\n"
                + "x-tc-action:" + ACTION.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String canonicalRequest = "POST\n"
                + "/\n"
                + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + sha256Hex(payload);

        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] secretDate = hmacSha256(("TC3" + secretKey.trim()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

        return ALGORITHM + " "
                + "Credential=" + secretId.trim() + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashed);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
