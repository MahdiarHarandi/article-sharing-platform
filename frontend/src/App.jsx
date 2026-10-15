import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import GuestRoute from "./components/GuestRoute.jsx";
import HomePage from "./pages/HomePage.jsx";
import ArticleDetailPage from "./pages/ArticleDetailPage.jsx";
import ArticleFormPage from "./pages/ArticleFormPage.jsx";
import ApiStatusPage from "./pages/ApiStatusPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";

const protectedPage = (page) => <ProtectedRoute>{page}</ProtectedRoute>;

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<GuestRoute><LoginPage /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><RegisterPage /></GuestRoute>} />
        <Route path="/articles/new" element={protectedPage(<ArticleFormPage mode="create" />)} />
        <Route path="/articles/:id" element={protectedPage(<ArticleDetailPage />)} />
        <Route path="/articles/:id/edit" element={protectedPage(<ArticleFormPage mode="edit" />)} />
        <Route path="/profile" element={protectedPage(<ProfilePage />)} />
        <Route path="/api-status" element={protectedPage(<ApiStatusPage />)} />
        <Route path="/home" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
