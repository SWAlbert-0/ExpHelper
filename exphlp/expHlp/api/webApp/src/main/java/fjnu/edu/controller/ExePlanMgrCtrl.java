package fjnu.edu.controller;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public boolean execute(@RequestParam String planId){
        return planExecuteService.execute(planId);
    }

}
