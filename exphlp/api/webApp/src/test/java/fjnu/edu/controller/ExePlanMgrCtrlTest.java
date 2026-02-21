package fjnu.edu.controller;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ExePlanMgrCtrlTest {

    private ExePlanMgrCtrl controller;
    private ExePlanMgrService exePlanMgrService;
    private PlanExecuteService planExecuteService;

    @BeforeEach
    void setUp() {
        controller = new ExePlanMgrCtrl();
        exePlanMgrService = mock(ExePlanMgrService.class);
        planExecuteService = mock(PlanExecuteService.class);
        ReflectionTestUtils.setField(controller, "exePlanMgrService", exePlanMgrService);
        ReflectionTestUtils.setField(controller, "planExecuteService", planExecuteService);
    }

    @Test
    void executeReturns400WhenPlanIdEmpty() {
        Map<String, Object> response = controller.execute("  ", new MockHttpServletRequest());
        assertEquals(400, response.get("code"));
        assertEquals("PLAN_ID_EMPTY", response.get("errorCode"));
    }

    @Test
    void executeReturns404WhenPlanNotFound() {
        when(exePlanMgrService.getExePlanById("p-1")).thenReturn(null);

        Map<String, Object> response = controller.execute("p-1", new MockHttpServletRequest());

        assertEquals(404, response.get("code"));
        assertEquals("PLAN_NOT_FOUND", response.get("errorCode"));
        verify(planExecuteService, never()).execute(anyString());
    }

    @Test
    void executeReturns200WhenAccepted() {
        ExePlan plan = new ExePlan();
        plan.setExeState(1);
        when(exePlanMgrService.getExePlanById("p-2")).thenReturn(plan);
        when(planExecuteService.execute("p-2")).thenReturn(true);

        Map<String, Object> response = controller.execute("p-2", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
    }
}

