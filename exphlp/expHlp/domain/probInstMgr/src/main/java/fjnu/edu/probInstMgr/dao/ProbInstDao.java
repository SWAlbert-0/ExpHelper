package fjnu.edu.probInstMgr.dao;

import fjnu.edu.probInstMgr.entity.ProbInst;
import org.bson.BsonObjectId;
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
    public boolean delProbInstByID(String instId) {
        Criteria criteria = Criteria.where("_id").is(new BsonObjectId(new ObjectId(instId)));
        Query query = new Query(criteria);
        mongoTemplate.remove(query, "probInstMgr");

        return true;
    }

    public ProbInst getProbInstByID(String instId) {
        Criteria criteria = Criteria.where("_id").is(new BsonObjectId(new ObjectId(instId)));
        Query query = new Query(criteria);
        ProbInst probInst =  mongoTemplate.findOne(query, ProbInst.class, "probInstMgr");

        return probInst;
    }
    /*@CachePut(value = "probInstMgr", key = "#probInst.instId")
    @CacheEvict(value = "probInstMgr.list", allEntries = true)*/
    public boolean updateProbInst(ProbInst probInst) {
        Criteria criteria = Criteria.where("instId").is(new BsonObjectId(new ObjectId(probInst.getInstId())));
        Query query = new Query(criteria);

        Update update = new Update().set("categoryName", probInst.getCategoryName())
                .set("instName", probInst.getInstName()).set("machineIp", probInst.getMachineIp())
                .set("dirName", probInst.getDirName()).set("machineName", probInst.getMachineName())
                .set("description", probInst.getDescription());
        mongoTemplate.updateFirst(query, update, ProbInst.class, "probInstMgr");
        return true;
    }

    public List<ProbInst> listProbInsts(int pageNum, int pageSize) {
        if (pageNum != 0) {
            pageNum--;
        }
        Sort sort = Sort.by(Sort.Order.desc("_id"));
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Query query = new Query().with(pageable).with(sort);
        List<ProbInst> ProbInsts = mongoTemplate.find(query, ProbInst.class, "probInstMgr");

        return ProbInsts;
    }

    public List<ProbInst> listProbInstsByinstName(String instName, int pageNum, int pageSize) {
        if (pageNum != 0) {
            pageNum--;
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
}
