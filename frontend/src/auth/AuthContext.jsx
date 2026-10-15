import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getProfile, loginAccount, logoutAccount, registerAccount } from "../api/authApi.js";
import { clearSession, loadSession, saveSession, updateStoredUser } from "./authStorage.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());
  const [initializing, setInitializing] = useState(Boolean(loadSession()));

  const endLocalSession = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  useEffect(() => {
    function handleExpired() {
      endLocalSession();
    }
    window.addEventListener("auth-expired", handleExpired);
    return () => window.removeEventListener("auth-expired", handleExpired);
  }, [endLocalSession]);

  useEffect(() => {
    const current = loadSession();
    if (!current) {
      setInitializing(false);
      return undefined;
    }

    const controller = new AbortController();
    getProfile(controller.signal)
      .then((profile) => {
        const next = updateStoredUser(profile.user);
        setSession(next);
      })
      .catch((error) => {
        if (error.name !== "AbortError") endLocalSession();
      })
      .finally(() => setInitializing(false));

    return () => controller.abort();
  }, [endLocalSession]);

  const authenticate = useCallback(async (credentials) => {
    const response = await loginAccount(credentials);
    const next = saveSession(response);
    setSession(next);
    return response;
  }, []);

  const register = useCallback(async (values) => {
    const response = await registerAccount(values);
    const next = saveSession(response);
    setSession(next);
    return response;
  }, []);

  const logout = useCallback(async () => {
    try {
      if (loadSession()?.token) await logoutAccount();
    } finally {
      endLocalSession();
    }
  }, [endLocalSession]);

  const replaceSession = useCallback((authResponse) => {
    const next = saveSession(authResponse);
    setSession(next);
  }, []);

  const updateUser = useCallback((user) => {
    const next = updateStoredUser(user);
    setSession(next);
  }, []);

  const value = useMemo(() => ({
    session,
    user: session?.user || null,
    token: session?.token || null,
    isAuthenticated: Boolean(session?.token),
    initializing,
    login: authenticate,
    register,
    logout,
    replaceSession,
    updateUser,
  }), [session, initializing, authenticate, register, logout, replaceSession, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider.");
  return context;
}
