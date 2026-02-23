package fjnu.edu.dao;

import fjnu.edu.entity.PlanExeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                .set("genResults", planExeResult.getGenResults());
        mongoTemplate.upsert(query, update, PlanExeResult.class, "algRltSave");
        return true;
    }



    public PlanExeResult  getAlgSaveByAlgName(String planId, String algId, String algName) {
        Query byAlgId = new Query(Criteria.where("planId").is(planId).and("algId").is(algId))
                .with(Sort.by(Sort.Order.desc("outputTime")))
                .limit(1);
        PlanExeResult latest = mongoTemplate.findOne(byAlgId, PlanExeResult.class, "algRltSave");
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
}
