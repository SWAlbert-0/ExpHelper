package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@RestController
public class AlgApp {
    public static void main(String[] args) {
        SpringApplication.run(AlgApp.class, args);
    }

    @PostMapping("/myAlg/")
    public List<EachResult> run(@RequestBody AlgRunCtx ctx) {
        List<EachResult> out = new ArrayList<>();
        String probName = ctx.probInst == null ? "unknown" : ctx.probInst.instName;
        out.add(new EachResult("case", probName, "string"));
        out.add(new EachResult("score", "100", "int"));
        return out;
    }

    public static class AlgRunCtx {
        public ProbInst probInst;
        public List<RunPara> runParas;
    }

    public static class ProbInst {
        public String instId;
        public String instName;
    }

    public static class RunPara {
        public int paraId;
        public String paraName;
        public String paraType;
        public String paraValue;
    }

    public static class EachResult {
        public String key;
        public String value;
        public String dataType;

        public EachResult() {}
        public EachResult(String key, String value, String dataType) {
            this.key = key;
            this.value = value;
            this.dataType = dataType;
        }
    }
}
