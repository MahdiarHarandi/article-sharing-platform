export default function SortSelector({ value, onChange }) {
  return (
    <label className="sort-selector">
      <span>Sort</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="newest">Newest first</option>
        <option value="references">Most referenced</option>
      </select>
    </label>
  );
}
