#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-8000}"
NO_BROWSER=0
USE_DOCKER=0

usage() {
    cat <<USAGE
Usage:
  scripts/start.sh [options]

Options:
  --backend-port <port>   Backend port. Default: 8080
  --frontend-port <port>  Frontend port for local PHP server. Default: 8000
  --no-browser            Start services without opening a browser
  --docker                Run docker compose up --build instead of local Java/PHP start
  -h, --help              Show this help
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --backend-port)
            BACKEND_PORT="${2:-}"
            shift 2
            ;;
        --frontend-port)
            FRONTEND_PORT="${2:-}"
            shift 2
            ;;
        --no-browser)
            NO_BROWSER=1
            shift
            ;;
        --docker)
            USE_DOCKER=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if [ -z "$BACKEND_PORT" ] || [ -z "$FRONTEND_PORT" ]; then
    echo "Backend and frontend ports must not be empty." >&2
    exit 1
fi

health_ok() {
    curl -fsS "http://localhost:${BACKEND_PORT}/api/health" >/dev/null 2>&1
}

http_ok() {
    curl -fsS "$1" >/dev/null 2>&1
}

port_in_use() {
    if command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
        return $?
    fi

    if command -v ss >/dev/null 2>&1; then
        ss -ltn | awk '{print $4}' | grep -Eq "(^|:)${1}$"
        return $?
    fi

    return 1
}

open_target() {
    local target="$1"

    if [ "$NO_BROWSER" -eq 1 ]; then
        echo "Browserstart uebersprungen. Ziel waere: $target"
        return 0
    fi

    if command -v open >/dev/null 2>&1; then
        open "$target"
    elif command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$target" >/dev/null 2>&1 &
    else
        echo "Kein Browser-Open-Befehl gefunden. Bitte manuell oeffnen: $target"
    fi
}

run_docker_compose() {
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker wurde nicht gefunden. Bitte Docker Desktop bzw. Docker Engine installieren oder ohne --docker lokal mit Java/PHP starten." >&2
        exit 1
    fi

    if ! docker compose version >/dev/null 2>&1; then
        echo "Docker Compose ist nicht verfuegbar. Bitte Docker mit Compose-Unterstuetzung installieren." >&2
        exit 1
    fi

    cd "$PROJECT_ROOT"
    exec docker compose up --build
}

if [ "$USE_DOCKER" -eq 1 ]; then
    "$SCRIPT_DIR/check-encoding.sh"
    run_docker_compose
fi

"$SCRIPT_DIR/check-encoding.sh"

if ! command -v curl >/dev/null 2>&1; then
    echo "curl wurde nicht gefunden. Bitte curl installieren oder Docker Compose verwenden." >&2
    exit 1
fi

BACKEND_SOURCE_ROOT="$PROJECT_ROOT/backend/src/main/java"
BACKEND_CLASSES="$PROJECT_ROOT/backend/target/classes"
SOURCE_LIST="$PROJECT_ROOT/backend/target/sources.list"
FRONTEND_ROOT="$PROJECT_ROOT/frontend"
FRONTEND_HTML="$FRONTEND_ROOT/index.html"
LOG_DIR="$PROJECT_ROOT/backend/target/logs"
BACKEND_OUT_LOG="$LOG_DIR/backend.out.log"
BACKEND_ERR_LOG="$LOG_DIR/backend.err.log"
FRONTEND_OUT_LOG="$LOG_DIR/frontend.out.log"
FRONTEND_ERR_LOG="$LOG_DIR/frontend.err.log"

mkdir -p "$BACKEND_CLASSES" "$LOG_DIR"

echo "SkyTeam Flugschule wird lokal im Demo-Modus gestartet..."

if [ ! -d "$BACKEND_SOURCE_ROOT" ]; then
    echo "Backend-Quellen nicht gefunden: $BACKEND_SOURCE_ROOT" >&2
    exit 1
fi

if [ ! -f "$FRONTEND_HTML" ]; then
    echo "Frontend-Fallback nicht gefunden: $FRONTEND_HTML" >&2
    exit 1
fi

if health_ok; then
    echo "[OK] Backend laeuft bereits: http://localhost:${BACKEND_PORT}/api"
else
    if port_in_use "$BACKEND_PORT"; then
        echo "Port $BACKEND_PORT ist belegt, aber /api/health antwortet nicht." >&2
        exit 1
    fi

    if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
        echo "Java/JDK wurde nicht gefunden. Bitte Java 17 oder neuer installieren. Alternativ: docker compose up --build" >&2
        exit 1
    fi

    find "$BACKEND_SOURCE_ROOT" -name "*.java" > "$SOURCE_LIST"
    if [ ! -s "$SOURCE_LIST" ]; then
        echo "Keine Java-Quellen gefunden unter: $BACKEND_SOURCE_ROOT" >&2
        exit 1
    fi

    javac -encoding UTF-8 -d "$BACKEND_CLASSES" @"$SOURCE_LIST"

    export APP_PROFILE="${APP_PROFILE:-demo}"
    CLASSPATH="$BACKEND_CLASSES"
    if [ -n "${EXTRA_CLASSPATH:-}" ]; then
        CLASSPATH="$CLASSPATH:$EXTRA_CLASSPATH"
    fi

    echo "[START] Backend wird auf Port $BACKEND_PORT gestartet..."
    (
        cd "$PROJECT_ROOT"
        nohup java "-Dserver.port=$BACKEND_PORT" -cp "$CLASSPATH" de.skyteam.flightschool.FlightSchoolApplication > "$BACKEND_OUT_LOG" 2> "$BACKEND_ERR_LOG" &
        echo $! > "$LOG_DIR/backend.pid"
    )

    for _ in $(seq 1 40); do
        if health_ok; then
            break
        fi
        sleep 0.5
    done

    if ! health_ok; then
        echo "Backend konnte nicht gestartet werden." >&2
        echo "Logs: $BACKEND_OUT_LOG, $BACKEND_ERR_LOG" >&2
        exit 1
    fi

    echo "[OK] Backend gestartet: http://localhost:${BACKEND_PORT}/api"
fi

FRONTEND_URL="http://localhost:${FRONTEND_PORT}"
FRONTEND_TARGET=""

if command -v php >/dev/null 2>&1; then
    if http_ok "$FRONTEND_URL"; then
        echo "[OK] Frontend-Server laeuft bereits: $FRONTEND_URL"
        FRONTEND_TARGET="$FRONTEND_URL"
    else
        if port_in_use "$FRONTEND_PORT"; then
            echo "[WARN] Frontend-Port $FRONTEND_PORT ist belegt. Oeffne statischen Fallback."
            FRONTEND_TARGET="$FRONTEND_HTML"
        else
            export API_BASE_URL="http://localhost:${BACKEND_PORT}/api"
            echo "[START] Frontend-Server wird auf Port $FRONTEND_PORT gestartet..."
            (
                cd "$PROJECT_ROOT"
                nohup php -S "localhost:${FRONTEND_PORT}" -t "$FRONTEND_ROOT" > "$FRONTEND_OUT_LOG" 2> "$FRONTEND_ERR_LOG" &
                echo $! > "$LOG_DIR/frontend.pid"
            )

            sleep 1

            if http_ok "$FRONTEND_URL"; then
                echo "[OK] Frontend-Server gestartet: $FRONTEND_URL"
                FRONTEND_TARGET="$FRONTEND_URL"
            else
                echo "[WARN] Frontend-Server antwortet nicht. Oeffne statischen Fallback."
                echo "Logs: $FRONTEND_OUT_LOG, $FRONTEND_ERR_LOG"
                FRONTEND_TARGET="$FRONTEND_HTML"
            fi
        fi
    fi
else
    echo "[INFO] PHP wurde nicht gefunden. Oeffne statischen Frontend-Fallback."
    FRONTEND_TARGET="$FRONTEND_HTML"
fi

echo "[OPEN] $FRONTEND_TARGET"
open_target "$FRONTEND_TARGET"
