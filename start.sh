#!/usr/bin/env bash
# One-command startup for macOS / Linux (equivalent of start.ps1).
set -euo pipefail

cd "$(dirname "$0")"

random_secret() {
    # 32 random bytes as hex; works without openssl.
    head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
}

touch .env
chmod 600 .env

# Make sure appended values start on a new line.
if [ -s .env ] && [ -n "$(tail -c1 .env)" ]; then
    echo >> .env
fi

# Generate every missing secret; existing values are kept.
for key in DB_PASSWORD API_ADMIN_PASSWORD API_VIEWER_PASSWORD; do
    if ! grep -q "^${key}=" .env; then
        echo "${key}=$(random_secret)" >> .env
        echo "Generated ${key}."
    fi
done

# Values exported in the shell would override the ones in .env.
unset DB_PASSWORD API_ADMIN_PASSWORD API_VIEWER_PASSWORD

docker compose up --build -d
docker compose ps

value() { grep "^$1=" .env | cut -d= -f2-; }

echo
echo "API credentials (HTTP Basic, also used in Swagger UI 'Authorize'):"
echo "  ADMIN  (full access): $(value API_ADMIN_USERNAME | grep . || echo admin) / $(value API_ADMIN_PASSWORD)"
echo "  VIEWER (read-only)  : $(value API_VIEWER_USERNAME | grep . || echo viewer) / $(value API_VIEWER_PASSWORD)"
echo
echo "Swagger UI: http://localhost:8080/swagger-ui/index.html"
