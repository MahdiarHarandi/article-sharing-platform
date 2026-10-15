function removeTrailingSlash(value) {
  return value?.replace(/\/$/, "");
}

function getBrowserHostname() {
  if (typeof window === "undefined") {
    return "localhost";
  }
  return window.location.hostname || "localhost";
}

function isLoopbackHostname(hostname) {
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1" || hostname === "0.0.0.0";
}

function rewriteLocalhostApiWhenOpenedFromNetwork(apiBaseUrl) {
  if (typeof window === "undefined") {
    return removeTrailingSlash(apiBaseUrl);
  }

  const browserHostname = getBrowserHostname();

  if (apiBaseUrl.startsWith("/")) {
    return removeTrailingSlash(apiBaseUrl);
  }

  try {
    const url = new URL(apiBaseUrl);

    // Important for LAN/network usage:
    // If the app is opened as http://192.168.x.x:5173 but VITE_API_BASE_URL or an old build says
    // http://localhost:8080/api, localhost would point to the OTHER user's device, not the server PC.
    // So when the frontend is opened from a network IP, automatically use that same IP for the API.
    if (!isLoopbackHostname(browserHostname) && isLoopbackHostname(url.hostname)) {
      url.hostname = browserHostname;
    }

    // The local Java server is plain HTTP.
    url.protocol = "http:";

    return removeTrailingSlash(url.toString());
  } catch (error) {
    return removeTrailingSlash(apiBaseUrl);
  }
}

function defaultApiBaseUrl() {
  const hostname = getBrowserHostname();
  return `http://${hostname}:8080/api`;
}

function resolveApiBaseUrl() {
  const configuredUrl = removeTrailingSlash(import.meta.env.VITE_API_BASE_URL);

  if (configuredUrl) {
    return rewriteLocalhostApiWhenOpenedFromNetwork(configuredUrl);
  }

  return defaultApiBaseUrl();
}

export const API_BASE_URL = resolveApiBaseUrl();

export const FRONTEND_URL =
  typeof window === "undefined" ? "http://localhost:5173" : window.location.href;

