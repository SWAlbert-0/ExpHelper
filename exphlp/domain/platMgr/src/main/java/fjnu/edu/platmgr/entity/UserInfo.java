package fjnu.edu.platmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
@ToString
public class UserInfo implements Serializable {
    @Id
    private String userId;//MongoDB自动生成的Id
    private String userName;//用户名
    private Integer role;//用户所属角色
    private String password;//用户密码
    private String email;//邮箱
    private String wechat;//微信号
    private String mobile;//手机号
    private String qq;//QQ号
    private String avatar;//头像访问路径
}
