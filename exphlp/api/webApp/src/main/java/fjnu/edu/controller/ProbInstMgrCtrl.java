package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@CrossOrigin
@RequestMapping("/api/ProbController")
public class ProbInstMgrCtrl {
    @Autowired
    ProbInstMgrService probInstMgrService;

    @Autowired
    ExePlanMgrService exePlanMgrService;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PlatMgrService platMgrService;

    @PostMapping("/addProblem")
    public Map<String, Object> addProbInst(@RequestBody ProbInst probInst, HttpServletRequest request) {
        if (probInst == null || !StringUtils.hasText(probInst.getInstName())) {
            return ApiResponse.failed(request, 400, "问题实例名称不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        bindOwner(probInst, currentAuth(request));
        probInstMgrService.addProbInst(probInst);
        return ApiResponse.ok(request, null, "添加成功");
    }

    @PostMapping("/importProblemsJson")
    public Map<String, Object> importProblemsJson(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
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
                    ProbInst probInst = objectMapper.convertValue(item, ProbInst.class);
                    normalizeProbInst(probInst);
                    if (probInst == null || !StringUtils.hasText(probInst.getInstName())) {
                        throw new IllegalArgumentException("instName不能为空");
                    }
                    if (probInstMgrService.countProbInstsByInstName(probInst.getInstName()) > 0) {
                        skipped++;
                        continue;
                    }
                    bindOwner(probInst, currentAuth(request));
                    probInstMgrService.addProbInst(probInst);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    Map<String, Object> row = new HashMap<>();
                    row.put("index", i);
                    row.put("instName", item.has("instName") ? item.get("instName").asText("") : "");
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
            return ApiResponse.ok(request, data, "问题实例JSON导入完成");
        } catch (Exception ex) {
            return ApiResponse.failed(request, 400, "JSON解析失败: " + ex.getMessage(), ErrorCode.INVALID_ARGUMENT.code());
        }
    }

    @PostMapping("/deleteProblemById")
    public Map<String, Object> delProbInstByID (@RequestParam(value = "proId") String proId, HttpServletRequest request) {
        if (!StringUtils.hasText(proId)) {
            return ApiResponse.failed(request, 400, "问题实例ID不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        ProbInst current = probInstMgrService.getProbInstByID(proId);
        if (current != null && !canManageProb(currentAuth(request), current)) {
            return ApiResponse.failed(request, 403, "仅问题实例创建者或管理员可删除", ErrorCode.AUTH_FORBIDDEN.code());
        }
        long refPlanCount = exePlanMgrService.countPlansByProbInstId(proId);
        if (refPlanCount > 0) {
            Map<String, Object> data = buildDeleteData(proId, 0L, false, false, true, true, refPlanCount,
                    exePlanMgrService.listPlanNamesByProbInstId(proId, 5));
            return ApiResponse.failed(request, 409, "删除失败，问题实例已被执行计划引用，请先解除关联", ErrorCode.PROB_IN_USE.code(), data);
        }
        ProbDeleteResult deleteResult = probInstMgrService.delProbInstByID(proId);
        long deletedCount = deleteResult == null ? 0L : deleteResult.getDeletedCount();
        boolean repaired = deleteResult != null && deleteResult.isRepaired();
        boolean noop = deleteResult == null || deleteResult.isNoop();
        boolean verified = deleteResult == null || deleteResult.isVerified();
        Map<String, Object> data = buildDeleteData(proId, deletedCount, deletedCount > 0, repaired, noop, verified, 0L, Collections.emptyList());
        if (noop && !verified) {
            return ApiResponse.failed(request, 500, "删除未生效，请刷新后重试", ErrorCode.INTERNAL_ERROR.code(), data);
        }
        return ApiResponse.ok(request, data, "删除成功");
    }

    @GetMapping("/getProblemById")
    public ProbInst getProbInstByID(@RequestParam(value = "proId") String proId){
        if (!StringUtils.hasText(proId)) {
            return null;
        }
        ProbInst probInst=probInstMgrService.getProbInstByID(proId);
        return probInst;
    }

    @PostMapping("/updateProblemById")
    public Map<String, Object> updateProbInst(@RequestBody ProbInst probInst, HttpServletRequest request){
        if (probInst == null || !StringUtils.hasText(probInst.getInstId())) {
            return ApiResponse.failed(request, 400, "问题实例ID不能为空", ErrorCode.INVALID_ARGUMENT.code());
        }
        ProbInst current = probInstMgrService.getProbInstByID(probInst.getInstId());
        if (current == null) {
            return ApiResponse.failed(request, 404, "问题实例不存在", ErrorCode.INVALID_ARGUMENT.code());
        }
        if (!canManageProb(currentAuth(request), current)) {
            return ApiResponse.failed(request, 403, "仅问题实例创建者或管理员可编辑", ErrorCode.AUTH_FORBIDDEN.code());
        }
        probInst.setOwnerUserId(current.getOwnerUserId());
        probInst.setOwnerUserName(current.getOwnerUserName());
        probInstMgrService.updateProbInst(probInst);
        return ApiResponse.ok(request, null, "更新成功");
    }

    @GetMapping("/getProblems")
    public List<ProbInst> listProbInsts(@RequestParam(value = "pageNum") int pageNum,
                                        @RequestParam(value = "pageSize") int pageSize) {
        List<ProbInst> listProbInsts = probInstMgrService.listProbInsts(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return listProbInsts == null ? Collections.emptyList() : listProbInsts;
    }

    @GetMapping("/getProblemsByName")
    public List<ProbInst> listProbInstsByPlatName(@RequestParam(value = "probName") String probName,
                                                  @RequestParam(value = "pageNum") int pageNum,
                                                  @RequestParam(value = "pageSize") int pageSize) {
        if (!StringUtils.hasText(probName)) {
            return Collections.emptyList();
        }
        List<ProbInst> listProbInsts = probInstMgrService.listProbInstsByinstName(probName, normalizePageNum(pageNum), normalizePageSize(pageSize));
        return listProbInsts == null ? Collections.emptyList() : listProbInsts;
    }

    @GetMapping("/countAllProbInsts")
    public long countAllProbInsts() {
        long count = probInstMgrService.countAllProbInsts();
        return  count;
    }

    @GetMapping("/countProbInstsByInstName")
    public long countProbInstsByInstName(@RequestParam(value = "probName") String probName) {
        if (!StringUtils.hasText(probName)) {
            return 0;
        }
        long count = probInstMgrService.countProbInstsByInstName(probName);
        return  count;
    }

    private int normalizePageNum(int pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }

    private Map<String, Object> buildDeleteData(String proId, long deletedCount, boolean existed, boolean repaired,
                                                boolean noop, boolean verified, long refPlanCount, List<String> refPlanNames) {
        Map<String, Object> data = new HashMap<>();
        data.put("proId", proId);
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

    private void normalizeProbInst(ProbInst probInst) {
        if (probInst == null) {
            return;
        }
        probInst.setInstId(null);
        probInst.setOwnerUserId(trimOrEmpty(probInst.getOwnerUserId()));
        probInst.setOwnerUserName(trimOrEmpty(probInst.getOwnerUserName()));
        probInst.setInstName(trimOrEmpty(probInst.getInstName()));
        probInst.setCategoryName(trimOrEmpty(probInst.getCategoryName()));
        probInst.setDirName(trimOrEmpty(probInst.getDirName()));
        probInst.setMachineName(trimOrEmpty(probInst.getMachineName()));
        probInst.setMachineIp(trimOrEmpty(probInst.getMachineIp()));
        probInst.setDescription(trimOrEmpty(probInst.getDescription()));
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
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

    private void bindOwner(ProbInst probInst, AuthUser auth) {
        if (probInst == null || auth == null) {
            return;
        }
        String ownerId = resolveAuthUserId(auth);
        if (StringUtils.hasText(ownerId)) {
            probInst.setOwnerUserId(ownerId);
        }
        if (StringUtils.hasText(auth.getUserName())) {
            probInst.setOwnerUserName(auth.getUserName().trim());
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

    private boolean canManageProb(AuthUser auth, ProbInst probInst) {
        if (auth == null) {
            return false;
        }
        if (auth.getRole() != null && auth.getRole() == 1) {
            return true;
        }
        if (probInst == null || !StringUtils.hasText(probInst.getOwnerUserId())) {
            return false;
        }
        String authUserId = resolveAuthUserId(auth);
        return StringUtils.hasText(authUserId) && authUserId.equals(probInst.getOwnerUserId());
    }
}
