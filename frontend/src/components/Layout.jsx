import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../auth/AuthContext.jsx";

export default function Layout() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    toast.success("You have been signed out and the Your access tokens were invalidated.");
    navigate("/", { replace: true });
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="container header-content">
          <div>
            <p className="eyebrow">Read, write and connect</p>
            <h1>Article Sharing Platform</h1>
          </div>
          <div className="project-badge" aria-label="Application features">
            <span>Full-stack application</span><strong>Articles · References · Profiles</strong>
          </div>
        </div>
        <nav className="container nav-bar" aria-label="Main navigation">
          <NavLink to="/" className={({ isActive }) => isActive ? "active" : undefined} end>Articles</NavLink>
          {isAuthenticated ? (
            <>
              <NavLink to="/articles/new" className={({ isActive }) => isActive ? "active" : undefined}>Add Article</NavLink>
              <NavLink to="/profile" className={({ isActive }) => isActive ? "active" : undefined}>Profile</NavLink>
              <NavLink to="/api-status" className={({ isActive }) => isActive ? "active" : undefined}>API Status</NavLink>
              <span className="nav-user">@{user?.username}</span>
              <button className="nav-button" type="button" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => isActive ? "active" : undefined}>Login</NavLink>
              <NavLink to="/register" className={({ isActive }) => isActive ? "active" : undefined}>Register</NavLink>
            </>
          )}
        </nav>
      </header>
      <main className="container main-content"><Outlet /></main>
      <footer className="site-footer">
        <div className="container footer-content">
          <span>Article Sharing Platform</span><span>React · Spring Boot · MySQL</span>
        </div>
      </footer>
    </div>
  );
}
