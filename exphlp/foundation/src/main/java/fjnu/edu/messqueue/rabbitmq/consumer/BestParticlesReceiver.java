package fjnu.edu.messqueue.rabbitmq.consumer;//package fjnu.edu.cn.sharedModules.basic.foundation.messqueue.rabbitmq.consumer;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//
//
//import fjnu.edu.cn.methods.llvm.COSUHA.domain.conBestopts.icAlgs.planExeResultDao.ExeResultDao;
//import fjnu.edu.cn.methods.llvm.COSUHA.domain.conBestopts.icAlgs.entity.Individual;
//import fjnu.edu.cn.methods.llvm.COSUHA.domain.conBestopts.icAlgs.entity.IndvRunResult;
//import fjnu.edu.cn.methods.llvm.COSUHA.domain.conBestopts.icAlgs.entity.RunNumToIndv;
//import org.springframework.amqp.rabbit.annotation.RabbitHandler;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//
//@Component
//@RabbitListener(queues = "BestParticlesQueue")//监听的队列名称 BestParticlesQueue
//public class BestParticlesReceiver {
//
//    @Autowired
//    ExeResultDao exeResultDao;
//    @RabbitHandler
//    public void process(String jsonString) throws IOException {
//        RunNumToIndv runNumToIndv = JSONObject.parseObject(jsonString, RunNumToIndv.class);
//        Individual individual = runNumToIndv.getIndividual();
//        IndvRunResult indvRunResult = new IndvRunResult(individual);
//
//        String paths = "/src_work/llvm12.txt";
//
//        Map<String,String> passestoHex = new HashMap<>();
//
//        FileInputStream inputStream = new FileInputStream(paths);
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//        String str = null;
//        while ((str = bufferedReader.readLine()) != null) {
//            String[] passestohex = str.split(" ");
//            passestoHex.put(passestohex[1], passestohex[0]);
//        }
//
//        StringBuilder sb = new StringBuilder();
//
//        for (String pass:individual.getOpSeq()
//             ) {
//            sb.append(passestoHex.get(pass));
//        }
//
//        indvRunResult.setOpSeq(sb.toString());
//
//        exeResultDao.insertIndv(runNumToIndv.getTotRunNum(), runNumToIndv.getAppId(),indvRunResult);
//        System.out.println("第一个BestParticlesReceiver消费者收到消息  : " + runNumToIndv.toString());
//
//    }
//}
