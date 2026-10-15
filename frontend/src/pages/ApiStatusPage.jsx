import { toast } from "react-toastify";
import { getHealth } from "../api/articlesApi.js";
import ErrorState from "../components/ErrorState.jsx";
import Loading from "../components/Loading.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { API_BASE_URL, FRONTEND_URL } from "../config.js";
import { formatDateTime } from "../utils/date.js";

export default function ApiStatusPage() {
  const { data, loading, error, execute } = useAsync((signal) => getHealth(signal), []);

  function handleRetry() {
    toast.info("Checking API connection...");
    execute();
  }

  return (
    <section className="page-section">
      <div className="content-card">
        <p className="eyebrow">Backend connection</p>
        <h2>Dockerized API Status</h2>
        <p>
          This page checks whether the React app can reach the REST API. For network access, the API must use the
          same computer/IP address as the frontend, not <code>localhost</code> from another device.
        </p>
        <dl className="diagnostic-list">
          <div>
            <dt>Frontend opened from</dt>
            <dd><code>{FRONTEND_URL}</code></dd>
          </div>
          <div>
            <dt>API currently used</dt>
            <dd><code>{API_BASE_URL}</code></dd>
          </div>
        </dl>
      </div>

      {loading && <Loading message="Checking backend status..." />}

      {error && !loading && (
        <ErrorState title="API is not reachable" error={error} onRetry={handleRetry} />
      )}

      {data && !loading && !error && (
        <div className="status-card success-card">
          <h3>API is reachable</h3>
          <dl>
            <div>
              <dt>Status</dt>
              <dd>{data.status}</dd>
            </div>
            <div>
              <dt>Service</dt>
              <dd>{data.service}</dd>
            </div>
            <div>
              <dt>Server time</dt>
              <dd>{formatDateTime(data.timestamp)}</dd>
            </div>
          </dl>
          <button className="button button-light" type="button" onClick={handleRetry}>
            Refresh status
          </button>
        </div>
      )}
    </section>
  );
}
