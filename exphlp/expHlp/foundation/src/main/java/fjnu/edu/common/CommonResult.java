package fjnu.edu.common;



import fjnu.edu.common.codeenum.CodeEnum;
import fjnu.edu.common.codeenum.UserError;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ApiModel(value = "响应信息主体")
public class CommonResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    @ApiModelProperty(value = "返回代号")
    private Integer code;

    @Getter
    @Setter
    @ApiModelProperty(value = "返回信息")
    private String message;

    @Getter
    @Setter
    @ApiModelProperty(value = "返回数据")
    private T data;

    //成功有返回data
    public static <T> CommonResult<T> ok(T data) {
        return apiResult(CodeEnum.SUCCESS_200.getCode(), CodeEnum.SUCCESS_200.getMsg(), data);
    }
    //成功无返回data
    public static <T> CommonResult<T> ok() {
        return apiResult(CodeEnum.SUCCESS_200.getCode(), CodeEnum.SUCCESS_200.getMsg(), null);
    }
    //失败时无指定msg
    public static <T> CommonResult<T> failed() {
        return apiResult(CodeEnum.ERROR_500.getCode(), CodeEnum.ERROR_500.getMsg(), null);
    }
    //失败时有指定msg
    public static <T> CommonResult<T> failed(String msg) {
        return apiResult(CodeEnum.ERROR_500.getCode(), msg, null);
    }

    //失败时有指定msg
    public static <T> CommonResult<T> failed(UserError userError) {
        return apiResult(userError.getErrorCode(), userError.getErrorMessage(), null);
    }

    //异常时有指定msg
    public static <T> CommonResult<T> failedWithException(String msg) {
//        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();//手动
        return apiResult(CodeEnum.ERROR_500.getCode(), msg, null);
    }

    private static <T> CommonResult<T> apiResult(int code,String msg,T data) {
        CommonResult<T> apiResult = new CommonResult<>();
        apiResult.setCode(code);
        apiResult.setMessage(msg);
        apiResult.setData(data);
        return apiResult;
    }
}