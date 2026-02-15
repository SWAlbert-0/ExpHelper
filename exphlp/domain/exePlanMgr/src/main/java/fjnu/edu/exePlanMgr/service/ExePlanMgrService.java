package fjnu.edu.exePlanMgr.service;

import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.exePlanMgr.service.impl.ExePlanMgrServiceImpl;
import fjnu.edu.probInstMgr.entity.ProbInst;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ExePlanMgrService {

    public List<ExePlan> getExePlans(int pageNum, int pageSize);

    public String addExePlan(ExePlan exeplan) throws Exception;

    public ExePlan getExePlanByName(String planName);

    public boolean deleteExePlanById(String planId);

    public boolean updateExePlanById(ExePlan exeplan);

    public long countAllExePlans();

}
