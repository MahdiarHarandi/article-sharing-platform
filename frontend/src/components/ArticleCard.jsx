import { Link } from "react-router-dom";
import { formatDateTime } from "../utils/date.js";
import { pluralize, truncate } from "../utils/text.js";

export default function ArticleCard({ article, isAuthenticated }) {
  const referenceCount = article.referenceCount ?? 0;
  const ownReferences = article.referenceIds?.length ?? 0;
  return (
    <article className="article-card">
      <div className="article-card-main">
        <div className="article-card-meta">
          <span>{formatDateTime(article.createdAt)}</span>
          <span>by @{article.authorUsername}</span>
          <span>{referenceCount} incoming {pluralize(referenceCount, "reference")}</span>
          <span>{ownReferences} cited {pluralize(ownReferences, "article")}</span>
        </div>
        <h2><Link to={`/articles/${article.id}`}>{article.title}</Link></h2>
        <p>{truncate(article.abstractText, 230)}</p>
      </div>
      <div className="article-card-actions">
        <Link className="button button-light" to={`/articles/${article.id}`}>
          {isAuthenticated ? "Read" : "Sign in to read"}
        </Link>
        {article.canModify && (
          <Link className="button button-ghost" to={`/articles/${article.id}/edit`}>Edit</Link>
        )}
      </div>
    </article>
  );
}
