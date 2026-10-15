import { request } from "./httpClient.js";

function toQueryString(params) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      query.set(key, String(value).trim());
    }
  });

  const queryString = query.toString();
  return queryString ? `?${queryString}` : "";
}

export function getHealth(signal) {
  return request("/health", { signal });
}

export function listArticles({ search = "", sort = "newest", signal } = {}) {
  return request(`/articles${toQueryString({ search, sort })}`, { signal });
}

export function getArticle(id, signal) {
  return request(`/articles/${encodeURIComponent(id)}`, { signal });
}

export function createArticle(article) {
  return request("/articles", {
    method: "POST",
    body: article,
  });
}

export function updateArticle(id, article) {
  return request(`/articles/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: article,
  });
}

export function deleteArticle(id) {
  return request(`/articles/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export function getArticleReferences(id, signal) {
  return request(`/articles/${encodeURIComponent(id)}/references`, { signal });
}

export function getArticleReferencedBy(id, signal) {
  return request(`/articles/${encodeURIComponent(id)}/referenced-by`, { signal });
}
