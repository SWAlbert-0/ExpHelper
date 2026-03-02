package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.algruntime.exception.AlgBuildValidationException;
import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.algruntime.service.AlgBuildTaskService;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/AlgController")
public class AlgLibMgrCtrl {
    @Autowired
    AlgLibMgrService algLibMgrService;

    @Autowired
    ExePlanMgrService exePlanMgrService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AlgBuildTaskService algBuildTaskService;
    @Autowired
    PlatMgrService platMgrService;

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.nacos.discovery.server-addr:localhost:8848}")
    private String nacosServerAddr;

    @org.springframework.beans.factory.annotation.Value("${alg.runtime.image-retain-count:3}")
    private int imageRetainCount;

    @PostMapping("/addAlg")
    public Map<String, Object> addAlgInfo (@RequestBody AlgInfo algInfo, HttpServletRequest request) {
        normalizeAlgInfo(algInfo);
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgName())) {
            return ApiResponse.failed(request, 400, "算法名称不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo algInfo1 =algLibMgrService.getAlgInfoByName(algInfo.getAlgName());
        if(algInfo1!= null ){
            return ApiResponse.failed(request, 400, "算法名不能重复", ErrorCode.INVALID_ARGUMENT.code());
        }else {
            bindOwner(algInfo, currentAuth(request));
            algLibMgrService.addAlgInfo(algInfo);
            return ApiResponse.ok(request, algInfo.getAlgName(), "添加成功");
        }

    }

    @PostMapping("/importAlgsJson")
    public Map<String, Object> importAlgsJson(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String jsonText = payload == null ? "" : String.valueOf(payload.getOrDefault("jsonText", ""));
        if (!StringUtils.hasText(jsonText)) {
            return ApiResponse.failed(request, 400, "JSON内容不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            List<JsonNode> nodes = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(nodes::add);
            } else if (root.isObject() && root.has("items") && root.get("items").isArray()) {
                root.get("items").forEach(nodes::add);
            } else if (root.isObject()) {
                nodes.add(root);
            } else {
                return ApiResponse.failed(request, 400, "JSON结构不合法，需为对象、数组或包含items数组的对象", ErrorCode.INVALID_ARGUMENT.code());
            }

            int success = 0;
            int skipped = 0;
            int failed = 0;
            List<Map<String, Object>> failures = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                JsonNode item = nodes.get(i);
                try {
                    AlgInfo algInfo = objectMapper.convertValue(item, AlgInfo.class);
                    normalizeAlgInfo(algInfo);
                    if (algInfo == null || !StringUtils.hasText(algInfo.getAlgName())) {
                        throw new IllegalArgumentException("algName不能为空");
                    }
                    if (!StringUtils.hasText(algInfo.getServiceName())) {
                        throw new IllegalArgumentException("serviceName不能为空");
                    }
                    if (algLibMgrService.getAlgInfoByName(algInfo.getAlgName()) != null) {
                        skipped++;
                        continue;
                    }
                    bindOwner(algInfo, currentAuth(request));
                    algLibMgrService.addAlgInfo(algInfo);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    Map<String, Object> row = new HashMap<>();
                    row.put("index", i);
                    row.put("algName", item.has("algName") ? item.get("algName").asText("") : "");
                    row.put("reason", ex.getMessage());
                    failures.add(row);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", nodes.size());
            data.put("success", success);
            data.put("skipped", skipped);
            data.put("failed", failed);
            data.put("failures", failures);
            return ApiResponse.ok(request, data, "算法JSON导入完成");
        } catch (Exception ex) {
            return ApiResponse.failed(request, 400, "JSON解析失败: " + ex.getMessage(), ErrorCode.INVALID_ARGUMENT.code());
        }
    }

    @PostMapping("/uploadSource")
    public Map<String, Object> uploadSource(@RequestParam("algId") String algId,
                                            @RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        if (!StringUtils.hasText(algId)) {
            return ApiResponse.failed(request, 400, "algId不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo algInfo = algLibMgrService.getAlgInfoById(algId);
        if (algInfo == null) {
            return ApiResponse.failed(request, 404, "算法不存在", ErrorCode.ALG_NOT_FOUND.code());
        }
        if (!canManageAlg(currentAuth(request), algInfo)) {
            return ApiResponse.failed(request, 403, "仅算法创建者或管理员可上传源码", ErrorCode.AUTH_FORBIDDEN.code());
        }
        try {
            AlgBuildTask task = algBuildTaskService.createUploadTask(algInfo, file, request.getHeader("X-Trace-Id"));
            return ApiResponse.ok(request, task, "源码上传成功");
        } catch (AlgBuildValidationException ex) {
            Map<String, Object> data = new HashMap<>();
            data.put("phase", ex.getPhase());
            data.put("fixHints", ex.getFixHints());
            data.put("contractCheck", ex.getContractCheck());
            return ApiResponse.failed(request, 400, ex.getMessage(), ErrorCode.ALG_BUILD_CONTRACT_INVALID.code(), data);
        } catch (Exception ex) {
            return ApiResponse.failed(request, 400, "源码上传失败: " + ex.getMessage(), ErrorCode.INVALID_ARGUMENT.code());
        }
    }

    @PostMapping("/buildAndStart")
    public Map<String, Object> buildAndStart(@RequestParam("taskId") String taskId, HttpServletRequest request) {
        if (!StringUtils.hasText(taskId)) {
            return ApiResponse.failed(request, 400, "taskId不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        try {
            AlgBuildTask existing = algBuildTaskService.getTask(taskId);
            if (existing != null && StringUtils.hasText(existing.getAlgId())) {
                AlgInfo algInfo = algLibMgrService.getAlgInfoById(existing.getAlgId());
                if (algInfo != null && !canManageAlg(currentAuth(request), algInfo)) {
                    return ApiResponse.failed(request, 403, "仅算法创建者或管理员可执行构建启动", ErrorCode.AUTH_FORBIDDEN.code());
                }
            }
            AlgBuildTask task = algBuildTaskService.triggerBuild(taskId, request.getHeader("X-Trace-Id"));
            return ApiResponse.ok(request, task, "构建任务已启动");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.failed(request, 404, ex.getMessage(), ErrorCode.INVALID_ARGUMENT.code());
        } catch (Exception ex) {
            return ApiResponse.failed(request, 500, "构建任务启动失败: " + ex.getMessage(), ErrorCode.INTERNAL_ERROR.code());
        }
    }

    @GetMapping("/buildStatus")
    public Map<String, Object> getBuildStatus(@RequestParam("taskId") String taskId, HttpServletRequest request) {
        if (!StringUtils.hasText(taskId)) {
            return ApiResponse.failed(request, 400, "taskId不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgBuildTask task = algBuildTaskService.getTask(taskId);
        if (task == null) {
            return ApiResponse.failed(request, 404, "构建任务不存在", ErrorCode.INVALID_ARGUMENT.code());
        }
        return ApiResponse.ok(request, task, "获取构建状态成功");
    }

    @GetMapping("/buildLogs")
    public Map<String, Object> getBuildLogs(@RequestParam("taskId") String taskId,
                                            @RequestParam(value = "tail", required = false, defaultValue = "200") int tail,
                                            HttpServletRequest request) {
        if (!StringUtils.hasText(taskId)) {
            return ApiResponse.failed(request, 400, "taskId不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgBuildTask task = algBuildTaskService.getTask(taskId);
        if (task == null) {
            return ApiResponse.failed(request, 404, "构建任务不存在", ErrorCode.INVALID_ARGUMENT.code());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("task", task);
        data.put("logs", algBuildTaskService.tailLog(taskId, tail));
        return ApiResponse.ok(request, data, "获取构建日志成功");
    }

    @GetMapping("/sourceRuntimeInfo")
    public Map<String, Object> getSourceRuntimeInfo(@RequestParam("algId") String algId, HttpServletRequest request) {
        if (!StringUtils.hasText(algId)) {
            return ApiResponse.failed(request, 400, "algId不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo algInfo = algLibMgrService.getAlgInfoById(algId);
        if (algInfo == null) {
            return ApiResponse.failed(request, 404, "算法不存在", ErrorCode.ALG_NOT_FOUND.code());
        }
        AlgBuildTask latestTask = algBuildTaskService.getLatestTaskByAlgId(algId);
        boolean manageable = canManageAlg(currentAuth(request), algInfo);
        Map<String, Object> data = buildSourceRuntimeData(algInfo, latestTask, manageable, true);
        return ApiResponse.ok(request, data, "获取运行信息成功");
    }

    @PostMapping("/sourceRuntimeOperate")
    public Map<String, Object> sourceRuntimeOperate(@RequestParam("algId") String algId,
                                                    @RequestParam("action") String action,
                                                    HttpServletRequest request) {
        if (!StringUtils.hasText(algId) || !StringUtils.hasText(action)) {
            return ApiResponse.failed(request, 400, "algId/action不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        if (!isSupportedRuntimeAction(normalizedAction)) {
            return ApiResponse.failed(request, 400, "不支持的动作: " + normalizedAction, ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo algInfo = algLibMgrService.getAlgInfoById(algId);
        if (algInfo == null) {
            return ApiResponse.failed(request, 404, "算法不存在", ErrorCode.ALG_NOT_FOUND.code());
        }
        if (!canManageAlg(currentAuth(request), algInfo)) {
            return ApiResponse.failed(request, 403, "仅算法创建者或管理员可执行该操作", ErrorCode.AUTH_FORBIDDEN.code());
        }
        AlgBuildTask latestTask = algBuildTaskService.getLatestTaskByAlgId(algId);
        RuntimeTarget target = resolveRuntimeTarget(algInfo, latestTask, true);

        try {
            Map<String, Object> opResult = new HashMap<>();
            opResult.put("action", normalizedAction);
            if ("OFFLINE".equals(normalizedAction)) {
                if (!target.exists || !target.running) {
                    return ApiResponse.failed(request, 409, "当前容器未运行，无需下线", ErrorCode.INVALID_ARGUMENT.code());
                }
                execCmd("docker", "stop", target.containerName);
                opResult.put("message", "下线成功");
            } else if ("ONLINE".equals(normalizedAction)) {
                if (!target.exists) {
                    return ApiResponse.failed(request, 409, "容器不存在，请先执行构建并启动", ErrorCode.INVALID_ARGUMENT.code());
                }
                execCmd("docker", "start", target.containerName);
                opResult.put("message", "上线成功");
            } else if ("RESTART".equals(normalizedAction)) {
                if (!target.exists) {
                    return ApiResponse.failed(request, 409, "容器不存在，请先执行构建并启动", ErrorCode.INVALID_ARGUMENT.code());
                }
                execCmd("docker", "restart", target.containerName);
                opResult.put("message", "重启成功");
            } else if ("PRUNE_IMAGES".equals(normalizedAction)) {
                opResult.putAll(pruneAlgResources(algId, target.containerName));
                opResult.put("message", "旧资源清理完成");
            }
            Map<String, Object> data = buildSourceRuntimeData(algInfo, algBuildTaskService.getLatestTaskByAlgId(algId), true, false);
            data.put("operation", opResult);
            return ApiResponse.ok(request, data, String.valueOf(opResult.get("message")));
        } catch (Exception ex) {
            return ApiResponse.failed(request, 500, "执行失败: " + ex.getMessage(), ErrorCode.INTERNAL_ERROR.code());
        }
    }

    @PostMapping("/deleteAlgById")
    public Map<String, Object> deleteAlgInfoById(@RequestParam(value = "algId") String algId, HttpServletRequest request) {
        if (!StringUtils.hasText(algId)) {
            return ApiResponse.failed(request, 400, "算法ID不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo current = algLibMgrService.getAlgInfoById(algId);
        if (current != null && !canManageAlg(currentAuth(request), current)) {
            return ApiResponse.failed(request, 403, "仅算法创建者或管理员可删除", ErrorCode.AUTH_FORBIDDEN.code());
        }
        long refPlanCount = exePlanMgrService.countPlansByAlgId(algId);
        if (refPlanCount > 0) {
            Map<String, Object> data = buildDeleteData(algId, 0L, false, false, true, true, refPlanCount,
                    exePlanMgrService.listPlanNamesByAlgId(algId, 5));
            return ApiResponse.failed(request, 409, "删除失败，算法已被执行计划引用，请先解除关联", ErrorCode.ALG_IN_USE.code(), data);
        }
        AlgDeleteResult deleteResult = algLibMgrService.deleteAlgInfoById(algId);
        long deletedCount = deleteResult == null ? 0L : deleteResult.getDeletedCount();
        boolean repaired = deleteResult != null && deleteResult.isRepaired();
        boolean noop = deleteResult == null || deleteResult.isNoop();
        boolean verified = deleteResult == null || deleteResult.isVerified();
        Map<String, Object> data = buildDeleteData(algId, deletedCount, deletedCount > 0, repaired, noop, verified, 0L, Collections.emptyList());
        if (noop && !verified) {
            return ApiResponse.failed(request, 500, "删除未生效，请刷新后重试", ErrorCode.INTERNAL_ERROR.code(), data);
        }
        return ApiResponse.ok(request, data, "删除成功");
    }

    @GetMapping("/getAlgById")
    public AlgInfo getAlgInfoById(@RequestParam(value = "algId") String algId) {
        if (!StringUtils.hasText(algId)) {
            return null;
        }
        AlgInfo algInfo1 = algLibMgrService.getAlgInfoById(algId);
        return algInfo1;
    }

    @GetMapping("/getAlgsByName")
    public  List<AlgInfo>  getAlgInfoByName(@RequestParam(value = "algName") String algName,
                                            @RequestParam(value = "pageNum") int pageNum,
                                            @RequestParam(value = "pageSize" ) int pageSize) {
        if (!StringUtils.hasText(algName)) {
            return Collections.emptyList();
        }
        List<AlgInfo> algInfos = algLibMgrService.getAlgInfoByName(algName, normalizePageNum(pageNum), normalizePageSize(pageSize));
        return algInfos == null ? Collections.emptyList() : algInfos;
    }

    @GetMapping("/getAlgs")
    public List<AlgInfo> getAlgInfos(@RequestParam(value = "pageNum") int pageNum,
                                     @RequestParam(value = "pageSize" ) int pageSize) {
        List<AlgInfo> algInfos = algLibMgrService.getAlgInfos(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return algInfos == null ? Collections.emptyList() : algInfos;
    }

    @PostMapping("/updateAlgById")
    public Map<String, Object> updateAlgInfoById(@RequestBody AlgInfo algInfo, HttpServletRequest request) {
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgId())) {
            return ApiResponse.failed(request, 400, "算法ID不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo current = algLibMgrService.getAlgInfoById(algInfo.getAlgId());
        if (current == null) {
            return ApiResponse.failed(request, 404, "算法不存在", ErrorCode.ALG_NOT_FOUND.code());
        }
        if (!canManageAlg(currentAuth(request), current)) {
            return ApiResponse.failed(request, 403, "仅算法创建者或管理员可编辑", ErrorCode.AUTH_FORBIDDEN.code());
        }
        algInfo.setAlgName(trimOrEmpty(algInfo.getAlgName()));
        algInfo.setServiceName(trimOrEmpty(algInfo.getServiceName()));
        algInfo.setRuntimeType(normalizeRuntimeType(algInfo.getRuntimeType()));
        algInfo.setDescription(trimOrEmpty(algInfo.getDescription()));
        algInfo.setOwnerUserId(current.getOwnerUserId());
        algInfo.setOwnerUserName(current.getOwnerUserName());
        algLibMgrService.updateAlgInfoById(algInfo);
        return ApiResponse.ok(request, null, "更新成功");
    }

    @GetMapping("/getParaByAlgId")
    public List<DefPara> getParasByAlgInfoId(@RequestParam(value = "algId") String algId) {
        if (!StringUtils.hasText(algId)) {
            return Collections.emptyList();
        }
        List<DefPara> defParas = algLibMgrService.getParasByAlgInfoId(algId);
        return defParas == null ? Collections.emptyList() : defParas;
    }

    @GetMapping("/countAllAlgs")
    public long countAllProbInsts() {
        long count = algLibMgrService.countAllAlgs();
        return count;
    }

    @GetMapping("/countAlgsByAlgName")
    public long countProbInstsByInstName(@RequestParam(value = "algName") String algName) {
        if (!StringUtils.hasText(algName)) {
            return 0;
        }
        long count = algLibMgrService.countAlgsByAlgName(algName);
        return count;
    }

    @PostMapping("/generateDeployTemplate")
    public Map<String, Object> generateDeployTemplate(@RequestParam(value = "algId") String algId,
                                                       HttpServletRequest request) {
        if (!StringUtils.hasText(algId)) {
            return ApiResponse.failed(request, 400, "算法ID不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        AlgInfo algInfo = algLibMgrService.getAlgInfoById(algId);
        if (algInfo == null) {
            return ApiResponse.failed(request, 404, "算法不存在", ErrorCode.ALG_NOT_FOUND.code());
        }
        String serviceName = StringUtils.hasText(algInfo.getServiceName()) ? algInfo.getServiceName().trim() : "replace-service-name";
        String imageName = "replace-your-algorithm-image";

        Map<String, Object> data = new HashMap<>();
        data.put("algId", algInfo.getAlgId());
        data.put("algName", algInfo.getAlgName());
        data.put("serviceName", serviceName);
        data.put("runtimeType", normalizeRuntimeType(algInfo.getRuntimeType()));
        data.put("composeYaml", buildComposeYaml(imageName, serviceName));
        data.put("envTemplate", "NACOS_SERVER_ADDR=host.docker.internal:8848\nNACOS_NAMESPACE=public\nNACOS_GROUP=DEFAULT_GROUP");
        data.put("runCommand", "docker compose -f docker-compose.algorithm.yml --env-file .env.algorithm up -d");
        data.put("verifyCommand", "curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=" + serviceName + "&groupName=DEFAULT_GROUP&namespaceId=public");
        data.put("notes", "请确保算法服务内部监听端口与 compose ports 一致，并已在应用配置中启用 Nacos discovery");
        return ApiResponse.ok(request, data, "部署模板生成成功");
    }

    private int normalizePageNum(int pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }

    private Map<String, Object> buildDeleteData(String algId, long deletedCount, boolean existed, boolean repaired,
                                                boolean noop, boolean verified, long refPlanCount, List<String> refPlanNames) {
        Map<String, Object> data = new HashMap<>();
        data.put("algId", algId);
        data.put("deletedCount", deletedCount);
        data.put("existed", existed);
        data.put("repaired", repaired);
        data.put("noop", noop);
        data.put("verified", verified);
        data.put("blocked", refPlanCount > 0);
        data.put("refPlanCount", refPlanCount);
        data.put("refPlanNames", refPlanNames == null ? Collections.emptyList() : refPlanNames);
        return data;
    }

    private String buildComposeYaml(String imageName, String serviceName) {
        return "services:\n" +
                "  " + serviceName + ":\n" +
                "    image: " + imageName + "\n" +
                "    container_name: " + serviceName + "\n" +
                "    environment:\n" +
                "      - NACOS_SERVER_ADDR=${NACOS_SERVER_ADDR}\n" +
                "      - NACOS_NAMESPACE=${NACOS_NAMESPACE:-public}\n" +
                "      - NACOS_GROUP=${NACOS_GROUP:-DEFAULT_GROUP}\n" +
                "      - SPRING_APPLICATION_NAME=" + serviceName + "\n" +
                "    ports:\n" +
                "      - \"18082:18082\"\n" +
                "    restart: unless-stopped\n";
    }

    private void normalizeAlgInfo(AlgInfo algInfo) {
        if (algInfo == null) {
            return;
        }
        algInfo.setAlgId(null);
        algInfo.setOwnerUserId(trimOrEmpty(algInfo.getOwnerUserId()));
        algInfo.setOwnerUserName(trimOrEmpty(algInfo.getOwnerUserName()));
        algInfo.setAlgName(trimOrEmpty(algInfo.getAlgName()));
        algInfo.setServiceName(trimOrEmpty(algInfo.getServiceName()));
        algInfo.setRuntimeType(normalizeRuntimeType(algInfo.getRuntimeType()));
        algInfo.setDescription(trimOrEmpty(algInfo.getDescription()));
        if (algInfo.getDefParas() != null) {
            for (int i = 0; i < algInfo.getDefParas().size(); i++) {
                DefPara para = algInfo.getDefParas().get(i);
                if (para == null) {
                    continue;
                }
                para.setParaId(i + 1);
                para.setParaName(trimOrEmpty(para.getParaName()));
                para.setParaType(trimOrEmpty(para.getParaType()));
                para.setParaValue(trimOrEmpty(para.getParaValue()));
                para.setDescription(trimOrEmpty(para.getDescription()));
            }
        }
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private String normalizeRuntimeType(String runtimeType) {
        if (!StringUtils.hasText(runtimeType)) {
            return "java";
        }
        String normalized = runtimeType.trim().toLowerCase(Locale.ROOT);
        if ("python".equals(normalized)) {
            return "python";
        }
        return "java";
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

    private void bindOwner(AlgInfo algInfo, AuthUser auth) {
        if (algInfo == null || auth == null) {
            return;
        }
        String ownerId = resolveAuthUserId(auth);
        if (StringUtils.hasText(ownerId)) {
            algInfo.setOwnerUserId(ownerId);
        }
        if (StringUtils.hasText(auth.getUserName())) {
            algInfo.setOwnerUserName(auth.getUserName().trim());
        }
    }

    private String resolveAuthUserId(AuthUser auth) {
        if (auth == null) {
            return "";
        }
        if (StringUtils.hasText(auth.getUserId())) {
            return auth.getUserId().trim();
        }
        if (StringUtils.hasText(auth.getUserName())) {
            UserInfo user = platMgrService.getUserByName(auth.getUserName().trim());
            if (user != null && StringUtils.hasText(user.getUserId())) {
                return user.getUserId().trim();
            }
        }
        return "";
    }

    private boolean canManageAlg(AuthUser auth, AlgInfo algInfo) {
        if (auth == null) {
            return false;
        }
        if (auth.getRole() != null && auth.getRole() == 1) {
            return true;
        }
        if (algInfo == null || !StringUtils.hasText(algInfo.getOwnerUserId())) {
            return false;
        }
        String authUserId = resolveAuthUserId(auth);
        return StringUtils.hasText(authUserId) && authUserId.equals(algInfo.getOwnerUserId());
    }

    private Map<String, Object> buildSourceRuntimeData(AlgInfo algInfo,
                                                       AlgBuildTask latestTask,
                                                       boolean manageable,
                                                       boolean tryMigrate) {
        Map<String, Object> data = new HashMap<>();
        String algId = algInfo == null ? "" : trimOrEmpty(algInfo.getAlgId());
        String serviceName = algInfo == null ? "" : trimOrEmpty(algInfo.getServiceName());

        data.put("algId", algId);
        data.put("algName", algInfo == null ? "" : trimOrEmpty(algInfo.getAlgName()));
        data.put("serviceName", serviceName);
        data.put("runtimeType", normalizeRuntimeType(algInfo == null ? "" : algInfo.getRuntimeType()));
        data.put("expectedContainerName", buildContainerName(serviceName, algId));

        if (latestTask == null) {
            data.put("hasBuildTask", false);
        } else {
            data.put("hasBuildTask", true);
            data.put("taskId", latestTask.getTaskId());
            data.put("taskStatus", trimOrEmpty(latestTask.getStatus()));
            data.put("taskPhase", trimOrEmpty(latestTask.getPhase()));
            data.put("taskErrorCode", trimOrEmpty(latestTask.getErrorCode()));
            data.put("taskErrorMessage", trimOrEmpty(latestTask.getErrorMessage()));
            data.put("imageName", trimOrEmpty(latestTask.getImageName()));
            data.put("containerNameFromTask", trimOrEmpty(latestTask.getContainerName()));
            data.put("createdAt", latestTask.getCreatedAt());
            data.put("startedAt", latestTask.getStartedAt());
            data.put("finishedAt", latestTask.getFinishedAt());
        }

        RuntimeTarget target = resolveRuntimeTarget(algInfo, latestTask, tryMigrate);
        data.put("containerName", target.containerName);
        data.put("containerExists", target.exists);
        data.put("containerRunning", target.running);
        data.put("containerStatus", target.containerInfo.getOrDefault("status", ""));
        data.put("containerImage", target.containerInfo.getOrDefault("image", ""));
        data.put("containerStartedAt", target.containerInfo.getOrDefault("startedAt", ""));
        data.put("containerPorts", target.containerInfo.getOrDefault("ports", ""));
        data.put("migrationInfo", target.migrationInfo);

        long healthyCount = queryNacosHealthyCount(serviceName);
        data.put("nacosHealthyCount", healthyCount);
        data.put("canOperate", manageable);
        data.put("canOffline", manageable && target.exists && target.running);
        data.put("canOnline", manageable && target.exists && !target.running);
        data.put("canRestart", manageable && target.exists);
        data.put("canPruneImages", manageable);

        if (!target.exists) {
            data.put("message", latestTask == null ? "暂无构建记录，请先上传源码并构建" : "未检测到运行容器，可先构建或执行上线");
        } else if (healthyCount <= 0) {
            data.put("message", "容器已存在，但 Nacos 暂无健康实例");
        } else {
            data.put("message", "");
        }
        return data;
    }

    private RuntimeTarget resolveRuntimeTarget(AlgInfo algInfo, AlgBuildTask latestTask, boolean tryMigrate) {
        String algId = algInfo == null ? "" : trimOrEmpty(algInfo.getAlgId());
        String serviceName = algInfo == null ? "" : trimOrEmpty(algInfo.getServiceName());
        String expectedName = buildContainerName(serviceName, algId);
        RuntimeTarget target = new RuntimeTarget();
        target.expectedContainerName = expectedName;

        Map<String, String> expected = inspectContainer(expectedName);
        if (Boolean.parseBoolean(expected.getOrDefault("exists", "false"))) {
            target.containerName = expectedName;
            target.containerInfo = expected;
            target.exists = true;
            target.running = Boolean.parseBoolean(expected.getOrDefault("running", "false"));
            return target;
        }

        Set<String> candidates = new LinkedHashSet<>();
        if (latestTask != null && StringUtils.hasText(latestTask.getContainerName())) {
            candidates.add(latestTask.getContainerName().trim());
        }
        if (StringUtils.hasText(algId)) {
            candidates.add("c_user_alg_" + safeToken(algId));
        }
        String serviceMatched = findContainerByServiceName(serviceName);
        if (StringUtils.hasText(serviceMatched)) {
            candidates.add(serviceMatched.trim());
        }

        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate) || expectedName.equals(candidate)) {
                continue;
            }
            Map<String, String> info = inspectContainer(candidate);
            if (!Boolean.parseBoolean(info.getOrDefault("exists", "false"))) {
                continue;
            }

            String effectiveName = candidate;
            if (tryMigrate) {
                try {
                    execCmd("docker", "rename", candidate, expectedName);
                    target.migrationInfo.put("migrated", true);
                    target.migrationInfo.put("from", candidate);
                    target.migrationInfo.put("to", expectedName);
                    effectiveName = expectedName;
                    if (latestTask != null && StringUtils.hasText(latestTask.getTaskId())) {
                        algBuildTaskService.updateContainerName(latestTask.getTaskId(), expectedName);
                    }
                    info = inspectContainer(effectiveName);
                } catch (Exception ex) {
                    target.migrationInfo.put("migrated", false);
                    target.migrationInfo.put("from", candidate);
                    target.migrationInfo.put("to", expectedName);
                    target.migrationInfo.put("error", ex.getMessage());
                }
            }

            target.containerName = effectiveName;
            target.containerInfo = info;
            target.exists = Boolean.parseBoolean(info.getOrDefault("exists", "false"));
            target.running = Boolean.parseBoolean(info.getOrDefault("running", "false"));
            return target;
        }

        target.containerName = expectedName;
        target.containerInfo = expected;
        target.exists = false;
        target.running = false;
        return target;
    }

    private boolean isSupportedRuntimeAction(String action) {
        return "OFFLINE".equals(action)
                || "ONLINE".equals(action)
                || "RESTART".equals(action)
                || "PRUNE_IMAGES".equals(action);
    }

    private String buildContainerName(String serviceName, String algId) {
        String serviceToken = safeToken(StringUtils.hasText(serviceName) ? serviceName.toLowerCase(Locale.ROOT) : "unknown");
        String idToken = safeToken(algId);
        String shortId = idToken.length() <= 8 ? idToken : idToken.substring(0, 8);
        return "c_alg_" + serviceToken + "_" + shortId;
    }

    private String safeToken(String text) {
        if (!StringUtils.hasText(text)) {
            return "unknown";
        }
        return text.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String findContainerByServiceName(String serviceName) {
        if (!StringUtils.hasText(serviceName)) {
            return "";
        }
        try {
            String raw = execCmd("docker", "ps", "-a", "--format", "{{.Names}}");
            if (!StringUtils.hasText(raw)) {
                return "";
            }
            String[] lines = raw.split("\\r?\\n");
            for (String line : lines) {
                String name = line == null ? "" : line.trim();
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                String envRaw = execCmd("docker", "inspect", name, "--format", "{{range .Config.Env}}{{println .}}{{end}}");
                if (!StringUtils.hasText(envRaw)) {
                    continue;
                }
                String[] envLines = envRaw.split("\\r?\\n");
                for (String env : envLines) {
                    if (env == null) {
                        continue;
                    }
                    String item = env.trim();
                    if (item.equals("ALG_SERVICE_NAME=" + serviceName) || item.equals("SPRING_APPLICATION_NAME=" + serviceName)) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private Map<String, Object> pruneAlgResources(String algId, String activeContainerName) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> containerPart = pruneStoppedContainers(algId, activeContainerName);
        Map<String, Object> imagePart = pruneAlgImages(algId);
        result.putAll(containerPart);
        result.putAll(imagePart);
        return result;
    }

    private Map<String, Object> pruneStoppedContainers(String algId, String activeContainerName) {
        Map<String, Object> result = new HashMap<>();
        List<String> removed = new ArrayList<>();
        List<String> skippedRunning = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> candidates = collectAlgContainerCandidates(algId, activeContainerName);

        for (String name : candidates) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Map<String, String> info = inspectContainer(name);
            if (!Boolean.parseBoolean(info.getOrDefault("exists", "false"))) {
                continue;
            }
            boolean running = Boolean.parseBoolean(info.getOrDefault("running", "false"));
            if (running) {
                skippedRunning.add(name);
                continue;
            }
            try {
                execCmd("docker", "rm", "-f", name);
                removed.add(name);
            } catch (Exception ex) {
                errors.add(name + ": " + ex.getMessage());
            }
        }
        result.put("removedContainers", removed.size());
        result.put("removedContainerNames", removed);
        result.put("skippedRunningContainers", skippedRunning);
        result.put("containerErrors", errors);
        return result;
    }

    private Set<String> collectAlgContainerCandidates(String algId, String activeContainerName) {
        Set<String> candidates = new LinkedHashSet<>();
        if (StringUtils.hasText(activeContainerName)) {
            candidates.add(activeContainerName.trim());
        }
        if (StringUtils.hasText(algId)) {
            candidates.add("c_user_alg_" + safeToken(algId));
        }
        try {
            String raw = execCmd("docker", "ps", "-a", "--filter", "label=exphlp.alg.id=" + algId, "--format", "{{.Names}}");
            if (StringUtils.hasText(raw)) {
                for (String line : raw.split("\\r?\\n")) {
                    if (StringUtils.hasText(line)) {
                        candidates.add(line.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return candidates;
    }

    private Map<String, Object> pruneAlgImages(String algId) {
        String repo = "exphlp-user-alg-" + safeToken(algId);
        int keepCount = Math.max(1, imageRetainCount);
        Map<String, Object> result = new HashMap<>();
        result.put("repo", repo);
        result.put("keepCount", keepCount);
        try {
            String raw = execCmd("docker", "image", "ls", repo, "--format", "{{.ID}}|{{.Repository}}:{{.Tag}}|{{.CreatedAt}}");
            if (!StringUtils.hasText(raw)) {
                result.put("removedImages", 0);
                result.put("keptImages", 0);
                result.put("imageErrors", Collections.emptyList());
                return result;
            }

            Set<String> inUseRefs = listInUseImageRefs(algId);
            List<String> lines = new ArrayList<>();
            for (String line : raw.split("\\r?\\n")) {
                if (StringUtils.hasText(line)) {
                    lines.add(line.trim());
                }
            }

            Set<String> keptIds = new LinkedHashSet<>();
            Set<String> keptRefs = new LinkedHashSet<>();
            List<String> deleteRefs = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 2) {
                    continue;
                }
                String imageId = parts[0].trim();
                String imageRef = parts[1].trim();
                if (inUseRefs.contains(imageRef)) {
                    keptIds.add(imageId);
                    keptRefs.add(imageRef);
                    continue;
                }
                if (keptIds.size() < keepCount) {
                    keptIds.add(imageId);
                    keptRefs.add(imageRef);
                } else if (!keptIds.contains(imageId)) {
                    deleteRefs.add(imageRef);
                }
            }

            int removed = 0;
            List<String> errors = new ArrayList<>();
            for (String ref : deleteRefs) {
                try {
                    execCmd("docker", "rmi", ref);
                    removed++;
                } catch (Exception ex) {
                    errors.add(ref + ": " + ex.getMessage());
                }
            }
            result.put("removedImages", removed);
            result.put("keptImages", keptRefs.size());
            result.put("keptImageRefs", new ArrayList<>(keptRefs));
            result.put("imageErrors", errors);
            return result;
        } catch (Exception ex) {
            result.put("removedImages", 0);
            result.put("keptImages", 0);
            result.put("imageErrors", Collections.singletonList(ex.getMessage()));
            return result;
        }
    }

    private Set<String> listInUseImageRefs(String algId) {
        Set<String> refs = new LinkedHashSet<>();
        try {
            String raw = execCmd("docker", "ps", "-a", "--filter", "label=exphlp.alg.id=" + algId, "--format", "{{.Image}}");
            if (StringUtils.hasText(raw)) {
                for (String line : raw.split("\\r?\\n")) {
                    if (StringUtils.hasText(line)) {
                        refs.add(line.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return refs;
    }

    private Map<String, String> inspectContainer(String containerName) {
        Map<String, String> info = new HashMap<>();
        info.put("exists", "false");
        info.put("running", "false");
        if (!StringUtils.hasText(containerName)) {
            return info;
        }
        try {
            String inspect = execCmd("docker", "inspect", containerName,
                    "--format",
                    "{{.State.Status}}|{{.State.Running}}|{{.Config.Image}}|{{.State.StartedAt}}|{{range $p,$v := .NetworkSettings.Ports}}{{$p}}={{if $v}}{{(index $v 0).HostPort}}{{else}}-{{end}};{{end}}");
            if (!StringUtils.hasText(inspect)) {
                return info;
            }
            String[] parts = inspect.split("\\|", -1);
            if (parts.length >= 5) {
                info.put("exists", "true");
                info.put("status", parts[0]);
                info.put("running", parts[1]);
                info.put("image", parts[2]);
                info.put("startedAt", parts[3]);
                info.put("ports", parts[4]);
            }
        } catch (Exception ignored) {
        }
        return info;
    }

    private long queryNacosHealthyCount(String serviceName) {
        if (!StringUtils.hasText(serviceName)) {
            return 0L;
        }
        try {
            String addr = StringUtils.hasText(nacosServerAddr) ? nacosServerAddr.trim() : "localhost:8848";
            if (!addr.startsWith("http://") && !addr.startsWith("https://")) {
                addr = "http://" + addr;
            }
            String encodedService = URLEncoder.encode(serviceName, StandardCharsets.UTF_8);
            String url = addr + "/nacos/v1/ns/instance/list?serviceName=" + encodedService + "&groupName=DEFAULT_GROUP&namespaceId=public";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() != 200) {
                return 0L;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String body = reader.lines().collect(Collectors.joining());
                JsonNode node = objectMapper.readTree(body);
                JsonNode hosts = node.get("hosts");
                if (hosts == null || !hosts.isArray()) {
                    return 0L;
                }
                long count = 0L;
                for (JsonNode host : hosts) {
                    boolean healthy = host.path("healthy").asBoolean(false);
                    boolean enabled = host.path("enabled").asBoolean(true);
                    if (healthy && enabled) {
                        count++;
                    }
                }
                return count;
            }
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String execCmd(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException(output == null ? "" : output.trim());
        }
        return output == null ? "" : output.trim();
    }

    private static final class RuntimeTarget {
        private String expectedContainerName = "";
        private String containerName = "";
        private Map<String, String> containerInfo = new HashMap<>();
        private boolean exists = false;
        private boolean running = false;
        private Map<String, Object> migrationInfo = new HashMap<>();
    }

}
