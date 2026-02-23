package fjnu.edu.dao;

import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.entity.MetricRunCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AlgRltSaveDao {
    @Autowired
    MongoTemplate mongoTemplate;

    public boolean insertPlanExeResult(PlanExeResult planExeResult){
        Criteria criteria = Criteria.where("planId").is(planExeResult.getPlanId())
                .and("algId").is(planExeResult.getAlgId())
                .and("algName").is(planExeResult.getAlgName());
        Query query = new Query(criteria);
        Update update = new Update()
                .set("planId", planExeResult.getPlanId())
                .set("algId", planExeResult.getAlgId())
                .set("algName", planExeResult.getAlgName())
                .set("runNum", planExeResult.getRunNum())
                .set("startTime", planExeResult.getStartTime())
                .set("outputTime", planExeResult.getOutputTime())
                .set("genResults", planExeResult.getGenResults())
                .set("metricVersion", null)
                .set("metricComputedAt", 0L)
                .set("metricProblemTag", null)
                .set("refPointF1", null)
                .set("refPointF2", null)
                .set("refFrontSize", null)
                .set("metricRuns", null);
        mongoTemplate.upsert(query, update, PlanExeResult.class, "algRltSave");
        return true;
    }


    public PlanExeResult getLatestByPlanAndAlg(String planId, String algId) {
        if (!StringUtils.hasText(planId) || !StringUtils.hasText(algId)) {
            return null;
        }
        Query byAlgId = new Query(Criteria.where("planId").is(planId).and("algId").is(algId))
                .with(Sort.by(Sort.Order.desc("outputTime")))
                .limit(1);
        return mongoTemplate.findOne(byAlgId, PlanExeResult.class, "algRltSave");
    }

    public PlanExeResult  getAlgSaveByAlgName(String planId, String algId, String algName) {
        PlanExeResult latest = getLatestByPlanAndAlg(planId, algId);
        if (latest != null) {
            return latest;
        }
        if (!StringUtils.hasText(algName)) {
            return null;
        }
        Query fallback = new Query(Criteria.where("planId").is(planId).and("algName").is(algName))
                .with(Sort.by(Sort.Order.desc("outputTime")))
                .limit(1);
        return mongoTemplate.findOne(fallback, PlanExeResult.class, "algRltSave");
    }

    public void updateMetricCache(String planExeResultId,
                                  String metricVersion,
                                  String metricProblemTag,
                                  Double refPointF1,
                                  Double refPointF2,
                                  Integer refFrontSize,
                                  List<MetricRunCache> metricRuns,
                                  long metricComputedAt) {
        if (!StringUtils.hasText(planExeResultId)) {
            return;
        }
        Query query = new Query(Criteria.where("_id").is(planExeResultId));
        Update update = new Update()
                .set("metricVersion", metricVersion)
                .set("metricProblemTag", metricProblemTag)
                .set("refPointF1", refPointF1)
                .set("refPointF2", refPointF2)
                .set("refFrontSize", refFrontSize)
                .set("metricRuns", metricRuns)
                .set("metricComputedAt", metricComputedAt);
        mongoTemplate.updateFirst(query, update, PlanExeResult.class, "algRltSave");
    }
}
