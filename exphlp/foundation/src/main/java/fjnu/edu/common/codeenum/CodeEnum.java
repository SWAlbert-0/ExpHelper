package fjnu.edu.common.codeenum;

public enum CodeEnum {
    SUCCESS_200(200,"成功"),
    ERROR_400(400,"请求参数错误"),
    ERROR_401(401,"用户没有权限"),
    ERROR_403(403,"没有访问接口的权限"),
    ERROR_404(404,"无法找到资源"),
    ERROR_405(405,"未绑定账号或账号已超时"),
    ERROR_410(410,"请求限制"),
    ERROR_500(500,"服务器内部错误"),
    ERROR_510(510,"调用外部服务返回"),
    ERROR_511(511,"调用外部服务异常");



    private Integer code;
    private String msg;
    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public String getMsg(String msg) {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }

    CodeEnum(Integer code, String msg){
        this.setCode(code);
        this.setMsg(msg);

    }
}