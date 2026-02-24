package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.entity.DisplayResult;
import fjnu.edu.entity.ExeResultDetail;
import fjnu.edu.entity.ExeResultRunDetail;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/AlgRltSaveController")
public class AlgRltSaveCtrl {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Autowired
    AlgRltSaveService algRltSaveService;
    @Autowired
    ProbInstMgrService probInstMgrService;

    @GetMapping("/getAlgSaveByAlgName")
    public List<DisplayResult> getAlgSaveByAlgName(@RequestParam(value = "planId") String planId,
                                                    @RequestParam(value = "algId") String algId,
                                                    @RequestParam(value = "algName", required = false) String algName) {
        PlanExeResult  planExeResult = algRltSaveService.getAlgSaveByAlgName(planId,algId,algName);
        List<DisplayResult>  displayResults= new ArrayList<>();
        if (planExeResult == null || planExeResult.getGenResults() == null || planExeResult.getGenResults().isEmpty()) {
            return displayResults;
        }
        String startTime = DT_FMT.format(Instant.ofEpochMilli(planExeResult.getStartTime()));
        for (int i = 0; i < planExeResult.getGenResults().size(); i++) {
            DisplayResult displayResult = new DisplayResult();
            displayResult.setStartTime(startTime);
            displayResult.setOutputTime(DT_FMT.format(Instant.ofEpochMilli(planExeResult.getGenResults().get(i).getOutTime())));
            String probInstId = planExeResult.getGenResults().get(i).getProbInstId();
            String probInstName = probInstId;
            if (probInstId != null) {
                ProbInst probInst = probInstMgrService.getProbInstByID(probInstId);
                if (probInst != null) {
                    probInstName = probInst.getInstName();
                }
            }
            displayResult.setProbInstName(probInstName);
            displayResult.setEachResults(planExeResult.getGenResults().get(i).getEachResults());
            displayResults.add(displayResult);

        }
        return displayResults;
    }

    @GetMapping("/getExeResultDetail")
    public java.util.Map<String, Object> getExeResultDetail(@RequestParam(value = "planId") String planId,
                                                            @RequestParam(value = "algId") String algId,
                                                            HttpServletRequest request) {
        ExeResultDetail detail = algRltSaveService.getExeResultDetail(planId, algId);
        if (detail.getRuns() != null) {
            for (ExeResultRunDetail run : detail.getRuns()) {
                if (run == null || run.getProbInstId() == null) {
                    continue;
                }
                ProbInst probInst = probInstMgrService.getProbInstByID(run.getProbInstId());
                if (probInst != null && probInst.getInstName() != null && !probInst.getInstName().trim().isEmpty()) {
                    run.setProbInstName(probInst.getInstName());
                }
            }
        }
        return ApiResponse.ok(request, detail);
    }
}
