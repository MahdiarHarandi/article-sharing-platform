export default function ErrorState({ title = "Something went wrong", error, onRetry }) {
  return (
    <div className="state-panel error-panel" role="alert">
      <h2>{title}</h2>
      <p>{error?.message || "An unexpected error occurred."}</p>
      {error?.status && <span className="status-pill">HTTP {error.status}</span>}
      {onRetry && (
        <button className="button" type="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
