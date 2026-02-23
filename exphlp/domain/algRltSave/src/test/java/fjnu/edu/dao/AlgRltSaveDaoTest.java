package fjnu.edu.dao;

import fjnu.edu.entity.PlanExeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AlgRltSaveDaoTest {

    private AlgRltSaveDao dao;
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        dao = new AlgRltSaveDao();
        mongoTemplate = mock(MongoTemplate.class);
        ReflectionTestUtils.setField(dao, "mongoTemplate", mongoTemplate);
    }

    @Test
    void insertPlanExeResultUsesUpsertByPlanAlgAndName() {
        PlanExeResult result = new PlanExeResult();
        result.setPlanId("plan-1");
        result.setAlgId("alg-1");
        result.setAlgName("nsga2-1");
        result.setRunNum(1);
        result.setStartTime(1000L);
        result.setOutputTime(2000L);

        boolean ok = dao.insertPlanExeResult(result);

        assertTrue(ok);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate, times(1))
                .upsert(queryCaptor.capture(), updateCaptor.capture(), eq(PlanExeResult.class), eq("algRltSave"));
        String queryJson = queryCaptor.getValue().toString();
        assertTrue(queryJson.contains("plan-1"));
        assertTrue(queryJson.contains("alg-1"));
        assertTrue(queryJson.contains("nsga2-1"));
    }
}

