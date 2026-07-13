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
- `403` falsche Rolle
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
  "username": "sc901",
  "password": "demo901"
}
```

Demo-Zugaenge:

| Benutzername | Passwort | Rolle | Schüler-ID |
|---|---|---|---|
| `sc901` | `demo901` | `SCHUELER` | `SC901` |
| `sc902` | `demo902` | `SCHUELER` | `SC902` |
| `sc903` | `demo903` | `SCHUELER` | `SC903` |
| `sc904` | `demo904` | `SCHUELER` | `SC904` |
| `sc905` | `demo905` | `SCHUELER` | `SC905` |
| `sc906` | `demo906` | `SCHUELER` | `SC906` |
| `sc907` | `demo907` | `SCHUELER` | `SC907` |
| `demo2` | `demo2` | `SCHUELERVERWALTUNG` | `null` |

`demo/demo` bleibt nur aus Kompatibilitätsgründen als Legacy-Alias für `SC901` erhalten und wird für Präsentation, Dokumentation und neue Tests nicht mehr verwendet. Die Demo-Zugänge sind im Profil `oracle` nicht aktiviert.

Antwort:

```json
{
  "success": true,
  "message": "Login erfolgreich.",
  "data": {
    "token": "session-...",
    "username": "sc901",
    "displayName": "Jonas Keller",
    "role": "SCHUELER",
    "schuelerId": "SC901"
  },
  "errors": []
}
```

`GET /api/auth/me` liefert:

```json
{
  "authenticated": true,
  "username": "sc901",
  "displayName": "Jonas Keller",
  "role": "SCHUELER",
  "schuelerId": "SC901"
}
```

Für `demo2` liefert `GET /api/auth/me` die Rolle `SCHUELERVERWALTUNG` und `"schuelerId": null`.

Bei falscher Rolle:

```json
{
  "success": false,
  "message": "Keine Berechtigung für diese Funktion.",
  "data": null,
  "errors": [
    "Keine Berechtigung für diese Funktion."
  ]
}
```

Der alte Pfad `POST /api/login` bleibt als Kompatibilitaetsalias erhalten. Neue Clients sollen `POST /api/auth/login` nutzen.

Alle folgenden Endpunkte ausser `GET /api/health`, `GET /api/version` und `POST /api/auth/login` erwarten:

```text
Authorization: Bearer <token-aus-login>
```

## Rollen

`SCHUELER` darf eigene Ausbildungsdaten lesen, Theorie/Praxis buchen, Prüfungen anmelden und eine Abschlussanfrage stellen.

`SCHUELERVERWALTUNG` darf Schülerdaten prüfen/anlegen/löschen, Vertrags- und Statusdaten prüfen, Prüfungsergebnisse speichern, Abschlussanfragen lesen sowie Abschlüsse bestätigen oder ablehnen.

Schreibende Aktionen werden serverseitig geprüft; verbotene Aktionen liefern `403`. Schüleraktionen sind immer auf den jeweils zugeordneten Datensatz beschränkt. Beispielsweise ist `sc901/demo901` fest `SC901` zugeordnet und darf keine Theorie-, Praxis- oder Prüfungsanmeldung für andere Schüler auslösen.

Bei Schülerkonten ist die `schuelerId` aus der Session verbindlich. Die `/me`-Endpunkte und Schüleraktionen benötigen deshalb keine `schuelerId` oder `studentId` vom Client. Abweichende IDs in Pfad, Query oder JSON werden mit `403` abgewiesen. Die Verwaltung verwendet weiterhin die Endpunkte unter `/api/verwaltung/**` mit expliziten Schüler-IDs.

Theorie-, Praxis- und Prüfungsanmeldungen sind ausschließlich Schülerfunktionen:

- `POST /api/theorie/buchen`
- `POST /api/praxis/buchen`
- `POST /api/pruefung/theorie/anmelden`
- `POST /api/pruefung/praxis/anmelden`

`SCHUELERVERWALTUNG` erhält für diese Endpunkte `403`. Prüfungsergebnisse bleiben eine Verwaltungsfunktion; `SCHUELER` erhält für `POST /api/pruefung/ergebnis` ebenfalls `403`.

## Verwaltung

Alle Endpunkte unter `/api/verwaltung/**` erfordern die Rolle `SCHUELERVERWALTUNG`. Schülerkonten wie `sc901/demo901` erhalten für diese Endpunkte `403`.

- `GET /api/verwaltung/schueler`
- `GET /api/verwaltung/schueler/{id}`
- `POST /api/verwaltung/schueler`
- `GET /api/verwaltung/schueler/{id}/vertrag`
- `POST /api/verwaltung/schueler/{id}/vertrag/pruefen`

`POST /api/verwaltung/schueler` legt einen Schueler an und erzeugt bei fehlender Vertrags-ID automatisch einen Demo-Ausbildungsvertrag:

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

`POST /api/verwaltung/schueler/{id}/vertrag/pruefen` dokumentiert die Vertragspruefung im vorhandenen Vertragsfeld `NOTIZ`:

```json
{
  "pruefer": "Demo Schuelerverwaltung",
  "bemerkung": "Vertrag fachlich geprueft"
}
```

Antwort:

```json
{
  "id": "AV901",
  "schuleId": "S001",
  "status": "Unterschrieben",
  "notiz": "Vertrag geprueft durch Demo Schuelerverwaltung: Vertrag fachlich geprueft",
  "geprueft": true
}
```

## Allgemein

- `GET /api/health`
- `GET /api/schueler/me`
- `GET /api/status/me`
- `GET /api/schueler`
- `GET /api/schueler/{id}`
- `DELETE /api/schueler/{id}`
- `GET /api/status/{schuelerId}`
- `GET /api/status/{schuelerId}/gesamt`

Beispiel:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"sc901","password":"demo901"}'
$headers = @{ Authorization = "Bearer $($login.data.token)" }
Invoke-RestMethod http://localhost:8080/api/schueler/me -Headers $headers
```

Schueleranlage fuer die Verwaltung erfolgt fachlich ueber `POST /api/verwaltung/schueler`. Der alte Pfad `POST /api/schueler` bleibt als Kompatibilitaetsroute erhalten.

Request:

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

- `GET /api/theorie/me`
- `GET /api/theorie/{schuelerId}` (Kompatibilitätsroute mit Zugriffskontrolle)
- `POST /api/theorie/buchen`
- `POST /api/theorie/stornieren`
- `GET /api/status/{schuelerId}/theorie`

`TheorieBuchungRequest`:

```json
{
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
  "kursId": "KTB951",
  "grund": "Termin verschoben"
}
```

Beispiel:

```powershell
$body = @{
  kursId = "KTB951"
  grund = "Termin verschoben"
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8080/api/theorie/stornieren -Headers $headers -ContentType "application/json" -Body $body
```

## Praxis

- `GET /api/praxis/me`
- `GET /api/praxis/{schuelerId}` (Kompatibilitätsroute mit Zugriffskontrolle)
- `POST /api/praxis/buchen`
- `POST /api/praxis/stornieren`
- `GET /api/status/{schuelerId}/praxis`

`PraxisBuchungRequest`:

```json
{
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

- `GET /api/pruefung/me`
- `GET /api/pruefung/{schuelerId}` (Kompatibilitätsroute mit Zugriffskontrolle)
- `POST /api/pruefung/theorie/anmelden`
- `POST /api/pruefung/praxis/anmelden`
- `POST /api/pruefung/ergebnis`

`PruefungAnmeldungRequest`:

```json
{
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

- `GET /api/abschluss/meine-anfrage`
- `POST /api/abschluss/anfragen`
- `GET /api/verwaltung/abschlussanfragen`
- `GET /api/verwaltung/abschlussanfragen/alle`
- `GET /api/verwaltung/abschlussanfragen/{id}`
- `POST /api/verwaltung/abschlussanfragen/{id}/bestaetigen`
- `POST /api/verwaltung/abschlussanfragen/{id}/ablehnen`

`SCHUELER` stellt die Abschlussanfrage fuer den eigenen Datensatz und sieht den eigenen Anfragezustand. `SCHUELERVERWALTUNG` liest nur vorhandene offene Abschlussanfragen und bestaetigt oder lehnt sie ab. Die Bestaetigung ist nur moeglich, wenn eine Anfrage vorhanden ist und Theorie- sowie Praxis-Abnahmekriterien erfuellt sind. Andernfalls liefert die API `409`. Bei falscher Rolle liefert die API `403`.

Beispiel Abschlussanfrage:

```json
{
  "success": true,
  "message": "Abschlussanfrage gestellt.",
  "data": {
    "id": "AA907",
    "schuelerId": "SC906",
    "status": "ANGEFRAGT",
    "begruendung": "",
    "angefragtAm": "2026-06-22T10:15:00",
    "geprueftAm": null,
    "theorieKriterienErfuellt": true,
    "praxisKriterienErfuellt": true,
    "bestaetigungMoeglich": true
  },
  "errors": []
}
```

Beispiel Ablehnung:

```json
{
  "begruendung": "Abnahmekriterien nicht erfuellt."
}
```

## Curl-Beispiele

```bash
curl http://localhost:8080/api/health
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"sc901\",\"password\":\"demo901\"}"
```

```bash
curl -X POST http://localhost:8080/api/praxis/buchen \
  -H "Authorization: Bearer <token-aus-login>" \
  -H "Content-Type: application/json" \
  -d "{\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-01T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Platzrunde\"}"
```
