export default function FormField({ label, name, error, children, hint }) {
  return (
    <div className={`form-field ${error ? "has-error" : ""}`}>
      <label htmlFor={name}>{label}</label>
      {children}
      {hint && <small className="field-hint">{hint}</small>}
      {error && <small className="field-error">{error}</small>}
    </div>
  );
}
