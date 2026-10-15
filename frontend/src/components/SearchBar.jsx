import { useEffect, useState } from "react";

export default function SearchBar({ initialValue = "", onSearch }) {
  const [value, setValue] = useState(initialValue);

  useEffect(() => {
    setValue(initialValue);
  }, [initialValue]);

  function handleSubmit(event) {
    event.preventDefault();
    onSearch(value.trim());
  }

  function handleClear() {
    setValue("");
    onSearch("");
  }

  return (
    <form className="search-bar" onSubmit={handleSubmit}>
      <label className="sr-only" htmlFor="article-search">
        Search articles
      </label>
      <input
        id="article-search"
        type="search"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder="Search by title or abstract..."
      />
      <button className="button" type="submit">
        Search
      </button>
      {initialValue && (
        <button className="button button-ghost" type="button" onClick={handleClear}>
          Clear
        </button>
      )}
    </form>
  );
}
