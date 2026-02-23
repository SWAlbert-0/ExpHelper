package fjnu.edu.alglibmgr.dao;

import fjnu.edu.alglibmgr.entity.DefPara ;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import com.mongodb.client.result.DeleteResult;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
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
        AlgDeleteResult deleteResult = new AlgDeleteResult();
        if (!StringUtils.hasText(algId)) {
            deleteResult.setDeletedCount(0L);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(true);
            return deleteResult;
        }
        Query primaryQuery = new Query(buildIdCriteria(algId));
        DeleteResult primaryResult = mongoTemplate.remove(primaryQuery, "algLibMgr");
        long primaryDeleted = primaryResult == null ? 0L : primaryResult.getDeletedCount();
        if (primaryDeleted > 0) {
            deleteResult.setDeletedCount(primaryDeleted);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(false);
            return deleteResult;
        }

        // 兼容历史脏数据：曾有文档将业务id保存在 algId 字段而非 _id
        Query legacyQuery = new Query(Criteria.where("algId").is(algId));
        DeleteResult legacyResult = mongoTemplate.remove(legacyQuery, "algLibMgr");
        long legacyDeleted = legacyResult == null ? 0L : legacyResult.getDeletedCount();
        deleteResult.setDeletedCount(legacyDeleted);
        deleteResult.setRepaired(legacyDeleted > 0);
        deleteResult.setNoop(legacyDeleted <= 0);
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
        Criteria stringIdCriteria = Criteria.where("_id").is(id);
        if (ObjectId.isValid(id)) {
            return new Criteria().orOperator(
                    stringIdCriteria,
                    Criteria.where("_id").is(new ObjectId(id))
            );
        }
        return stringIdCriteria;
    }

}
