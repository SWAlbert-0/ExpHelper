package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/ProbController")
public class ProbInstMgrCtrl {
    @Autowired
    ProbInstMgrService probInstMgrService;

    @Autowired
    ExePlanMgrService exePlanMgrService;

    @PostMapping("/addProblem")
    public void addProbInst(@RequestBody ProbInst probInst) {
        if (probInst == null || !StringUtils.hasText(probInst.getInstName())) {
            throw new IllegalArgumentException("问题实例名称不能为空");
        }
        probInstMgrService.addProbInst(probInst);
    }

    @PostMapping("/deleteProblemById")
    public Map<String, Object> delProbInstByID (@RequestParam(value = "proId") String proId, HttpServletRequest request) {
        if (!StringUtils.hasText(proId)) {
            throw new IllegalArgumentException("问题实例ID不能为空");
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
    public void updateProbInst(@RequestBody ProbInst probInst){
        if (probInst == null || !StringUtils.hasText(probInst.getInstId())) {
            throw new IllegalArgumentException("问题实例ID不能为空");
        }
        probInstMgrService.updateProbInst(probInst);
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
}
