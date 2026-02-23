package fjnu.edu.controller;

import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        plan.setLastError(null);
        when(exePlanMgrService.getExePlanById("p-2")).thenReturn(plan);
        when(planExecuteService.execute("p-2")).thenReturn(true);

        Map<String, Object> response = controller.execute("p-2", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        assertNotNull(response.get("data"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("accepted"));
        assertEquals(null, data.get("lastError"));
    }

    @Test
    void executeReturnsLastErrorWhenNotAccepted() {
        ExePlan plan = new ExePlan();
        plan.setExeState(3);
        plan.setLastError("算法服务未注册: a-1");
        when(exePlanMgrService.getExePlanById("p-3")).thenReturn(plan);
        when(planExecuteService.execute("p-3")).thenReturn(false);

        Map<String, Object> response = controller.execute("p-3", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(false, data.get("accepted"));
        assertEquals("算法服务未注册: a-1", data.get("lastError"));
    }

    @Test
    void getPlanLogsReturnsDataAndNextSeq() {
        ExePlan plan = new ExePlan();
        plan.setExeState(2);
        plan.setLastError(null);
        ExePlanLog log = new ExePlanLog();
        log.setSeq(3L);
        log.setMessage("计划开始执行");
        when(exePlanMgrService.getPlanLogs("p-log-1", 0L, 200)).thenReturn(Collections.singletonList(log));
        when(exePlanMgrService.getExePlanById("p-log-1")).thenReturn(plan);

        Map<String, Object> response = controller.getPlanLogs("p-log-1", 0L, 200, new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(3L, data.get("nextSeq"));
        assertEquals(2, data.get("planState"));
    }
}
