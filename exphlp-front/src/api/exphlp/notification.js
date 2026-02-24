import request from "@/utils/request";

export function getNotifyProfile() {
  return request({
    url: "/api/notification/profile",
    method: "get",
  });
}

export function updateNotifyProfile(data) {
  return request({
    url: "/api/notification/profile",
    method: "put",
    data,
  });
}

export function listNotifications(params) {
  return request({
    url: "/api/notification/list",
    method: "get",
    params,
  });
}

export function resendNotification(notificationId) {
  return request({
    url: "/api/notification/resend",
    method: "post",
    params: { notificationId },
  });
}

export function resendByExecution(planId, executionId) {
  return request({
    url: "/api/notification/resendByExecution",
    method: "post",
    params: { planId, executionId },
  });
}

