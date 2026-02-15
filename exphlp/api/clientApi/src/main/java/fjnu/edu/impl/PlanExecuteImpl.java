package fjnu.edu.impl;

import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.GenResult;
import fjnu.edu.entity.OutPuter;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.exePlanMgr.Constant.Constant;
import fjnu.edu.exePlanMgr.dao.ExePlanMgrDao;
import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.AlgRunInfo;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.mail.EmailService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class PlanExecuteImpl implements PlanExecuteService {


    @Autowired
    ExePlanMgrDao exePlanMgrDao;
    @Autowired
    AlgRltSaveService algRltSaveService;
    @Autowired
    AlgLibMgrService algLibMgrService;

    @Autowired
    ProbInstDao probInstDao;
    @Autowired
    PlatMgrService platMgrService;
    @Autowired
    EmailService emailService;
    @Resource(name = "algRestTemplate")
    private RestTemplate restTemplate;
    private final ExecutorService planExecutor = new ThreadPoolExecutor(
            2,
            8,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadPoolExecutor.AbortPolicy()
    );
    private final Set<String> runningPlans = ConcurrentHashMap.newKeySet();

    @org.springframework.beans.factory.annotation.Value("${alg.call.retry-times:1}")
    private int retryTimes;

    @Override
    public boolean execute(String planId){
        if (planId == null || planId.trim().isEmpty()) {
            return false;
        }
        if (!runningPlans.add(planId)) {
            return false;
        }
        ExePlan exePlan = exePlanMgrDao.getExePlanById(planId);
        if (exePlan == null) {
            runningPlans.remove(planId);
            return false;
        }
        exePlan.setExeState(Constant.IN_EXECUTION);
        exePlan.setExeStartTime(System.currentTimeMillis());
        exePlan.setExeEndTime(0L);
        exePlan.setLastError(null);
        exePlanMgrDao.updateExePlanById(exePlan);

        try {
            planExecutor.execute(() -> runPlan(planId));
            return true;
        } catch (RejectedExecutionException ex) {
            markFailed(exePlan, "执行任务队列已满");
            exePlan.setExeEndTime(System.currentTimeMillis());
            exePlanMgrDao.updateExePlanById(exePlan);
            runningPlans.remove(planId);
            return false;
        }
    }

    private void runPlan(String planId) {
        ExePlan exePlan = exePlanMgrDao.getExePlanById(planId);
        if (exePlan == null) {
            runningPlans.remove(planId);
            return;
        }
        try {
            List<ProbInst> probInsts = new ArrayList<>();
            for (String probInstId : exePlan.getProbInstIds()) {
                ProbInst probInst = probInstDao.getProbInstByID(probInstId);
                if (probInst == null) {
                    throw new IllegalStateException("问题实例不存在: " + probInstId);
                }
                probInsts.add(probInst);
            }

            List<AlgRunInfo> algRunInfos = exePlan.getAlgRunInfos();
            for (AlgRunInfo algRunInfo : algRunInfos) {
                String algId = algRunInfo.getAlgId();
                String serviceName = algLibMgrService.getServiceNameById(algId);
                if (serviceName == null || serviceName.trim().isEmpty()) {
                    throw new IllegalStateException("算法服务未注册: " + algId);
                }
                String serverURL = "http://" + serviceName;

                List<RunPara> runParas = algRunInfo.getRunParas();
                int runNum = algRunInfo.getRunNum();
                PlanExeResult planExeResult = new PlanExeResult();
                planExeResult.setAlgName(algRunInfo.getAlgName() + "-" + algRunInfo.getAlgRunInfoId());
                planExeResult.setStartTime(System.currentTimeMillis());
                List<GenResult> genResults = new ArrayList<>();

                for (int time = 0; time < runNum; time++) {
                    for (ProbInst probInst : probInsts) {
                        AlgRunCtx algRunCtx = buildAlgRunCtx(planId, algId, probInst, runParas, time + 1);
                        HttpEntity<AlgRunCtx> request = new HttpEntity<>(algRunCtx);
                        List<EachResult> eachResults = invokeAlgWithRetry(serverURL, request);

                        GenResult genResult = new GenResult();
                        genResult.setEachResults(eachResults);
                        genResult.setProbInstId(probInst.getInstId());
                        genResult.setOutTime(System.currentTimeMillis());
                        genResults.add(genResult);
                    }
                }

                planExeResult.setPlanId(planId);
                planExeResult.setAlgId(algId);
                planExeResult.setRunNum(runNum);
                planExeResult.setOutputTime(System.currentTimeMillis());
                planExeResult.setGenResults(genResults);
                algRltSaveService.insertPlanExeResult(planExeResult);
            }

            exePlan.setExeState(Constant.NORMAL_TERMINATION);
            exePlan.setLastError(null);
            notifyPlanResult(exePlan, true);
        } catch (Exception ex) {
            markFailed(exePlan, ex.getMessage());
            notifyPlanResult(exePlan, false);
            return;
        } finally {
            exePlan.setExeEndTime(System.currentTimeMillis());
            exePlanMgrDao.updateExePlanById(exePlan);
            runningPlans.remove(planId);
        }
    }

    /**
     * 构建算法运行的上下文，应该是一个很小的级别
     * @param planId
     * @return
     */
    @Override
    public AlgRunCtx buildAlgRunCtx(String planId, String algId, ProbInst probInst, List<RunPara> runParas, int runNum) {

        OutPuter outPuter = new OutPuter(planId, algId, runNum);
        AlgRunCtx algRunCtx = new AlgRunCtx(probInst, runParas, outPuter);

        return algRunCtx;
    }

    private List<EachResult> invokeAlgWithRetry(String serverURL, HttpEntity<AlgRunCtx> request) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt <= retryTimes; attempt++) {
            try {
                ResponseEntity<EachResult[]> eachResult = restTemplate.postForEntity(serverURL + "/myAlg/", request, EachResult[].class);
                EachResult[] body = eachResult.getBody();
                if (body == null) {
                    throw new IllegalStateException("算法返回空结果");
                }
                return Arrays.asList(body);
            } catch (RuntimeException ex) {
                lastException = ex;
            }
        }
        throw lastException == null ? new RuntimeException("算法调用失败") : lastException;
    }

    private void markFailed(ExePlan exePlan, String error) {
        exePlan.setExeState(Constant.ABNORMAL_TERMINATION);
        exePlan.setLastError(error == null ? "未知错误" : error);
    }

    private void notifyPlanResult(ExePlan exePlan, boolean success) {
        if (exePlan.getUserIds() == null || exePlan.getUserIds().isEmpty()) {
            return;
        }
        String subject = "执行计划通知: " + exePlan.getPlanName();
        String status = success ? "正常结束" : "异常结束";
        String content = "计划[" + exePlan.getPlanName() + "]执行" + status +
                (exePlan.getLastError() == null ? "" : ("，错误信息: " + exePlan.getLastError()));
        for (String userId : exePlan.getUserIds()) {
            try {
                UserInfo user = platMgrService.getUserById(userId);
                if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    emailService.sendMail(user.getEmail(), subject, content);
                }
            } catch (Exception ignored) {
                // Notification failures should not change execution result.
            }
        }
    }
}
