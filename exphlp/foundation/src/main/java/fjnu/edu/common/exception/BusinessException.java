package fjnu.edu.common.exception;

/**
 * @ClassName BusinessException
 * @Author zhh
 * @Date 2021/11/23 0:08
 **/
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1877734890673094989L;

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }
}