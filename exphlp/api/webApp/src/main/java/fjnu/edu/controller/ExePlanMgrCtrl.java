package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckItem;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckResult;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


@RestController
@CrossOrigin
@RequestMapping("/api/ExePlanController")
public class ExePlanMgrCtrl {
    private static final Logger log = LoggerFactory.getLogger(ExePlanMgrCtrl.class);

    @Autowired
    ExePlanMgrService exePlanMgrService;

    @Autowired
    PlanExecuteService planExecuteService;

    @Autowired
    ProbInstMgrService probInstMgrService;

    @Autowired
    AlgLibMgrService algLibMgrService;

    @Autowired
    DiscoveryClient discoveryClient;

    @GetMapping("/getExePlans")
    public List<ExePlan> getExePlans(@RequestParam int pageNum,
                                     @RequestParam int pageSize){
        List<ExePlan> plans = exePlanMgrService.getExePlans(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return plans == null ? Collections.emptyList() : plans;

    }

    @PostMapping("/addExePlan")
    public String addExePlan(@RequestBody ExePlan exeplan) throws Exception {
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanName())) {
            throw new IllegalArgumentException("计划名称不能为空");
        }
        return exePlanMgrService.addExePlan(exeplan);
    }

    @GetMapping("/getExePlanByName")
    public ExePlan getExeplanByName(@RequestParam String planName){
        if (!StringUtils.hasText(planName)) {
            return null;
        }
        return exePlanMgrService.getExePlanByName(planName);
    }

    @PostMapping("/deleteExePlanById")
    public boolean deleteExePlanById(@RequestParam String planId){
        if (!StringUtils.hasText(planId)) {
            return false;
        }
        return exePlanMgrService.deleteExePlanById(planId);
    }

    @PostMapping("updateExePlanById")
    public boolean updateExePlanById(@RequestBody ExePlan exeplan){
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return false;
        }
        return exePlanMgrService.updateExePlanById(exeplan);
    }

    @GetMapping("/countAllExePlans")
    public long countAllExePlans() {
        long count = exePlanMgrService.countAllExePlans();
        return  count;
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestParam String planId, HttpServletRequest request){
        String traceId = TraceContext.getTraceId(request);
        if (planId == null || planId.trim().isEmpty()) {
            log.warn("traceId={} path={} errorCode={}", traceId, "/api/ExePlanController/execute", ErrorCode.PLAN_ID_EMPTY.code());
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        ExePlan exePlan;
        try {
            exePlan = exePlanMgrService.getExePlanById(planId);
        } catch (Exception ex) {
            log.warn("traceId={} path={} planId={} errorCode={}", traceId, "/api/ExePlanController/execute", planId, ErrorCode.PLAN_ID_FORMAT_INVALID.code());
            return ApiResponse.failed(request, 400, "planId格式非法", ErrorCode.PLAN_ID_FORMAT_INVALID.code());
        }
        if (exePlan == null) {
            log.warn("traceId={} path={} planId={} errorCode={}", traceId, "/api/ExePlanController/execute", planId, ErrorCode.PLAN_NOT_FOUND.code());
            return ApiResponse.failed(request, 404, "执行计划不存在", ErrorCode.PLAN_NOT_FOUND.code());
        }
        boolean accepted = planExecuteService.execute(planId);
        ExePlan latest = exePlanMgrService.getExePlanById(planId);
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("accepted", accepted);
        data.put("state", latest == null ? null : latest.getExeState());
        data.put("lastError", latest == null ? null : latest.getLastError());
        data.put("preCheckErrorCode", inferPreCheckErrorCode(latest == null ? null : latest.getLastError()));
        log.info("traceId={} path={} planId={} accepted={} state={} lastError={}", traceId, "/api/ExePlanController/execute",
                planId, accepted, data.get("state"), data.get("lastError"));
        if (!accepted) {
            return ApiResponse.ok(request, data, "计划未被受理，可能正在执行或执行队列已满");
        }
        return ApiResponse.ok(request, data);
    }

    @GetMapping("/preCheck")
    public Map<String, Object> preCheck(@RequestParam String planId, HttpServletRequest request) {
        if (!StringUtils.hasText(planId)) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        PlanPreCheckResult result = planExecuteService.preCheck(planId);
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("pass", result != null && result.isPass());
        data.put("errorCode", result == null ? ErrorCode.INTERNAL_ERROR.code() : result.getErrorCode());
        data.put("message", result == null ? "执行前检查失败" : result.getMessage());
        data.put("items", result == null ? Collections.emptyList() : result.getItems());
        if (result == null || !result.isPass()) {
            String code = result == null ? ErrorCode.INTERNAL_ERROR.code() : normalizePreCheckErrorCode(result.getErrorCode());
            return ApiResponse.failed(request, 400, data.get("message").toString(),
                    code, data);
        }
        return ApiResponse.ok(request, data, "执行前检查通过");
    }

    @GetMapping("/wizardPrecheck")
    public Map<String, Object> wizardPrecheck(@RequestParam(required = false) String planId,
                                              @RequestParam(required = false) String probInstId,
                                              @RequestParam(required = false) String algId,
                                              @RequestParam(required = false) String serviceName,
                                              HttpServletRequest request) {
        if (StringUtils.hasText(planId)) {
            PlanPreCheckResult result = planExecuteService.preCheck(planId);
            return buildWizardResponse(request, planId, result);
        }

        List<PlanPreCheckItem> items = new ArrayList<>();
        if (!StringUtils.hasText(probInstId)) {
            return wizardFailed(request, "", ErrorCode.INVALID_ARGUMENT.code(), "问题实例不能为空", items);
        }
        ProbInst probInst = probInstMgrService.getProbInstByID(probInstId);
        if (probInst == null) {
            return wizardFailed(request, "", ErrorCode.INVALID_ARGUMENT.code(), "问题实例不存在", items);
        }

        if (!StringUtils.hasText(algId)) {
            return wizardFailed(request, "", ErrorCode.INVALID_ARGUMENT.code(), "算法不能为空", items);
        }
        AlgInfo algInfo = algLibMgrService.getAlgInfoById(algId);
        if (algInfo == null) {
            return wizardFailed(request, "", ErrorCode.ALG_NOT_FOUND.code(), "算法不存在", items);
        }

        String finalServiceName = StringUtils.hasText(serviceName) ? serviceName.trim() : (algInfo.getServiceName() == null ? "" : algInfo.getServiceName().trim());
        PlanPreCheckItem item = new PlanPreCheckItem();
        item.setAlgId(algId);
        item.setAlgName(algInfo.getAlgName());
        item.setServiceName(finalServiceName);
        item.setReachable(false);
        item.setInstanceCount(0);

        if (!StringUtils.hasText(finalServiceName)) {
            item.setErrorCode(ErrorCode.ALG_SERVICE_NAME_EMPTY.code());
            item.setDiagnosis("算法服务名为空");
            item.setSuggestion("请在算法库管理中填写服务名，并确保与Nacos注册名一致");
            items.add(item);
            return wizardFailed(request, "", ErrorCode.ALG_SERVICE_NAME_EMPTY.code(), "执行前检查失败: 算法服务名为空", items);
        }

        List<ServiceInstance> instances;
        try {
            instances = discoveryClient.getInstances(finalServiceName);
        } catch (RuntimeException ex) {
            item.setErrorCode(ErrorCode.NACOS_UNREACHABLE.code());
            item.setDiagnosis("Nacos访问失败: " + ex.getMessage());
            item.setSuggestion("请检查Nacos地址和网络连通性");
            items.add(item);
            return wizardFailed(request, "", ErrorCode.NACOS_UNREACHABLE.code(), "执行前检查失败: Nacos不可用或不可达", items);
        }

        int availableCount = instances == null ? 0 : instances.size();
        item.setInstanceCount(availableCount);
        if (availableCount <= 0) {
            item.setErrorCode(ErrorCode.ALG_SERVICE_NO_INSTANCE.code());
            item.setDiagnosis("服务[" + finalServiceName + "]在Nacos中没有可用实例");
            item.setSuggestion("请先启动算法服务，再点击检查");
            items.add(item);
            return wizardFailed(request, "", ErrorCode.ALG_SERVICE_NO_INSTANCE.code(),
                    "执行前检查失败: 服务[" + finalServiceName + "]在Nacos中无可用实例", items);
        }

        item.setReachable(true);
        item.setErrorCode("");
        item.setDiagnosis("服务可达，可执行");
        item.setSuggestion("");
        items.add(item);
        PlanPreCheckResult result = PlanPreCheckResult.passed(items);
        return buildWizardResponse(request, "", result);
    }

    @GetMapping("/getPlanLogs")
    public Map<String, Object> getPlanLogs(@RequestParam String planId,
                                           @RequestParam(required = false, defaultValue = "0") long afterSeq,
                                           @RequestParam(required = false, defaultValue = "200") int limit,
                                           @RequestParam(required = false) String executionId,
                                           @RequestParam(required = false, defaultValue = "latest") String scope,
                                           HttpServletRequest request) {
        if (!StringUtils.hasText(planId)) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        if (limit <= 0 || limit > 1000) {
            limit = 200;
        }
        ExePlan latestPlan = exePlanMgrService.getExePlanById(planId);
        boolean latestOnly = !"all".equalsIgnoreCase(scope);
        String effectiveExecutionId = resolveExecutionId(executionId, latestOnly, latestPlan);
        List<ExePlanLog> items = exePlanMgrService.getPlanLogs(planId, effectiveExecutionId, Math.max(afterSeq, 0L), limit);
        long nextSeq = afterSeq;
        if (items != null && !items.isEmpty()) {
            nextSeq = items.get(items.size() - 1).getSeq();
        } else {
            nextSeq = Math.max(nextSeq, exePlanMgrService.getLatestPlanLogSeq(planId, effectiveExecutionId));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("items", items == null ? Collections.emptyList() : items);
        data.put("nextSeq", nextSeq);
        data.put("executionId", effectiveExecutionId == null ? "" : effectiveExecutionId);
        data.put("planState", latestPlan == null ? null : latestPlan.getExeState());
        data.put("lastError", latestPlan == null ? null : latestPlan.getLastError());
        return ApiResponse.ok(request, data);
    }

    @GetMapping("/exportPlanLogs")
    public Map<String, Object> exportPlanLogs(@RequestParam String planId,
                                              @RequestParam(required = false) String executionId,
                                              @RequestParam(required = false, defaultValue = "latest") String scope,
                                              @RequestParam(required = false, defaultValue = "5000") int limit,
                                              HttpServletRequest request) {
        if (!StringUtils.hasText(planId)) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        int safeLimit = limit <= 0 ? 5000 : Math.min(limit, 20000);
        ExePlan latestPlan = exePlanMgrService.getExePlanById(planId);
        boolean latestOnly = !"all".equalsIgnoreCase(scope);
        String effectiveExecutionId = resolveExecutionId(executionId, latestOnly, latestPlan);
        List<ExePlanLog> items = exePlanMgrService.listPlanLogs(planId, effectiveExecutionId, safeLimit);
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("scope", latestOnly ? "latest" : "all");
        data.put("executionId", effectiveExecutionId == null ? "" : effectiveExecutionId);
        data.put("count", items == null ? 0 : items.size());
        data.put("items", items == null ? Collections.emptyList() : items);
        return ApiResponse.ok(request, data);
    }

    private int normalizePageNum(int pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }

    private String inferPreCheckErrorCode(String lastError) {
        if (!StringUtils.hasText(lastError)) {
            return "";
        }
        if (lastError.contains("无可用实例")) {
            return "ALG_SERVICE_NO_INSTANCE";
        }
        if (lastError.contains("算法服务未注册")) {
            return "ALG_SERVICE_NAME_EMPTY";
        }
        if (lastError.contains("Nacos不可用") || lastError.contains("Nacos")) {
            return "NACOS_UNREACHABLE";
        }
        return "";
    }

    private String normalizePreCheckErrorCode(String errorCode) {
        if (!StringUtils.hasText(errorCode)) {
            return ErrorCode.INTERNAL_ERROR.code();
        }
        if (ErrorCode.ALG_SERVICE_NO_INSTANCE.code().equals(errorCode)) {
            return ErrorCode.ALG_SERVICE_NO_INSTANCE.code();
        }
        if (ErrorCode.ALG_SERVICE_NAME_EMPTY.code().equals(errorCode)) {
            return ErrorCode.ALG_SERVICE_NAME_EMPTY.code();
        }
        if (ErrorCode.NACOS_UNREACHABLE.code().equals(errorCode)) {
            return ErrorCode.NACOS_UNREACHABLE.code();
        }
        if (ErrorCode.PLAN_NOT_FOUND.code().equals(errorCode)) {
            return ErrorCode.PLAN_NOT_FOUND.code();
        }
        if (ErrorCode.ALG_NOT_FOUND.code().equals(errorCode)) {
            return ErrorCode.ALG_NOT_FOUND.code();
        }
        if (ErrorCode.INVALID_ARGUMENT.code().equals(errorCode)) {
            return ErrorCode.INVALID_ARGUMENT.code();
        }
        return ErrorCode.INTERNAL_ERROR.code();
    }

    private Map<String, Object> wizardFailed(HttpServletRequest request, String planId, String errorCode,
                                             String message, List<PlanPreCheckItem> items) {
        PlanPreCheckResult failed = PlanPreCheckResult.failed(errorCode, message, items);
        return buildWizardResponse(request, planId, failed);
    }

    private Map<String, Object> buildWizardResponse(HttpServletRequest request, String planId, PlanPreCheckResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("pass", result != null && result.isPass());
        data.put("errorCode", result == null ? ErrorCode.INTERNAL_ERROR.code() : result.getErrorCode());
        data.put("message", result == null ? "执行前检查失败" : result.getMessage());
        data.put("items", result == null ? Collections.emptyList() : result.getItems());
        if (result == null || !result.isPass()) {
            String code = result == null ? ErrorCode.INTERNAL_ERROR.code() : normalizePreCheckErrorCode(result.getErrorCode());
            return ApiResponse.failed(request, 400, data.get("message").toString(), code, data);
        }
        return ApiResponse.ok(request, data, "执行前检查通过");
    }

    private String resolveExecutionId(String executionId, boolean latestOnly, ExePlan latestPlan) {
        if (StringUtils.hasText(executionId)) {
            return executionId.trim();
        }
        if (!latestOnly || latestPlan == null || !StringUtils.hasText(latestPlan.getExecutionId())) {
            return null;
        }
        return latestPlan.getExecutionId().trim();
    }

}
