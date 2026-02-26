package fjnu.edu.controller;

import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.algruntime.service.AlgBuildTaskService;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.alglibmgr.entity.AlgDeleteResult;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlgLibMgrCtrlTest {

    private AlgLibMgrCtrl controller;
    private AlgLibMgrService algLibMgrService;
    private ExePlanMgrService exePlanMgrService;
    private AlgBuildTaskService algBuildTaskService;

    @BeforeEach
    void setUp() {
        controller = new AlgLibMgrCtrl();
        algLibMgrService = mock(AlgLibMgrService.class);
        exePlanMgrService = mock(ExePlanMgrService.class);
        algBuildTaskService = mock(AlgBuildTaskService.class);
        ReflectionTestUtils.setField(controller, "algLibMgrService", algLibMgrService);
        ReflectionTestUtils.setField(controller, "exePlanMgrService", exePlanMgrService);
        ReflectionTestUtils.setField(controller, "algBuildTaskService", algBuildTaskService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
    }

    @Test
    void deleteAlgByIdReturns200WhenRecordDeleted() {
        AlgDeleteResult result = new AlgDeleteResult();
        result.setDeletedCount(1L);
        result.setRepaired(false);
        result.setNoop(false);
        result.setVerified(true);
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
        result.setVerified(true);
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
        result.setVerified(true);
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
    void deleteAlgByIdReturns500WhenNoopButUnverified() {
        AlgDeleteResult result = new AlgDeleteResult();
        result.setDeletedCount(0L);
        result.setRepaired(false);
        result.setNoop(true);
        result.setVerified(false);
        when(exePlanMgrService.countPlansByAlgId("alg-bad")).thenReturn(0L);
        when(algLibMgrService.deleteAlgInfoById("alg-bad")).thenReturn(result);

        Map<String, Object> response = controller.deleteAlgInfoById("alg-bad", new MockHttpServletRequest());

        assertEquals(500, response.get("code"));
        assertEquals("INTERNAL_ERROR", response.get("errorCode"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(false, data.get("verified"));
        assertEquals(true, data.get("noop"));
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

    @Test
    void generateDeployTemplateReturns200() {
        AlgInfo info = new AlgInfo();
        info.setAlgId("alg-2");
        info.setAlgName("nsga2");
        info.setServiceName("nsga2-zdt1-ls");
        when(algLibMgrService.getAlgInfoById("alg-2")).thenReturn(info);

        Map<String, Object> response = controller.generateDeployTemplate("alg-2", new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("nsga2-zdt1-ls", data.get("serviceName"));
    }

    @Test
    void importAlgsJsonReturns200() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jsonText", "[{\"algName\":\"demo-alg-a\",\"serviceName\":\"demo-alg-a\",\"description\":\"demo\"}]");
        when(algLibMgrService.getAlgInfoByName("demo-alg-a")).thenReturn(null);

        Map<String, Object> response = controller.importAlgsJson(payload, new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(1, data.get("success"));
        assertEquals(0, data.get("failed"));
        org.mockito.ArgumentCaptor<AlgInfo> captor = org.mockito.ArgumentCaptor.forClass(AlgInfo.class);
        verify(algLibMgrService).addAlgInfo(captor.capture());
        assertEquals("java", captor.getValue().getRuntimeType());
    }

    @Test
    void addAlgDefaultsRuntimeTypeToJava() {
        AlgInfo info = new AlgInfo();
        info.setAlgName("demo-add");
        info.setServiceName("demo-service");
        info.setDescription("demo");
        when(algLibMgrService.getAlgInfoByName("demo-add")).thenReturn(null);

        String result = controller.addAlgInfo(info);

        assertEquals("demo-add", result);
        org.mockito.ArgumentCaptor<AlgInfo> captor = org.mockito.ArgumentCaptor.forClass(AlgInfo.class);
        verify(algLibMgrService).addAlgInfo(captor.capture());
        assertEquals("java", captor.getValue().getRuntimeType());
    }

    @Test
    void importAlgsJsonReturns400WhenJsonInvalid() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jsonText", "{bad-json");

        Map<String, Object> response = controller.importAlgsJson(payload, new MockHttpServletRequest());

        assertEquals(400, response.get("code"));
        assertEquals("INVALID_ARGUMENT", response.get("errorCode"));
    }

    @Test
    void uploadSourceReturns200WhenUploadAccepted() {
        AlgInfo info = new AlgInfo();
        info.setAlgId("alg-1");
        info.setAlgName("demo");
        info.setServiceName("demo-service");
        when(algLibMgrService.getAlgInfoById("alg-1")).thenReturn(info);
        AlgBuildTask task = new AlgBuildTask();
        task.setTaskId("t-1");
        task.setStatus("PENDING");
        when(algBuildTaskService.createUploadTask(org.mockito.ArgumentMatchers.eq(info),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(task);

        MockMultipartFile file = new MockMultipartFile("file", "demo.zip", "application/zip", "zip".getBytes());
        Map<String, Object> response = controller.uploadSource("alg-1", file, new MockHttpServletRequest());

        assertEquals(200, response.get("code"));
    }

    @Test
    void buildStatusReturns404WhenTaskMissing() {
        when(algBuildTaskService.getTask("missing")).thenReturn(null);
        Map<String, Object> response = controller.getBuildStatus("missing", new MockHttpServletRequest());
        assertEquals(404, response.get("code"));
    }
}
