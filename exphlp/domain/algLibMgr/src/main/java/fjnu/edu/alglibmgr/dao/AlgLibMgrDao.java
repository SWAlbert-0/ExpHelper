package fjnu.edu.alglibmgr.dao;

import fjnu.edu.alglibmgr.entity.DefPara ;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import fjnu.edu.common.utils.mongo.MongoIdCompatSupport;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class AlgLibMgrDao {
    @Autowired
    MongoTemplate mongoTemplate;



    //分页获取算法列表
    public List<AlgInfo> getAlgInfos(int pageNum, int pageSize){
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageNum != 0){
            pageNum--;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        Sort sort = Sort.by(Sort.Order.desc("_id"));
        Pageable pageable = PageRequest.of(pageNum,pageSize);
        Query query = new Query().with(pageable).with(sort);
        List<AlgInfo> algInfos = mongoTemplate.find(query, AlgInfo.class,"algLibMgr");

        return algInfos;
    }

    //通过Id查找算法
    public AlgInfo getAlgInfoById(String algId){
        if (!StringUtils.hasText(algId)) {
            return null;
        }
        Query query = new Query(buildIdCriteria(algId));
        AlgInfo algInfo = mongoTemplate.findOne(query, AlgInfo.class,"algLibMgr");

        return algInfo;
    }

    //通过算法Id获得指定算法的参数
    public List<DefPara> getParasByAlgInfoId(String algId){
        if (!StringUtils.hasText(algId)) {
            return Collections.emptyList();
        }
        Query query = new Query(buildIdCriteria(algId));
        AlgInfo algInfo = mongoTemplate.findOne(query, AlgInfo.class,"algLibMgr");
        return algInfo == null || algInfo.getDefParas() == null ? Collections.emptyList() : algInfo.getDefParas();
    }


    //通过算法名查找算法
    public List<AlgInfo> getAlgInfoByName(String algName,int pageNum,int pageSize){
        if (!StringUtils.hasText(algName)) {
            return Collections.emptyList();
        }

        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageNum != 0) {
            pageNum--;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Criteria criteria = Criteria.where("algName").is(algName);
        Query query = new Query(criteria).with(pageable);
        List<AlgInfo> algInfos = mongoTemplate.find(query, AlgInfo.class, "algLibMgr");

        return algInfos;
    }
    public AlgInfo getAlgInfoByName(String algName){


        Criteria criteria = Criteria.where("algName").is(algName);
        Query query = new Query(criteria);
        AlgInfo algInfo = mongoTemplate.findOne(query, AlgInfo.class, "algLibMgr");

        return algInfo;
    }

    //增加一个算法
    public boolean addAlgInfo(AlgInfo algInfo){

        mongoTemplate.insert(algInfo,"algLibMgr");

        return true;
    }



    //通过Id删除算法
    public AlgDeleteResult deleteAlgInfoById(String algId){
        String normalizedId = MongoIdCompatSupport.normalizeId(algId);
        AlgDeleteResult deleteResult = new AlgDeleteResult();
        if (!StringUtils.hasText(normalizedId)) {
            deleteResult.setDeletedCount(0L);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(true);
            deleteResult.setVerified(true);
            return deleteResult;
        }
        long deletedCount = 0L;
        boolean repaired = false;

        // 兼容历史脏数据：业务主键可能落在 _id 或 algId，且 _id 可能是 string/ObjectId。
        // 1) 优先按字符串 _id 删除（兼容历史 string _id）
        deletedCount += deleteByRawField("_id", normalizedId);
        if (deletedCount <= 0 && ObjectId.isValid(normalizedId)) {
            // 2) 再按 ObjectId _id 删除
            deletedCount += deleteByRawField("_id", new ObjectId(normalizedId));
        }
        if (deletedCount <= 0) {
            // 3) 使用映射层 Query 删除，兼容底层驱动匹配差异
            deletedCount += deleteByQuery(buildIdCriteria(normalizedId));
        }
        if (deletedCount <= 0) {
            // 4) 兼容历史脏数据：曾有文档将业务id保存在 algId 字段而非 _id
            long legacyDeleted = deleteByRawField("algId", normalizedId);
            if (legacyDeleted <= 0) {
                legacyDeleted = deleteByQuery(Criteria.where("algId").is(normalizedId));
            }
            deletedCount += legacyDeleted;
            repaired = legacyDeleted > 0;
        }

        deleteResult.setDeletedCount(deletedCount);
        deleteResult.setRepaired(repaired);
        deleteResult.setNoop(deletedCount <= 0);
        deleteResult.setVerified(!existsByAnyKnownId(normalizedId));
        return deleteResult;
    }

    //更新算法
    public boolean updateAlgInfoById(AlgInfo algInfo){
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgId())) {
            return false;
        }
        Query query = new Query(buildIdCriteria(algInfo.getAlgId()));

        Update update = new Update().set("algName", algInfo.getAlgName())
                .set("serviceName", algInfo.getServiceName())
                .set("defParas", algInfo.getDefParas()).set("description", algInfo.getDescription());
        mongoTemplate.updateFirst(query,update, AlgInfo.class,"algLibMgr");

        return true;
    }

    // 计算算法个数
    public long countAllAlgs() {
        long count = mongoTemplate.count(new Query(), AlgInfo.class, "algLibMgr");
        return count;
    }

    // 计算通过名字查询的算法个数
    public long countAlgsByAlgName(String algName) {
        Criteria criteria = Criteria.where("algName").is(algName);
        long count = mongoTemplate.count(new Query(criteria), AlgInfo.class, "algLibMgr");
        return count;
    }

     // 根据id获得算法注册的服务名
    public String getServiceNameById(String algId) {
        if (!StringUtils.hasText(algId)) {
            return null;
        }
        Query query = new Query(buildIdCriteria(algId));
        AlgInfo algInfo = mongoTemplate.findOne(query, AlgInfo.class,"algLibMgr");
        return algInfo == null ? null : algInfo.getServiceName();
    }

    private Criteria buildIdCriteria(String id) {
        return MongoIdCompatSupport.buildStringOrObjectIdCriteria("_id", id);
    }

    private long deleteByRawField(String field, Object value) {
        return MongoIdCompatSupport.deleteByRawField(mongoTemplate, "algLibMgr", field, value);
    }

    private long deleteByQuery(Criteria criteria) {
        return MongoIdCompatSupport.deleteByCriteria(mongoTemplate, "algLibMgr", AlgInfo.class, criteria);
    }

    private boolean existsByAnyKnownId(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        if (MongoIdCompatSupport.existsByCriteria(mongoTemplate, "algLibMgr", AlgInfo.class, buildIdCriteria(id))) {
            return true;
        }
        if (existsByRawField("_id", id)) {
            return true;
        }
        if (ObjectId.isValid(id) && existsByRawField("_id", new ObjectId(id))) {
            return true;
        }
        return existsByRawField("algId", id);
    }

    private boolean existsByRawField(String field, Object value) {
        return MongoIdCompatSupport.existsByRawField(mongoTemplate, "algLibMgr", field, value);
    }

}
