package fjnu.edu.controller;

import fjnu.edu.entity.DisplayResult;
import fjnu.edu.entity.GenResult;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import fjnu.edu.service.AlgRltSaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AlgRltSaveCtrlTest {

    private AlgRltSaveCtrl ctrl;
    private AlgRltSaveService algRltSaveService;
    private ProbInstMgrService probInstMgrService;

    @BeforeEach
    void setUp() {
        ctrl = new AlgRltSaveCtrl();
        algRltSaveService = mock(AlgRltSaveService.class);
        probInstMgrService = mock(ProbInstMgrService.class);
        ReflectionTestUtils.setField(ctrl, "algRltSaveService", algRltSaveService);
        ReflectionTestUtils.setField(ctrl, "probInstMgrService", probInstMgrService);
    }

    @Test
    void returnsEmptyListWhenNoSavedResult() {
        when(algRltSaveService.getAlgSaveByAlgName("p1", "a1", "alg-1")).thenReturn(null);

        List<DisplayResult> result = ctrl.getAlgSaveByAlgName("p1", "a1", "alg-1");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void fallsBackToProbInstIdWhenProblemMissing() {
        GenResult genResult = new GenResult();
        genResult.setProbInstId("prob-1");
        genResult.setOutTime(1710000000000L);
        genResult.setEachResults(Collections.emptyList());

        PlanExeResult saved = new PlanExeResult();
        saved.setStartTime(1700000000000L);
        saved.setGenResults(Collections.singletonList(genResult));

        when(algRltSaveService.getAlgSaveByAlgName("p1", "a1", "alg-1")).thenReturn(saved);
        when(probInstMgrService.getProbInstByID("prob-1")).thenReturn(null);

        List<DisplayResult> result = ctrl.getAlgSaveByAlgName("p1", "a1", "alg-1");

        assertEquals(1, result.size());
        assertEquals("prob-1", result.get(0).getProbInstName());
    }

    @Test
    void resolvesProbInstNameWhenProblemExists() {
        GenResult genResult = new GenResult();
        genResult.setProbInstId("prob-2");
        genResult.setOutTime(1710000000000L);
        genResult.setEachResults(Collections.emptyList());

        PlanExeResult saved = new PlanExeResult();
        saved.setStartTime(1700000000000L);
        saved.setGenResults(Collections.singletonList(genResult));

        ProbInst probInst = new ProbInst();
        probInst.setInstName("demo-prob");

        when(algRltSaveService.getAlgSaveByAlgName("p2", "a2", "alg-2")).thenReturn(saved);
        when(probInstMgrService.getProbInstByID("prob-2")).thenReturn(probInst);

        List<DisplayResult> result = ctrl.getAlgSaveByAlgName("p2", "a2", "alg-2");

        assertEquals(1, result.size());
        assertEquals("demo-prob", result.get(0).getProbInstName());
    }
}
