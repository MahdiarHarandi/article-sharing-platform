import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../auth/AuthContext.jsx";
import FormField from "../components/FormField.jsx";

export default function LoginPage() {
  const [values, setValues] = useState({ username: "", password: "" });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!values.username.trim()) nextErrors.username = "Username is required.";
    if (!values.password) nextErrors.password = "Password is required.";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    setSubmitting(true);
    try {
      await login({ username: values.username.trim(), password: values.password });
      toast.success("Welcome back. You are now signed in.");
      navigate(location.state?.from || "/", { replace: true });
    } catch (error) {
      toast.error(error.message || "Login failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card">
        <p className="eyebrow">Secure account access</p>
        <h2>Sign in</h2>
        <p className="muted-text">Use your username and password to access article details, publishing, and your profile.</p>
        <form onSubmit={handleSubmit} noValidate>
          <FormField label="Username" name="username" error={errors.username}>
            <input id="username" autoComplete="username" value={values.username}
              onChange={(event) => setValues({ ...values, username: event.target.value })} />
          </FormField>
          <FormField label="Password" name="password" error={errors.password}>
            <input id="password" type="password" autoComplete="current-password" value={values.password}
              onChange={(event) => setValues({ ...values, password: event.target.value })} />
          </FormField>
          <button className="button button-large full-width" disabled={submitting}>
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>
        <p className="auth-switch">No account yet? <Link to="/register">Create one</Link></p>
      </div>
    </section>
  );
}
