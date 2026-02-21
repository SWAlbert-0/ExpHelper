package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin
@RequestMapping("/api/ExePlanController")
public class ExePlanMgrCtrl {
    private static final Logger log = LoggerFactory.getLogger(ExePlanMgrCtrl.class);

    @Autowired
    ExePlanMgrService exePlanMgrService;

    @Autowired
    PlanExecuteService planExecuteService;

    @GetMapping("/getExePlans")
    public List<ExePlan> getExePlans(@RequestParam int pageNum,
                                     @RequestParam int pageSize){
        return exePlanMgrService.getExePlans(pageNum,pageSize);

    }

    @PostMapping("/addExePlan")
    public String addExePlan(@RequestBody ExePlan exeplan) throws Exception {
        return exePlanMgrService.addExePlan(exeplan);
    }

    @GetMapping("/getExePlanByName")
    public ExePlan getExeplanByName(@RequestParam String planName){
        return exePlanMgrService.getExePlanByName(planName);
    }

    @PostMapping("/deleteExePlanById")
    public boolean deleteExePlanById(@RequestParam String planId){
        return exePlanMgrService.deleteExePlanById(planId);
    }

    @PostMapping("updateExePlanById")
    public boolean updateExePlanById(@RequestBody ExePlan exeplan){
        return exePlanMgrService.updateExePlanById(exeplan);
    }

    @GetMapping("/countAllExePlans")
    public long countAllExePlans() {
        long count = exePlanMgrService.countAllExePlans();
        return  count;
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestParam String planId, HttpServletRequest request){
        String traceId = TraceContext.getTraceId(request);
        if (planId == null || planId.trim().isEmpty()) {
            log.warn("traceId={} path={} errorCode={}", traceId, "/api/ExePlanController/execute", ErrorCode.PLAN_ID_EMPTY.code());
            return ApiResponse.failed(request, 400, "planId不能为空", ErrorCode.PLAN_ID_EMPTY.code());
        }
        ExePlan exePlan;
        try {
            exePlan = exePlanMgrService.getExePlanById(planId);
        } catch (Exception ex) {
            log.warn("traceId={} path={} planId={} errorCode={}", traceId, "/api/ExePlanController/execute", planId, ErrorCode.PLAN_ID_FORMAT_INVALID.code());
            return ApiResponse.failed(request, 400, "planId格式非法", ErrorCode.PLAN_ID_FORMAT_INVALID.code());
        }
        if (exePlan == null) {
            log.warn("traceId={} path={} planId={} errorCode={}", traceId, "/api/ExePlanController/execute", planId, ErrorCode.PLAN_NOT_FOUND.code());
            return ApiResponse.failed(request, 404, "执行计划不存在", ErrorCode.PLAN_NOT_FOUND.code());
        }
        boolean accepted = planExecuteService.execute(planId);
        ExePlan latest = exePlanMgrService.getExePlanById(planId);
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("accepted", accepted);
        data.put("state", latest == null ? null : latest.getExeState());
        data.put("lastError", latest == null ? null : latest.getLastError());
        log.info("traceId={} path={} planId={} accepted={} state={} lastError={}", traceId, "/api/ExePlanController/execute",
                planId, accepted, data.get("state"), data.get("lastError"));
        if (!accepted) {
            return ApiResponse.ok(request, data, "计划未被受理，可能正在执行或执行队列已满");
        }
        return ApiResponse.ok(request, data);
    }

}
