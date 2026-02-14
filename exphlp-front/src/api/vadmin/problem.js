import request from "@/utils/request";

export function getProbList(pageNum, pageSize) {
  return request({
    url: "/api/ProbController/getProblems",
    method: "get",
    params: {
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function getProById(proId) {
  return request({
    url: "/api/ProbController/getProblemById",
    method: "get",
    params: {
      proId: proId
    }
  });
}
export function deleteProbById(proId) {
  return request({
    url: "/api/ProbController/deleteProblemById",
    method: "post",
    params: {
      proId: proId
    }
  });
}
export function addProblem(problem) {
  return request({
    url: "/api/ProbController/addProblem",
    method: "post",
    data: {
      instName: problem.instName,
      categoryName: problem.categoryName,
      machineName: problem.machineName,
      machineIp: problem.machineIp,
      dirName: problem.dirName,
      description: problem.description
    }
  });
}
export function getProbByName(probName) {
  return request({
    url: "/api/ProbController/getProblemsByName",
    method: "get",
    params: {
      probName: probName
    }
  });
}
export function updateProbById(problem) {
  return request({
    url: "/api/ProbController/updateProblemById",
    method: "post",
    data: {
      instId: problem.instId,
      instName: problem.instName,
      categoryName: problem.categoryName,
      machineName: problem.machineName,
      machineIp: problem.machineIp,
      dirName: problem.dirName,
      description: problem.description
    }
  });
}
