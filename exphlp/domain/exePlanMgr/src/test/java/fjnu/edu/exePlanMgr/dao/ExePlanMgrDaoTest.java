package fjnu.edu.exePlanMgr.dao;

import com.mongodb.client.result.DeleteResult;
import fjnu.edu.exePlanMgr.Constant.Constant;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanDeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExePlanMgrDaoTest {

    private ExePlanMgrDao dao;
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        dao = new ExePlanMgrDao();
        mongoTemplate = mock(MongoTemplate.class);
        ReflectionTestUtils.setField(dao, "mongoTemplate", mongoTemplate);
    }

    @Test
    void getExePlanById_returnsNullWhenPlanIdBlank() {
        ExePlan actual = dao.getExePlanById(" ");
        assertNull(actual);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void getExePlanById_usesObjectIdFallbackWhenCollectionUnavailable() {
        when(mongoTemplate.findOne(any(Query.class), eq(ExePlan.class), eq("exePlanMgr"))).thenReturn(new ExePlan());

        dao.getExePlanById("507f1f77bcf86cd799439011");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findOne(captor.capture(), eq(ExePlan.class), eq("exePlanMgr"));
        Document query = captor.getValue().getQueryObject();
        assertTrue(query.containsKey("_id"));
    }

    @Test
    void getExePlanById_returnsNullWhenStringIdNotFound() {
        ExePlan result = dao.getExePlanById("demo-plan-001");
        assertNull(result);
    }

    @Test
    void deleteExePlanById_returnsDeletedWhenRemoveSuccess() {
        ExePlan plan = new ExePlan();
        plan.setExeState(Constant.NON_EXECUTION);
        when(mongoTemplate.findOne(any(Query.class), eq(ExePlan.class), eq("exePlanMgr")))
                .thenReturn(plan)
                .thenReturn(null);
        when(mongoTemplate.remove(any(Query.class), eq("exePlanMgr"))).thenReturn(DeleteResult.acknowledged(1L));

        ExePlanDeleteResult result = dao.deleteExePlanById("6991e1f0392d9d656cb30553");

        assertTrue(result.getDeletedCount() > 0);
        assertFalse(result.isNoop());
        assertTrue(result.isVerified());
        assertFalse(result.isBlocked());
    }

    @Test
    void deleteExePlanById_returnsBlockedWhenRunning() {
        ExePlan plan = new ExePlan();
        plan.setExeState(Constant.IN_EXECUTION);
        when(mongoTemplate.findOne(any(Query.class), eq(ExePlan.class), eq("exePlanMgr"))).thenReturn(plan);

        ExePlanDeleteResult result = dao.deleteExePlanById("6991e1f0392d9d656cb30553");

        assertTrue(result.isBlocked());
        assertTrue(result.isNoop());
        verify(mongoTemplate, never()).remove(any(Query.class), eq("exePlanMgr"));
    }

    @Test
    void updateExePlanById_requiresNonExecutionState() {
        ExePlan plan = new ExePlan();
        plan.setPlanId("6991e1f0392d9d656cb30553");
        plan.setExeState(Constant.IN_EXECUTION);
        when(mongoTemplate.updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), eq(ExePlan.class), eq("exePlanMgr")))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));

        dao.updateExePlanById(plan);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).updateFirst(captor.capture(), any(org.springframework.data.mongodb.core.query.Update.class), eq(ExePlan.class), eq("exePlanMgr"));
        Document query = captor.getValue().getQueryObject();
        assertTrue(query.toJson().contains("\"exeState\""));
    }

    @Test
    void updateExePlanExecutionById_doesNotRequireNonExecutionState() {
        ExePlan plan = new ExePlan();
        plan.setPlanId("6991e1f0392d9d656cb30553");
        plan.setExeState(Constant.NORMAL_TERMINATION);
        when(mongoTemplate.updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), eq(ExePlan.class), eq("exePlanMgr")))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));

        dao.updateExePlanExecutionById(plan);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).updateFirst(captor.capture(), any(org.springframework.data.mongodb.core.query.Update.class), eq(ExePlan.class), eq("exePlanMgr"));
        Document query = captor.getValue().getQueryObject();
        assertFalse(query.toJson().contains("\"exeState\""));
    }
}
