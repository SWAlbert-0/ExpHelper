package fjnu.edu.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class JwtUtil {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${auth.jwt.secret:change-me-in-dev}")
    private String secret;

    @Value("${auth.jwt.expire-seconds:86400}")
    private long expireSeconds;

    public String generateToken(AuthUser user) {
        long exp = System.currentTimeMillis() / 1000 + expireSeconds;
        String payload = user.getUserId() + "|" + user.getUserName() + "|" + exp + "|" + (user.getRole() == null ? 0 : user.getRole());
        String payloadEncoded = URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(payloadEncoded);
        return payloadEncoded + "." + signature;
    }

    public AuthUser verify(String token) {
        if (token == null || token.trim().isEmpty() || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String expected = sign(parts[0]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String payload = new String(URL_DECODER.decode(parts[0]), StandardCharsets.UTF_8);
        String[] fields = payload.split("\\|", 4);
        if (fields.length != 4) {
            return null;
        }
        long exp;
        try {
            exp = Long.parseLong(fields[2]);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (System.currentTimeMillis() / 1000 > exp) {
            return null;
        }
        Integer role;
        try {
            role = Integer.parseInt(fields[3]);
        } catch (NumberFormatException ex) {
            role = 0;
        }
        return new AuthUser(fields[0], fields[1], role);
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] sign = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(sign);
        } catch (Exception ex) {
            throw new IllegalStateException("jwt sign failed", ex);
        }
    }
}
