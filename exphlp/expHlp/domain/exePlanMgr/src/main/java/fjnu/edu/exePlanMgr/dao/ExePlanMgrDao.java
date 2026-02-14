package fjnu.edu.exePlanMgr.dao;

import com.mongodb.client.result.UpdateResult;
import fjnu.edu.exePlanMgr.Constant.Constant;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class ExePlanMgrDao {
    @Autowired
    MongoTemplate mongoTemplate;


    public List<ExePlan> getExePlans(int pageNum, int pageSize) {
        //创建查询对象
        if (pageNum != 0) {
            pageNum--;
        }
        Sort sort = Sort.by(Sort.Order.desc("_id"));
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Query query = new Query().with(pageable).with(sort);
        List<ExePlan> exeplanlist = mongoTemplate.find(query, ExePlan.class, "exePlanMgr");
        return exeplanlist;
    }

    /***
     *@Description 插入计划，返回MongoDB自生成的_id(插入之前判断计划名是否重名以及是否为空）
     * @param exeplan 计划
     *@return {@link String} MongoDB自生成的_id
     *@Date 2022/7/27 9:12
     */
    public String addExePlan(ExePlan exeplan) throws Exception {
        String planName = exeplan.getPlanName();
        if(StringUtils.isEmpty(planName)){
            throw new Exception("planName is empty!");
        }
        if(getExePlanByName(planName) != null){
            throw new Exception("planName is used!");
        }
        return mongoTemplate.insert(exeplan, "exePlanMgr").getPlanId();
    }

    public ExePlan getExePlanById(String planId) {
        Criteria criteria = Criteria.where("_id").is(new BsonObjectId(new ObjectId(planId)));
        Query query = new Query(criteria);
        ExePlan exePlan = mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
        return exePlan;
    }

    //根据执行计划名字获取执行计划实体
    public ExePlan getExePlanByName(String planName) {
        //创建条件对象
        Criteria criteria = Criteria.where("planName").is(planName);
        //创建查询对象，然后将条件对象添加到其中
        Query query = new Query(criteria);
        //查询结果
        ExePlan exePlan = mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
        return exePlan;
    }

    //通过执行计划ID删除指定的执行计划
    public boolean deleteExePlanById(String planId) {
        Criteria criteria = Criteria.where("_id").is(new BsonObjectId(new ObjectId(planId)));
        Query query = new Query(criteria);
        ExePlan exeplan = mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
        if (exeplan.getExeState() != Constant.IN_EXECUTION) {                  //未在执行中
            mongoTemplate.remove(query, "exePlanMgr");
        }
        return true;
    }

    //通过Id修改执行计划
    public boolean updateExePlanById(ExePlan exeplan) {
        Criteria criteria = Criteria.where("_id").is(new BsonObjectId(new ObjectId(exeplan.getPlanId())));
        Query query = new Query(criteria);
        Update update = new Update().set("planName", exeplan.getPlanName())
                .set("probInstId",exeplan.getProbInstIds())
                .set("algRunInfos",exeplan.getAlgRunInfos())
                .set("userIds",exeplan.getUserIds())
                .set("description", exeplan.getDescription())
                .set("exeStartTime", exeplan.getExeStartTime())
                .set("exeEndTime", exeplan.getExeEndTime())
                .set("exeState", exeplan.getExeState());
        UpdateResult result = mongoTemplate.updateFirst(query, update, ExePlan.class, "exePlanMgr");
        return true;
    }

    public long countAllExePlans() {
        long count = mongoTemplate.count(new Query(), ExePlan.class, "exePlanMgr");
        return count;
    }


}
