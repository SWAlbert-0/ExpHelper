package fjnu.edu.exePlanMgr.dao;

import com.mongodb.client.MongoCollection;
import fjnu.edu.exePlanMgr.Constant.Constant;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanDeleteResult;
import fjnu.edu.common.utils.mongo.MongoIdCompatSupport;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

@Service
public class ExePlanMgrDao {
    @Autowired
    MongoTemplate mongoTemplate;


    public List<ExePlan> getExePlans(int pageNum, int pageSize) {
        return getExePlans(pageNum, pageSize, null);
    }

    public List<ExePlan> getExePlans(int pageNum, int pageSize, String scope) {
        return getExePlans(pageNum, pageSize, scope, null, null, null, null);
    }

    public List<ExePlan> getExePlans(int pageNum, int pageSize, String scope, String planName, Integer exeState, Long exeStartTime, Long exeEndTime) {
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
        Query query = buildPlanQuery(scope, planName, exeState, exeStartTime, exeEndTime).with(pageable).with(sort);
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
        ExePlan byRawId = findOneByRawStringId(planId);
        if (byRawId != null) {
            return byRawId;
        }
        if (ObjectId.isValid(planId)) {
            ExePlan byObjectId = findOneByRawField("_id", new ObjectId(planId));
            if (byObjectId != null) {
                return byObjectId;
            }
        }
        ExePlan byLegacyPlanId = findOneByRawField("planId", planId);
        if (byLegacyPlanId != null) {
            return byLegacyPlanId;
        }
        if (ObjectId.isValid(planId)) {
            return findOneByRawField("planId", new ObjectId(planId));
        }
        return null;
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
    public ExePlanDeleteResult deleteExePlanById(String planId) {
        ExePlanDeleteResult result = new ExePlanDeleteResult();
        String normalizedPlanId = MongoIdCompatSupport.normalizeId(planId);
        if (!StringUtils.hasText(normalizedPlanId)) {
            result.setDeletedCount(0L);
            result.setExisted(false);
            result.setNoop(true);
            result.setVerified(true);
            result.setBlocked(false);
            return result;
        }
        ExePlan exeplan = getExePlanById(normalizedPlanId);
        if (exeplan == null) {
            result.setDeletedCount(0L);
            result.setExisted(false);
            result.setNoop(true);
            result.setVerified(true);
            result.setBlocked(false);
            return result;
        }
        if (exeplan.getExeState() == Constant.IN_EXECUTION) {
            result.setDeletedCount(0L);
            result.setExisted(true);
            result.setNoop(true);
            result.setVerified(true);
            result.setBlocked(true);
            return result;
        }
        long deletedCount = 0L;
        // 兼容历史数据：_id / planId 可能是 string 或 ObjectId，多路径兜底删除。
        deletedCount += deleteByRawStringField("_id", normalizedPlanId);
        deletedCount += deleteByRawStringField("planId", normalizedPlanId);
        if (ObjectId.isValid(normalizedPlanId)) {
            ObjectId objectId = new ObjectId(normalizedPlanId);
            deletedCount += deleteByRawField("_id", objectId);
            deletedCount += deleteByRawField("planId", objectId);
        }
        boolean stillExists = getExePlanById(normalizedPlanId) != null;
        result.setDeletedCount(deletedCount);
        result.setExisted(true);
        result.setNoop(deletedCount <= 0);
        result.setVerified(!stillExists);
        result.setBlocked(false);
        return result;
    }

    //通过Id修改执行计划
    public boolean updateExePlanById(ExePlan exeplan) {
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return false;
        }
        Query query = new Query(new Criteria().andOperator(
                buildIdCriteria(exeplan.getPlanId()),
                Criteria.where("exeState").is(Constant.NON_EXECUTION)
        ));
        Update update = buildExePlanUpdate(exeplan);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ExePlan.class, "exePlanMgr");
        return result != null && result.getMatchedCount() > 0;
    }

    /**
     * 执行链路专用更新：不再要求当前状态必须是“未执行”，用于执行中->结束状态回写。
     */
    public boolean updateExePlanExecutionById(ExePlan exeplan) {
        if (exeplan == null || !StringUtils.hasText(exeplan.getPlanId())) {
            return false;
        }
        Query query = new Query(buildIdCriteria(exeplan.getPlanId()));
        Update update = buildExePlanUpdate(exeplan);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ExePlan.class, "exePlanMgr");
        return result != null && result.getMatchedCount() > 0;
    }

    public long countAllExePlans() {
        return countAllExePlans(null);
    }

    public long countAllExePlans(String scope) {
        return countAllExePlans(scope, null, null, null, null);
    }

    public long countAllExePlans(String scope, String planName, Integer exeState, Long exeStartTime, Long exeEndTime) {
        Query query = buildPlanQuery(scope, planName, exeState, exeStartTime, exeEndTime);
        return mongoTemplate.count(query, ExePlan.class, "exePlanMgr");
    }

    private Query buildPlanQuery(String scope, String planName, Integer exeState, Long exeStartTime, Long exeEndTime) {
        Query query = new Query();
        String normalizedScope = scope == null ? "" : scope.trim().toLowerCase();
        if ("current".equals(normalizedScope)) {
            query.addCriteria(Criteria.where("exeState").in(Constant.NON_EXECUTION, Constant.IN_EXECUTION));
        } else if ("history".equals(normalizedScope)) {
            query.addCriteria(Criteria.where("exeState").in(Constant.ABNORMAL_TERMINATION, Constant.NORMAL_TERMINATION));
        }
        if (StringUtils.hasText(planName)) {
            String safe = Pattern.quote(planName.trim());
            query.addCriteria(Criteria.where("planName").regex(".*" + safe + ".*", "i"));
        }
        if (exeState != null && exeState > 0) {
            query.addCriteria(Criteria.where("exeState").is(exeState));
        }
        if (exeStartTime != null && exeStartTime > 0) {
            query.addCriteria(Criteria.where("exeStartTime").gte(exeStartTime));
        }
        if (exeEndTime != null && exeEndTime > 0) {
            query.addCriteria(Criteria.where("exeEndTime").lte(exeEndTime));
        }
        return query;
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

    public List<ExePlanLog> getPlanLogs(String planId, String executionId, long afterSeq, int limit) {
        if (!StringUtils.hasText(planId)) {
            return java.util.Collections.emptyList();
        }
        if (limit <= 0) {
            limit = 200;
        }
        Criteria criteria = Criteria.where("planId").is(planId).and("seq").gt(afterSeq);
        if (StringUtils.hasText(executionId)) {
            criteria = criteria.and("executionId").is(executionId);
        }
        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.asc("seq")))
                .limit(limit);
        return mongoTemplate.find(query, ExePlanLog.class, "exePlanLog");
    }

    public long getLatestPlanLogSeq(String planId, String executionId) {
        if (!StringUtils.hasText(planId)) {
            return 0L;
        }
        Criteria criteria = Criteria.where("planId").is(planId);
        if (StringUtils.hasText(executionId)) {
            criteria = criteria.and("executionId").is(executionId);
        }
        Query latestQuery = new Query(criteria)
                .with(Sort.by(Sort.Order.desc("seq")))
                .limit(1);
        ExePlanLog latest = mongoTemplate.findOne(latestQuery, ExePlanLog.class, "exePlanLog");
        return latest == null ? 0L : latest.getSeq();
    }

    public List<ExePlanLog> listPlanLogs(String planId, String executionId, int limit) {
        if (!StringUtils.hasText(planId)) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 5000 : Math.min(limit, 20000);
        Criteria criteria = Criteria.where("planId").is(planId);
        if (StringUtils.hasText(executionId)) {
            criteria = criteria.and("executionId").is(executionId);
        }
        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.asc("seq")))
                .limit(safeLimit);
        return mongoTemplate.find(query, ExePlanLog.class, "exePlanLog");
    }

    /**
     * 兜底修复：若计划状态仍为“执行中”，但日志中已出现 PLAN_DONE/PLAN_FAIL，
     * 则自动回写为“正常结束/异常结束”。
     */
    public boolean repairExecutionStateFromLogs(String planId, String executionId) {
        if (!StringUtils.hasText(planId)) {
            return false;
        }
        ExePlan plan = getExePlanById(planId);
        if (plan == null || plan.getExeState() != Constant.IN_EXECUTION) {
            return false;
        }
        Criteria base = Criteria.where("planId").is(planId);
        if (StringUtils.hasText(executionId)) {
            base = base.and("executionId").is(executionId);
        }
        Query latestTerminalQuery = new Query(new Criteria().andOperator(
                base,
                Criteria.where("stage").in(Arrays.asList("PLAN_DONE", "PLAN_FAIL"))
        )).with(Sort.by(Sort.Order.desc("seq"))).limit(1);
        ExePlanLog latestTerminal = mongoTemplate.findOne(latestTerminalQuery, ExePlanLog.class, "exePlanLog");
        if (latestTerminal == null) {
            return false;
        }

        int targetState = "PLAN_DONE".equals(latestTerminal.getStage())
                ? Constant.NORMAL_TERMINATION
                : Constant.ABNORMAL_TERMINATION;
        long ts = latestTerminal.getTs() > 0 ? latestTerminal.getTs() : System.currentTimeMillis();
        Query updateQuery = new Query(new Criteria().andOperator(
                buildIdCriteria(planId),
                Criteria.where("exeState").is(Constant.IN_EXECUTION)
        ));
        Update update = new Update()
                .set("exeState", targetState)
                .set("exeEndTime", ts);
        if (targetState == Constant.NORMAL_TERMINATION) {
            update.set("lastError", null);
        } else if (!StringUtils.hasText(plan.getLastError())) {
            update.set("lastError", "执行失败（状态由日志兜底修复）");
        }
        ExePlan repaired = mongoTemplate.findAndModify(updateQuery, update, FindAndModifyOptions.options().returnNew(true),
                ExePlan.class, "exePlanMgr");
        return repaired != null;
    }

    private Criteria buildIdCriteria(String planId) {
        if (!StringUtils.hasText(planId)) {
            return Criteria.where("_id").is(planId);
        }
        Criteria stringIdCriteria = Criteria.where("_id").is(planId);
        Criteria legacyStringIdCriteria = Criteria.where("planId").is(planId);
        if (ObjectId.isValid(planId)) {
            return new Criteria().orOperator(
                    stringIdCriteria,
                    Criteria.where("_id").is(new ObjectId(planId)),
                    legacyStringIdCriteria,
                    Criteria.where("planId").is(new ObjectId(planId))
            );
        }
        return new Criteria().orOperator(stringIdCriteria, legacyStringIdCriteria);
    }

    private ExePlan findOneByRawField(String fieldName, Object value) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }
        Query query = new Query(Criteria.where(fieldName).is(value));
        return mongoTemplate.findOne(query, ExePlan.class, "exePlanMgr");
    }

    private Update buildExePlanUpdate(ExePlan exeplan) {
        return new Update()
                .set("planName", exeplan.getPlanName())
                .set("probInstIds", exeplan.getProbInstIds())
                .set("algRunInfos", exeplan.getAlgRunInfos())
                .set("userIds", exeplan.getUserIds())
                .set("description", exeplan.getDescription())
                .set("exeStartTime", exeplan.getExeStartTime())
                .set("exeEndTime", exeplan.getExeEndTime())
                .set("exeState", exeplan.getExeState())
                .set("lastError", exeplan.getLastError())
                .set("executionId", exeplan.getExecutionId());
    }

    private long deleteByRawField(String fieldName, Object value) {
        if (!StringUtils.hasText(fieldName)) {
            return 0L;
        }
        Query query = new Query(Criteria.where(fieldName).is(value));
        return mongoTemplate.remove(query, "exePlanMgr").getDeletedCount();
    }

    private long deleteByRawStringField(String fieldName, String value) {
        if (!StringUtils.hasText(fieldName) || value == null) {
            return 0L;
        }
        MongoCollection<Document> collection = mongoTemplate.getCollection("exePlanMgr");
        if (collection == null) {
            return 0L;
        }
        return collection.deleteMany(new Document(fieldName, value)).getDeletedCount();
    }

    private ExePlan findOneByRawStringId(String planId) {
        if (!StringUtils.hasText(planId)) {
            return null;
        }
        MongoCollection<Document> collection = mongoTemplate.getCollection("exePlanMgr");
        if (collection == null) {
            return null;
        }
        Document byUnderscoreId = collection.find(new Document("_id", planId)).first();
        if (byUnderscoreId != null) {
            return mongoTemplate.getConverter().read(ExePlan.class, byUnderscoreId);
        }
        Document byLegacyPlanId = collection.find(new Document("planId", planId)).first();
        if (byLegacyPlanId != null) {
            return mongoTemplate.getConverter().read(ExePlan.class, byLegacyPlanId);
        }
        return null;
    }


}
