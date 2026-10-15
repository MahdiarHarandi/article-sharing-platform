#!/usr/bin/env python3
"""Static checks for the Project that do not require a Docker daemon."""

from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    yaml = None

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def text(relative: str) -> str:
    path = ROOT / relative
    require(path.is_file(), f"Missing file: {relative}")
    return path.read_text(encoding="utf-8") if path.is_file() else ""


compose_text = text("docker-compose.yml")
if yaml is not None:
    compose = yaml.safe_load(compose_text) or {}
    services = compose.get("services", {})
    require(set(services) == {"database", "backend", "frontend"}, "Compose must define database, backend, and frontend services.")
    require("ports" not in services.get("database", {}), "Database port must not be published to the host.")
    require("ports" not in services.get("backend", {}), "Backend port must not be published to the host.")
    require(bool(services.get("frontend", {}).get("ports")), "Frontend must publish the public application port.")
    require("mysql_data" in compose.get("volumes", {}), "Named mysql_data volume is missing.")
    require(compose.get("networks", {}).get("database_network", {}).get("internal") is True, "Database network must be internal.")
else:
    for service in ("database", "backend", "frontend"):
        require(re.search(rf"^  {service}:\s*$", compose_text, re.MULTILINE) is not None,
                f"Compose service is missing: {service}")
    require("mysql_data:/var/lib/mysql" in compose_text, "Named mysql_data volume is missing.")
    require(re.search(r"^    internal: true\s*$", compose_text, re.MULTILINE) is not None,
            "Database network must be internal.")

backend_dockerfile = text("backend/Dockerfile")
frontend_dockerfile = text("frontend/Dockerfile")
nginx = text("frontend/nginx.conf")
application = text("backend/src/main/resources/application.yml")
gitignore = text(".gitignore")
env_example = text(".env.example")

require(len(re.findall(r"^FROM ", backend_dockerfile, flags=re.MULTILINE)) >= 2, "Backend Dockerfile is not multi-stage.")
require("maven:" in backend_dockerfile and "jre" in backend_dockerfile, "Backend build/runtime base images are incomplete.")
require("USER app" in backend_dockerfile, "Backend must run as non-root app user.")
require(len(re.findall(r"^FROM ", frontend_dockerfile, flags=re.MULTILINE)) >= 2, "Frontend Dockerfile is not multi-stage.")
require("node:" in frontend_dockerfile and "nginx:" in frontend_dockerfile, "Frontend build/runtime base images are incomplete.")
require("npm run build" in frontend_dockerfile, "Frontend production build command is missing.")
require("USER nginx" in frontend_dockerfile, "Frontend must run as non-root nginx user.")
require("location /api/" in nginx and "proxy_pass http://article_backend" in nginx, "Nginx reverse proxy is missing.")
require("try_files $uri $uri/ /index.html" in nginx, "React SPA fallback is missing.")
require("password: ${DB_PASSWORD}" in application, "Database password must not have a source-code default.")
require("secret: ${JWT_SECRET}" in application, "JWT secret must not have a source-code default.")
require(".env" in gitignore, "Root .env must be ignored.")
require("MYSQL_USER=ie_user" in env_example, "The example must use a non-root MySQL application user.")
require("JWT_SECRET=" in env_example, "The example must document the required JWT secret.")

# Detect common accidentally committed secret values.
for relative in ["docker-compose.yml", "backend/src/main/resources/application.yml", ".env.example"]:
    value = text(relative)
    require("root_password" not in value, f"Hard-coded root password found in {relative}.")
    require("ie_password" not in value, f"Hard-coded database password found in {relative}.")
    require("change-this-development-secret" not in value, f"Hard-coded JWT fallback found in {relative}.")

if errors:
    print("Project validation failed:", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    raise SystemExit(1)

print("Project static validation passed.")
