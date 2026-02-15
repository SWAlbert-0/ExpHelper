package fjnu.edu.impl;

import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.GenResult;
import fjnu.edu.entity.OutPuter;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.exePlanMgr.dao.ExePlanMgrDao;
import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.AlgRunInfo;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.intf.IAlgRun;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class PlanExecuteImpl implements PlanExecuteService {


    @Autowired
    ExePlanMgrDao exePlanMgrDao;
    @Autowired
    AlgRltSaveService algRltSaveService;
    @Autowired
    AlgLibMgrService algLibMgrService;

    @Autowired
    ProbInstDao probInstDao;
    @Resource
    private RestTemplate restTemplate;
//    @Value("${service-url.nacos-user-service}")
//    private String serverURL;

    @Override
    public boolean execute(String planId){

        /**
         * 拆解执行计划
         */
        ExePlan exePlan = exePlanMgrDao.getExePlanById(planId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 得到此次执行计划中要进行测试的所有问题实例 ProbInsts
        List<ProbInst> probInsts = new ArrayList<>();
        for (String probInstId : exePlan.getProbInstIds()) {
            probInsts.add(probInstDao.getProbInstByID(probInstId));
        }

        /**
         * 将算法运行的信息进行拆分，构造循环（相当于 for algorithm 的第一层循环）
         */
        List<AlgRunInfo> algRunInfos = exePlan.getAlgRunInfos();
        for (int i = 0; i < algRunInfos.size(); i++) {

            // 得到要运行的算法的ID algId
            String algId = algRunInfos.get(i).getAlgId();

            // 得到用户注册的算法的服务名
            String serviceName = algLibMgrService.getServiceNameById(algId);
            String serverURL = "http://" + serviceName;


            // 得到算法的参数 runParas
            List<RunPara> runParas = algRunInfos.get(i).getRunParas();

            // 得到算法要运行的次数 runNum
            int runNum = algRunInfos.get(i).getRunNum();

            //封装存到数据库的结果
            PlanExeResult planExeResult= new PlanExeResult();

            String algName = algRunInfos.get(i).getAlgName()+"-"+algRunInfos.get(i).getAlgRunInfoId();
            planExeResult.setAlgName(algName);

            OutPuter outPuter = new OutPuter(planId, algId, runNum);
            int a = runNum;
            List<GenResult> genResults =new ArrayList<>();

            /**
             * 构建算法运行的上下文，构造循环（相当于 for runNum 的第二层循环）
             */
            for (int time = 0; time < runNum; time++) {


                /**
                 * 得到算法运行的对象：单个问题实例，构造循环（相当于 for probInst 的第三层循环）
                 */
                for (ProbInst probInst : probInsts) {
                    AlgRunCtx algRunCtx = buildAlgRunCtx(planId, algId, probInst, runParas, time+1);

                    try {
                        // 此处利用反射机制，去运行用户所写的算法
//                    Class clazz = Class.forName(algRunInfos.get(i).getMainClazzPath());
//                        Class clazz = Class.forName("fjnu.edu.com.algs.MyAlg");
                        planExeResult.setStartTime(System.currentTimeMillis());//算法开始时间
//                        IAlgRun iAlgRun = (IAlgRun) clazz.newInstance();
//                        List<EachResult> eachResults = iAlgRun.run(algRunCtx);
                        GenResult genResult = new GenResult();
                        HttpEntity<AlgRunCtx> request = new HttpEntity<>(algRunCtx);

                        try {
                            ResponseEntity<EachResult[]> eachResult = restTemplate.postForEntity(serverURL+"/myAlg/", request, EachResult[].class);
                            exePlan.setExeState(2);//执行中
                            exePlanMgrDao.updateExePlanById(exePlan);
                            List<EachResult> eachResults= Arrays.asList(eachResult.getBody());
                            genResult.setEachResults(eachResults);
                            genResult.setProbInstId(probInst.getInstId());
                            genResult.setOutTime(System.currentTimeMillis());

                            genResults.add(genResult);
                        } catch (Exception e) {
                            return  false;
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
            planExeResult.setPlanId(planId);
            planExeResult.setAlgId(algId);
            planExeResult.setRunNum(runNum);
            planExeResult.setOutputTime(System.currentTimeMillis());
            planExeResult.setGenResults(genResults);

            //将运行结果存入数据库
            algRltSaveService.insertPlanExeResult(planExeResult);


        }

        exePlan.setExeState(3);
        exePlanMgrDao.updateExePlanById(exePlan);

        return true;
    }

    /**
     * 构建算法运行的上下文，应该是一个很小的级别
     * @param planId
     * @return
     */
    @Override
    public AlgRunCtx buildAlgRunCtx(String planId, String algId, ProbInst probInst, List<RunPara> runParas, int runNum) {

        OutPuter outPuter = new OutPuter(planId, algId, runNum);
        AlgRunCtx algRunCtx = new AlgRunCtx(probInst, runParas, outPuter);

        return algRunCtx;
    }
}
