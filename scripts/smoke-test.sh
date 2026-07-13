#!/usr/bin/env bash
set -e

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api}"
API_BASE_URL="${API_BASE_URL%/}"
RESPONSE_FILE="$(mktemp)"
HTTP_STATUS=""
RESPONSE_BODY=""

cleanup() {
    rm -f "$RESPONSE_FILE"
}
trap cleanup EXIT

if ! command -v curl >/dev/null 2>&1; then
    echo "curl wurde nicht gefunden. Bitte curl installieren." >&2
    exit 1
fi

request() {
    local method="$1"
    local path="$2"
    local token="${3:-}"
    local payload="${4:-}"
    local args=(-sS -o "$RESPONSE_FILE" -w "%{http_code}" -X "$method" "$API_BASE_URL$path")

    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [ "$method" != "GET" ]; then
        if [ -z "$payload" ]; then
            payload='{}'
        fi
        args+=(-H "Content-Type: application/json" -d "$payload")
    fi

    if ! HTTP_STATUS="$(curl "${args[@]}")"; then
        echo "API-Aufruf $method $path fehlgeschlagen. Ist die Demo-Anwendung gestartet?" >&2
        exit 1
    fi
    RESPONSE_BODY="$(tr -d '\r\n' < "$RESPONSE_FILE")"
}

assert_status() {
    local expected="$1"
    local case_name="$2"
    if [ "$HTTP_STATUS" != "$expected" ]; then
        echo "$case_name fehlgeschlagen: erwartet HTTP $expected, erhalten HTTP $HTTP_STATUS. Antwort: $RESPONSE_BODY" >&2
        exit 1
    fi
}

assert_contains() {
    local expected="$1"
    local case_name="$2"
    if [[ "$RESPONSE_BODY" != *"$expected"* ]]; then
        echo "$case_name fehlgeschlagen: '$expected' fehlt in der Antwort. Antwort: $RESPONSE_BODY" >&2
        exit 1
    fi
}

login() {
    local username="$1"
    local password="$2"
    request POST "/auth/login" "" "{\"username\":\"$username\",\"password\":\"$password\"}"
    assert_status 200 "Login $username"
    local token
    token="$(printf '%s' "$RESPONSE_BODY" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
    if [ -z "$token" ]; then
        echo "Login $username lieferte kein Token. Antwort: $RESPONSE_BODY" >&2
        exit 1
    fi
    printf '%s' "$token"
}

echo "SkyTeam Präsentations-Smoke-Test gegen $API_BASE_URL"
echo "Hinweis: Der Test legt Buchungen an und bestätigt AA906. Vor jedem Lauf Demo-Daten zurücksetzen."

request GET "/health"
assert_status 200 "Health-Endpoint"
assert_contains '"status":"ok"' "Health-Endpoint"
assert_contains '"activeProfile":"demo"' "Health-Endpoint"
assert_contains '"databaseMode":"demo"' "Health-Endpoint"
echo "[OK] Vorbereitung: API erreichbar."

STUDENT_TOKEN="$(login sc901 demo901)"
MANAGEMENT_TOKEN="$(login demo2 demo2)"

request GET "/verwaltung/abschlussanfragen/AA906" "$MANAGEMENT_TOKEN"
assert_status 200 "Ausgangszustand AA906"
if [[ "$RESPONSE_BODY" != *'"status":"ANGEFRAGT"'* ]]; then
    echo "AA906 ist nicht mehr offen. Bitte zuerst scripts/reset-demo.sh ausführen. Antwort: $RESPONSE_BODY" >&2
    exit 1
fi
OPEN_REQUEST="$RESPONSE_BODY"

request POST "/theorie/buchen" "$STUDENT_TOKEN" '{"thema":"Praesentations-Smoke Theorie","termin":"2026-11-20","dauerMinuten":60,"dozent":"Elias Schulz"}'
assert_status 201 "1. erfolgreiche Theoriebuchung"
request GET "/theorie/me" "$STUDENT_TOKEN"
assert_status 200 "1. Abruf der Theoriebuchung"
assert_contains "Praesentations-Smoke Theorie" "1. Abruf der Theoriebuchung"
echo "[OK] 1. Theoriebuchung für SC901 erstellt und wieder abgerufen (HTTP 201/200)."

request POST "/praxis/buchen" "$STUDENT_TOKEN" '{"flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-11-21T08:00","dauerMinuten":60,"ausbildungsinhalt":"Praesentations-Smoke Praxis","startFlughafen":"EDDV","zielFlughafen":"EDDV"}'
assert_status 201 "2. erfolgreiche Praxisbuchung"
assert_contains '"flugzeugId":"FZ002"' "2. erfolgreiche Praxisbuchung"
assert_contains '"startFlughafen":"EDDV"' "2. erfolgreiche Praxisbuchung"
assert_contains '"zielFlughafen":"EDDV"' "2. erfolgreiche Praxisbuchung"
echo "[OK] 2. Praxisbuchung mit P001, FZ002 und EDDV/EDDV erstellt (HTTP 201)."

request POST "/praxis/buchen" "$STUDENT_TOKEN" '{"flugzeugId":"FZ001","fluglehrer":"P001","termin":"2026-11-21T10:00","dauerMinuten":60,"ausbildungsinhalt":"Negativtest Wartung","startFlughafen":"EDDV","zielFlughafen":"EDDV"}'
assert_status 409 "3. blockiertes Flugzeug FZ001"
assert_contains "FZ001" "3. blockiertes Flugzeug FZ001"
if [[ "$RESPONSE_BODY" != *"wartung"* && "$RESPONSE_BODY" != *"nicht buchbar"* && "$RESPONSE_BODY" != *"nicht verfuegbar"* ]]; then
    echo "3. FZ001-Fehlermeldung erklärt Wartungs- oder Verfügbarkeitsstatus nicht: $RESPONSE_BODY" >&2
    exit 1
fi
echo "[OK] 3. FZ001 fachlich blockiert; verständliche Meldung vorhanden (HTTP 409)."

request POST "/pruefung/theorie/anmelden" "$STUDENT_TOKEN" '{"pruefungsart":"Theoriepruefung","wunschtermin":"2026-11-25","pruefer":"P001"}'
assert_status 409 "4. Prüfungsanmeldung ohne Mindeststunden"
assert_contains "mindestens 10.0 Theoriestunden erforderlich" "4. Prüfungsanmeldung ohne Mindeststunden"
echo "[OK] 4. Prüfungsanmeldung von SC901 wegen fehlender Mindeststunden blockiert (HTTP 409)."

request POST "/theorie/buchen" "$MANAGEMENT_TOKEN" '{"schuelerId":"SC901","thema":"Nicht erlaubt","termin":"2026-11-20","dauerMinuten":60,"dozent":"Elias Schulz"}'
assert_status 403 "5. unerlaubter Verwaltungszugriff"
assert_contains "Keine Berechtigung für diese Funktion." "5. unerlaubter Verwaltungszugriff"
echo "[OK] 5. Schülerverwaltung bei Schüleraktion blockiert (HTTP 403)."

RESPONSE_BODY="$OPEN_REQUEST"
assert_contains '"schuelerId":"SC906"' "6. Abschlussanfrage AA906"
assert_contains '"theorieKriterienErfuellt":true' "6. Abschlussanfrage AA906"
assert_contains '"praxisKriterienErfuellt":true' "6. Abschlussanfrage AA906"
request POST "/verwaltung/abschlussanfragen/AA906/bestaetigen" "$MANAGEMENT_TOKEN" '{}'
assert_status 200 "6. erfolgreiche Abschlussbestätigung"
assert_contains '"status":"ABGESCHLOSSEN"' "6. erfolgreiche Abschlussbestätigung"
request GET "/status/SC906/gesamt" "$MANAGEMENT_TOKEN"
assert_status 200 "6. Gesamtstatus SC906"
assert_contains '"status":"ABGESCHLOSSEN"' "6. Gesamtstatus SC906"
echo "[OK] 6. AA906 bestätigt; SC906 ist ABGESCHLOSSEN (HTTP 200)."

echo "Alle sechs Präsentations-Smoke-Fälle waren erfolgreich."
