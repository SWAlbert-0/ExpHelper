package fjnu.edu.controller;

import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.entity.DisplayResult;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@RestController
@CrossOrigin
@RequestMapping("/api/AlgRltSaveController")
public class AlgRltSaveCtrl {
    @Autowired
    AlgRltSaveService algRltSaveService;
    @Autowired
    ProbInstMgrService probInstMgrService;

    @GetMapping("/getAlgSaveByAlgName")
    public List<DisplayResult> getAlgSaveByAlgName(@RequestParam(value = "planId") String planId, @RequestParam(value = "algId") String algId, @RequestParam(value = "algName") String algName) {
        PlanExeResult  planExeResult = algRltSaveService.getAlgSaveByAlgName(planId,algId,algName);
        List<DisplayResult>  displayResults= new ArrayList<>();
        Date dat = new Date(planExeResult.getStartTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < planExeResult.getGenResults().size(); i++) {
            DisplayResult displayResult = new DisplayResult();
            displayResult.setStartTime(sdf.format(dat));
            Date date = new Date(planExeResult.getGenResults().get(i).getOutTime());
            displayResult.setOutputTime(sdf.format(date));
            displayResult.setProbInstName(probInstMgrService.getProbInstByID(planExeResult.getGenResults().get(i).getProbInstId()).getInstName());
            displayResult.setEachResults(planExeResult.getGenResults().get(i).getEachResults());
            displayResults.add(displayResult);

        }
        return displayResults;
    }
}
