import Cookies from "js-cookie";

const TokenKey = "Admin-Token";
const RememberTokenKey = "Remember-Token";

export function getToken() {
  return Cookies.get(TokenKey);
}

export function setToken(token) {
  return Cookies.set(TokenKey, token);
}

export function removeToken() {
  return Cookies.remove(TokenKey);
}

export function getRememberToken() {
  return window.localStorage.getItem(RememberTokenKey) || "";
}

export function setRememberToken(token) {
  if (!token) {
    window.localStorage.removeItem(RememberTokenKey);
    return "";
  }
  window.localStorage.setItem(RememberTokenKey, token);
  return token;
}

export function removeRememberToken() {
  window.localStorage.removeItem(RememberTokenKey);
}
