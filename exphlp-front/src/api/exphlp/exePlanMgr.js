import request from "@/utils/request";

export function getExePlans(pageNum, pageSize, scope, query){
  const params = Object.assign({
    pageNum: pageNum,
    pageSize: pageSize,
    scope: scope
  }, query || {});
  return request({
    url: "/api/ExePlanController/getExePlans",
    method: "get",
    params
  });
}

export function addExePlan(exePlan) {
  return request({
    url: "/api/ExePlanController/addExePlan",
    method: "post",
    data: exePlan
  });
}

export function getExePlanByName(planName) {
  return request({
    url: "/api/ExePlanController/getExePlanByName",
    method: "get",
    params: {
      planName: planName
    }
  });
}

export function deleteExePlanById(planId) {
  return request({
    url: "/api/ExePlanController/deleteExePlanById",
    method: "post",
    params: {
      planId: planId
    }
  });
}

export function updateExePlanById(exePlan) {
  return request({
    url: "/api/ExePlanController/updateExePlanById",
    method: "post",
    data: exePlan
  });
}

export function countAllExePlans(scope, query){
  const params = Object.assign({
    scope: scope
  }, query || {});
  return request({
    url: "/api/ExePlanController/countAllExePlans",
    method: "get",
    params
  });
}

export function execute(planId) {
  return request({
    url: "/api/ExePlanController/execute",
    method: "post",
    params: {
      planId: planId
    }
  });
}

export function preCheck(planId) {
  return request({
    url: "/api/ExePlanController/preCheck",
    method: "get",
    params: {
      planId: planId
    }
  });
}

export function wizardPrecheck(params) {
  return request({
    url: "/api/ExePlanController/wizardPrecheck",
    method: "get",
    params
  });
}

export function getPlanLogs(planId, afterSeq, limit, executionId, scope = "latest") {
  return request({
    url: "/api/ExePlanController/getPlanLogs",
    method: "get",
    params: {
      planId: planId,
      afterSeq: afterSeq,
      limit: limit,
      executionId: executionId,
      scope: scope
    }
  });
}

export function exportPlanLogs(planId, executionId, scope = "latest", limit = 5000) {
  return request({
    url: "/api/ExePlanController/exportPlanLogs",
    method: "get",
    params: {
      planId: planId,
      executionId: executionId,
      scope: scope,
      limit: limit,
    }
  });
}
