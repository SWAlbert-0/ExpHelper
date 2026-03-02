package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanDeleteResult;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckItem;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckResult;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @Autowired
    PlatMgrService platMgrService;
    @Autowired
    ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.nacos.discovery.server-addr:localhost:8848}")
    private String nacosServerAddr;

    @GetMapping("/getExePlans")
    public List<ExePlan> getExePlans(@RequestParam int pageNum,
                                     @RequestParam int pageSize,
                                     @RequestParam(required = false) String scope,
                                     @RequestParam(required = false) String planName,
                                     @RequestParam(required = false) Integer exeState,
                                     @RequestParam(required = false) Long exeStartTime,
                                     @RequestParam(required = false) Long exeEndTime){
        List<ExePlan> plans = exePlanMgrService.getExePlans(
                normalizePageNum(pageNum),
                normalizePageSize(pageSize),
                scope,
                planName,
                exeState,
                exeStartTime,
                exeEndTime
        );
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExePlan> normalized = new ArrayList<>(plans.size());
        for (ExePlan plan : plans) {
            if (plan == null) {
                continue;
            }
            if (plan.getExeState() == fjnu.edu.exePlanMgr.Constant.Constant.IN_EXECUTION) {
                boolean repaired = exePlanMgrService.repairExecutionStateFromLogs(plan.getPlanId(), plan.getExecutionId());
                if (repaired) {
                    ExePlan latest = exePlanMgrService.getExePlanById(plan.getPlanId());
                    normalized.add(latest == null ? plan : latest);
                    continue;
                }
            }
            normalized.add(plan);
        }
        return normalized;

    }

    @PostMapping("/addExePlan")
    public String addExePlan(@RequestBody ExePlan exeplan, HttpServletRequest request) throws Exception {
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanName())) {
            throw new IllegalArgumentException("计划名称不能为空");
        }
        AuthUser auth = currentAuth(request);
        if (auth != null) {
            String ownerId = resolveAuthUserId(auth);
            if (StringUtils.hasText(ownerId)) {
                exeplan.setOwnerUserId(ownerId);
            }
            if (StringUtils.hasText(auth.getUserName())) {
                exeplan.setOwnerUserName(auth.getUserName().trim());
            }
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
    public Map<String, Object> deleteExePlanById(@RequestParam String planId, HttpServletRequest request){
        if (!StringUtils.hasText(planId)) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        ExePlan current = exePlanMgrService.getExePlanById(planId);
        AuthUser auth = currentAuth(request);
        if (!canManagePlan(auth, current)) {
            return ApiResponse.failed(request, 403, "仅计划创建者或管理员可删除", ErrorCode.AUTH_FORBIDDEN.code());
        }
        ExePlanDeleteResult deleteResult = exePlanMgrService.deleteExePlanById(planId);
        long deletedCount = deleteResult == null ? 0L : deleteResult.getDeletedCount();
        boolean existed = deleteResult != null && deleteResult.isExisted();
        boolean noop = deleteResult == null || deleteResult.isNoop();
        boolean verified = deleteResult == null || deleteResult.isVerified();
        boolean blocked = deleteResult != null && deleteResult.isBlocked();
        Map<String, Object> data = buildDeleteData(planId, deletedCount, existed, noop, verified, blocked);
        if (blocked) {
            return ApiResponse.ok(request, data, "删除失败，计划执行中");
        }
        if (noop && !verified) {
            return ApiResponse.failed(request, 500, "删除未生效，请刷新后重试", ErrorCode.INTERNAL_ERROR.code(), data);
        }
        if (noop) {
            return ApiResponse.ok(request, data, "记录已不存在，列表已同步");
        }
        return ApiResponse.ok(request, data, "删除成功");
    }

    @PostMapping("updateExePlanById")
    public Map<String, Object> updateExePlanById(@RequestBody ExePlan exeplan, HttpServletRequest request){
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        ExePlan current = exePlanMgrService.getExePlanById(exeplan.getPlanId());
        AuthUser auth = currentAuth(request);
        if (!canManagePlan(auth, current)) {
            return ApiResponse.failed(request, 403, "仅计划创建者或管理员可编辑", ErrorCode.AUTH_FORBIDDEN.code());
        }
        if (current != null) {
            // 避免更新时覆盖owner归属，保持“创建者可编辑、他人只读”的权限模型。
            exeplan.setOwnerUserId(current.getOwnerUserId());
            exeplan.setOwnerUserName(current.getOwnerUserName());
        }
        boolean ok = exePlanMgrService.updateExePlanById(exeplan);
        if (!ok) {
            return ApiResponse.failed(request, 500, "更新失败", ErrorCode.INTERNAL_ERROR.code());
        }
        return ApiResponse.ok(request, null, "更新成功");
    }

    @PostMapping("/updateExePlanByIdWithAuth")
    public Map<String, Object> updateExePlanByIdWithAuth(@RequestBody ExePlan exeplan, HttpServletRequest request){
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        ExePlan current = exePlanMgrService.getExePlanById(exeplan.getPlanId());
        AuthUser auth = currentAuth(request);
        if (!canManagePlan(auth, current)) {
            return ApiResponse.failed(request, 403, "仅计划创建者或管理员可编辑", ErrorCode.AUTH_FORBIDDEN.code());
        }
        if (current != null) {
            // 防止非预期覆盖创建者归属
            exeplan.setOwnerUserId(current.getOwnerUserId());
            exeplan.setOwnerUserName(current.getOwnerUserName());
        }
        boolean ok = exePlanMgrService.updateExePlanById(exeplan);
        if (!ok) {
            return ApiResponse.failed(request, 500, "更新失败", ErrorCode.INTERNAL_ERROR.code());
        }
        return ApiResponse.ok(request, null, "更新成功");
    }

    @GetMapping("/countAllExePlans")
    public long countAllExePlans(@RequestParam(required = false) String scope,
                                 @RequestParam(required = false) String planName,
                                 @RequestParam(required = false) Integer exeState,
                                 @RequestParam(required = false) Long exeStartTime,
                                 @RequestParam(required = false) Long exeEndTime) {
        long count = exePlanMgrService.countAllExePlans(scope, planName, exeState, exeStartTime, exeEndTime);
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
        AuthUser auth = currentAuth(request);
        if (!canManagePlan(auth, exePlan)) {
            return ApiResponse.failed(request, 403, "仅计划创建者或管理员可执行", ErrorCode.AUTH_FORBIDDEN.code());
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
            NacosInstanceStats stats = queryNacosInstanceStats(finalServiceName);
            if (stats.totalCount > 0 && stats.healthyCount <= 0) {
                item.setDiagnosis("服务[" + finalServiceName + "]存在实例，但全部不健康");
                item.setSuggestion("请在算法库管理-源码弹窗中重新构建并启动算法服务，确保Nacos心跳正常上报");
            } else {
                item.setDiagnosis("服务[" + finalServiceName + "]在Nacos中没有可用实例");
                item.setSuggestion("请先启动算法服务，再点击检查");
            }
            items.add(item);
            return wizardFailed(request, "", ErrorCode.ALG_SERVICE_NO_INSTANCE.code(),
                    "执行前检查失败: 服务[" + finalServiceName + "]在Nacos中无可用实例", items);
        }

        item.setReachable(true);
        item.setErrorCode("");
        item.setDiagnosis("服务可达，可执行");
        item.setSuggestion("");
        items.add(item);

        String runtimeType = normalizeRuntimeType(algInfo.getRuntimeType());
        if ("python".equals(runtimeType)) {
            PlanPreCheckItem apiItem = new PlanPreCheckItem();
            apiItem.setAlgId(algId);
            apiItem.setAlgName(algInfo.getAlgName());
            apiItem.setServiceName(finalServiceName);
            apiItem.setReachable(false);
            apiItem.setInstanceCount(availableCount);
            String endpoint = selectProbeEndpoint(instances);
            if (!StringUtils.hasText(endpoint)) {
                apiItem.setErrorCode(ErrorCode.ALG_ENDPOINT_UNREACHABLE.code());
                apiItem.setDiagnosis("无法构建算法健康探测地址");
                apiItem.setSuggestion("请确认算法服务实例在Nacos中包含可访问主机与端口");
                items.add(apiItem);
                return wizardFailed(request, "", ErrorCode.ALG_ENDPOINT_UNREACHABLE.code(),
                        "执行前检查失败: 算法服务健康接口不可用", items);
            }
            String err = probeMyAlgEndpoint(endpoint);
            if (StringUtils.hasText(err)) {
                apiItem.setErrorCode(ErrorCode.ALG_ENDPOINT_UNREACHABLE.code());
                apiItem.setDiagnosis("健康检查失败: " + err);
                apiItem.setSuggestion("请确认算法服务提供 GET /myAlg/ 且返回 2xx");
                items.add(apiItem);
                return wizardFailed(request, "", ErrorCode.ALG_ENDPOINT_UNREACHABLE.code(),
                        "执行前检查失败: 算法服务接口不可用", items);
            }
            apiItem.setReachable(true);
            apiItem.setErrorCode("");
            apiItem.setDiagnosis("健康接口可达: " + endpoint + "/myAlg/");
            apiItem.setSuggestion("");
            items.add(apiItem);
        }
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
        if (ErrorCode.ALG_ENDPOINT_UNREACHABLE.code().equals(errorCode)) {
            return ErrorCode.ALG_ENDPOINT_UNREACHABLE.code();
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

    private String normalizeRuntimeType(String runtimeType) {
        if (!StringUtils.hasText(runtimeType)) {
            return "java";
        }
        return "python".equalsIgnoreCase(runtimeType.trim()) ? "python" : "java";
    }

    private String selectProbeEndpoint(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return "";
        }
        for (ServiceInstance instance : instances) {
            if (instance == null) {
                continue;
            }
            URI uri = instance.getUri();
            if (uri != null && StringUtils.hasText(uri.toString())) {
                String text = uri.toString();
                return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
            }
            if (StringUtils.hasText(instance.getHost()) && instance.getPort() > 0) {
                return "http://" + instance.getHost() + ":" + instance.getPort();
            }
        }
        return "";
    }

    private String probeMyAlgEndpoint(String endpoint) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint + "/myAlg/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return "";
            }
            return "HTTP " + code;
        } catch (Exception ex) {
            return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private NacosInstanceStats queryNacosInstanceStats(String serviceName) {
        NacosInstanceStats stats = new NacosInstanceStats();
        if (!StringUtils.hasText(serviceName)) {
            return stats;
        }
        HttpURLConnection conn = null;
        try {
            String addr = StringUtils.hasText(nacosServerAddr) ? nacosServerAddr.trim() : "localhost:8848";
            if (!addr.startsWith("http://") && !addr.startsWith("https://")) {
                addr = "http://" + addr;
            }
            String encodedService = URLEncoder.encode(serviceName, StandardCharsets.UTF_8);
            String url = addr + "/nacos/v1/ns/instance/list?serviceName=" + encodedService + "&groupName=DEFAULT_GROUP&namespaceId=public";
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() != 200) {
                return stats;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                ObjectMapper mapper = objectMapper == null ? new ObjectMapper() : objectMapper;
                JsonNode root = mapper.readTree(body.toString());
                JsonNode hosts = root.get("hosts");
                if (hosts == null || !hosts.isArray()) {
                    return stats;
                }
                for (JsonNode host : hosts) {
                    stats.totalCount++;
                    boolean healthy = host.path("healthy").asBoolean(false);
                    boolean enabled = host.path("enabled").asBoolean(true);
                    if (healthy && enabled) {
                        stats.healthyCount++;
                    }
                }
            }
        } catch (Exception ignored) {
            return stats;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return stats;
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

    private Map<String, Object> buildDeleteData(String planId, long deletedCount, boolean existed, boolean noop,
                                                boolean verified, boolean blocked) {
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("deletedCount", deletedCount);
        data.put("existed", existed);
        data.put("noop", noop);
        data.put("verified", verified);
        data.put("blocked", blocked);
        return data;
    }

    private AuthUser currentAuth(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object authObj = request.getAttribute("authUser");
        if (authObj instanceof AuthUser) {
            return (AuthUser) authObj;
        }
        return null;
    }

    private String resolveAuthUserId(AuthUser auth) {
        if (auth == null) {
            return "";
        }
        if (StringUtils.hasText(auth.getUserId())) {
            return auth.getUserId().trim();
        }
        if (StringUtils.hasText(auth.getUserName())) {
            UserInfo user = platMgrService.getUserByName(auth.getUserName());
            if (user != null && StringUtils.hasText(user.getUserId())) {
                return user.getUserId().trim();
            }
        }
        return "";
    }

    private boolean canManagePlan(AuthUser auth, ExePlan plan) {
        if (auth == null) {
            return false;
        }
        if (auth.getRole() != null && auth.getRole() == 1) {
            return true;
        }
        if (plan == null || !StringUtils.hasText(plan.getOwnerUserId())) {
            // 历史无owner计划：仅管理员可操作
            return false;
        }
        String authUserId = resolveAuthUserId(auth);
        return StringUtils.hasText(authUserId) && authUserId.equals(plan.getOwnerUserId());
    }

    private static final class NacosInstanceStats {
        private int totalCount = 0;
        private int healthyCount = 0;
    }

}
