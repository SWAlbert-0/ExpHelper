package fjnu.edu.probInstMgr.dao;

import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
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
        ProbDeleteResult deleteResult = new ProbDeleteResult();
        if (!StringUtils.hasText(instId)) {
            deleteResult.setDeletedCount(0L);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(true);
            return deleteResult;
        }
        Query query = new Query(buildIdCriteria(instId));
        DeleteResult primaryResult = mongoTemplate.remove(query, "probInstMgr");
        long primaryDeleted = primaryResult == null ? 0L : primaryResult.getDeletedCount();
        if (primaryDeleted > 0) {
            deleteResult.setDeletedCount(primaryDeleted);
            deleteResult.setRepaired(false);
            deleteResult.setNoop(false);
            return deleteResult;
        }
        Query legacyQuery = new Query(Criteria.where("instId").is(instId));
        DeleteResult legacyResult = mongoTemplate.remove(legacyQuery, "probInstMgr");
        long legacyDeleted = legacyResult == null ? 0L : legacyResult.getDeletedCount();
        deleteResult.setDeletedCount(legacyDeleted);
        deleteResult.setRepaired(legacyDeleted > 0);
        deleteResult.setNoop(legacyDeleted <= 0);
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
