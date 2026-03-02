import request from "@/utils/request";

export function addAlg(algInfo) {
  return request({
    url: "/api/AlgController/addAlg",
    method: "post",
    data: algInfo
  });
}

export function deleteAlgById(algId) {
  return request({
    url: "/api/AlgController/deleteAlgById",
    method: "post",
    params: {
      algId: algId
    }
  });
}

export function getAlgById(algId) {
  return request({
    url: "/api/AlgController/getAlgById",
    method: "get",
    params: {
      algId: algId
    }
  });
}

export function getAlgs(pageNum, pageSize) {
  return request({
    url: "/api/AlgController/getAlgs",
    method: "get",
    params: {
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function getAlgsByName(algName, pageNum, pageSize) {
  return request({
    url: "/api/AlgController/getAlgsByName",
    method: "get",
    params: {
      algName: algName,
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function updateAlgById(algInfo) {
  return request({
    url: "/api/AlgController/updateAlgById",
    method: "post",
    data: algInfo
  });
}

export function getParaByAlgId(algId) {
  return request({
    url: "/api/AlgController/getParaByAlgId",
    method: "get",
    params: {
      algId: algId
    }
  });
}

export function countAllAlgInfos(){
  return request({
    url: "/api/AlgController/countAllAlgs",
    method: "get"
  });
}

export function countAlgInfosByAlgName(algName) {
  return request({
    url: "/api/AlgController/countAlgsByAlgName",
    method: "get",
    params: {
      algName: algName
    }
  });
}

export function generateDeployTemplate(algId) {
  return request({
    url: "/api/AlgController/generateDeployTemplate",
    method: "post",
    params: {
      algId: algId
    }
  });
}

export function importAlgsJson(jsonText) {
  return request({
    url: "/api/AlgController/importAlgsJson",
    method: "post",
    data: {
      jsonText
    }
  });
}

export function uploadAlgSource(algId, file) {
  const formData = new FormData();
  formData.append("algId", algId);
  formData.append("file", file);
  return request({
    url: "/api/AlgController/uploadSource",
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function triggerAlgBuild(taskId) {
  return request({
    url: "/api/AlgController/buildAndStart",
    method: "post",
    params: { taskId },
  });
}

export function getAlgBuildStatus(taskId) {
  return request({
    url: "/api/AlgController/buildStatus",
    method: "get",
    params: { taskId },
  });
}

export function getAlgBuildLogs(taskId, tail = 200) {
  return request({
    url: "/api/AlgController/buildLogs",
    method: "get",
    params: { taskId, tail },
  });
}

export function getAlgSourceRuntimeInfo(algId) {
  return request({
    url: "/api/AlgController/sourceRuntimeInfo",
    method: "get",
    params: { algId },
  });
}

export function operateAlgSourceRuntime(algId, action) {
  return request({
    url: "/api/AlgController/sourceRuntimeOperate",
    method: "post",
    params: { algId, action },
  });
}
