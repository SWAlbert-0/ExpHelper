package fjnu.edu.exePlanMgr.dao;

import fjnu.edu.exePlanMgr.Constant.Constant;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.ExePlan;
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
import java.util.Collections;

@Service
public class ExePlanMgrDao {
    @Autowired
    MongoTemplate mongoTemplate;


    public List<ExePlan> getExePlans(int pageNum, int pageSize) {
        //创建查询对象
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageNum != 0) {
            pageNum--;
        }
        if (pageSize <= 0) {
            pageSize = 10;
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
        if (exeplan == null) {
            throw new Exception("exeplan is null!");
        }
        String planName = exeplan.getPlanName();
        if(!StringUtils.hasText(planName)){
            throw new Exception("planName is empty!");
        }
        if(getExePlanByName(planName) != null){
            throw new Exception("planName is used!");
        }
        return mongoTemplate.insert(exeplan, "exePlanMgr").getPlanId();
    }

    public ExePlan getExePlanById(String planId) {
        if (!StringUtils.hasText(planId)) {
            return null;
        }
        Query query = new Query(buildIdCriteria(planId));
        ExePlan exePlan = mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
        return exePlan;
    }

    //根据执行计划名字获取执行计划实体
    public ExePlan getExePlanByName(String planName) {
        if (!StringUtils.hasText(planName)) {
            return null;
        }
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
        if (!StringUtils.hasText(planId)) {
            return false;
        }
        Query query = new Query(buildIdCriteria(planId));
        ExePlan exeplan = mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
        if (exeplan == null) {
            return false;
        }
        if (exeplan.getExeState() != Constant.IN_EXECUTION) {                  //未在执行中
            mongoTemplate.remove(query, "exePlanMgr");
        }
        return true;
    }

    //通过Id修改执行计划
    public boolean updateExePlanById(ExePlan exeplan) {
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return false;
        }
        Query query = new Query(buildIdCriteria(exeplan.getPlanId()));
        Update update = new Update().set("planName", exeplan.getPlanName())
                .set("probInstIds",exeplan.getProbInstIds())
                .set("algRunInfos",exeplan.getAlgRunInfos())
                .set("userIds",exeplan.getUserIds())
                .set("description", exeplan.getDescription())
                .set("exeStartTime", exeplan.getExeStartTime())
                .set("exeEndTime", exeplan.getExeEndTime())
                .set("exeState", exeplan.getExeState())
                .set("lastError", exeplan.getLastError());
        mongoTemplate.updateFirst(query, update, ExePlan.class, "exePlanMgr");
        return true;
    }

    public long countAllExePlans() {
        long count = mongoTemplate.count(new Query(), ExePlan.class, "exePlanMgr");
        return count;
    }

    public long countPlansByProbInstId(String probInstId) {
        if (!StringUtils.hasText(probInstId)) {
            return 0L;
        }
        Query query = new Query(Criteria.where("probInstIds").in(probInstId));
        return mongoTemplate.count(query, ExePlan.class, "exePlanMgr");
    }

    public List<String> listPlanNamesByProbInstId(String probInstId, int limit) {
        if (!StringUtils.hasText(probInstId)) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 5 : limit;
        Query query = new Query(Criteria.where("probInstIds").in(probInstId))
                .with(Sort.by(Sort.Order.desc("_id")))
                .limit(safeLimit);
        query.fields().include("planName");
        List<ExePlan> plans = mongoTemplate.find(query, ExePlan.class, "exePlanMgr");
        return plans.stream()
                .map(ExePlan::getPlanName)
                .filter(StringUtils::hasText)
                .toList();
    }

    public long countPlansByAlgId(String algId) {
        if (!StringUtils.hasText(algId)) {
            return 0L;
        }
        Query query = new Query(Criteria.where("algRunInfos.algId").is(algId));
        return mongoTemplate.count(query, ExePlan.class, "exePlanMgr");
    }

    public List<String> listPlanNamesByAlgId(String algId, int limit) {
        if (!StringUtils.hasText(algId)) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 5 : limit;
        Query query = new Query(Criteria.where("algRunInfos.algId").is(algId))
                .with(Sort.by(Sort.Order.desc("_id")))
                .limit(safeLimit);
        query.fields().include("planName");
        List<ExePlan> plans = mongoTemplate.find(query, ExePlan.class, "exePlanMgr");
        return plans.stream()
                .map(ExePlan::getPlanName)
                .filter(StringUtils::hasText)
                .toList();
    }

    public void appendPlanLog(ExePlanLog exePlanLog) {
        if (exePlanLog == null || !StringUtils.hasText(exePlanLog.getPlanId())) {
            return;
        }
        Query latestQuery = new Query(Criteria.where("planId").is(exePlanLog.getPlanId()))
                .with(Sort.by(Sort.Order.desc("seq")))
                .limit(1);
        ExePlanLog latest = mongoTemplate.findOne(latestQuery, ExePlanLog.class, "exePlanLog");
        long nextSeq = latest == null ? 1L : latest.getSeq() + 1L;
        exePlanLog.setSeq(nextSeq);
        if (exePlanLog.getTs() <= 0) {
            exePlanLog.setTs(System.currentTimeMillis());
        }
        mongoTemplate.insert(exePlanLog, "exePlanLog");
    }

    public List<ExePlanLog> getPlanLogs(String planId, long afterSeq, int limit) {
        if (!StringUtils.hasText(planId)) {
            return java.util.Collections.emptyList();
        }
        if (limit <= 0) {
            limit = 200;
        }
        Query query = new Query(Criteria.where("planId").is(planId).and("seq").gt(afterSeq))
                .with(Sort.by(Sort.Order.asc("seq")))
                .limit(limit);
        return mongoTemplate.find(query, ExePlanLog.class, "exePlanLog");
    }

    public long getLatestPlanLogSeq(String planId) {
        if (!StringUtils.hasText(planId)) {
            return 0L;
        }
        Query latestQuery = new Query(Criteria.where("planId").is(planId))
                .with(Sort.by(Sort.Order.desc("seq")))
                .limit(1);
        ExePlanLog latest = mongoTemplate.findOne(latestQuery, ExePlanLog.class, "exePlanLog");
        return latest == null ? 0L : latest.getSeq();
    }

    private Criteria buildIdCriteria(String planId) {
        if (!StringUtils.hasText(planId)) {
            return Criteria.where("_id").is(planId);
        }
        Criteria stringIdCriteria = Criteria.where("_id").is(planId);
        if (ObjectId.isValid(planId)) {
            return new Criteria().orOperator(
                    stringIdCriteria,
                    Criteria.where("_id").is(new ObjectId(planId))
            );
        }
        return stringIdCriteria;
    }


}
