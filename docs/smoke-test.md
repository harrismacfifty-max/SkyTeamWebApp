# Smoke-Test MVP

Ziel: Nachweisen, dass die WebApp im Demo-Modus ohne Oracle startet, beide Rollen korrekt getrennt sind und die Abschlusslogik nach BPMN funktioniert.

## 1. Start

Empfohlen über Docker Compose:

```bash
cd <projektverzeichnis>
docker compose up --build
```

Prüfen:

```text
Frontend: http://localhost:8081
Backend:  http://localhost:8080/api/health
```

Healthcheck:

```bash
curl http://localhost:8080/api/health
```

Erwartung: JSON mit `status`, `activeProfile`, `databaseMode`, `databaseReachable` und `timestamp`.

Lokaler Start ohne Docker:

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1
```

```bash
cd <projektverzeichnis>
chmod +x scripts/start.sh
./scripts/start.sh
```

## 2. Durchlauf A: Schüler

Login:

```text
username: demo
password: demo
role: SCHUELER
```

UI-Prüfung:

1. `Main Menu / Dashboard` öffnen.
2. Prüfen: Schülerdashboard mit eigenem Schülernamen wird angezeigt.
3. Prüfen: sichtbar sind Ausbildungsstatus, Theorie-Fortschritt, Praxis-Fortschritt, Prüfungsstatus und Abschlussanfrage-Status.
4. Prüfen: Menü enthält `Theorie anmelden`, `Praxis anmelden`, `Prüfung anmelden`, `Abschluss anfragen`.
5. Prüfen: `Schüler anlegen`, `Schülerdaten prüfen`, `Ausbildungsvertrag prüfen` und `Abschlussanfragen prüfen` sind nicht sichtbar.
6. `Theorie anmelden` öffnen und eine eigene Theoriestunde buchen.
7. `Praxis anmelden` öffnen und eine eigene Flugstunde buchen.
8. `Prüfung anmelden` öffnen. Wenn die Mindeststunden fehlen, muss eine klare fachliche Meldung erscheinen.
9. `Abschluss anfragen` öffnen und Anfrage stellen oder vorhandenen Anfragezustand anzeigen.

Beispielwerte für Praxis:

```text
Fluglehrer: P001
Flugzeug: FZ002
Start/Ziel: EDDV
```

Backend-Sperren für Schüler:

```powershell
$studentLogin = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo","password":"demo"}'
$studentToken = $studentLogin.data.token

curl.exe -i -X POST http://localhost:8080/api/verwaltung/schueler `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"vorname\":\"Nicht\",\"name\":\"Erlaubt\"}"

curl.exe -i -X POST http://localhost:8080/api/verwaltung/abschlussanfragen/AA906/bestaetigen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{}"
```

Erwartung: jeweils `403` mit strukturierter `ApiResponse`.

Fremden Schüler buchen:

```powershell
curl.exe -i -X POST http://localhost:8080/api/theorie/buchen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC902\",\"thema\":\"Fremder Schueler\",\"termin\":\"2026-10-22\",\"dauerMinuten\":60,\"dozent\":\"Demo\"}"
```

Erwartung: `403`, weil `demo/demo` nur den eigenen Datensatz nutzen darf.

## 3. Durchlauf B: Schülerverwaltung

Login:

```text
username: demo2
password: demo2
role: SCHUELERVERWALTUNG
```

UI-Prüfung:

1. `Main Menu / Dashboard` öffnen.
2. Prüfen: Verwaltungsdashboard mit Verwaltungskennzahlen wird angezeigt.
3. Prüfen: Menü enthält `Selbstverwaltung` mit `Schüler anlegen`, `Schülerdaten prüfen`, `Ausbildungsvertrag prüfen`, `Abschlussanfragen prüfen`.
4. Prüfen: `Theorie anmelden`, `Praxis anmelden` und `Prüfung anmelden` sind nicht sichtbar.
5. `Schüler anlegen` öffnen und einen Demo-Schüler anlegen.
6. `Schülerdaten prüfen` öffnen, nach `Oskar`, `Keller` oder einem Status suchen und einen Schüler auswählen.
7. `Ausbildungsvertrag prüfen` öffnen und Vertrag prüfen.
8. `Abschlussanfragen prüfen` öffnen.
9. Demo-Anfrage `AA906` für `SC906 Oskar Lange` öffnen.
10. Theorie- und Praxis-Abnahmekriterien prüfen.
11. `Bestätigen` klicken.

Erwartung: Abschlussanfrage wird bestätigt, der Gesamtstatus wird `ABGESCHLOSSEN`.

Backend-Sperren für Schülerverwaltung:

```powershell
$managementLogin = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo2","password":"demo2"}'
$managementToken = $managementLogin.data.token

curl.exe -i -X POST http://localhost:8080/api/theorie/buchen `
  -H "Authorization: Bearer $managementToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"thema\":\"Nicht erlaubt\",\"termin\":\"2026-10-22\",\"dauerMinuten\":60,\"dozent\":\"Demo\"}"

curl.exe -i -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $managementToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-22T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Nicht erlaubt\"}"

curl.exe -i -X POST http://localhost:8080/api/pruefung/theorie/anmelden `
  -H "Authorization: Bearer $managementToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC902\",\"pruefungsart\":\"Theoriepruefung\",\"wunschtermin\":\"2026-10-22\",\"pruefer\":\"P001\"}"
```

Erwartung: jeweils `403`.

## 4. Zentrale API-Smoke-Checks

Login Schüler:

```powershell
$studentLogin = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo","password":"demo"}'
$studentToken = $studentLogin.data.token
```

Theorie buchen:

```powershell
curl.exe -X POST http://localhost:8080/api/theorie/buchen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"thema\":\"Theorie - Smoke Test\",\"termin\":\"2026-10-20\",\"dauerMinuten\":60,\"dozent\":\"Elias Schulz\",\"notizen\":\"Smoke-Test\"}"
```

Praxis buchen:

```powershell
curl.exe -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ002\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T10:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Smoke-Testflug\",\"notizen\":\"Smoke-Test\"}"
```

Prüfung anmelden, wenn Kriterien fehlen:

```powershell
curl.exe -i -X POST http://localhost:8080/api/pruefung/theorie/anmelden `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"pruefungsart\":\"Theoriepruefung\",\"wunschtermin\":\"2026-10-22\",\"pruefer\":\"P001\",\"bemerkung\":\"Smoke-Test\"}"
```

Erwartung: `409`, solange `SC901` die Mindeststunden noch nicht erreicht hat.

Abschluss anfragen:

```powershell
curl.exe -X POST http://localhost:8080/api/abschluss/anfragen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{}"
```

Login Schülerverwaltung:

```powershell
$managementLogin = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"username":"demo2","password":"demo2"}'
$managementToken = $managementLogin.data.token
```

Abschlussanfragen lesen und bestätigen:

```powershell
curl.exe http://localhost:8080/api/verwaltung/abschlussanfragen `
  -H "Authorization: Bearer $managementToken"

curl.exe -X POST http://localhost:8080/api/verwaltung/abschlussanfragen/AA906/bestaetigen `
  -H "Authorization: Bearer $managementToken" `
  -H "Content-Type: application/json" `
  -d "{}"
```

Erwartung: `AA906` kann bestätigt werden, weil `SC906` Theorie und Praxis bestanden hat.

## 5. Fehlerfälle

Ungültige Schüler-ID:

```powershell
curl.exe -i http://localhost:8080/api/status/SC999999/gesamt `
  -H "Authorization: Bearer $managementToken"
```

Erwartung: `404`.

Nicht verfügbares Flugzeug:

```powershell
curl.exe -i -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ004\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T12:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Konflikt-Test\"}"
```

Erwartung: `409`.

Wartungsrelevantes Flugzeug:

```powershell
curl.exe -i -X POST http://localhost:8080/api/praxis/buchen `
  -H "Authorization: Bearer $studentToken" `
  -H "Content-Type: application/json" `
  -d "{\"schuelerId\":\"SC901\",\"flugzeugId\":\"FZ001\",\"fluglehrer\":\"P001\",\"termin\":\"2026-10-21T13:00\",\"dauerMinuten\":60,\"ausbildungsinhalt\":\"Wartungs-Test\"}"
```

Erwartung: `409`.

Nicht vorhandene Abschlussanfrage:

```powershell
curl.exe -i http://localhost:8080/api/verwaltung/abschlussanfragen/AA999 `
  -H "Authorization: Bearer $managementToken"
```

Erwartung: `404`.

## 6. Backend-Tests

```powershell
cd <projektverzeichnis>
powershell -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

Erwartung: Alle Backend-Tests laufen erfolgreich.
