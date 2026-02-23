package fjnu.edu.controller;

import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProbInstMgrCtrlTest {

    private ProbInstMgrCtrl controller;
    private ProbInstMgrService probInstMgrService;
    private ExePlanMgrService exePlanMgrService;

    @BeforeEach
    void setUp() {
        controller = new ProbInstMgrCtrl();
        probInstMgrService = mock(ProbInstMgrService.class);
        exePlanMgrService = mock(ExePlanMgrService.class);
        ReflectionTestUtils.setField(controller, "probInstMgrService", probInstMgrService);
        ReflectionTestUtils.setField(controller, "exePlanMgrService", exePlanMgrService);
    }

    @Test
    void deleteProblemByIdReturns200WhenDeleted() {
        ProbDeleteResult result = new ProbDeleteResult();
        result.setDeletedCount(1L);
        result.setRepaired(false);
        result.setNoop(false);
        result.setVerified(true);
        when(exePlanMgrService.countPlansByProbInstId("prob-1")).thenReturn(0L);
        when(probInstMgrService.delProbInstByID("prob-1")).thenReturn(result);

        Map<String, Object> response = controller.delProbInstByID("prob-1", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("prob-1", data.get("proId"));
        assertEquals(1L, data.get("deletedCount"));
        assertEquals(false, data.get("noop"));
    }

    @Test
    void deleteProblemByIdReturns200WhenNoop() {
        ProbDeleteResult result = new ProbDeleteResult();
        result.setDeletedCount(0L);
        result.setRepaired(false);
        result.setNoop(true);
        result.setVerified(true);
        when(exePlanMgrService.countPlansByProbInstId("prob-missing")).thenReturn(0L);
        when(probInstMgrService.delProbInstByID("prob-missing")).thenReturn(result);

        Map<String, Object> response = controller.delProbInstByID("prob-missing", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(0L, data.get("deletedCount"));
        assertEquals(true, data.get("noop"));
    }

    @Test
    void deleteProblemByIdReturns500WhenNoopButNotVerified() {
        ProbDeleteResult result = new ProbDeleteResult();
        result.setDeletedCount(0L);
        result.setRepaired(false);
        result.setNoop(true);
        result.setVerified(false);
        when(exePlanMgrService.countPlansByProbInstId("prob-bad")).thenReturn(0L);
        when(probInstMgrService.delProbInstByID("prob-bad")).thenReturn(result);

        Map<String, Object> response = controller.delProbInstByID("prob-bad", new MockHttpServletRequest());

        assertEquals(500, response.get("code"));
        assertEquals("INTERNAL_ERROR", response.get("errorCode"));
    }

    @Test
    void deleteProblemByIdReturns409WhenReferencedByPlan() {
        when(exePlanMgrService.countPlansByProbInstId("prob-in-use")).thenReturn(1L);
        when(exePlanMgrService.listPlanNamesByProbInstId("prob-in-use", 5)).thenReturn(java.util.Collections.singletonList("计划A"));

        Map<String, Object> response = controller.delProbInstByID("prob-in-use", new MockHttpServletRequest());

        assertEquals(409, response.get("code"));
        assertEquals("PROB_IN_USE", response.get("errorCode"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("blocked"));
        assertEquals(1L, data.get("refPlanCount"));
    }

    @Test
    void deleteProblemByIdReturns400WhenIdEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.delProbInstByID(" ", new MockHttpServletRequest()));
        assertEquals("问题实例ID不能为空", ex.getMessage());
    }
}
