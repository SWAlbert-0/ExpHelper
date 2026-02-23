package fjnu.edu.intf;

import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckResult;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.probInstMgr.entity.ProbInst;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PlanExecuteService {

    public boolean execute(String planId); // 总的计划执行

    public AlgRunCtx buildAlgRunCtx(String planId,
                                    String algId,
                                    ProbInst probInst,
                                    List<RunPara> runParas,
                                    int runNum);  // 构建算法运行的上下文

    public PlanPreCheckResult preCheck(String planId);
}
