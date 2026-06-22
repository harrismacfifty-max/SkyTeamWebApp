# Smoke-Test MVP

Ziel: Nachweisen, dass die Flight-School-WebApp ohne Oracle im Demo-Modus vorfuehrbar ist und die zentralen Prozessschritte funktionieren.

## 1. Anwendung starten

Empfohlen ueber Docker Compose, auf Windows, macOS und Linux gleich:

```bash
cd <projektverzeichnis>
docker compose up --build
```

Frontend:

```text
http://localhost:8081
```

Backend-Healthcheck:

```text
http://localhost:8080/api/health
```

Healthcheck per Terminal:

```bash
curl http://localhost:8080/api/health
```

Alternativ Backend lokal ueber Windows PowerShell:

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1 -NoBrowser
```

Alternativ lokal ueber macOS/Linux Terminal:

```bash
cd <projektverzeichnis>
chmod +x scripts/start.sh
./scripts/start.sh --no-browser
```

Healthcheck per PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Lokales Frontend ohne Docker:

```bash
cd <projektverzeichnis>
php -S localhost:8000 -t ./frontend
```

Wenn PHP nicht installiert ist, kann direkt geoeffnet werden:

```text
./frontend/index.html
```

## 2. Login

Demo-Zugang:

```text
Benutzername: demo
Passwort: demo
```

Erwartung: Dashboard wird angezeigt, Schuelerauswahl ist sichtbar.

Zusaetzliche UI-Erwartung:

- SkyTeam-Logo erscheint oben links.
- Dashboard ist der aktive Einstiegstab.
- Statusuebersicht fuer Theorie, Praxis, Pruefung und Ausbildung ist sichtbar.

## 3. Schueler suchen und auswaehlen

UI:

1. Tab `Schueler` oeffnen.
2. Nach `Oskar`, `Keller` oder einem Ausbildungsstatus filtern.
3. In der Tabelle z.B. `SC906 - Oskar Lange` waehlen.
4. Optional Rechtsklick oder Drei-Punkte-Button testen.

Erwartung:

- Dashboard zeigt Gesamtstatus.
- Aktiver Tab ist klar markiert.
- BPMN-Prozessanzeige zeigt Ausbildungsverwaltung, Theorie und Praxis getrennt.
- `SC906` ist fuer Abschluss bereit, aber noch nicht abgeschlossen.

## 4. Theoriekurs buchen

UI:

1. Tab `Theorie` oeffnen.
2. Thema, Termin, Dauer und Dozent eingeben.
3. `Theoriekurs buchen` klicken.

Erwartung: Kurs erscheint in der Tabelle, Theoriestunden steigen.
Fehlerfall-Erwartung: API-Fehler erscheinen als lesbare Meldung im Frontend, nicht als Stacktrace.
Storno-Erwartung: Der Kurs kann im Theorie-Tab ueber `Stornieren` entfernt werden; die Theoriestunden sinken wieder.

curl:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo","password":"demo"}'
$token = $login.data.token
curl.exe -X POST http://localhost:8080/api/theorie/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"thema\":\"Theorie - Smoke Test\",\"termin\":\"2026-10-20\",\"dauerMinuten\":60,\"dozent\":\"Elias Schulz\",\"notizen\":\"Smoke-Test\"}"
```

Optionales Storno per PowerShell:

```powershell
$course = Invoke-RestMethod -Method Post http://localhost:8080/api/theorie/buchen `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"schuelerId":"SC901","thema":"Theorie - Smoke Storno","termin":"2026-10-20","dauerMinuten":60,"dozent":"Elias Schulz","notizen":"Smoke-Test"}'

$courseId = $course.data.id
Invoke-RestMethod -Method Post http://localhost:8080/api/theorie/stornieren `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body "{`"schuelerId`":`"SC901`",`"kursId`":`"$courseId`",`"grund`":`"Smoke-Test`"}"
```

Planung:

1. Tab `Planung` oeffnen.
2. Offene Theorieanfrage in `Geplante Theoriestunden` ziehen.
3. Die geplante Theoriestunde wieder in `Offene Theorieanfragen` ziehen.
4. Storno bestaetigen.

## 5. Flugstunde buchen

UI:

1. Tab `Praxis` oeffnen.
2. Datum, Startzeit, Endzeit, Fluglehrer, Flugzeug und Route eingeben.
3. `Flugstunde buchen` klicken.

Empfohlene gueltige Werte:

```text
Fluglehrer: P001
Flugzeug: FZ002
Start/Ziel: EDDV
```

Erwartung: Flugstunde erscheint in der Tabelle, Flugstunden steigen.
Fehlerfall-Erwartung: nicht verfuegbare Piloten oder Flugzeuge liefern einen sichtbaren fachlichen Fehler.

curl:

```powershell
curl.exe -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Smoke-Testflug\",\"notizen\":\"Smoke-Test\"}"
```

## 6. Pruefung anmelden

UI:

1. Tab `Theorie` oder `Praxis` oeffnen.
2. Bei freigeschaltetem Schueler Pruefung anmelden.

Geeignete Demo-Faelle:

```text
SC902  Theoriepruefung kann angemeldet werden
SC903  Praxispruefung kann angemeldet werden
```

curl Theorie:

```powershell
curl.exe -X POST http://localhost:8080/api/pruefung/theorie/anmelden `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC902\",\"pruefungsart\":\"Theoriepruefung\",\"wunschtermin\":\"2026-10-22\",\"pruefer\":\"P001\",\"bemerkung\":\"Smoke-Test\"}"
```

curl Praxis:

```powershell
curl.exe -X POST http://localhost:8080/api/pruefung/praxis/anmelden `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC903\",\"pruefungsart\":\"Praxispruefung\",\"wunschtermin\":\"2026-10-23\",\"pruefer\":\"P002\",\"bemerkung\":\"Smoke-Test\"}"
```

## 7. Ergebnis speichern

UI:

1. Tab `Pruefungen` oeffnen.
2. Pruefung auswaehlen.
3. Art und Ergebnis waehlen.
4. `Ergebnis speichern` klicken.

curl:

```powershell
curl.exe -X POST http://localhost:8080/api/pruefung/ergebnis `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"pruefungId\":\"PRB904\",\"schuelerId\":\"SC906\",\"pruefungsart\":\"Theoriepruefung\",\"datum\":\"2026-10-24\",\"bestanden\":true,\"ergebnisText\":\"Smoke-Test bestanden\",\"notizen\":\"Smoke-Test\"}"
```

## 8. Ausbildung abschliessen

UI:

1. `SC906 - Oskar Lange` auswaehlen.
2. Tab `Abschluss` oeffnen.
3. `Ausbildung abschliessen` klicken.

Erwartung: Gesamtstatus wird `ABGESCHLOSSEN`.
Abschluss pruefen: Dashboard zeigt danach `Ausbildung: Abgeschlossen`, Theorie und Praxis bleiben `Bestanden`.

curl:

```powershell
curl.exe -X POST http://localhost:8080/api/ausbildung/SC906/abschliessen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{}"
```

## Fehlerfaelle

Ungueltige Schueler-ID:

```powershell
curl.exe -i http://localhost:8080/api/status/SC999999/gesamt `
  -H "Authorization: Bearer $token"
```

Erwartung: `404`, strukturierte JSON-Fehlerantwort.

Nicht verfuegbares Flugzeug:

```powershell
curl.exe -i -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ004\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T12:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Konflikt-Test\"}"
```

Erwartung: `409`, fachlicher Konflikt.

Wartungsrelevantes Flugzeug:

```powershell
curl.exe -i -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ001\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T13:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Wartungs-Test\"}"
```

Erwartung: `409`, fachlicher Konflikt.

Abschluss blockiert:

```powershell
curl.exe -i -X POST http://localhost:8080/api/ausbildung/SC905/abschliessen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{}"
```

Erwartung: `409`, da die Praxispruefung nicht bestanden ist.

## Backend-Tests

```powershell
cd <projektverzeichnis>
powershell -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

Erwartung: Alle 10 Tests laufen erfolgreich.
