import request from "@/utils/request";

export function login(username, password, rememberMe = false) {
  return request({
    url: "/api/auth/login",
    method: "post",
    data: { username, password, rememberMe }
  });
}

export function rememberLogin(rememberToken) {
  return request({
    url: "/api/auth/remember-login",
    method: "post",
    data: { rememberToken }
  });
}

export function getInfo() {
  return request({
    url: "/api/auth/me",
    method: "get"
  });
}

export function logout() {
  return request({
    url: "/api/auth/logout",
    method: "post"
  });
}

export function getProfile() {
  return request({
    url: "/api/auth/profile",
    method: "get"
  });
}

export function updateProfile(data) {
  return request({
    url: "/api/auth/profile",
    method: "put",
    data
  });
}

export function updatePassword(oldPassword, newPassword) {
  return request({
    url: "/api/auth/password",
    method: "put",
    data: { oldPassword, newPassword }
  });
}

export function uploadAvatar(data) {
  return request({
    url: "/api/auth/avatar",
    method: "post",
    data,
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
}

export function getHealthz() {
  return request({
    url: "/api/auth/healthz",
    method: "get"
  });
}
