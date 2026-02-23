package fjnu.edu.service;

import fjnu.edu.dao.AlgRltSaveDao;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.ExeResultDetail;
import fjnu.edu.entity.GenResult;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.service.impl.AlgRltSaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlgRltSaveServiceImplTest {

    private AlgRltSaveServiceImpl service;
    private AlgRltSaveDao dao;

    @BeforeEach
    void setUp() {
        service = new AlgRltSaveServiceImpl();
        dao = mock(AlgRltSaveDao.class);
        ReflectionTestUtils.setField(service, "algRltSaveDao", dao);
    }

    @Test
    void getExeResultDetailReturnsMissingWhenNotFound() {
        when(dao.getLatestByPlanAndAlg("p-1", "a-1")).thenReturn(null);

        ExeResultDetail detail = service.getExeResultDetail("p-1", "a-1");

        assertEquals("MISSING", detail.getStatus());
        assertEquals("RESULT_NOT_FOUND", detail.getReasonCode());
    }

    @Test
    void getExeResultDetailComputesMetricsForZdt1() {
        PlanExeResult saved = new PlanExeResult();
        saved.setPlanExeResultId("rid-1");
        saved.setPlanId("p-2");
        saved.setAlgId("nsga2-zdt1-ls");
        saved.setAlgName("nsga2-zdt1-ls-1");
        saved.setStartTime(1700000000000L);

        GenResult gen = new GenResult();
        gen.setProbInstId("prob-1");
        gen.setOutTime(1700000005000L);
        gen.setEachResults(Arrays.asList(
                new EachResult("runtimeMs", "120", "long"),
                new EachResult("paretoSize", "2", "int"),
                new EachResult("paretoPoint_1", "f1=0.10,f2=0.68", "pair"),
                new EachResult("paretoPoint_2", "f1=0.40,f2=0.40", "pair")
        ));
        saved.setGenResults(Collections.singletonList(gen));
        when(dao.getLatestByPlanAndAlg("p-2", "nsga2-zdt1-ls")).thenReturn(saved);

        ExeResultDetail detail = service.getExeResultDetail("p-2", "nsga2-zdt1-ls");

        assertEquals("SUCCESS", detail.getStatus());
        assertNotNull(detail.getAggregate());
        assertTrue(detail.getRuns().get(0).getHv() != null && detail.getRuns().get(0).getHv() > 0d);
        assertTrue(detail.getRuns().get(0).getIgdPlus() != null && detail.getRuns().get(0).getIgdPlus() >= 0d);
        verify(dao, times(1)).updateMetricCache(anyString(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt(), any(), anyLong());
    }
}

