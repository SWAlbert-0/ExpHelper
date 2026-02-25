import request from "@/utils/request";

export function getProbInstList(pageNum, pageSize) {
  return request({
    url: "/api/ProbController/getProblems",
    method: "get",
    params: {
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function getProbInstById(proId) {
  return request({
    url: "/api/ProbController/getProblemById",
    method: "get",
    params: {
      proId: proId
    }
  });
}
export function deleteProbInstById(proId) {
  return request({
    url: "/api/ProbController/deleteProblemById",
    method: "post",
    params: {
      proId: proId
    }
  });
}
export function addProbInst(probInst) {
  return request({
    url: "/api/ProbController/addProblem",
    method: "post",
    data: probInst
  });
}
export function getProbInstByInstName(instName, pageNum, pageSize) {
  return request({
    url: "/api/ProbController/getProblemsByName",
    method: "get",
    params: {
      probName: instName,
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}
export function updateProbInstById(probInst) {
  return request({
    url: "/api/ProbController/updateProblemById",
    method: "post",
    data: probInst
  });
}

export function countAllProbInsts() {
  return request({
    url: "/api/ProbController/countAllProbInsts",
    method: "get"
  });
}

export function countProbInstsByInstName(instName) {
  return request({
    url: "/api/ProbController/countProbInstsByInstName",
    method: "get",
    params: {
      probName: instName
    }
  });
}

export function importProbInstsJson(jsonText) {
  return request({
    url: "/api/ProbController/importProblemsJson",
    method: "post",
    data: {
      jsonText
    }
  });
}
