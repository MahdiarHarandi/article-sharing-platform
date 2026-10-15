import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <section className="state-panel empty-panel">
      <h2>Page not found</h2>
      <p>The page you requested does not exist in the React application.</p>
      <Link className="button" to="/">
        Go to articles
      </Link>
    </section>
  );
}
