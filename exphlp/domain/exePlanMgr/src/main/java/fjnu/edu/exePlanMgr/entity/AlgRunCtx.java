package fjnu.edu.exePlanMgr.entity;

import fjnu.edu.entity.OutPuter;
import fjnu.edu.probInstMgr.entity.ProbInst;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString
@Data
@NoArgsConstructor
public class AlgRunCtx {
    private ProbInst probInst;
    private List<RunPara> runParas;
    private OutPuter outPuter;

    public AlgRunCtx(ProbInst probInst, List<RunPara> runParas, OutPuter outPuter) {
        this.probInst = probInst;
        this.runParas = runParas;
        this.outPuter = outPuter;
    }



}
