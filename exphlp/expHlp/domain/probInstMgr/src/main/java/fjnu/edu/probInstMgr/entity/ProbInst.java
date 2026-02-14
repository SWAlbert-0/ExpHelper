package fjnu.edu.probInstMgr.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;
@Data
@ToString
@NoArgsConstructor
public class ProbInst  implements Serializable {
    @MongoId
    private String instId;//MongoDB自动生成的ID
    private String categoryName;
    private String instName;
    private String machineIp;//保存程序的机器的IP地址
    private String dirName;
    private String machineName;//保存程序的机器名
    private String description;//程序描述
 }

