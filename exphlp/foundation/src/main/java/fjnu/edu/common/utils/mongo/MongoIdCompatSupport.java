package fjnu.edu.common.utils.mongo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

public final class MongoIdCompatSupport {

    private MongoIdCompatSupport() {
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }

    public static Criteria buildStringOrObjectIdCriteria(String fieldName, String id) {
        if (!StringUtils.hasText(fieldName)) {
            throw new IllegalArgumentException("fieldName is empty");
        }
        Criteria stringIdCriteria = Criteria.where(fieldName).is(id);
        if (ObjectId.isValid(id)) {
            return new Criteria().orOperator(
                    stringIdCriteria,
                    Criteria.where(fieldName).is(new ObjectId(id))
            );
        }
        return stringIdCriteria;
    }

    public static long deleteByRawField(MongoTemplate mongoTemplate, String collectionName, String fieldName, Object value) {
        // 使用原生 collection 删除，兼容历史数据中 _id 为 string/ObjectId 混存场景。
        if (mongoTemplate == null || !StringUtils.hasText(collectionName) || !StringUtils.hasText(fieldName) || value == null) {
            return 0L;
        }
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        if (collection == null) {
            return 0L;
        }
        DeleteResult deleteResult = collection.deleteMany(new Document(fieldName, value));
        return deleteResult == null ? 0L : deleteResult.getDeletedCount();
    }

    public static long deleteByCriteria(MongoTemplate mongoTemplate, String collectionName, Class<?> entityClass, Criteria criteria) {
        if (mongoTemplate == null || !StringUtils.hasText(collectionName) || entityClass == null || criteria == null) {
            return 0L;
        }
        DeleteResult deleteResult = mongoTemplate.remove(new Query(criteria), entityClass, collectionName);
        return deleteResult == null ? 0L : deleteResult.getDeletedCount();
    }

    public static boolean existsByCriteria(MongoTemplate mongoTemplate, String collectionName, Class<?> entityClass, Criteria criteria) {
        if (mongoTemplate == null || !StringUtils.hasText(collectionName) || entityClass == null || criteria == null) {
            return false;
        }
        return mongoTemplate.exists(new Query(criteria), entityClass, collectionName);
    }

    public static boolean existsByRawField(MongoTemplate mongoTemplate, String collectionName, String fieldName, Object value) {
        if (mongoTemplate == null || !StringUtils.hasText(collectionName) || !StringUtils.hasText(fieldName) || value == null) {
            return false;
        }
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        if (collection == null) {
            return false;
        }
        long count = collection.countDocuments(new Document(fieldName, value));
        return count > 0;
    }
}
