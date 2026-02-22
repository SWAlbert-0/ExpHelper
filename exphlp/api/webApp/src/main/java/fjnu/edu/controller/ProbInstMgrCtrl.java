package fjnu.edu.controller;

import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/ProbController")
public class ProbInstMgrCtrl {
    @Autowired
    ProbInstMgrService probInstMgrService;

    @PostMapping("/addProblem")
    public void addProbInst(@RequestBody ProbInst probInst) {
        if (probInst == null || !StringUtils.hasText(probInst.getInstName())) {
            throw new IllegalArgumentException("问题实例名称不能为空");
        }
        probInstMgrService.addProbInst(probInst);
    }

    @PostMapping("/deleteProblemById")
    public void delProbInstByID (@RequestParam(value = "proId") String proId) {
        if (!StringUtils.hasText(proId)) {
            throw new IllegalArgumentException("问题实例ID不能为空");
        }
        probInstMgrService.delProbInstByID(proId);
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
}
