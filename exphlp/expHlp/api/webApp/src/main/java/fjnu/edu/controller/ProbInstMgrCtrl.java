package fjnu.edu.controller;

import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/ProbController")
public class ProbInstMgrCtrl {
    @Autowired
    ProbInstMgrService probInstMgrService;

    @PostMapping("/addProblem")
    public void addProbInst(@RequestBody ProbInst probInst) {
        probInstMgrService.addProbInst(probInst);
    }

    @PostMapping("/deleteProblemById")
    public void delProbInstByID (@RequestParam(value = "proId") String proId) {
        probInstMgrService.delProbInstByID(proId);
    }

    @GetMapping("/getProblemById")
    public ProbInst getProbInstByID(@RequestParam(value = "proId") String proId){
        ProbInst probInst=probInstMgrService.getProbInstByID(proId);
        return probInst;
    }

    @PostMapping("/updateProblemById")
    public void updateProbInst(@RequestBody ProbInst probInst){
        probInstMgrService.updateProbInst(probInst);
    }

    @GetMapping("/getProblems")
    public List<ProbInst> listProbInsts(@RequestParam(value = "pageNum") int pageNum,
                                        @RequestParam(value = "pageSize") int pageSize) {
        List<ProbInst> listProbInsts = probInstMgrService.listProbInsts(pageNum, pageSize);
        return listProbInsts;
    }

    @GetMapping("/getProblemsByName")
    public List<ProbInst> listProbInstsByPlatName(@RequestParam(value = "probName") String probName,
                                                  @RequestParam(value = "pageNum") int pageNum,
                                                  @RequestParam(value = "pageSize") int pageSize) {
        List<ProbInst> listProbInsts = probInstMgrService.listProbInstsByinstName(probName, pageNum, pageSize);
        return listProbInsts;
    }

    @GetMapping("/countAllProbInsts")
    public long countAllProbInsts() {
        long count = probInstMgrService.countAllProbInsts();
        return  count;
    }

    @GetMapping("/countProbInstsByInstName")
    public long countProbInstsByInstName(@RequestParam(value = "probName") String probName) {
        long count = probInstMgrService.countProbInstsByInstName(probName);
        return  count;
    }
}
