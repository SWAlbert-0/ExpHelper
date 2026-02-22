package fjnu.edu.controller;

import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/AlgController")
public class AlgLibMgrCtrl {
    @Autowired
    AlgLibMgrService algLibMgrService;

    @PostMapping("/addAlg")
    public String addAlgInfo (@RequestBody AlgInfo algInfo) {
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

    @PostMapping("/deleteAlgById")
    public void deleteAlgInfoById(@RequestParam(value = "algId") String algId) {
        if (!StringUtils.hasText(algId)) {
            throw new IllegalArgumentException("算法ID不能为空");
        }
        algLibMgrService.deleteAlgInfoById(algId);
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

    private int normalizePageNum(int pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }

}
