package fjnu.edu.exePlanMgr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;


@Data
@ToString
@NoArgsConstructor
public class ExePlan {
    @MongoId
    private String planId;//MongoDB自动生成的Id
    private String planName;//计划名
    private List<String> probInstIds;//问题实例Id
    private List<AlgRunInfo> algRunInfos;
    private List<String> userIds;//需要通知的用户的Id
    private long exeStartTime;//计划开始时间
    private long exeEndTime;//计划结束时间
    /**
     * 未执行		1
     * 执行中		2
     * 异常结束		3
     * 正常结束		4
     */
    private int exeState;//执行状态（包括以下几种：未执行，执行中，异常结束，正常结束)
    private String description;//计划的描述


}//end ExePlan
