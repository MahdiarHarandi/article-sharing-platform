# Frontend

React 18 single-page application built with Vite and React Router. It uses the backend REST API for accounts, articles, and references.

## Local commands

```bash
npm ci
npm run dev
npm run build
```

For development, create `frontend/.env.local` containing `VITE_API_BASE_URL=http://localhost:8080/api` (or another backend base URL). When deployed with Docker Compose, the frontend uses `/api` through Nginx.

See the project root [README](../README.md) for the complete Docker setup.
