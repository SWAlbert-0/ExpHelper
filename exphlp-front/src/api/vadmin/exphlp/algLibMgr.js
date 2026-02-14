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
