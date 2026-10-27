#!/bin/sh
# Dev entrypoint: runs mvn jetty:run and watches for .java changes to recompile
# and restart the container. Template hot-reload works via Thymeleaf cache=off.
set -e

# target/ is bind-mounted from the host and Maven's incremental compile never
# deletes classes whose source disappeared. After a branch switch, orphan
# classes can reference methods that no longer exist and crash Spring startup
# with NoSuchMethodError (observed: PlanScheduleBackfill -> 503). Delete any
# main class without a matching source, and drop test classes entirely since
# this dev entrypoint never compiles tests.
if [ -d target/classes ]; then
    find target/classes -name '*.class' | while read -r cls; do
        base=${cls#target/classes/}
        base=${base%.*}
        base=${base%%\$*}   # strip inner-class suffix
        [ -f "src/main/java/${base}.java" ] || { echo "[dev-watch] removing orphan ${cls}"; rm -f "$cls"; }
    done
fi
rm -rf target/test-classes

# Start jetty in background. seed.demoPlans: [PPV] feed con usuarios/planes fantasma de demo.
# maven.test.skip, not skipTests: the latter only skips execution, while the
# dev profile never needs tests compiled at all (~16s saved per start).
mvn jetty:run -Dmaven.test.skip=true -Pdev -Dseed.demoPlans=true &
JETTY_PID=$!

# Wait for target/classes to exist (first compilation)
while [ ! -d target/classes ]; do sleep 1; done

# Capture initial timestamps of all .java files
INITIAL_STAMPS=$(find src/main -name '*.java' -printf '%T@\n' 2>/dev/null | sort -rn | head -1)
LAST_STAMPS="$INITIAL_STAMPS"

echo "[dev-watch] Watching src/**/*.java for changes..."

while kill -0 "$JETTY_PID" 2>/dev/null; do
    sleep 2

    CURRENT_STAMPS=$(find src/main -name '*.java' -printf '%T@\n' 2>/dev/null | sort -rn | head -1)

    if [ "$CURRENT_STAMPS" != "$LAST_STAMPS" ]; then
        echo "[dev-watch] Java change detected, recompiling..."
        mvn compile -q -Pdev -DskipTests 2>/dev/null || true
        LAST_STAMPS="$CURRENT_STAMPS"
        echo "[dev-watch] Recompiled. Restarting container..."
        kill "$JETTY_PID" 2>/dev/null
        wait "$JETTY_PID" 2>/dev/null
        exit 1  # Non-zero exit → Docker restarts container
    fi
done

wait "$JETTY_PID"