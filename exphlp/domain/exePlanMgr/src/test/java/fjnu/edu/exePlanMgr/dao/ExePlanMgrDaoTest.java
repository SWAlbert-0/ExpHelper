package fjnu.edu.exePlanMgr.dao;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    void getExePlanById_usesDualCriteriaWhenObjectIdFormat() {
        when(mongoTemplate.findOne(any(Query.class), eq(ExePlan.class), eq("exePlanMgr"))).thenReturn(new ExePlan());

        dao.getExePlanById("507f1f77bcf86cd799439011");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findOne(captor.capture(), eq(ExePlan.class), eq("exePlanMgr"));
        Document query = captor.getValue().getQueryObject();
        assertTrue(query.containsKey("$or"));
    }

    @Test
    void getExePlanById_usesStringIdCriteriaWhenNonObjectIdFormat() {
        when(mongoTemplate.findOne(any(Query.class), eq(ExePlan.class), eq("exePlanMgr"))).thenReturn(new ExePlan());

        dao.getExePlanById("demo-plan-001");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findOne(captor.capture(), eq(ExePlan.class), eq("exePlanMgr"));
        Document query = captor.getValue().getQueryObject();
        assertFalse(query.containsKey("$or"));
        assertEquals("demo-plan-001", query.get("_id"));
    }
}
