package fjnu.edu.service.impl;

import fjnu.edu.common.exception.BusinessException;
import fjnu.edu.dao.AlgRltSaveDao;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.ExeResultAggregate;
import fjnu.edu.entity.ExeResultDetail;
import fjnu.edu.entity.ExeResultRunDetail;
import fjnu.edu.entity.GenResult;
import fjnu.edu.entity.MetricRunCache;
import fjnu.edu.entity.ParetoPoint;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AlgRltSaveServiceImpl implements AlgRltSaveService {
    private static final String METRIC_VERSION = "v3";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_EMPTY = "EMPTY";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_MISSING = "MISSING";
    private static final String METRIC_OK = "OK";
    private static final String REASON_OK = "OK";
    private static final String REASON_RESULT_NOT_FOUND = "RESULT_NOT_FOUND";
    private static final String REASON_GEN_RESULTS_EMPTY = "GEN_RESULTS_EMPTY";
    private static final String REASON_PARETO_PARSE_FAILED = "PARETO_PARSE_FAILED";
    private static final String REASON_NO_REFERENCE_FRONT = "NO_REFERENCE_FRONT";
    private static final Pattern PAIR_PATTERN = Pattern.compile("f1\\s*=\\s*([-+]?\\d*\\.?\\d+)\\s*,\\s*f2\\s*=\\s*([-+]?\\d*\\.?\\d+)");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final double REF_F1 = 1.1d;
    private static final double REF_F2 = 1.1d;
    private static final int REF_FRONT_SIZE = 1000;

    @Autowired
    AlgRltSaveDao algRltSaveDao;
    @Autowired
    ProbInstDao probInstDao;

    @Override
    public boolean insertPlanExeResult(PlanExeResult planExeResult) {
        try {
            algRltSaveDao.insertPlanExeResult(planExeResult);
            return true;
        } catch (Exception e) {
            throw new BusinessException("添加结果失败");
        }
    }

    @Override
    public PlanExeResult getAlgSaveByAlgName(String planId, String algId, String algName) {
        return algRltSaveDao.getAlgSaveByAlgName(planId, algId, algName);
    }

    @Override
    public ExeResultDetail getExeResultDetail(String planId, String algId) {
        ExeResultDetail detail = new ExeResultDetail();
        detail.setPlanId(planId);
        detail.setAlgId(algId);
        detail.setMetricVersion(METRIC_VERSION);
        PlanExeResult saved = algRltSaveDao.getLatestByPlanAndAlg(planId, algId);
        if (saved == null) {
            detail.setStatus(STATUS_MISSING);
            detail.setReasonCode(REASON_RESULT_NOT_FOUND);
            detail.setMessage("未找到执行结果，请确认计划是否已实际产生算法输出");
            return detail;
        }
        detail.setPlanId(saved.getPlanId());
        detail.setAlgId(saved.getAlgId());
        if (saved.getGenResults() == null || saved.getGenResults().isEmpty()) {
            detail.setStatus(STATUS_EMPTY);
            detail.setReasonCode(REASON_GEN_RESULTS_EMPTY);
            detail.setMessage("执行记录存在，但未保存任何分代结果");
            return detail;
        }

        String problemTag = detectProblemTag(saved);
        Map<String, String> probInstNameMap = resolveProbInstNameMap(saved.getGenResults());
        List<MetricRunCache> caches = new ArrayList<>();
        boolean cacheChanged = false;
        boolean hasPartial = false;
        List<ExeResultRunDetail> runs = new ArrayList<>();
        for (int i = 0; i < saved.getGenResults().size(); i++) {
            GenResult genResult = saved.getGenResults().get(i);
            ExeResultRunDetail run = buildRunDetail(i + 1, genResult, saved.getStartTime(), problemTag, probInstNameMap);
            runs.add(run);
            MetricRunCache nextCache = toCache(run, genResult == null ? null : genResult.getOutTime());
            caches.add(nextCache);
            MetricRunCache oldCache = findOldCache(saved.getMetricRuns(), nextCache.getRunIndex(), nextCache.getProbInstId(), nextCache.getOutputTime());
            if (!sameCache(oldCache, nextCache)) {
                cacheChanged = true;
            }
            if (!METRIC_OK.equals(run.getMetricStatus())) {
                hasPartial = true;
            }
        }
        detail.setRuns(runs);
        applyCoverageByPlan(planId, algId, runs);
        detail.setAggregate(buildAggregate(runs));

        if (cacheChanged || !METRIC_VERSION.equals(saved.getMetricVersion())) {
            algRltSaveDao.updateMetricCache(saved.getPlanExeResultId(), METRIC_VERSION, problemTag, REF_F1, REF_F2,
                    REF_FRONT_SIZE, caches, System.currentTimeMillis());
        }

        detail.setStatus(hasPartial ? STATUS_PARTIAL : STATUS_SUCCESS);
        detail.setReasonCode(hasPartial ? REASON_PARETO_PARSE_FAILED : REASON_OK);
        detail.setMessage(hasPartial ? "部分运行结果缺少可计算指标，请查看每行原因" : "执行结果已就绪");
        return detail;
    }

    private ExeResultRunDetail buildRunDetail(int runIndex, GenResult genResult, long startTime, String problemTag,
                                              Map<String, String> probInstNameMap) {
        ExeResultRunDetail run = new ExeResultRunDetail();
        run.setRunIndex(runIndex);
        run.setMetricStatus(METRIC_OK);
        run.setReasonCode(REASON_OK);
        run.setStartTime(formatTs(startTime));
        if (genResult == null) {
            run.setMetricStatus(STATUS_PARTIAL);
            run.setReasonCode(REASON_GEN_RESULTS_EMPTY);
            run.setParetoPoints(new ArrayList<>());
            run.setRawEachResults(new ArrayList<>());
            return run;
        }
        run.setProbInstId(genResult.getProbInstId());
        run.setProbInstName(resolveProbInstName(genResult.getProbInstId(), probInstNameMap));
        run.setOutputTime(formatTs(genResult.getOutTime()));
        List<EachResult> eachResults = genResult.getEachResults() == null ? new ArrayList<>() : genResult.getEachResults();
        run.setRawEachResults(eachResults);
        Map<String, String> kv = toKeyValue(eachResults);
        run.setRuntimeMs(parseLong(kv.get("runtimeMs")));
        run.setParetoSize(parseInteger(kv.get("paretoSize")));

        List<ParetoPoint> points = parseParetoPoints(eachResults);
        run.setParetoPoints(points);
        if (run.getParetoSize() == null) {
            run.setParetoSize(points.size());
        }
        if (points.isEmpty()) {
            run.setMetricStatus(STATUS_PARTIAL);
            run.setReasonCode(REASON_PARETO_PARSE_FAILED);
            return run;
        }
        run.setSpreadDelta(round6(computeSpreadDelta(points)));
        run.setSpacing(round6(computeSpacing(points)));
        if (!"ZDT1".equals(problemTag)) {
            run.setMetricStatus(STATUS_PARTIAL);
            run.setReasonCode(REASON_NO_REFERENCE_FRONT);
            return run;
        }
        run.setHv(round6(computeHv(points, REF_F1, REF_F2)));
        run.setIgdPlus(round6(computeIgdPlus(points, buildZdt1ReferenceFront(REF_FRONT_SIZE))));
        run.setGd(round6(computeGd(points, buildZdt1ReferenceFront(REF_FRONT_SIZE))));
        return run;
    }

    private Map<String, String> resolveProbInstNameMap(List<GenResult> genResults) {
        Map<String, String> map = new HashMap<>();
        if (genResults == null || genResults.isEmpty()) {
            return map;
        }
        for (GenResult genResult : genResults) {
            if (genResult == null || !StringUtils.hasText(genResult.getProbInstId())) {
                continue;
            }
            String instId = genResult.getProbInstId();
            if (map.containsKey(instId)) {
                continue;
            }
            if (probInstDao == null) {
                map.put(instId, instId);
                continue;
            }
            ProbInst probInst = probInstDao.getProbInstByID(instId);
            map.put(instId, probInst == null ? instId : probInst.getInstName());
        }
        return map;
    }

    private String resolveProbInstName(String probInstId, Map<String, String> probInstNameMap) {
        if (!StringUtils.hasText(probInstId)) {
            return "";
        }
        if (probInstNameMap == null) {
            return probInstId;
        }
        String name = probInstNameMap.get(probInstId);
        return StringUtils.hasText(name) ? name : probInstId;
    }

    private ExeResultAggregate buildAggregate(List<ExeResultRunDetail> runs) {
        ExeResultAggregate agg = new ExeResultAggregate();
        agg.setRunCount(runs == null ? 0 : runs.size());
        if (runs == null || runs.isEmpty()) {
            return agg;
        }
        agg.setRuntimeMsMean(round6(meanLong(runs, ExeResultRunDetail::getRuntimeMs)));
        agg.setHvMean(round6(meanDouble(runs, ExeResultRunDetail::getHv)));
        agg.setIgdPlusMean(round6(meanDouble(runs, ExeResultRunDetail::getIgdPlus)));
        agg.setGdMean(round6(meanDouble(runs, ExeResultRunDetail::getGd)));
        agg.setCoverageMean(round6(meanDouble(runs, ExeResultRunDetail::getCoverage)));
        agg.setSpreadDeltaMean(round6(meanDouble(runs, ExeResultRunDetail::getSpreadDelta)));
        agg.setSpacingMean(round6(meanDouble(runs, ExeResultRunDetail::getSpacing)));
        return agg;
    }

    private Double meanLong(List<ExeResultRunDetail> runs, java.util.function.Function<ExeResultRunDetail, Long> extractor) {
        long count = 0L;
        double sum = 0d;
        for (ExeResultRunDetail run : runs) {
            Long v = extractor.apply(run);
            if (v == null) {
                continue;
            }
            sum += v;
            count++;
        }
        return count == 0 ? null : (sum / count);
    }

    private Double meanDouble(List<ExeResultRunDetail> runs, java.util.function.Function<ExeResultRunDetail, Double> extractor) {
        long count = 0L;
        double sum = 0d;
        for (ExeResultRunDetail run : runs) {
            Double v = extractor.apply(run);
            if (v == null) {
                continue;
            }
            sum += v;
            count++;
        }
        return count == 0 ? null : (sum / count);
    }

    private MetricRunCache toCache(ExeResultRunDetail run, Long outTime) {
        MetricRunCache cache = new MetricRunCache();
        cache.setRunIndex(run.getRunIndex());
        cache.setProbInstId(run.getProbInstId());
        cache.setOutputTime(outTime);
        cache.setRuntimeMs(run.getRuntimeMs());
        cache.setParetoSize(run.getParetoSize());
        cache.setHv(run.getHv());
        cache.setIgdPlus(run.getIgdPlus());
        cache.setGd(run.getGd());
        cache.setCoverage(run.getCoverage());
        cache.setSpreadDelta(run.getSpreadDelta());
        cache.setSpacing(run.getSpacing());
        cache.setMetricStatus(run.getMetricStatus());
        cache.setReasonCode(run.getReasonCode());
        return cache;
    }

    private MetricRunCache findOldCache(List<MetricRunCache> old, Integer runIndex, String probInstId, Long outputTime) {
        if (old == null || old.isEmpty()) {
            return null;
        }
        for (MetricRunCache cache : old) {
            if (cache == null) {
                continue;
            }
            if (!safeEq(cache.getRunIndex(), runIndex)) {
                continue;
            }
            if (!safeEq(cache.getProbInstId(), probInstId)) {
                continue;
            }
            if (!safeEq(cache.getOutputTime(), outputTime)) {
                continue;
            }
            return cache;
        }
        return null;
    }

    private boolean sameCache(MetricRunCache old, MetricRunCache next) {
        if (old == null || next == null) {
            return false;
        }
        return safeEq(old.getRunIndex(), next.getRunIndex())
                && safeEq(old.getProbInstId(), next.getProbInstId())
                && safeEq(old.getOutputTime(), next.getOutputTime())
                && safeEq(old.getRuntimeMs(), next.getRuntimeMs())
                && safeEq(old.getParetoSize(), next.getParetoSize())
                && safeEq(old.getHv(), next.getHv())
                && safeEq(old.getIgdPlus(), next.getIgdPlus())
                && safeEq(old.getGd(), next.getGd())
                && safeEq(old.getCoverage(), next.getCoverage())
                && safeEq(old.getSpreadDelta(), next.getSpreadDelta())
                && safeEq(old.getSpacing(), next.getSpacing())
                && safeEq(old.getMetricStatus(), next.getMetricStatus())
                && safeEq(old.getReasonCode(), next.getReasonCode());
    }

    private <T> boolean safeEq(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private String detectProblemTag(PlanExeResult saved) {
        if (saved == null) {
            return "UNKNOWN";
        }
        String algId = saved.getAlgId() == null ? "" : saved.getAlgId().toLowerCase(Locale.ROOT);
        String algName = saved.getAlgName() == null ? "" : saved.getAlgName().toLowerCase(Locale.ROOT);
        if (algId.contains("zdt1") || algName.contains("zdt1")) {
            return "ZDT1";
        }
        List<GenResult> genResults = saved.getGenResults();
        if (genResults != null) {
            for (GenResult genResult : genResults) {
                if (genResult == null || genResult.getEachResults() == null) {
                    continue;
                }
                for (EachResult eachResult : genResult.getEachResults()) {
                    if (eachResult == null || !"problemFamily".equalsIgnoreCase(eachResult.getKey())) {
                        continue;
                    }
                    String value = eachResult.getValue();
                    if (StringUtils.hasText(value) && value.toUpperCase(Locale.ROOT).contains("ZDT1")) {
                        return "ZDT1";
                    }
                }
            }
        }
        return "UNKNOWN";
    }

    private Map<String, String> toKeyValue(List<EachResult> eachResults) {
        Map<String, String> map = new HashMap<>();
        if (eachResults == null) {
            return map;
        }
        for (EachResult r : eachResults) {
            if (r == null || !StringUtils.hasText(r.getKey())) {
                continue;
            }
            map.put(r.getKey(), r.getValue());
        }
        return map;
    }

    private List<ParetoPoint> parseParetoPoints(List<EachResult> eachResults) {
        List<ParetoPoint> points = new ArrayList<>();
        if (eachResults == null) {
            return points;
        }
        for (EachResult eachResult : eachResults) {
            if (eachResult == null || !StringUtils.hasText(eachResult.getKey())) {
                continue;
            }
            String key = eachResult.getKey().trim();
            if (!key.startsWith("paretoPoint_")) {
                continue;
            }
            String value = eachResult.getValue() == null ? "" : eachResult.getValue().trim();
            Matcher matcher = PAIR_PATTERN.matcher(value);
            if (!matcher.find()) {
                continue;
            }
            double f1 = parseDouble(matcher.group(1), Double.NaN);
            double f2 = parseDouble(matcher.group(2), Double.NaN);
            if (Double.isFinite(f1) && Double.isFinite(f2)) {
                points.add(new ParetoPoint(f1, f2));
            }
        }
        points.sort(Comparator.comparingDouble(ParetoPoint::getF1));
        return points;
    }

    private Double computeHv(List<ParetoPoint> points, double refF1, double refF2) {
        List<ParetoPoint> nd = nonDominated(points);
        if (nd.isEmpty()) {
            return null;
        }
        nd.sort(Comparator.comparingDouble(ParetoPoint::getF1));
        double hv = 0d;
        double prevF2 = refF2;
        for (ParetoPoint p : nd) {
            double x = clamp(p.getF1(), 0d, refF1);
            double y = clamp(p.getF2(), 0d, refF2);
            double width = refF1 - x;
            double currentTop = Math.min(prevF2, y);
            double height = prevF2 - currentTop;
            if (width > 0d && height > 0d) {
                hv += width * height;
            }
            prevF2 = Math.min(prevF2, y);
        }
        return hv;
    }

    private Double computeIgdPlus(List<ParetoPoint> approx, List<ParetoPoint> reference) {
        if (approx == null || approx.isEmpty() || reference == null || reference.isEmpty()) {
            return null;
        }
        double sum = 0d;
        for (ParetoPoint r : reference) {
            double min = Double.POSITIVE_INFINITY;
            for (ParetoPoint p : approx) {
                double d1 = Math.max(p.getF1() - r.getF1(), 0d);
                double d2 = Math.max(p.getF2() - r.getF2(), 0d);
                double d = Math.sqrt(d1 * d1 + d2 * d2);
                if (d < min) {
                    min = d;
                }
            }
            sum += min;
        }
        return sum / reference.size();
    }

    private Double computeGd(List<ParetoPoint> approx, List<ParetoPoint> reference) {
        if (approx == null || approx.isEmpty() || reference == null || reference.isEmpty()) {
            return null;
        }
        double sum = 0d;
        for (ParetoPoint p : approx) {
            double min = Double.POSITIVE_INFINITY;
            for (ParetoPoint r : reference) {
                double d = euclidean(p, r);
                if (d < min) {
                    min = d;
                }
            }
            sum += min;
        }
        return sum / approx.size();
    }

    private Double computeSpreadDelta(List<ParetoPoint> points) {
        if (points == null || points.size() < 2) {
            return null;
        }
        List<ParetoPoint> nd = nonDominated(points);
        if (nd.size() < 2) {
            return null;
        }
        nd.sort(Comparator.comparingDouble(ParetoPoint::getF1));
        List<Double> ds = new ArrayList<>();
        for (int i = 0; i < nd.size() - 1; i++) {
            ds.add(euclidean(nd.get(i), nd.get(i + 1)));
        }
        double dBar = ds.stream().mapToDouble(v -> v).average().orElse(0d);
        double df = euclidean(nd.get(0), new ParetoPoint(0d, 1d));
        double dl = euclidean(nd.get(nd.size() - 1), new ParetoPoint(1d, 0d));
        double num = df + dl;
        double den = df + dl + (nd.size() - 1) * dBar;
        for (Double d : ds) {
            num += Math.abs(d - dBar);
        }
        if (den <= 1e-12) {
            return null;
        }
        return num / den;
    }

    private Double computeSpacing(List<ParetoPoint> points) {
        if (points == null || points.size() < 2) {
            return null;
        }
        List<ParetoPoint> nd = nonDominated(points);
        if (nd.size() < 2) {
            return null;
        }
        List<Double> nearest = new ArrayList<>();
        for (int i = 0; i < nd.size(); i++) {
            double min = Double.POSITIVE_INFINITY;
            for (int j = 0; j < nd.size(); j++) {
                if (i == j) {
                    continue;
                }
                double d = euclidean(nd.get(i), nd.get(j));
                if (d < min) {
                    min = d;
                }
            }
            if (Double.isFinite(min)) {
                nearest.add(min);
            }
        }
        if (nearest.size() < 2) {
            return 0d;
        }
        double dBar = nearest.stream().mapToDouble(v -> v).average().orElse(0d);
        double sq = 0d;
        for (Double d : nearest) {
            sq += (d - dBar) * (d - dBar);
        }
        return Math.sqrt(sq / (nearest.size() - 1));
    }

    private void applyCoverageByPlan(String planId, String currentAlgId, List<ExeResultRunDetail> runs) {
        if (runs == null || runs.isEmpty() || !StringUtils.hasText(planId) || !StringUtils.hasText(currentAlgId)) {
            return;
        }
        Map<String, List<List<ParetoPoint>>> peerByRunKey = buildPeerRunFronts(planId, currentAlgId);
        for (ExeResultRunDetail run : runs) {
            if (run == null) {
                continue;
            }
            String runKey = buildRunKey(run.getRunIndex(), run.getProbInstId());
            List<List<ParetoPoint>> peers = peerByRunKey.get(runKey);
            if (peers == null || peers.isEmpty()) {
                // Coverage is a pairwise metric; when no peer algorithm is available,
                // keep it as N/A instead of downgrading an otherwise valid run.
                run.setCoverage(null);
                continue;
            }
            List<ParetoPoint> currentFront = nonDominated(run.getParetoPoints());
            if (currentFront.isEmpty()) {
                continue;
            }
            double sum = 0d;
            int count = 0;
            for (List<ParetoPoint> peerFront : peers) {
                Double c = computeCoverage(currentFront, peerFront);
                if (c == null) {
                    continue;
                }
                sum += c;
                count++;
            }
            run.setCoverage(count == 0 ? null : round6(sum / count));
        }
    }

    private Map<String, List<List<ParetoPoint>>> buildPeerRunFronts(String planId, String currentAlgId) {
        Map<String, List<List<ParetoPoint>>> out = new HashMap<>();
        List<PlanExeResult> peers = algRltSaveDao.listLatestByPlan(planId);
        for (PlanExeResult peer : peers) {
            if (peer == null || !StringUtils.hasText(peer.getAlgId())) {
                continue;
            }
            if (peer.getAlgId().equals(currentAlgId)) {
                continue;
            }
            List<GenResult> genResults = peer.getGenResults();
            if (genResults == null || genResults.isEmpty()) {
                continue;
            }
            for (int i = 0; i < genResults.size(); i++) {
                GenResult genResult = genResults.get(i);
                if (genResult == null) {
                    continue;
                }
                List<ParetoPoint> points = parseParetoPoints(genResult.getEachResults());
                if (points.isEmpty()) {
                    continue;
                }
                String key = buildRunKey(i + 1, genResult.getProbInstId());
                out.computeIfAbsent(key, k -> new ArrayList<>()).add(nonDominated(points));
            }
        }
        return out;
    }

    private String buildRunKey(Integer runIndex, String probInstId) {
        return String.valueOf(runIndex == null ? -1 : runIndex) + "::" + (probInstId == null ? "" : probInstId);
    }

    private Double computeCoverage(List<ParetoPoint> frontA, List<ParetoPoint> frontB) {
        if (frontA == null || frontA.isEmpty() || frontB == null || frontB.isEmpty()) {
            return null;
        }
        int dominatedCount = 0;
        for (ParetoPoint b : frontB) {
            if (b == null) {
                continue;
            }
            boolean covered = false;
            for (ParetoPoint a : frontA) {
                if (a == null) {
                    continue;
                }
                if (dominates(a, b)) {
                    covered = true;
                    break;
                }
            }
            if (covered) {
                dominatedCount++;
            }
        }
        if (frontB.isEmpty()) {
            return null;
        }
        return ((double) dominatedCount) / ((double) frontB.size());
    }

    private List<ParetoPoint> nonDominated(List<ParetoPoint> points) {
        List<ParetoPoint> out = new ArrayList<>();
        if (points == null) {
            return out;
        }
        for (ParetoPoint p : points) {
            if (p == null) {
                continue;
            }
            boolean dominated = false;
            for (ParetoPoint q : points) {
                if (q == null || q == p) {
                    continue;
                }
                if (dominates(q, p)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                out.add(p);
            }
        }
        return out;
    }

    private boolean dominates(ParetoPoint a, ParetoPoint b) {
        boolean betterAny = false;
        if (a.getF1() > b.getF1() || a.getF2() > b.getF2()) {
            return false;
        }
        if (a.getF1() < b.getF1()) {
            betterAny = true;
        }
        if (a.getF2() < b.getF2()) {
            betterAny = true;
        }
        return betterAny;
    }

    private List<ParetoPoint> buildZdt1ReferenceFront(int size) {
        int n = Math.max(size, 20);
        List<ParetoPoint> ref = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double f1 = (double) i / (double) (n - 1);
            double f2 = 1d - Math.sqrt(f1);
            ref.add(new ParetoPoint(f1, f2));
        }
        return ref;
    }

    private String formatTs(long ts) {
        if (ts <= 0) {
            return "";
        }
        return DT_FMT.format(Instant.ofEpochMilli(ts));
    }

    private Long parseLong(String text) {
        try {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return Long.parseLong(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(String text) {
        try {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private double parseDouble(String text, double dft) {
        try {
            if (!StringUtils.hasText(text)) {
                return dft;
            }
            return Double.parseDouble(text.trim());
        } catch (Exception ex) {
            return dft;
        }
    }

    private double euclidean(ParetoPoint a, ParetoPoint b) {
        double d1 = a.getF1() - b.getF1();
        double d2 = a.getF2() - b.getF2();
        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private Double round6(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        return Double.valueOf(String.format(Locale.US, "%.6f", value));
    }
}
