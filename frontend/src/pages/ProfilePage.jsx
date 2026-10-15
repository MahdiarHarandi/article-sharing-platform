import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import { changePassword, getProfile, updateContact } from "../api/authApi.js";
import { useAuth } from "../auth/AuthContext.jsx";
import ErrorState from "../components/ErrorState.jsx";
import FormField from "../components/FormField.jsx";
import Loading from "../components/Loading.jsx";
import { formatDateTime } from "../utils/date.js";

export default function ProfilePage() {
  const { user, updateUser, replaceSession } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [contact, setContact] = useState({ email: user?.email || "", phone: user?.phone || "" });
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [savingContact, setSavingContact] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    getProfile(controller.signal)
      .then((data) => {
        setProfile(data);
        setContact({ email: data.user.email || "", phone: data.user.phone || "" });
      })
      .catch((caught) => { if (caught.name !== "AbortError") setError(caught); })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, []);

  async function saveContact(event) {
    event.preventDefault();
    if (!contact.email.trim() && !contact.phone.trim()) {
      toast.error("At least one contact method is required.");
      return;
    }
    setSavingContact(true);
    try {
      const nextUser = await updateContact({ email: contact.email.trim() || null, phone: contact.phone.trim() || null });
      updateUser(nextUser);
      setProfile((current) => ({ ...current, user: nextUser }));
      toast.success("Contact information updated.");
    } catch (caught) {
      toast.error(caught.message || "Could not update contact information.");
    } finally {
      setSavingContact(false);
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    if (passwords.newPassword !== passwords.confirmPassword) {
      toast.error("New passwords do not match.");
      return;
    }
    setSavingPassword(true);
    try {
      const authResponse = await changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      replaceSession(authResponse);
      setPasswords({ currentPassword: "", newPassword: "", confirmPassword: "" });
      toast.success("Password changed. Previous tokens were invalidated.");
    } catch (caught) {
      toast.error(caught.message || "Could not change password.");
    } finally {
      setSavingPassword(false);
    }
  }

  if (loading) return <Loading message="Loading your profile..." />;
  if (error) return <ErrorState title="Could not load profile" error={error} />;

  const articles = profile?.articles || [];
  return (
    <section className="profile-page">
      <div className="profile-hero">
        <div>
          <p className="eyebrow">Authenticated profile</p>
          <h2>{profile.user.username}</h2>
          <p>Member since {formatDateTime(profile.user.createdAt)}</p>
        </div>
        <div className="profile-stat"><strong>{profile.articleCount}</strong><span>published articles</span></div>
      </div>

      <div className="profile-grid">
        <section className="content-card">
          <h3>Contact information</h3>
          <form onSubmit={saveContact}>
            <FormField label="Email" name="profile-email">
              <input id="profile-email" type="email" value={contact.email} onChange={(e) => setContact({ ...contact, email: e.target.value })} />
            </FormField>
            <FormField label="Phone" name="profile-phone">
              <input id="profile-phone" type="tel" value={contact.phone} onChange={(e) => setContact({ ...contact, phone: e.target.value })} />
            </FormField>
            <button className="button" disabled={savingContact}>{savingContact ? "Saving..." : "Save contact"}</button>
          </form>
        </section>

        <section className="content-card">
          <h3>Change password</h3>
          <form onSubmit={savePassword}>
            <FormField label="Current password" name="currentPassword">
              <input id="currentPassword" type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} required />
            </FormField>
            <FormField label="New password" name="newPassword" hint="At least 6 characters with lowercase, uppercase, and a number.">
              <input id="newPassword" type="password" autoComplete="new-password" value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} required />
            </FormField>
            <FormField label="Confirm new password" name="confirmPassword">
              <input id="confirmPassword" type="password" autoComplete="new-password" value={passwords.confirmPassword} onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })} required />
            </FormField>
            <button className="button" disabled={savingPassword}>{savingPassword ? "Changing..." : "Change password"}</button>
          </form>
        </section>
      </div>

      <section className="content-card">
        <div className="section-heading-row">
          <div><h3>Your published articles</h3><p className="muted-text">Only you can edit or delete these articles.</p></div>
          <Link className="button" to="/articles/new">Publish article</Link>
        </div>
        {articles.length === 0 ? (
          <p className="muted-text">You have not published an article yet.</p>
        ) : (
          <div className="profile-article-list">
            {articles.map((article) => (
              <div className="profile-article-row" key={article.id}>
                <div><Link to={`/articles/${article.id}`}><strong>{article.title}</strong></Link><span>{formatDateTime(article.createdAt)}</span></div>
                <Link className="button button-ghost" to={`/articles/${article.id}/edit`}>Edit</Link>
              </div>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}
