package fjnu.edu.controller;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin
@RequestMapping("/api/ExePlanController")
public class ExePlanMgrCtrl {

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
    public Map<String, Object> execute(@RequestParam String planId){
        if (planId == null || planId.trim().isEmpty()) {
            return failed("planId不能为空");
        }
        ExePlan exePlan;
        try {
            exePlan = exePlanMgrService.getExePlanById(planId);
        } catch (Exception ex) {
            return failed("planId格式非法");
        }
        if (exePlan == null) {
            return failed("执行计划不存在");
        }
        boolean accepted = planExecuteService.execute(planId);
        ExePlan latest = exePlanMgrService.getExePlanById(planId);
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        data.put("accepted", accepted);
        data.put("state", latest == null ? null : latest.getExeState());
        data.put("lastError", latest == null ? null : latest.getLastError());
        if (!accepted) {
            return ok(data, "计划未被受理，可能正在执行或执行队列已满");
        }
        return ok(data);
    }

    private Map<String, Object> ok(Object data) {
        return ok(data, "success");
    }

    private Map<String, Object> ok(Object data, String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", msg);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> failed(String msg) {
        return failed(msg, null);
    }

    private Map<String, Object> failed(String msg, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("msg", msg);
        response.put("data", data);
        return response;
    }

}
