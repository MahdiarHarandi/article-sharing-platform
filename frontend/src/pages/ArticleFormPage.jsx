import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { createArticle, getArticle, listArticles, updateArticle } from "../api/articlesApi.js";
import ErrorState from "../components/ErrorState.jsx";
import FormField from "../components/FormField.jsx";
import Loading from "../components/Loading.jsx";
import ReferencePicker from "../components/ReferencePicker.jsx";

const EMPTY_FORM = {
  title: "",
  abstractText: "",
  body: "",
  referenceIds: [],
};

function validateForm(values) {
  const errors = {};

  if (!values.title.trim()) {
    errors.title = "Title is required.";
  }

  if (!values.abstractText.trim()) {
    errors.abstractText = "Abstract is required.";
  }

  if (!values.body.trim()) {
    errors.body = "Body is required.";
  }

  return errors;
}

export default function ArticleFormPage({ mode }) {
  const isEdit = mode === "edit";
  const { id } = useParams();
  const navigate = useNavigate();

  const [values, setValues] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [availableArticles, setAvailableArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState(null);

  const pageTitle = isEdit ? "Edit article" : "Add a new article";

  const selectedReferenceCount = useMemo(
    () => values.referenceIds.length,
    [values.referenceIds]
  );

  useEffect(() => {
    let ignore = false;
    const controller = new AbortController();

    async function loadInitialData() {
      setLoading(true);
      setLoadError(null);

      try {
        const articlesResponse = await listArticles({ sort: "newest", signal: controller.signal });
        if (!ignore) {
          setAvailableArticles(articlesResponse.items || []);
        }

        if (isEdit) {
          const existingArticle = await getArticle(id, controller.signal);
          if (!ignore) {
            setValues({
              title: existingArticle.title || "",
              abstractText: existingArticle.abstractText || "",
              body: existingArticle.body || "",
              referenceIds: (existingArticle.referenceIds || []).map(Number),
            });
          }
        } else if (!ignore) {
          setValues(EMPTY_FORM);
        }
      } catch (caughtError) {
        if (!ignore && caughtError.name !== "AbortError") {
          setLoadError(caughtError);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadInitialData();

    return () => {
      ignore = true;
      controller.abort();
    };
  }, [id, isEdit]);

  function updateField(fieldName, fieldValue) {
    setValues((currentValues) => ({
      ...currentValues,
      [fieldName]: fieldValue,
    }));

    setErrors((currentErrors) => ({
      ...currentErrors,
      [fieldName]: undefined,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const nextErrors = validateForm(values);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      toast.error("Please fix the highlighted fields.");
      return;
    }

    const payload = {
      title: values.title.trim(),
      abstractText: values.abstractText.trim(),
      body: values.body.trim(),
      referenceIds: values.referenceIds.map(Number),
    };

    setSaving(true);
    try {
      const savedArticle = isEdit ? await updateArticle(id, payload) : await createArticle(payload);
      toast.success(isEdit ? "Article updated successfully." : "Article created successfully.");
      navigate(`/articles/${savedArticle.id}`);
    } catch (caughtError) {
      toast.error(caughtError.message || "Could not save article.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <Loading message={isEdit ? "Loading article for editing..." : "Preparing article form..."} />;
  }

  if (loadError) {
    return <ErrorState title="Could not prepare the form" error={loadError} />;
  }

  return (
    <section className="form-page">
      <div className="breadcrumb-row">
        <Link to="/">← Back to articles</Link>
      </div>

      <div className="form-card">
        <div className="form-header">
          <div>
            <p className="eyebrow">{isEdit ? `Article #${id}` : "New article"}</p>
            <h2>{pageTitle}</h2>
            <p>
              Fill in the article information. The title must be unique because the backend checks it strictly.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <FormField label="Title" name="title" error={errors.title}>
            <input
              id="title"
              name="title"
              type="text"
              value={values.title}
              onChange={(event) => updateField("title", event.target.value)}
              placeholder="Example: Understanding REST APIs"
              maxLength={160}
            />
          </FormField>

          <FormField
            label="Abstract"
            name="abstractText"
            error={errors.abstractText}
            hint="This is used in article cards and search results."
          >
            <textarea
              id="abstractText"
              name="abstractText"
              value={values.abstractText}
              onChange={(event) => updateField("abstractText", event.target.value)}
              placeholder="Write a short summary of the article..."
              rows={5}
            />
          </FormField>

          <FormField label="Body" name="body" error={errors.body}>
            <textarea
              id="body"
              name="body"
              value={values.body}
              onChange={(event) => updateField("body", event.target.value)}
              placeholder="Write the full article body..."
              rows={11}
            />
          </FormField>

          <fieldset className="reference-fieldset">
            <legend>References</legend>
            <p>
              Optional bonus feature: select existing articles that this article references.
              Selected references: <strong>{selectedReferenceCount}</strong>
            </p>
            <ReferencePicker
              articles={availableArticles}
              selectedIds={values.referenceIds}
              disabledId={isEdit ? id : undefined}
              onChange={(referenceIds) => updateField("referenceIds", referenceIds)}
            />
          </fieldset>

          <div className="form-actions">
            <button className="button button-large" type="submit" disabled={saving}>
              {saving ? "Saving..." : isEdit ? "Update Article" : "Create Article"}
            </button>
            <Link className="button button-ghost" to={isEdit ? `/articles/${id}` : "/"}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </section>
  );
}
