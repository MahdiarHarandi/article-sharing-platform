import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../auth/AuthContext.jsx";
import FormField from "../components/FormField.jsx";

const INITIAL = { username: "", email: "", phone: "", password: "", confirmPassword: "" };

function validate(values) {
  const errors = {};
  if (!/^[A-Za-z0-9._-]{3,50}$/.test(values.username.trim())) {
    errors.username = "Use 3-50 letters, numbers, dot, underscore, or hyphen.";
  }
  if (!values.email.trim() && !values.phone.trim()) {
    errors.email = "At least email or phone is required.";
    errors.phone = "At least email or phone is required.";
  }
  if (values.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = "Enter a valid email address.";
  }
  const normalizedPhone = values.phone.replace(/[\s()-]/g, "");
  if (values.phone.trim() && !/^\+?[0-9]{10,15}$/.test(normalizedPhone)) {
    errors.phone = "Use 10-15 digits with an optional leading +.";
  }
  if (values.password.length < 6 || !/[a-z]/.test(values.password) || !/[A-Z]/.test(values.password) || !/[0-9]/.test(values.password)) {
    errors.password = "Use at least 6 characters with lowercase, uppercase, and a number.";
  }
  if (values.password !== values.confirmPassword) errors.confirmPassword = "Passwords do not match.";
  return errors;
}

export default function RegisterPage() {
  const [values, setValues] = useState(INITIAL);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  function update(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = validate(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      toast.error("Please fix the highlighted registration fields.");
      return;
    }
    setSubmitting(true);
    try {
      await register({
        username: values.username.trim(),
        email: values.email.trim() || null,
        phone: values.phone.trim() || null,
        password: values.password,
      });
      toast.success("Account created successfully.");
      navigate("/profile", { replace: true });
    } catch (error) {
      toast.error(error.message || "Registration failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card auth-card-wide">
        <p className="eyebrow">Authentication & authorization</p>
        <h2>Create an account</h2>
        <p className="muted-text">Username, email, and phone are unique. At least one contact method is required.</p>
        <form onSubmit={handleSubmit} noValidate>
          <div className="form-grid-two">
            <FormField label="Username" name="username" error={errors.username}>
              <input id="username" autoComplete="username" value={values.username} onChange={(e) => update("username", e.target.value)} />
            </FormField>
            <FormField label="Email (optional when phone is provided)" name="email" error={errors.email}>
              <input id="email" type="email" autoComplete="email" value={values.email} onChange={(e) => update("email", e.target.value)} />
            </FormField>
            <FormField label="Phone (optional when email is provided)" name="phone" error={errors.phone}>
              <input id="phone" type="tel" autoComplete="tel" value={values.phone} onChange={(e) => update("phone", e.target.value)} placeholder="+989121234567" />
            </FormField>
            <div />
            <FormField label="Password" name="password" error={errors.password}>
              <input id="password" type="password" autoComplete="new-password" value={values.password} onChange={(e) => update("password", e.target.value)} />
            </FormField>
            <FormField label="Confirm password" name="confirmPassword" error={errors.confirmPassword}>
              <input id="confirmPassword" type="password" autoComplete="new-password" value={values.confirmPassword} onChange={(e) => update("confirmPassword", e.target.value)} />
            </FormField>
          </div>
          <button className="button button-large full-width" disabled={submitting}>
            {submitting ? "Creating account..." : "Create account"}
          </button>
        </form>
        <p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p>
      </div>
    </section>
  );
}
