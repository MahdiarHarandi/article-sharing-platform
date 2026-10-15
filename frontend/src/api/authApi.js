import { request } from "./httpClient.js";

export function registerAccount(values) {
  return request("/auth/register", { method: "POST", body: values });
}

export function loginAccount(values) {
  return request("/auth/login", { method: "POST", body: values });
}

export function logoutAccount() {
  return request("/auth/logout", { method: "POST" });
}

export function getProfile(signal) {
  return request("/profile", { signal });
}

export function updateContact(values) {
  return request("/profile/contact", { method: "PUT", body: values });
}

export function changePassword(values) {
  return request("/profile/password", { method: "PUT", body: values });
}
