package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.algruntime.service.AlgBuildTaskService;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;

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

    @PostMapping("/addAlg")
    public String addAlgInfo (@RequestBody AlgInfo algInfo) {
        normalizeAlgInfo(algInfo);
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgName())) {
            throw new IllegalArgumentException("算法名称不能为空");
        }
        AlgInfo algInfo1 =algLibMgrService.getAlgInfoByName(algInfo.getAlgName());
        if(algInfo1!= null ){
            return "算法名不能重复";
        }else {
            algLibMgrService.addAlgInfo(algInfo);
            return algInfo.getAlgName();
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
        try {
            AlgBuildTask task = algBuildTaskService.createUploadTask(algInfo, file, request.getHeader("X-Trace-Id"));
            return ApiResponse.ok(request, task, "源码上传成功");
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

    @PostMapping("/deleteAlgById")
    public Map<String, Object> deleteAlgInfoById(@RequestParam(value = "algId") String algId, HttpServletRequest request) {
        if (!StringUtils.hasText(algId)) {
            throw new IllegalArgumentException("算法ID不能为空");
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
    public void updateAlgInfoById(@RequestBody AlgInfo algInfo) {
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgId())) {
            throw new IllegalArgumentException("算法ID不能为空");
        }
        algInfo.setAlgName(trimOrEmpty(algInfo.getAlgName()));
        algInfo.setServiceName(trimOrEmpty(algInfo.getServiceName()));
        algInfo.setRuntimeType(normalizeRuntimeType(algInfo.getRuntimeType()));
        algInfo.setDescription(trimOrEmpty(algInfo.getDescription()));
        algLibMgrService.updateAlgInfoById(algInfo);
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

}
