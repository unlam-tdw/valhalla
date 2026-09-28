#!/bin/sh
# One command to run the E2E suite locally.
#
# Brings up PostgreSQL, creates the dedicated E2E database if it is missing,
# installs Chromium, exports DB_*, and runs `mvn verify` -- which starts Jetty,
# runs failsafe, and stops Jetty.
#
# Usage: scripts/e2e.sh [--headed] [--slowmo N]
set -e

headed=false
slowmo=0
while [ $# -gt 0 ]; do
    case "$1" in
        --headed) headed=true ;;
        --slowmo) slowmo="$2"; shift ;;
        *) echo "unknown option: $1" >&2; exit 2 ;;
    esac
    shift
done

cd "$(dirname "$0")/.."
E2E_DB=valhalla_e2e

docker compose up -d postgres

# createdb is not idempotent, so ask the catalog first. -d postgres is required:
# without it psql connects to a database named after the user, which does not exist.
if ! docker compose exec -T postgres psql -U user -d postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname='$E2E_DB'" | grep -q 1; then
    echo "[e2e] creating database $E2E_DB"
    docker compose exec -T postgres createdb -U user "$E2E_DB"
fi

# Read from the pom so a Playwright bump cannot silently drift out of sync.
PW_VERSION=$(sed -n 's:.*<playwright\.version>\(.*\)</playwright\.version>.*:\1:p' pom.xml | head -1)
npx -y "playwright@$PW_VERSION" install chromium

# Jetty and failsafe must agree: Jetty needs the schema, ResetDatabase needs the
# same data. The name must contain "e2e" or ResetDatabase refuses to run.
export DB_NAME="$E2E_DB"
export DB_HOST=localhost
export DB_USER=user
export DB_PASSWORD=user

MVN_ARGS=verify
[ "$headed" = true ] && MVN_ARGS="$MVN_ARGS -De2e.headed=true"
[ "$slowmo" -gt 0 ] 2>/dev/null && MVN_ARGS="$MVN_ARGS -De2e.slowmo=$slowmo"

# shellcheck disable=SC2086
mvn $MVN_ARGS
