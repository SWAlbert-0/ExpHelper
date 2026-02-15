package fjnu.edu.dao;

import fjnu.edu.entity.PlanExeResult;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlgRltSaveDao {
    @Autowired
    MongoTemplate mongoTemplate;

    public boolean insertPlanExeResult(PlanExeResult planExeResult){
        mongoTemplate.insert(planExeResult,"algRltSave");
        return true;
    }



    public PlanExeResult  getAlgSaveByAlgName(String planId, String algId, String algName) {
        Criteria criteria = Criteria.where("planId").is(planId).and("algId").is(algId).and("algName").is(algName);
        Query query = new Query(criteria);
        PlanExeResult planExeResults = mongoTemplate.findOne(query, PlanExeResult.class, "algRltSave");
        return planExeResults;
    }
}
