package fjnu.edu.common.codeenum;

public enum UserError {


    PASSWORD_MISTAKE(40001,"password mistake"),
    USER_IDENTITY_DISCREPANCY(40002,"user identity discrepancy"),
    EMP_IS_NULL_EXIT(400003,"user not exist"),
    TOKEN_IS_VERITYED(40004,"invalid token,please login again"),
    TOKEN_IS_EXPIRED(40005,"token is expired,please send again with the new token"),
    TOKEN_ACQUISITION_FAILS(40006,"token acquisition fails"),
    NONE_TOKEN(40007, "without token,please login again"),
    TOKEN_CHECK_ERROR(40008, "token check error");
    Integer errorCode;
    String errorMessage;

    UserError(Integer errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

