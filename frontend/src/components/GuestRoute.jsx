import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Loading from "./Loading.jsx";

export default function GuestRoute({ children }) {
  const { isAuthenticated, initializing } = useAuth();
  if (initializing) return <Loading message="Checking your session..." />;
  return isAuthenticated ? <Navigate to="/" replace /> : children;
}
