package fjnu.edu.controller;

import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlgLibMgrCtrlTest {

    private AlgLibMgrCtrl controller;
    private AlgLibMgrService algLibMgrService;
    private ExePlanMgrService exePlanMgrService;

    @BeforeEach
    void setUp() {
        controller = new AlgLibMgrCtrl();
        algLibMgrService = mock(AlgLibMgrService.class);
        exePlanMgrService = mock(ExePlanMgrService.class);
        ReflectionTestUtils.setField(controller, "algLibMgrService", algLibMgrService);
        ReflectionTestUtils.setField(controller, "exePlanMgrService", exePlanMgrService);
    }

    @Test
    void deleteAlgByIdReturns200WhenRecordDeleted() {
        AlgDeleteResult result = new AlgDeleteResult();
        result.setDeletedCount(1L);
        result.setRepaired(false);
        result.setNoop(false);
        when(exePlanMgrService.countPlansByAlgId("alg-1")).thenReturn(0L);
        when(algLibMgrService.deleteAlgInfoById("alg-1")).thenReturn(result);

        Map<String, Object> response = controller.deleteAlgInfoById("alg-1", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("alg-1", data.get("algId"));
        assertEquals(1L, data.get("deletedCount"));
        assertEquals(true, data.get("existed"));
        assertEquals(false, data.get("repaired"));
    }

    @Test
    void deleteAlgByIdReturns200WhenNoRecordDeleted() {
        AlgDeleteResult result = new AlgDeleteResult();
        result.setDeletedCount(0L);
        result.setRepaired(false);
        result.setNoop(true);
        when(exePlanMgrService.countPlansByAlgId("alg-missing")).thenReturn(0L);
        when(algLibMgrService.deleteAlgInfoById("alg-missing")).thenReturn(result);

        Map<String, Object> response = controller.deleteAlgInfoById("alg-missing", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("alg-missing", data.get("algId"));
        assertEquals(0L, data.get("deletedCount"));
        assertEquals(false, data.get("existed"));
        assertEquals(true, data.get("noop"));
        assertEquals(false, data.get("repaired"));
    }

    @Test
    void deleteAlgByIdReturns200AndRepairedWhenLegacyRecordDeleted() {
        AlgDeleteResult result = new AlgDeleteResult();
        result.setDeletedCount(1L);
        result.setRepaired(true);
        result.setNoop(false);
        when(exePlanMgrService.countPlansByAlgId("legacy-alg")).thenReturn(0L);
        when(algLibMgrService.deleteAlgInfoById("legacy-alg")).thenReturn(result);

        Map<String, Object> response = controller.deleteAlgInfoById("legacy-alg", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("legacy-alg", data.get("algId"));
        assertEquals(1L, data.get("deletedCount"));
        assertEquals(true, data.get("repaired"));
    }

    @Test
    void deleteAlgByIdReturns400WhenAlgIdEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.deleteAlgInfoById(" ", new MockHttpServletRequest()));
        assertEquals("算法ID不能为空", ex.getMessage());
    }

    @Test
    void deleteAlgByIdReturns409WhenReferencedByPlan() {
        when(exePlanMgrService.countPlansByAlgId("alg-in-use")).thenReturn(2L);
        when(exePlanMgrService.listPlanNamesByAlgId("alg-in-use", 5)).thenReturn(java.util.Arrays.asList("计划A", "计划B"));

        Map<String, Object> response = controller.deleteAlgInfoById("alg-in-use", new MockHttpServletRequest());

        assertEquals(409, response.get("code"));
        assertEquals("ALG_IN_USE", response.get("errorCode"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("blocked"));
        assertEquals(2L, data.get("refPlanCount"));
    }
}
