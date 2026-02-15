package fjnu.edu.com.algs;

import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.OutPuter;
import fjnu.edu.exePlanMgr.entity.AlgRunCtx;
import fjnu.edu.exePlanMgr.entity.RunPara;
import fjnu.edu.intf.IAlgRun;
import fjnu.edu.probInstMgr.entity.ProbInst;

import java.util.ArrayList;
import java.util.List;

public class MyAlg implements IAlgRun {

    AlgInfo algInfo = new AlgInfo();

    @Override
    public List<EachResult> run(AlgRunCtx algRunCtx) {

        // 获取runPara,probInst,outPuter对象
        List<RunPara> runPara = algRunCtx.getRunParas();
        ProbInst probInst = algRunCtx.getProbInst();
        OutPuter outPuter = algRunCtx.getOutPuter();

        //模拟算法运行的过程：输出实例名和第一个参数，添加算法执行结果（此处根据需要转换为具体逻辑）
        System.out.println(probInst.getInstName());
        System.out.println(runPara.get(0).getParaName() + "=" + runPara.get(0).getParaValue());

        //创建存放算法运行结果的List<EachResult>,将算法运行结果放入其中，再将它放入OutPuter对象中
        List<EachResult> eachResults = new ArrayList<>();
        eachResults.add(new EachResult("res","2.333","float"));

        //往数据库中输出结果
        outPuter.write(eachResults);

        return eachResults;
    }

}
