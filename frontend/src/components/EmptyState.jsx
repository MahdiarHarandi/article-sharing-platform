import { Link } from "react-router-dom";

export default function EmptyState({ search }) {
  return (
    <div className="state-panel empty-panel">
      <h2>No articles found</h2>
      {search ? (
        <p>No article title or abstract matched “{search}”. Try a different keyword.</p>
      ) : (
        <p>No articles exist yet. Create the first one to start the collection.</p>
      )}
      <Link className="button" to="/articles/new">
        Add New Article
      </Link>
    </div>
  );
}
