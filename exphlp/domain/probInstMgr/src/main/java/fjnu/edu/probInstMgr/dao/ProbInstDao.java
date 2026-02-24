package fjnu.edu.probInstMgr.dao;

import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
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
public class ProbInstDao {
    @Autowired
    MongoTemplate mongoTemplate;

    /*@CachePut(value = "probInstMgr", key = "#probInst.appID")
    @CacheEvict(value = "probInstMgr.list", allEntries = true)*/
    public boolean addProbInst(ProbInst probInst) {
        mongoTemplate.insert(probInst, "probInstMgr");
        return true;
    }
    /*@Caching(evict = {
            @CacheEvict("probInstMgr"),
            @CacheEvict(value = "probInstMgr.list", allEntries = true)
    })*/
    public ProbDeleteResult delProbInstByID(String instId) {
        String normalizedId = MongoIdCompatSupport.normalizeId(instId);
        ProbDeleteResult deleteResult = new ProbDeleteResult();
        if (!StringUtils.hasText(normalizedId)) {
            deleteResult.setDeletedCount(0L);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(true);
            deleteResult.setVerified(true);
            return deleteResult;
        }
        long deletedCount = 0L;
        boolean repaired = false;

        // 历史数据可能混用 _id/instId/proId，且 _id 类型可能是 string/ObjectId，需按顺序兜底删除。
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
            // 4) 兼容历史字段 instId
            long legacyInstDeleted = deleteByRawField("instId", normalizedId);
            if (legacyInstDeleted <= 0) {
                legacyInstDeleted = deleteByQuery(Criteria.where("instId").is(normalizedId));
            }
            deletedCount += legacyInstDeleted;
            repaired = legacyInstDeleted > 0;
        }
        if (deletedCount <= 0) {
            // 5) 兼容更老字段 proId
            long legacyProDeleted = deleteByRawField("proId", normalizedId);
            if (legacyProDeleted <= 0) {
                legacyProDeleted = deleteByQuery(Criteria.where("proId").is(normalizedId));
            }
            deletedCount += legacyProDeleted;
            repaired = repaired || legacyProDeleted > 0;
        }

        deleteResult.setDeletedCount(deletedCount);
        deleteResult.setRepaired(repaired);
        deleteResult.setNoop(deletedCount <= 0);
        deleteResult.setVerified(!existsByAnyKnownId(normalizedId));
        return deleteResult;
    }

    public ProbInst getProbInstByID(String instId) {
        if (!StringUtils.hasText(instId)) {
            return null;
        }
        Query query = new Query(buildIdCriteria(instId));
        ProbInst probInst =  mongoTemplate.findOne(query, ProbInst.class, "probInstMgr");

        return probInst;
    }
    /*@CachePut(value = "probInstMgr", key = "#probInst.instId")
    @CacheEvict(value = "probInstMgr.list", allEntries = true)*/
    public boolean updateProbInst(ProbInst probInst) {
        if (probInst == null || !StringUtils.hasText(probInst.getInstId())) {
            return false;
        }
        Query query = new Query(buildIdCriteria(probInst.getInstId()));

        Update update = new Update().set("categoryName", probInst.getCategoryName())
                .set("instName", probInst.getInstName()).set("machineIp", probInst.getMachineIp())
                .set("dirName", probInst.getDirName()).set("machineName", probInst.getMachineName())
                .set("description", probInst.getDescription());
        mongoTemplate.updateFirst(query, update, ProbInst.class, "probInstMgr");
        return true;
    }

    public List<ProbInst> listProbInsts(int pageNum, int pageSize) {
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
        List<ProbInst> ProbInsts = mongoTemplate.find(query, ProbInst.class, "probInstMgr");

        return ProbInsts;
    }

    public List<ProbInst> listProbInstsByinstName(String instName, int pageNum, int pageSize) {
        if (!StringUtils.hasText(instName)) {
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
        Criteria criteria = Criteria.where("instName").is(instName);
        Query query = new Query(criteria).with(pageable);
        List<ProbInst> ProbInsts = mongoTemplate.find(query, ProbInst.class, "probInstMgr");

        return ProbInsts;
    }

    public long countAllProbInsts() {
        long count = mongoTemplate.count(new Query(), ProbInst.class, "probInstMgr");
        return count;
    }

    public long countProbInstsByInstName(String instName) {
        Criteria criteria = Criteria.where("instName").is(instName);
        long count = mongoTemplate.count(new Query(criteria), ProbInst.class, "probInstMgr");
        return count;
    }

    private Criteria buildIdCriteria(String id) {
        return MongoIdCompatSupport.buildStringOrObjectIdCriteria("_id", id);
    }

    private long deleteByRawField(String field, Object value) {
        return MongoIdCompatSupport.deleteByRawField(mongoTemplate, "probInstMgr", field, value);
    }

    private boolean existsByAnyKnownId(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        if (MongoIdCompatSupport.existsByCriteria(mongoTemplate, "probInstMgr", ProbInst.class, buildIdCriteria(id))) {
            return true;
        }
        if (existsByRawField("_id", id)) {
            return true;
        }
        if (ObjectId.isValid(id) && existsByRawField("_id", new ObjectId(id))) {
            return true;
        }
        return existsByRawField("instId", id) || existsByRawField("proId", id);
    }

    private boolean existsByRawField(String field, Object value) {
        return MongoIdCompatSupport.existsByRawField(mongoTemplate, "probInstMgr", field, value);
    }

    private long deleteByQuery(Criteria criteria) {
        return MongoIdCompatSupport.deleteByCriteria(mongoTemplate, "probInstMgr", ProbInst.class, criteria);
    }
}
