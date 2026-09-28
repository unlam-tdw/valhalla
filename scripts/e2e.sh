#!/bin/sh
# One command to run the E2E suite locally.
#
# Brings up PostgreSQL, creates the dedicated E2E database if it is missing,
# installs Chromium, exports DB_*, and runs `mvn verify` -- which starts Jetty,
# runs failsafe, and stops Jetty. PostgreSQL is stopped again on the way out,
# whether the build passed, failed, or was interrupted.
#
# Usage:
#   scripts/e2e.sh                                    run every E2E
#   scripts/e2e.sh LoginViewE2E                       one class
#   scripts/e2e.sh LoginViewE2E UserViewABME2E         several classes
#   scripts/e2e.sh LoginViewE2E#shouldLogout           one method
#   scripts/e2e.sh --headed --slowmo 300 LoginViewE2E
#   scripts/e2e.sh --list                             what is available
#
# Selectors go straight to failsafe's -Dit.test, so a name that matches nothing
# fails the build instead of reporting a green run over zero tests.
set -e

cd "$(dirname "$0")/.."

list=false
headed=false
slowmo=0
selectors=""

while [ $# -gt 0 ]; do
    case "$1" in
        --list)   list=true ;;
        --headed) headed=true ;;
        --slowmo) slowmo="$2"; shift ;;
        -h|--help) sed -n '3,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        -*)       echo "unknown option: $1" >&2; exit 2 ;;
        *)        selectors="$selectors${selectors:+,}$1" ;;
    esac
    shift
done

if [ "$list" = true ]; then
    find src/test/java -path '*/e2e/*E2E.java' | sort | while read -r f; do
        echo "$(basename "$f" .java)"
        awk '/@Test/{t=1; next}
             t && /void [A-Za-z0-9_]+\(/ {
                 match($0, /void [A-Za-z0-9_]+/)
                 print "  " substr($0, RSTART + 5, RLENGTH - 5); t = 0
             }' "$f"
    done
    exit 0
fi

E2E_DB=valhalla_e2e

docker compose up -d postgres

# From here on the container is up, so the trap owns the teardown: a failing mvn, an
# error under `set -e`, and Ctrl+C all still shut the database down. `stop`, not `down`,
# so the container and the valhalla_e2e database survive for the next run.
# status is captured first, because `docker compose stop` overwrites $? -- without it a
# red build would exit 0 and read as green. The stop is `|| true` for the same reason:
# under `set -e` a failing stop would abort the trap before `exit $status` and report 1.
cleanup() {
    status=$?
    echo '[e2e] stopping postgres'
    docker compose stop postgres || true
    exit $status
}
trap cleanup EXIT INT TERM

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
if [ -n "$selectors" ]; then
    MVN_ARGS="$MVN_ARGS -Dit.test=$selectors"
fi
if [ "$headed" = true ]; then
    MVN_ARGS="$MVN_ARGS -De2e.headed=true"
fi
if [ "$slowmo" -gt 0 ] 2>/dev/null; then
    MVN_ARGS="$MVN_ARGS -De2e.slowMo=$slowmo"
fi

# shellcheck disable=SC2086
mvn $MVN_ARGS
