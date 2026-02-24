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
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckItem;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckResult;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.notify.service.NotificationService;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.UUID;

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
    NotificationService notificationService;
    @Autowired
    DiscoveryClient discoveryClient;
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
    private static final String PRECHECK_NO_ALG = "PLAN_PRECHECK_NO_ALG";
    private static final String PRECHECK_SERVICE_NAME_EMPTY = "ALG_SERVICE_NAME_EMPTY";
    private static final String PRECHECK_SERVICE_NO_INSTANCE = "ALG_SERVICE_NO_INSTANCE";
    private static final String PRECHECK_NACOS_UNREACHABLE = "NACOS_UNREACHABLE";

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
        String executionId = newExecutionId();
        exePlan.setExecutionId(executionId);
        exePlanMgrDao.updateExePlanById(exePlan);
        PlanPreCheckResult preCheckResult = preCheckPlanReachability(exePlan, true);
        if (preCheckResult == null || !preCheckResult.isPass()) {
            String preCheckError = preCheckResult == null ? "执行前检查失败" : preCheckResult.getMessage();
            markFailed(exePlan, preCheckError);
            exePlan.setExeStartTime(System.currentTimeMillis());
            exePlan.setExeEndTime(System.currentTimeMillis());
            exePlanMgrDao.updateExePlanById(exePlan);
            appendPlanLog(planId, executionId, "ERROR", "PLAN_FAIL", preCheckError, null, null, null, null);
            runningPlans.remove(planId);
            return false;
        }
        exePlan.setExeState(Constant.IN_EXECUTION);
        exePlan.setExeStartTime(System.currentTimeMillis());
        exePlan.setExeEndTime(0L);
        exePlan.setLastError(null);
        exePlanMgrDao.updateExePlanById(exePlan);
        appendPlanLog(planId, executionId, "INFO", "PLAN_START", "计划开始执行", null, null, null, null);

        try {
            planExecutor.execute(() -> runPlan(planId));
            return true;
        } catch (RejectedExecutionException ex) {
            markFailed(exePlan, "执行任务队列已满");
            appendPlanLog(planId, executionId, "ERROR", "PLAN_FAIL", "执行任务队列已满", null, null, null, null);
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
            String executionId = exePlan.getExecutionId();
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
                appendPlanLog(planId, executionId, "INFO", "ALG_START",
                        "开始执行算法[" + algRunInfo.getAlgName() + "]，服务[" + serviceName + "]",
                        algId, null, null, "serviceUrl=" + serverURL);

                List<RunPara> runParas = algRunInfo.getRunParas();
                int runNum = algRunInfo.getRunNum();
                PlanExeResult planExeResult = new PlanExeResult();
                planExeResult.setAlgName(algRunInfo.getAlgName() + "-" + algRunInfo.getAlgRunInfoId());
                planExeResult.setStartTime(System.currentTimeMillis());
                List<GenResult> genResults = new ArrayList<>();

                for (int time = 0; time < runNum; time++) {
                    for (ProbInst probInst : probInsts) {
                        appendPlanLog(planId, executionId, "INFO", "ALG_CALL",
                                "调用算法，run=" + (time + 1) + "，问题实例=" + probInst.getInstName(),
                                algId, time + 1, probInst.getInstId(), null);
                        AlgRunCtx algRunCtx = buildAlgRunCtx(planId, algId, probInst, runParas, time + 1);
                        HttpEntity<AlgRunCtx> request = new HttpEntity<>(algRunCtx);
                        List<EachResult> eachResults = invokeAlgWithRetry(planId, executionId, algId, time + 1, probInst.getInstId(), serverURL, request);

                        GenResult genResult = new GenResult();
                        genResult.setEachResults(eachResults);
                        genResult.setProbInstId(probInst.getInstId());
                        genResult.setOutTime(System.currentTimeMillis());
                        genResults.add(genResult);
                        appendPlanLog(planId, executionId, "INFO", "ALG_CALL",
                                "算法调用成功，run=" + (time + 1) + "，实例=" + probInst.getInstName(),
                                algId, time + 1, probInst.getInstId(), "resultSize=" + eachResults.size());
                    }
                }

                planExeResult.setPlanId(planId);
                planExeResult.setAlgId(algId);
                planExeResult.setRunNum(runNum);
                planExeResult.setOutputTime(System.currentTimeMillis());
                planExeResult.setGenResults(genResults);
                algRltSaveService.insertPlanExeResult(planExeResult);
                appendPlanLog(planId, executionId, "INFO", "ALG_DONE",
                        "算法[" + algRunInfo.getAlgName() + "]执行完成，共生成结果条数=" + genResults.size(),
                        algId, null, null, null);
            }

            exePlan.setExeState(Constant.NORMAL_TERMINATION);
            exePlan.setLastError(null);
            appendPlanLog(planId, executionId, "INFO", "PLAN_DONE", "计划执行完成", null, null, null, null);
            notifyPlanResult(exePlan, true);
        } catch (Exception ex) {
            markFailed(exePlan, extractErrorMessage(ex));
            appendPlanLog(planId, exePlan.getExecutionId(), "ERROR", "PLAN_FAIL",
                    "计划执行失败: " + extractErrorMessage(ex), null, null, null,
                    ex.getClass().getSimpleName());
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

    @Override
    public PlanPreCheckResult preCheck(String planId) {
        if (planId == null || planId.trim().isEmpty()) {
            return PlanPreCheckResult.failed(PRECHECK_NO_ALG, "执行前检查失败: planId不能为空", Collections.emptyList());
        }
        ExePlan exePlan = exePlanMgrDao.getExePlanById(planId);
        if (exePlan == null) {
            return PlanPreCheckResult.failed("PLAN_NOT_FOUND", "执行前检查失败: 执行计划不存在", Collections.emptyList());
        }
        return preCheckPlanReachability(exePlan, true);
    }

    private List<EachResult> invokeAlgWithRetry(String planId, String executionId, String algId, int runIndex, String probInstId,
                                                String serverURL, HttpEntity<AlgRunCtx> request) {
        RuntimeException lastException = null;
        int totalAttempts = retryTimes + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                ResponseEntity<EachResult[]> eachResult = restTemplate.postForEntity(serverURL + "/myAlg/", request, EachResult[].class);
                EachResult[] body = eachResult.getBody();
                if (body == null) {
                    throw new IllegalStateException("算法返回空结果");
                }
                return Arrays.asList(body);
            } catch (RuntimeException ex) {
                lastException = ex;
                appendPlanLog(planId, executionId, "WARN", "RETRY",
                        "算法调用失败，第" + attempt + "/" + totalAttempts + "次: " + extractErrorMessage(ex),
                        algId, runIndex, probInstId, "serviceUrl=" + serverURL);
            }
        }
        String detail = extractErrorMessage(lastException);
        throw new IllegalStateException("算法调用失败(已重试" + totalAttempts + "次): " + detail, lastException);
    }

    private void markFailed(ExePlan exePlan, String error) {
        exePlan.setExeState(Constant.ABNORMAL_TERMINATION);
        String detail = error == null ? "未知错误" : error.trim();
        if (detail.isEmpty()) {
            detail = "未知错误";
        }
        if (detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        exePlan.setLastError(detail);
    }

    private void notifyPlanResult(ExePlan exePlan, boolean success) {
        int created = notificationService.enqueuePlanDoneNotifications(exePlan, success);
        appendPlanLog(exePlan.getPlanId(), exePlan.getExecutionId(), "INFO", "MAIL_NOTIFY",
                "通知任务入队完成，已创建任务数=" + created, null, null, null, "reasonCode=MAIL_OUTBOX_ENQUEUED");
    }

    private String extractErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = throwable.getMessage();
        }
        if (message == null || message.trim().isEmpty()) {
            return root.getClass().getSimpleName();
        }
        return message.trim();
    }

    private void appendPlanLog(String planId, String executionId, String level, String stage, String message,
                               String algId, Integer runIndex, String probInstId, String details) {
        ExePlanLog log = new ExePlanLog();
        log.setPlanId(planId);
        log.setExecutionId(executionId);
        log.setLevel(level);
        log.setStage(stage);
        log.setMessage(message);
        log.setAlgId(algId);
        log.setRunIndex(runIndex);
        log.setProbInstId(probInstId);
        log.setDetails(details);
        log.setTs(System.currentTimeMillis());
        exePlanMgrDao.appendPlanLog(log);
    }

    private PlanPreCheckResult preCheckPlanReachability(ExePlan exePlan, boolean withRetry) {
        if (exePlan == null || exePlan.getAlgRunInfos() == null || exePlan.getAlgRunInfos().isEmpty()) {
            return PlanPreCheckResult.failed(PRECHECK_NO_ALG, "执行前检查失败: 未配置算法", Collections.emptyList());
        }
        List<PlanPreCheckItem> items = new ArrayList<>();
        int maxAttempts = withRetry ? 3 : 1;
        for (AlgRunInfo algRunInfo : exePlan.getAlgRunInfos()) {
            String algId = algRunInfo.getAlgId();
            String serviceName = algLibMgrService.getServiceNameById(algId);

            PlanPreCheckItem item = new PlanPreCheckItem();
            item.setAlgId(algId);
            item.setAlgName(algRunInfo.getAlgName());
            item.setServiceName(serviceName == null ? "" : serviceName.trim());
            item.setInstanceCount(0);
            item.setReachable(false);

            if (serviceName == null || serviceName.trim().isEmpty()) {
                item.setErrorCode(PRECHECK_SERVICE_NAME_EMPTY);
                item.setDiagnosis("算法未配置服务名或服务名为空");
                item.setSuggestion("请在算法库中填写正确 serviceName，并与 Nacos 注册名保持一致");
                items.add(item);
                return PlanPreCheckResult.failed(PRECHECK_SERVICE_NAME_EMPTY,
                        "执行前检查失败: 算法服务未注册: " + algId, items);
            }

            boolean available = false;
            String failureCode = PRECHECK_SERVICE_NO_INSTANCE;
            String diagnosis = "";
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                    int count = instances == null ? 0 : instances.size();
                    item.setInstanceCount(count);
                    if (count > 0) {
                        available = true;
                        break;
                    }
                    diagnosis = "服务已配置，但在Nacos中无可用实例";
                    if (attempt < maxAttempts) {
                        sleepQuietly(800L);
                    }
                } catch (RuntimeException ex) {
                    failureCode = PRECHECK_NACOS_UNREACHABLE;
                    diagnosis = "调用Nacos失败: " + extractErrorMessage(ex);
                    if (attempt < maxAttempts) {
                        sleepQuietly(800L);
                    }
                }
            }

            if (!available) {
                item.setErrorCode(failureCode);
                item.setDiagnosis(diagnosis);
                if (PRECHECK_NACOS_UNREACHABLE.equals(failureCode)) {
                    item.setSuggestion("请确认Nacos地址可达，并检查 webapp 的 NACOS_SERVER_ADDR 配置");
                    items.add(item);
                    return PlanPreCheckResult.failed(PRECHECK_NACOS_UNREACHABLE,
                            "执行前检查失败: Nacos不可用或不可达", items);
                }
                item.setSuggestion("请先启动算法服务并确认服务名与Nacos注册名一致");
                items.add(item);
                return PlanPreCheckResult.failed(PRECHECK_SERVICE_NO_INSTANCE,
                        "执行前检查失败: 服务[" + serviceName + "]在Nacos中无可用实例", items);
            }

            item.setReachable(true);
            item.setErrorCode("");
            item.setDiagnosis("服务可用");
            item.setSuggestion("");
            items.add(item);
        }
        return PlanPreCheckResult.passed(items);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String newExecutionId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
