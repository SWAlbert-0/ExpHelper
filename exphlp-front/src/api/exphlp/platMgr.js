import request from "@/utils/request";

export function getUserList(pageNum, pageSize) {
  return request({
    url: "/api/PlatController/getUsersByPage",
    method: "get",
    params: {
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function getUserById(userId) {
  return request({
    url: "/api/PlatController/getUserById",
    method: "get",
    params: {
      userId: userId
    }
  });
}

export function deleteUserById(userId) {
  return request({
    url: "/api/PlatController/deleteUserById",
    method: "post",
    params: {
      userId: userId
    }
  });
}

export function addUser(userInfo) {
  return request({
    url: "/api/PlatController/addUser",
    method: "post",
    data: userInfo
  });
}

export function getUserByRegexName(userName, pageNum, pageSize) {
  return request({
    url: "/api/PlatController/getUserByRegexName",
    method: "get",
    params: {
      userName: userName,
      pageNum: pageNum,
      pageSize: pageSize
    }
  });
}

export function getUserByName(userName) {
  return request({
    url: "/api/PlatController/getUserByName",
    method: "get",
    params: {
      userName: userName
    }
  });
}

export function updateUserById(userInfo) {
  return request({
    url: "/api/PlatController/updateUserById",
    method: "post",
    data: userInfo
  });
}

export function countAllUsers() {
  return request({
    url: "/api/PlatController/countAllUsers",
    method: "get"
  });
}

export function countUserByUserName(userName) {
  return request({
    url: "/api/PlatController/countByUserName",
    method: "get",
    params: {
      userName: userName
    }
  });
}

export function resetUserPassword(userId, password, userName) {
  return request({
    url: "/api/PlatController/resetUserPassword",
    method: "post",
    data: {
      userId: userId,
      userName: userName,
      password: password
    }
  });
}
