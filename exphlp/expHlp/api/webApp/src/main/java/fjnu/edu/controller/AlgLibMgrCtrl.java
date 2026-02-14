package fjnu.edu.controller;

import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/AlgController")
public class AlgLibMgrCtrl {
    @Autowired
    AlgLibMgrService algLibMgrService;

    @PostMapping("/addAlg")
    public String addAlgInfo (@RequestBody AlgInfo algInfo) {
        AlgInfo algInfo1 =algLibMgrService.getAlgInfoByName(algInfo.getAlgName());
        if(algInfo1!= null ){
            return "算法名不能重复";
        }else {
            algLibMgrService.addAlgInfo(algInfo);
            return algInfo.getAlgName();
        }

    }

    @PostMapping("/deleteAlgById")
    public void deleteAlgInfoById(@RequestParam(value = "algId") String algId) {
        algLibMgrService.deleteAlgInfoById(algId);
    }

    @GetMapping("/getAlgById")
    public AlgInfo getAlgInfoById(@RequestParam(value = "algId") String algId) {
        AlgInfo algInfo1 = algLibMgrService.getAlgInfoById(algId);
        return algInfo1;
    }

    @GetMapping("/getAlgsByName")
    public  List<AlgInfo>  getAlgInfoByName(@RequestParam(value = "algName") String algName,
                                            @RequestParam(value = "pageNum") int pageNum,
                                            @RequestParam(value = "pageSize" ) int pageSize) {
        List<AlgInfo> algInfos = algLibMgrService.getAlgInfoByName(algName,pageNum,pageSize);
        return algInfos;
    }

    @GetMapping("/getAlgs")
    public List<AlgInfo> getAlgInfos(@RequestParam(value = "pageNum") int pageNum,
                                     @RequestParam(value = "pageSize" ) int pageSize) {
        List<AlgInfo> algInfos = algLibMgrService.getAlgInfos(pageNum,pageSize);
        return algInfos;
    }

    @PostMapping("/updateAlgById")
    public void updateAlgInfoById(@RequestBody AlgInfo algInfo) {
        algLibMgrService.updateAlgInfoById(algInfo);
    }

    @GetMapping("/getParaByAlgId")
    public List<DefPara> getParasByAlgInfoId(@RequestParam(value = "algId") String algId) {
        List<DefPara> defParas = algLibMgrService.getParasByAlgInfoId(algId);
        return defParas;
    }

    @GetMapping("/countAllAlgs")
    public long countAllProbInsts() {
        long count = algLibMgrService.countAllAlgs();
        return count;
    }

    @GetMapping("/countAlgsByAlgName")
    public long countProbInstsByInstName(@RequestParam(value = "algName") String algName) {
        long count = algLibMgrService.countAlgsByAlgName(algName);
        return count;
    }

}
