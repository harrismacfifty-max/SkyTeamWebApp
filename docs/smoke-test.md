# Smoke-Test MVP

Ziel: Nachweisen, dass die Flight-School-WebApp ohne Oracle im Dev-Modus vorfuehrbar ist und die zentralen Prozessschritte funktionieren.

## 1. Backend starten

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
$env:APP_PROFILE = "dev"
powershell -ExecutionPolicy Bypass -File .\backend\run.ps1
```

Healthcheck:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Erwartung: `success = true`, `data.status = ok`.

## 2. Frontend starten

Empfohlen ueber Launcher:

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
.\start-skyteam.ps1
```

Wenn PHP nicht installiert ist, oeffnet der Launcher automatisch:

```text
frontend/index.html
```

## 3. Login

Demo-Zugang:

```text
Benutzername: demo
Passwort: demo
```

Erwartung: Dashboard wird angezeigt, Schuelerauswahl ist sichtbar.

## 4. Schueler auswaehlen

In der Schuelerauswahl z.B. `SC906 - Oskar Lange` waehlen.

Erwartung:

- Dashboard zeigt Gesamtstatus.
- BPMN-Prozessanzeige zeigt Ausbildungsverwaltung, Theorie und Praxis getrennt.
- `SC906` ist fuer Abschluss bereit, aber noch nicht abgeschlossen.

## 5. Theoriekurs buchen

UI:

1. Tab `Theorie` oeffnen.
2. Thema, Termin, Dauer und Dozent eingeben.
3. `Theoriekurs buchen` klicken.

Erwartung: Kurs erscheint in der Tabelle, Theoriestunden steigen.

curl:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo","password":"demo"}'
$token = $login.data.token
curl.exe -X POST http://localhost:8080/api/theorie/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"thema\":\"Theorie - Smoke Test\",\"termin\":\"2026-10-20\",\"dauerMinuten\":60,\"dozent\":\"Elias Schulz\",\"notizen\":\"Smoke-Test\"}"
```

## 6. Flugstunde buchen

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

curl:

```powershell
curl.exe -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Smoke-Testflug\",\"notizen\":\"Smoke-Test\"}"
```

## 7. Pruefung anmelden

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

## 8. Ergebnis speichern

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

## 9. Ausbildung abschliessen

UI:

1. `SC906 - Oskar Lange` auswaehlen.
2. Tab `Abschluss` oeffnen.
3. `Ausbildung abschliessen` klicken.

Erwartung: Gesamtstatus wird `ABGESCHLOSSEN`.

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
cd C:\Users\pasca\Desktop\SkyTeamWebApp
powershell -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

Erwartung: Alle 10 Tests laufen erfolgreich.
