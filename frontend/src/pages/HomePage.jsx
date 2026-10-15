import { Link, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import { listArticles } from "../api/articlesApi.js";
import { useAuth } from "../auth/AuthContext.jsx";
import ArticleCard from "../components/ArticleCard.jsx";
import EmptyState from "../components/EmptyState.jsx";
import ErrorState from "../components/ErrorState.jsx";
import Loading from "../components/Loading.jsx";
import SearchBar from "../components/SearchBar.jsx";
import SortSelector from "../components/SortSelector.jsx";
import { useAsync } from "../hooks/useAsync.js";

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated, user } = useAuth();
  const search = searchParams.get("search") || "";
  const sort = searchParams.get("sort") || "newest";

  const { data, loading, error, execute } = useAsync(
    (signal) => listArticles({ search, sort, signal }),
    [search, sort, user?.id]
  );

  function updateParams(nextValues) {
    const nextParams = new URLSearchParams(searchParams);
    Object.entries(nextValues).forEach(([key, value]) => {
      if (!value || value === "newest") nextParams.delete(key);
      else nextParams.set(key, value);
    });
    setSearchParams(nextParams);
  }

  function handleRetry() {
    toast.info("Trying to reconnect to the API...");
    execute();
  }

  const articles = data?.items || [];
  return (
    <section className="page-section">
      <div className="hero-card">
        <div>
          <p className="eyebrow">Public article catalogue</p>
          <h2>Explore and search published articles</h2>
          <p>
            Everyone can view this list. Sign in to read full article details, publish content, and access your profile.
          </p>
        </div>
        <Link className="button button-large" to={isAuthenticated ? "/articles/new" : "/login"}>
          {isAuthenticated ? "Add New Article" : "Sign in to continue"}
        </Link>
      </div>

      {!isAuthenticated && (
        <div className="guest-notice">
          <strong>Guest access:</strong> the article catalogue is public; full article details and publishing require an account.
        </div>
      )}

      <div className="toolbar-card">
        <SearchBar initialValue={search} onSearch={(nextSearch) => updateParams({ search: nextSearch })} />
        <SortSelector value={sort} onChange={(nextSort) => updateParams({ sort: nextSort })} />
      </div>

      {search && <p className="result-note">Showing results for <strong>{search}</strong>. Title matches are ranked first.</p>}
      {loading && <Loading message="Loading articles from the article API..." />}
      {error && !loading && <ErrorState title="Could not load articles" error={error} onRetry={handleRetry} />}
      {!loading && !error && articles.length === 0 && <EmptyState search={search} />}
      {!loading && !error && articles.length > 0 && (
        <div className="article-list" aria-live="polite">
          <div className="list-summary">
            <strong>{data.count}</strong> article{data.count === 1 ? "" : "s"}
            {sort === "references" ? " sorted by incoming references" : " sorted by newest first"}
          </div>
          {articles.map((article) => <ArticleCard article={article} key={article.id} isAuthenticated={isAuthenticated} />)}
        </div>
      )}
    </section>
  );
}
