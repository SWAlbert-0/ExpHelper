package fjnu.edu.auth;

import java.util.regex.Pattern;

public final class UserFieldValidator {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern QQ_PATTERN = Pattern.compile("^[1-9]\\d{4,11}$");

    private UserFieldValidator() {
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches() ? null : "邮箱格式不正确";
    }

    public static String validateMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        return MOBILE_PATTERN.matcher(mobile.trim()).matches() ? null : "手机号格式不正确";
    }

    public static String validateQq(String qq) {
        if (qq == null || qq.trim().isEmpty()) {
            return null;
        }
        return QQ_PATTERN.matcher(qq.trim()).matches() ? null : "QQ号格式不正确";
    }
}

