export default function ReferencePicker({ articles, selectedIds, onChange, disabledId }) {
  const selectableArticles = articles.filter((article) => String(article.id) !== String(disabledId));

  function toggleReference(id) {
    const normalizedId = Number(id);
    const nextIds = selectedIds.includes(normalizedId)
      ? selectedIds.filter((selectedId) => selectedId !== normalizedId)
      : [...selectedIds, normalizedId];

    onChange(nextIds);
  }

  if (selectableArticles.length === 0) {
    return (
      <div className="reference-picker muted-box">
        There are no other articles available to reference yet.
      </div>
    );
  }

  return (
    <div className="reference-picker">
      {selectableArticles.map((article) => (
        <label className="reference-option" key={article.id}>
          <input
            type="checkbox"
            checked={selectedIds.includes(Number(article.id))}
            onChange={() => toggleReference(article.id)}
          />
          <span>{article.title}</span>
          <small>{article.referenceCount ?? 0} incoming references</small>
        </label>
      ))}
    </div>
  );
}
