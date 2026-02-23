package fjnu.edu.exePlanMgr.service;

import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
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

    public ExePlan getExePlanById(String planId);

    public boolean deleteExePlanById(String planId);

    public boolean updateExePlanById(ExePlan exeplan);

    public long countAllExePlans();

    public long countPlansByProbInstId(String probInstId);

    public List<String> listPlanNamesByProbInstId(String probInstId, int limit);

    public long countPlansByAlgId(String algId);

    public List<String> listPlanNamesByAlgId(String algId, int limit);

    public void appendPlanLog(ExePlanLog exePlanLog);

    public List<ExePlanLog> getPlanLogs(String planId, long afterSeq, int limit);

    public long getLatestPlanLogSeq(String planId);

}
