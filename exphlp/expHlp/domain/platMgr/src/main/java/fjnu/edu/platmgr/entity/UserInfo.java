package fjnu.edu.platmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;

@Data
@ToString
public class UserInfo implements Serializable {
    @MongoId
    private String userId;//MongoDB自动生成的Id
    private String userName;//用户名
    private Integer role;//用户所属角色
    private String password;//用户密码
    private String email;//邮箱
    private String wechat;//微信号
}
