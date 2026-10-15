const SESSION_KEY = "article_sharing_auth_session";

export function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session?.token || !session?.user) return null;
    return session;
  } catch {
    return null;
  }
}

export function saveSession(authResponse) {
  const session = {
    token: authResponse.token,
    tokenType: authResponse.tokenType || "Bearer",
    expiresAt: authResponse.expiresAt,
    user: authResponse.user,
  };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function updateStoredUser(user) {
  const current = loadSession();
  if (!current) return null;
  const next = { ...current, user };
  localStorage.setItem(SESSION_KEY, JSON.stringify(next));
  return next;
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function getAccessToken() {
  return loadSession()?.token || null;
}
