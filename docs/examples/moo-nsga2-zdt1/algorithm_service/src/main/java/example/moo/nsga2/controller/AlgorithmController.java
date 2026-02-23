package example.moo.nsga2.controller;

import example.moo.nsga2.model.AlgRunCtx;
import example.moo.nsga2.model.EachResult;
import example.moo.nsga2.model.RunPara;
import example.moo.nsga2.solver.Nsga2Zdt1Solver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class AlgorithmController {
    @PostMapping("/myAlg/")
    public List<EachResult> run(@RequestBody AlgRunCtx ctx) {
        Map<String, String> paraMap = toParamMap(ctx == null ? null : ctx.runParas);
        Nsga2Zdt1Solver.Config config = toConfig(paraMap);
        Nsga2Zdt1Solver solver = new Nsga2Zdt1Solver();

        long start = System.currentTimeMillis();
        List<Nsga2Zdt1Solver.Point> pareto = solver.solve(config);
        long runtimeMs = System.currentTimeMillis() - start;

        List<EachResult> out = new ArrayList<>();
        out.add(new EachResult("problemFamily", "ZDT1", "string"));
        out.add(new EachResult("paretoSize", String.valueOf(pareto.size()), "int"));
        out.add(new EachResult("runtimeMs", String.valueOf(runtimeMs), "long"));
        if (!pareto.isEmpty()) {
            double bestF1 = pareto.stream().mapToDouble(p -> p.f1).min().orElse(Double.NaN);
            double bestF2 = pareto.stream().mapToDouble(p -> p.f2).min().orElse(Double.NaN);
            out.add(new EachResult("bestF1", String.format(Locale.US, "%.8f", bestF1), "double"));
            out.add(new EachResult("bestF2", String.format(Locale.US, "%.8f", bestF2), "double"));
        } else {
            out.add(new EachResult("bestF1", "NaN", "double"));
            out.add(new EachResult("bestF2", "NaN", "double"));
        }

        for (int i = 0; i < pareto.size(); i++) {
            Nsga2Zdt1Solver.Point p = pareto.get(i);
            String key = "paretoPoint_" + (i + 1);
            String value = String.format(Locale.US, "f1=%.8f,f2=%.8f", p.f1, p.f2);
            out.add(new EachResult(key, value, "pair"));
        }
        return out;
    }

    private Map<String, String> toParamMap(List<RunPara> runParas) {
        Map<String, String> map = new HashMap<>();
        if (runParas == null) {
            return map;
        }
        for (RunPara para : runParas) {
            if (para == null || para.paraName == null) {
                continue;
            }
            map.put(para.paraName, para.paraValue);
        }
        return map;
    }

    private Nsga2Zdt1Solver.Config toConfig(Map<String, String> map) {
        Nsga2Zdt1Solver.Config c = new Nsga2Zdt1Solver.Config();
        c.nVars = parseInt(map.get("nVars"), c.nVars, 2, 1000);
        c.populationSize = parseInt(map.get("populationSize"), c.populationSize, 20, 2000);
        c.maxGenerations = parseInt(map.get("maxGenerations"), c.maxGenerations, 10, 2000);
        c.crossoverProbability = parseDouble(map.get("crossoverProbability"), c.crossoverProbability, 0.1, 1.0);
        c.mutationProbability = parseDouble(map.get("mutationProbability"), c.mutationProbability, 0.0001, 1.0);
        c.seed = parseLong(map.get("seed"), c.seed);
        c.maxReturnedPoints = parseInt(map.get("maxReturnedPoints"), c.maxReturnedPoints, 10, 200);
        return c;
    }

    private int parseInt(String value, int dft, int min, int max) {
        try {
            if (value == null || value.trim().isEmpty()) return dft;
            int v = Integer.parseInt(value.trim());
            if (v < min || v > max) return dft;
            return v;
        } catch (Exception ex) {
            return dft;
        }
    }

    private long parseLong(String value, long dft) {
        try {
            if (value == null || value.trim().isEmpty()) return dft;
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            return dft;
        }
    }

    private double parseDouble(String value, double dft, double min, double max) {
        try {
            if (value == null || value.trim().isEmpty()) return dft;
            double v = Double.parseDouble(value.trim());
            if (v < min || v > max) return dft;
            return v;
        } catch (Exception ex) {
            return dft;
        }
    }
}
