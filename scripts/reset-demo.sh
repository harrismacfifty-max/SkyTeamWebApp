#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
    echo "[FEHLER] Docker wurde nicht gefunden. Bitte Docker Desktop bzw. Docker Engine installieren." >&2
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    echo "[FEHLER] Docker Compose ist nicht verfuegbar. Bitte Docker mit Compose-Unterstuetzung installieren." >&2
    exit 1
fi

cd "$PROJECT_ROOT"

echo "[RESET] Laufende SkyTeam-Container und persistente Demo-Daten werden entfernt..."
docker compose down -v

echo "[START] SkyTeam wird mit dem definierten Demo-Ausgangszustand neu gebaut und gestartet..."
docker compose up --build
