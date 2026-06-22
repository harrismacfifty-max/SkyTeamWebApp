# API-Dokumentation

Basis-URL lokal:

```text
http://localhost:8080/api
```

Alle Antworten nutzen dasselbe Format:

```json
{
  "success": true,
  "message": "API erreichbar.",
  "data": {
    "status": "ok",
    "activeProfile": "demo",
    "databaseMode": "demo",
    "databaseReachable": true,
    "timestamp": "2026-06-22T10:30:00+02:00"
  },
  "errors": []
}
```

Fehlerantwort:

```json
{
  "success": false,
  "message": "Validierungsfehler.",
  "data": null,
  "errors": [
    "thema ist erforderlich."
  ]
}
```

Statuscodes:

- `200` erfolgreich gelesen oder verarbeitet
- `201` Ressource angelegt
- `400` Validierungsfehler
- `401` Login fehlt
- `404` Entitaet oder Endpoint nicht gefunden
- `409` fachlicher Konflikt
- `500` unerwarteter Fehler

CORS ist fuer lokale Frontends aktiviert.

## Auth

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

```json
{
  "username": "demo",
  "password": "demo"
}
```

Antwort:

```json
{
  "success": true,
  "message": "Login erfolgreich.",
  "data": {
    "token": "session-...",
    "displayName": "demo"
  },
  "errors": []
}
```

Der alte Pfad `POST /api/login` bleibt als Kompatibilitaetsalias erhalten. Neue Clients sollen `POST /api/auth/login` nutzen.

Alle folgenden Endpunkte ausser `GET /api/health`, `GET /api/version` und `POST /api/auth/login` erwarten:

```text
Authorization: Bearer <token-aus-login>
```

## Allgemein

- `GET /api/health`
- `GET /api/schueler`
- `POST /api/schueler`
- `GET /api/schueler/{id}`
- `DELETE /api/schueler/{id}`
- `GET /api/status/{schuelerId}`
- `GET /api/status/{schuelerId}/gesamt`

Beispiel:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo","password":"demo"}'
$headers = @{ Authorization = "Bearer $($login.data.token)" }
Invoke-RestMethod http://localhost:8080/api/schueler/SC901 -Headers $headers
```

`POST /api/schueler`:

```json
{
  "vorname": "Alex",
  "name": "Muster",
  "startzeit": "2026-06-20",
  "theorieStunden": 0,
  "flugStunden": 0,
  "vertragsStatus": "Unterschrieben",
  "notiz": "Manuell angelegter Demo-Schueler"
}
```

`DELETE /api/schueler/{id}` entfernt im Dev-Modus auch zugehoerige Kurse, Fluege, Pruefungen und den Ausbildungsvertrag.

## Theorie

- `GET /api/theorie/{schuelerId}`
- `POST /api/theorie/buchen`
- `POST /api/theorie/stornieren`
- `GET /api/status/{schuelerId}/theorie`

`TheorieBuchungRequest`:

```json
{
  "schuelerId": "SC901",
  "thema": "Theorie - Navigation",
  "termin": "2026-09-30",
  "dauerMinuten": 90,
  "dozent": "Elias Schulz",
  "notizen": "Vorbereitung auf Theoriepruefung"
}
```

Beispiel:

```powershell
$body = @{
  schuelerId = "SC901"
  thema = "Theorie - Navigation"
  termin = "2026-09-30"
  dauerMinuten = 90
  dozent = "Elias Schulz"
  notizen = "Vorbereitung auf Theoriepruefung"
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8080/api/theorie/buchen -Headers $headers -ContentType "application/json" -Body $body
```

`TheorieStornierungRequest`:

```json
{
  "schuelerId": "SC901",
  "kursId": "KTB951",
  "grund": "Termin verschoben"
}
```

Beispiel:

```powershell
$body = @{
  schuelerId = "SC901"
  kursId = "KTB951"
  grund = "Termin verschoben"
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8080/api/theorie/stornieren -Headers $headers -ContentType "application/json" -Body $body
```

## Praxis

- `GET /api/praxis/{schuelerId}`
- `POST /api/praxis/buchen`
- `POST /api/praxis/stornieren`
- `GET /api/status/{schuelerId}/praxis`

`PraxisBuchungRequest`:

```json
{
  "schuelerId": "SC901",
  "flugzeugId": "FZ002",
  "fluglehrer": "P001",
  "termin": "2026-10-01T10:00",
  "dauerMinuten": 60,
  "ausbildungsinhalt": "Platzrunde",
  "notizen": "Landetraining"
}
```

Storno-Request:

```json
{
  "schuelerId": "SC901",
  "flugId": "FL907",
  "grund": "Termin verschoben"
}
```

Fachliche Konflikte, z.B. wartungsrelevantes Flugzeug:

```json
{
  "success": false,
  "message": "Flugzeug FZ001 ist wegen Status 'in_wartung' nicht buchbar.",
  "data": null,
  "errors": [
    "Flugzeug FZ001 ist wegen Status 'in_wartung' nicht buchbar."
  ]
}
```

## Pruefung

- `GET /api/pruefung/{schuelerId}`
- `POST /api/pruefung/theorie/anmelden`
- `POST /api/pruefung/praxis/anmelden`
- `POST /api/pruefung/ergebnis`

`PruefungAnmeldungRequest`:

```json
{
  "schuelerId": "SC901",
  "pruefungsart": "Theoriepruefung Navigation",
  "wunschtermin": "2026-10-05",
  "pruefer": "P001",
  "bemerkung": "Erstanmeldung"
}
```

`PruefungsErgebnisRequest`:

```json
{
  "pruefungId": "PRB902",
  "schuelerId": "SC902",
  "pruefungsart": "Praxispruefung Start und Landung",
  "datum": "2026-10-07",
  "bestanden": false,
  "ergebnisText": "Wiederholung erforderlich",
  "notizen": "Landung zu spaet stabilisiert"
}
```

Nicht bestandene Pruefungen werden als Wiederholungsbedarf markiert.

## Abschluss

`POST /api/ausbildung/{schuelerId}/abschliessen`

Die Ausbildung kann nur abgeschlossen werden, wenn Theorie- und Praxispruefung bestanden sind. Andernfalls liefert die API `409`.

## Curl-Beispiele

```bash
curl http://localhost:8080/api/health
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo\",\"password\":\"demo\"}"
```

```bash
curl -X POST http://localhost:8080/api/praxis/buchen \
  -H "Authorization: Bearer <token-aus-login>" \
  -H "Content-Type: application/json" \
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-01T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Platzrunde\"}"
```
