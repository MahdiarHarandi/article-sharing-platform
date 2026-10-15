#!/usr/bin/env sh
set -eu

APP_PORT="${APP_PORT:-8080}"
BASE_URL="http://127.0.0.1:${APP_PORT}"

command -v curl >/dev/null 2>&1 || {
  echo "curl is required for this smoke test." >&2
  exit 1
}

echo "Checking Nginx health..."
curl --fail --silent --show-error "${BASE_URL}/healthz" >/dev/null

echo "Checking proxied backend health..."
curl --fail --silent --show-error "${BASE_URL}/api/health" | grep -q '"status"'

echo "Checking React index page..."
curl --fail --silent --show-error "${BASE_URL}/" | grep -qi '<div id="root"'

echo "Checking public article list through reverse proxy..."
curl --fail --silent --show-error "${BASE_URL}/api/articles" | grep -q '"items"'

echo "Checking SPA route fallback..."
curl --fail --silent --show-error "${BASE_URL}/login" | grep -qi '<div id="root"'

echo "All Docker smoke checks passed."
