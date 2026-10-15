#!/usr/bin/env python3
"""Generate a local Docker Compose .env file without overwriting an existing one."""

from __future__ import annotations

import secrets
import string
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".env"


def random_password(length: int = 32) -> str:
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))


def main() -> None:
    if OUTPUT.exists():
        raise SystemExit(f"Refusing to overwrite existing file: {OUTPUT}")

    mysql_password = random_password()
    root_password = random_password()
    jwt_secret = secrets.token_urlsafe(64)

    content = f"""APP_PORT=8080

MYSQL_DATABASE=ie_articles
MYSQL_USER=ie_user
MYSQL_PASSWORD={mysql_password}
MYSQL_ROOT_PASSWORD={root_password}

JWT_SECRET={jwt_secret}
JWT_ISSUER=article-sharing-api
JWT_LIFETIME_MINUTES=120
PASSWORD_HASH_ITERATIONS=210000
SEED_DEMO_DATA=false
"""
    OUTPUT.write_text(content, encoding="utf-8")
    try:
        OUTPUT.chmod(0o600)
    except OSError:
        pass
    print(f"Created {OUTPUT}. Keep this file private and do not commit it.")


if __name__ == "__main__":
    main()
