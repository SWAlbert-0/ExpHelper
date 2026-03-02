package fjnu.edu.platmgr.dao;

import fjnu.edu.platmgr.entity.UserInfo;
import org.bson.types.ObjectId;
import org.bson.Document;
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

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserInfoDao {

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * @Author wsn
     * @Description 获取用户列表
     **/
    public List<UserInfo> getUserInfos(){
        Query query = new Query();
        List<UserInfo> userInfos = mongoTemplate.find(query, UserInfo.class, "userMgr");
        return userInfos;
    }

    /**
     * @Author wsn
     * @Description 获取用户的分页列表
     **/
    public List<UserInfo> getUsersByPage(int pageNum, int pageSize) {
        if (pageNum != 0) {
            pageNum--;
        }
        Sort sort = Sort.by(Sort.Order.desc("_id"));
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Query query = new Query().with(pageable).with(sort);
        List<UserInfo> userInfos = mongoTemplate.find(query, UserInfo.class, "userMgr");

        return userInfos;
    }

    /**
     * @Author wsn
     * @Description 获取用户详情
     **/
    public UserInfo getUserById(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        Query query = new Query(buildIdCriteria(userId));
        UserInfo userInfo = mongoTemplate.findOne(query, UserInfo.class, "userMgr");

        return userInfo;
    }

    /**
     * @Author wsn
     * @Description 根据Id删除用户
     **/
    public boolean deleteUserById(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        Query query = new Query(buildIdCriteria(userId));
        mongoTemplate.remove(query, "userMgr");

        return true;
    }

    /**
     * @Author wsn
     * @Description 添加一个用户
     **/
    public boolean addUser(UserInfo userInfo) {
//        long userId = getCurMax()+1;
//        userInfo.setUserId(userId);
//        userInfo.setUserId(new ObjectId().toString());
        mongoTemplate.insert(userInfo, "userMgr");

        return true;
    }

    /**
     * @Author wsn
     * @Description 根据名字模糊查询用户
     **/
    public List<UserInfo> getUsersByName(String userName, int pageNum, int pageSize) {
        if (pageNum != 0) {
            pageNum--;
        }
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Criteria criteria = Criteria.where("userName").regex(userName);
        Query query = new Query(criteria).with(pageable);
        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class, "userMgr");

        return users;
    }

    //根据用户名查询用户
    public  UserInfo getUserByName(String userName) {
        Criteria criteria = Criteria.where("userName").is(userName);
        Query query = new Query(criteria);
        UserInfo userInfo = mongoTemplate.findOne(query, UserInfo.class, "userMgr");
        return userInfo;
    }

    public UserInfo getUserByRememberTokenHash(String rememberTokenHash) {
        if (!StringUtils.hasText(rememberTokenHash)) {
            return null;
        }
        Query query = new Query(Criteria.where("rememberTokenHash").is(rememberTokenHash));
        return mongoTemplate.findOne(query, UserInfo.class, "userMgr");
    }

    /**
     * @Author wsn
     * @Description 根据用户id更新用户
     **/
    public boolean updateUserById(UserInfo user) {
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            return false;
        }
        Query query = new Query(buildIdCriteria(user.getUserId()));

        Update update = new Update().set("userName", user.getUserName())
                .set("role", user.getRole())
                .set("email", user.getEmail())
                .set("wechat", user.getWechat())
                .set("mobile", user.getMobile())
                .set("qq", user.getQq())
                .set("avatar", user.getAvatar());
        if (StringUtils.hasText(user.getPassword())) {
            update.set("password", user.getPassword());
        }
        mongoTemplate.updateFirst(query, update, UserInfo.class, "userMgr");

        return true;

    }

    public boolean updateRememberToken(String userId, String tokenHash, long issuedAt, long expireAt, int version) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(tokenHash)) {
            return false;
        }
        Query query = new Query(buildIdCriteria(userId));
        Update update = new Update()
                .set("rememberTokenHash", tokenHash)
                .set("rememberTokenIssuedAt", issuedAt)
                .set("rememberTokenExpireAt", expireAt)
                .set("rememberTokenVersion", version);
        mongoTemplate.updateFirst(query, update, UserInfo.class, "userMgr");
        return true;
    }

    public boolean clearRememberToken(String userId, int version) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        Query query = new Query(buildIdCriteria(userId));
        Update update = new Update()
                .unset("rememberTokenHash")
                .unset("rememberTokenIssuedAt")
                .unset("rememberTokenExpireAt")
                .set("rememberTokenVersion", version);
        mongoTemplate.updateFirst(query, update, UserInfo.class, "userMgr");
        return true;
    }

    public long countAllUsers() {
        long count = mongoTemplate.count(new Query(), UserInfo.class, "userMgr");
        return count;
    }

    public long countByUserName(String userName) {
        Criteria criteria = Criteria.where("userName").regex(userName);
        long count = mongoTemplate.count(new Query(criteria), UserInfo.class, "userMgr");
        return count;
    }

    private Criteria buildIdCriteria(String id) {
        Criteria stringIdCriteria = Criteria.where("_id").is(id);
        Criteria legacyUserIdCriteria = Criteria.where("userId").is(id);
        if (ObjectId.isValid(id)) {
            return new Criteria().orOperator(
                    stringIdCriteria,
                    legacyUserIdCriteria,
                    Criteria.where("_id").is(new ObjectId(id))
            );
        }
        return new Criteria().orOperator(stringIdCriteria, legacyUserIdCriteria);
    }

    public Map<String, Object> repairInvalidUserIds() {
        Map<String, Object> result = new LinkedHashMap<>();
        int scanned = 0;
        int repaired = 0;
        int failed = 0;
        var collection = mongoTemplate.getCollection("userMgr");
        var cursor = collection.find(new Document("_id", ""));
        for (Document oldDoc : cursor) {
            scanned++;
            try {
                Document newDoc = new Document(oldDoc);
                newDoc.remove("_id");
                ObjectId newId = new ObjectId();
                newDoc.put("_id", newId);
                collection.insertOne(newDoc);
                collection.deleteOne(new Document("_id", ""));
                repaired++;
            } catch (Exception ex) {
                failed++;
            }
        }
        result.put("scanned", scanned);
        result.put("repaired", repaired);
        result.put("failed", failed);
        return result;
    }

}
