import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { deleteArticle, getArticle } from "../api/articlesApi.js";
import ErrorState from "../components/ErrorState.jsx";
import Loading from "../components/Loading.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { formatDateTime } from "../utils/date.js";
import { pluralize } from "../utils/text.js";

export default function ArticleDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: article, loading, error, execute } = useAsync((signal) => getArticle(id, signal), [id]);

  async function handleDelete() {
    if (!window.confirm("Are you sure you want to delete your article? This cannot be undone.")) return;
    try {
      await deleteArticle(id);
      toast.success("Article deleted successfully.");
      navigate("/");
    } catch (caughtError) {
      toast.error(caughtError.message || "Could not delete article.");
    }
  }

  if (loading) return <Loading message="Loading protected article details..." />;
  if (error) return <ErrorState title="Could not load article" error={error} onRetry={execute} />;
  if (!article) return <ErrorState title="Article not found" error={{ message: "The selected article does not exist." }} />;

  const references = article.references || [];
  const referencedBy = article.referencedBy || [];
  return (
    <article className="detail-page">
      <div className="breadcrumb-row"><Link to="/">← Back to articles</Link></div>
      <header className="detail-header">
        <div>
          <p className="eyebrow">Article #{article.id} · by @{article.authorUsername}</p>
          <h2>{article.title}</h2>
          <div className="detail-meta">
            <span>{formatDateTime(article.createdAt)}</span>
            <span>{article.referenceCount ?? 0} incoming {pluralize(article.referenceCount ?? 0, "reference")}</span>
            <span>{references.length} cited {pluralize(references.length, "article")}</span>
          </div>
        </div>
        {article.canModify && (
          <div className="detail-actions">
            <Link className="button button-light" to={`/articles/${article.id}/edit`}>Edit</Link>
            <button className="button button-danger" type="button" onClick={handleDelete}>Delete</button>
          </div>
        )}
      </header>
      {!article.canModify && <div className="ownership-note">Only @{article.authorUsername} can edit or delete this article.</div>}
      <section className="content-card"><h3>Abstract</h3><p className="abstract-text">{article.abstractText}</p></section>
      <section className="content-card article-body-card">
        <h3>Body</h3>
        <div className="article-body">{article.body.split("\n").map((paragraph, index) => <p key={`${index}-${paragraph.slice(0, 20)}`}>{paragraph || "\u00A0"}</p>)}</div>
      </section>
      <div className="two-column-grid">
        <section className="content-card">
          <h3>References used by this article</h3>
          {references.length === 0 ? <p className="muted-text">This article does not cite another article.</p> : (
            <ul className="link-list">{references.map((reference) => <li key={reference.id}><Link to={`/articles/${reference.id}`}>{reference.title}</Link></li>)}</ul>
          )}
        </section>
        <section className="content-card">
          <h3>Articles referencing this one</h3>
          {referencedBy.length === 0 ? <p className="muted-text">No article references this article yet.</p> : (
            <ul className="link-list">{referencedBy.map((source) => <li key={source.id}><Link to={`/articles/${source.id}`}>{source.title}</Link></li>)}</ul>
          )}
        </section>
      </div>
    </article>
  );
}
