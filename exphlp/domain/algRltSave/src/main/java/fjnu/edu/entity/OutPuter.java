package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class OutPuter {

    private String planExeResultId;
    private String planId;
    private String algId;
    private Integer runNum;
    private Integer genResultNum;

    private List<EachResult> eachResults;

    public  OutPuter(String planId, String algId, Integer runNum, Integer genResultNum) {
        this.planId = planId;
        this.algId = algId;
        this.runNum = runNum;
        this.genResultNum = genResultNum;
    }

    public OutPuter(String planId, String algId, Integer runNum) {
        this.planId = planId;
        this.algId = algId;
        this.runNum = runNum;
    }

    public OutPuter() {

    }

    public boolean write(List<EachResult> eachResults) {
        return false;
    }
}
