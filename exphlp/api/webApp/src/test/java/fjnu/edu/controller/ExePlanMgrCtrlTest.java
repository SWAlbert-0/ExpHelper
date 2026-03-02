package fjnu.edu.controller;

import fjnu.edu.auth.AuthUser;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.entity.ExePlanDeleteResult;
import fjnu.edu.exePlanMgr.entity.ExePlanLog;
import fjnu.edu.exePlanMgr.entity.PlanPreCheckResult;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.intf.PlanExecuteService;
import fjnu.edu.platmgr.service.PlatMgrService;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ExePlanMgrCtrlTest {

    private ExePlanMgrCtrl controller;
    private ExePlanMgrService exePlanMgrService;
    private PlanExecuteService planExecuteService;
    private ProbInstMgrService probInstMgrService;
    private AlgLibMgrService algLibMgrService;
    private DiscoveryClient discoveryClient;
    private PlatMgrService platMgrService;

    @BeforeEach
    void setUp() {
        controller = new ExePlanMgrCtrl();
        exePlanMgrService = mock(ExePlanMgrService.class);
        planExecuteService = mock(PlanExecuteService.class);
        probInstMgrService = mock(ProbInstMgrService.class);
        algLibMgrService = mock(AlgLibMgrService.class);
        discoveryClient = mock(DiscoveryClient.class);
        platMgrService = mock(PlatMgrService.class);
        ReflectionTestUtils.setField(controller, "exePlanMgrService", exePlanMgrService);
        ReflectionTestUtils.setField(controller, "planExecuteService", planExecuteService);
        ReflectionTestUtils.setField(controller, "probInstMgrService", probInstMgrService);
        ReflectionTestUtils.setField(controller, "algLibMgrService", algLibMgrService);
        ReflectionTestUtils.setField(controller, "discoveryClient", discoveryClient);
        ReflectionTestUtils.setField(controller, "platMgrService", platMgrService);
    }

    @Test
    void executeReturns400WhenPlanIdEmpty() {
        Map<String, Object> response = controller.execute("  ", new MockHttpServletRequest());
        assertEquals(400, response.get("code"));
        assertEquals("PLAN_ID_EMPTY", response.get("errorCode"));
    }

    @Test
    void getExePlansSupportsScope() {
        ExePlan p1 = new ExePlan();
        p1.setPlanId("p-1");
        when(exePlanMgrService.getExePlans(1, 10, "history", null, null, null, null)).thenReturn(Arrays.asList(p1));

        java.util.List<ExePlan> result = controller.getExePlans(1, 10, "history", null, null, null, null);

        assertEquals(1, result.size());
        verify(exePlanMgrService, times(1)).getExePlans(1, 10, "history", null, null, null, null);
    }

    @Test
    void countAllExePlansSupportsScope() {
        when(exePlanMgrService.countAllExePlans("current", null, null, null, null)).thenReturn(2L);

        long result = controller.countAllExePlans("current", null, null, null, null);

        assertEquals(2L, result);
        verify(exePlanMgrService, times(1)).countAllExePlans("current", null, null, null, null);
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

        Map<String, Object> response = controller.execute("p-2", adminRequest());

        assertEquals(200, response.get("code"));
        assertNotNull(response.get("data"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("accepted"));
        assertEquals(null, data.get("lastError"));
    }

    @Test
    void deleteExePlanByIdReturns200WhenDeleted() {
        ExePlan plan = new ExePlan();
        plan.setPlanId("p-del-1");
        plan.setOwnerUserId("u-admin");
        when(exePlanMgrService.getExePlanById("p-del-1")).thenReturn(plan);
        ExePlanDeleteResult result = new ExePlanDeleteResult();
        result.setDeletedCount(1L);
        result.setExisted(true);
        result.setNoop(false);
        result.setVerified(true);
        result.setBlocked(false);
        when(exePlanMgrService.deleteExePlanById("p-del-1")).thenReturn(result);

        Map<String, Object> response = controller.deleteExePlanById("p-del-1", adminRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(1L, data.get("deletedCount"));
    }

    @Test
    void deleteExePlanByIdReturns200WhenRunningBlocked() {
        ExePlan plan = new ExePlan();
        plan.setPlanId("p-del-2");
        plan.setOwnerUserId("u-admin");
        when(exePlanMgrService.getExePlanById("p-del-2")).thenReturn(plan);
        ExePlanDeleteResult result = new ExePlanDeleteResult();
        result.setDeletedCount(0L);
        result.setExisted(true);
        result.setNoop(true);
        result.setVerified(true);
        result.setBlocked(true);
        when(exePlanMgrService.deleteExePlanById("p-del-2")).thenReturn(result);

        Map<String, Object> response = controller.deleteExePlanById("p-del-2", adminRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("blocked"));
    }

    @Test
    void executeReturnsLastErrorWhenNotAccepted() {
        ExePlan plan = new ExePlan();
        plan.setExeState(3);
        plan.setLastError("算法服务未注册: a-1");
        when(exePlanMgrService.getExePlanById("p-3")).thenReturn(plan);
        when(planExecuteService.execute("p-3")).thenReturn(false);

        Map<String, Object> response = controller.execute("p-3", adminRequest());

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
        plan.setExecutionId("exec-1");
        ExePlanLog log = new ExePlanLog();
        log.setSeq(3L);
        log.setMessage("计划开始执行");
        when(exePlanMgrService.getPlanLogs("p-log-1", "exec-1", 0L, 200)).thenReturn(Collections.singletonList(log));
        when(exePlanMgrService.getExePlanById("p-log-1")).thenReturn(plan);

        Map<String, Object> response = controller.getPlanLogs("p-log-1", 0L, 200, null, "latest", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(3L, data.get("nextSeq"));
        assertEquals(2, data.get("planState"));
        assertEquals("exec-1", data.get("executionId"));
    }

    @Test
    void preCheckReturns400WhenPlanIdEmpty() {
        Map<String, Object> response = controller.preCheck(" ", new MockHttpServletRequest());
        assertEquals(400, response.get("code"));
        assertEquals("PLAN_ID_EMPTY", response.get("errorCode"));
    }

    @Test
    void preCheckReturns200WhenPassed() {
        PlanPreCheckResult result = PlanPreCheckResult.passed(Collections.emptyList());
        when(planExecuteService.preCheck("p-ok")).thenReturn(result);

        Map<String, Object> response = controller.preCheck("p-ok", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(true, data.get("pass"));
        assertEquals("执行前检查通过", response.get("msg"));
    }

    @Test
    void preCheckReturns400WhenFailed() {
        PlanPreCheckResult result = PlanPreCheckResult.failed("ALG_SERVICE_NO_INSTANCE",
                "执行前检查失败: 服务[nsga2-zdt1-ls]在Nacos中无可用实例", Collections.emptyList());
        when(planExecuteService.preCheck("p-fail")).thenReturn(result);

        Map<String, Object> response = controller.preCheck("p-fail", new MockHttpServletRequest());

        assertEquals(400, response.get("code"));
        assertEquals("ALG_SERVICE_NO_INSTANCE", response.get("errorCode"));
    }

    @Test
    void wizardPrecheckReturns400WhenProblemMissing() {
        when(probInstMgrService.getProbInstByID("prob-1")).thenReturn(null);
        Map<String, Object> response = controller.wizardPrecheck(null, "prob-1", "alg-1", "svc", new MockHttpServletRequest());
        assertEquals(400, response.get("code"));
    }

    @Test
    void wizardPrecheckReturns200WhenReachable() {
        ProbInst probInst = new ProbInst();
        probInst.setInstId("prob-1");
        AlgInfo algInfo = new AlgInfo();
        algInfo.setAlgId("alg-1");
        algInfo.setAlgName("testAlg");
        algInfo.setServiceName("svc-1");
        when(probInstMgrService.getProbInstByID("prob-1")).thenReturn(probInst);
        when(algLibMgrService.getAlgInfoById("alg-1")).thenReturn(algInfo);
        when(discoveryClient.getInstances("svc-1")).thenReturn(Collections.singletonList(mock(ServiceInstance.class)));

        Map<String, Object> response = controller.wizardPrecheck(null, "prob-1", "alg-1", "svc-1", new MockHttpServletRequest());
        assertEquals(200, response.get("code"));
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new AuthUser("u-admin", "admin", 1));
        return request;
    }
}
