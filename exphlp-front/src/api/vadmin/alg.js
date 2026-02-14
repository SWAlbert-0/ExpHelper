import request from "@/utils/request";

export function addAlg(alg) {
  return request({
    url: "/api/AlgController/addAlg",
    method: "post",
    data: {
      algName: alg.algName,
      defPara: alg.defPara,
      description: alg.description
    }
  });
}
export function deleteAlgById(algId) {
  return request({
    url: "/api/AlgController/deleteAlgById",
    method: "post",
    data: {
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
export function updateAlgById(alg) {
  return request({
    url: "/api/AlgController/updateAlgById",
    method: "post",
    data: {
      algId: alg.algId,
      algName: alg.algName,
      defPara: alg.defPara,
      description: alg.description
    }
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
