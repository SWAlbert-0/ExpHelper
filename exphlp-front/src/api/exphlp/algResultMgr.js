import request from "@/utils/request";

export function getExeResult(planId, algId, algName) {
  return request({
    url: "/api/AlgRltSaveController/getAlgSaveByAlgName",
    method: "get",
    params: {
      planId: planId,
      algId: algId,
      algName: algName
    }
  });
}

export function getExeResultDetail(planId, algId) {
  return request({
    url: "/api/AlgRltSaveController/getExeResultDetail",
    method: "get",
    params: {
      planId: planId,
      algId: algId
    }
  });
}
