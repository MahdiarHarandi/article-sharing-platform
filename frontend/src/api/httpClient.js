import { clearSession, getAccessToken } from "../auth/authStorage.js";
import { API_BASE_URL } from "../config.js";

function normalizePath(path) { return path.startsWith("/") ? path : `/${path}`; }
function buildUrl(path) { return `${API_BASE_URL}${normalizePath(path)}`; }

async function parseResponseBody(response) {
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); }
  catch { throw new Error(`Server returned invalid JSON. Status: ${response.status}`); }
}

export async function request(path, options = {}) {
  const { method = "GET", body, headers = {}, signal } = options;
  const hasBody = body !== undefined && body !== null;
  const token = getAccessToken();
  let response;
  try {
    response = await fetch(buildUrl(path), {
      method,
      signal,
      headers: {
        Accept: "application/json",
        ...(hasBody ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: hasBody ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error(`Could not connect to the article API at ${API_BASE_URL}. Make sure the backend is running.`);
  }

  if (response.status === 204) return null;
  const data = await parseResponseBody(response);
  if (!response.ok) {
    if (response.status === 401 && token) {
      clearSession();
      window.dispatchEvent(new Event("auth-expired"));
    }
    const apiError = new Error(data?.message || `Request failed with status ${response.status}.`);
    apiError.status = response.status;
    apiError.code = data?.error || "REQUEST_FAILED";
    apiError.details = data;
    throw apiError;
  }
  return data;
}
