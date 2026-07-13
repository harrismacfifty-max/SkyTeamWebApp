# Präsentations-Smoke-Test

Ziel: Die sechs zentralen Erfolgs- und Fehlerfälle im Demo-Modus reproduzierbar prüfen. Der Ablauf verändert Demo-Daten. Deshalb vor jedem vollständigen Durchlauf zurücksetzen.

## Vorbereitung

1. Im Projektverzeichnis alle persistenten Demo-Daten entfernen:

   ```bash
   docker compose down -v
   ```

2. Anwendung vollständig im definierten Ausgangszustand starten:

   ```bash
   docker compose up --build
   ```

3. In einem zweiten Terminal den Health-Endpoint prüfen:

   ```bash
   curl http://localhost:8080/api/health
   ```

   Erwartet: HTTP `200`, `status: ok`, `activeProfile: demo`, `databaseMode: demo` und `databaseReachable: true`. Der Header stellt diesen Datenbankmodus als `InMemory` dar.

4. Optional den automatisierten API-Smoke-Test ausführen. Er prüft beide Logins und alle sechs API-Fälle, legt dabei Buchungen an und bestätigt `AA906`:

   Windows PowerShell:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
   ```

   macOS/Linux:

   ```bash
   bash scripts/smoke-test.sh
   ```

   Für einen erneuten Lauf zuerst wieder `docker compose down -v` und `docker compose up --build` ausführen.

## Konten- und Isolationsprüfung

Vor den sechs fachlichen Fällen alle aktuellen Demo-Konten einmal prüfen. Auf der Login-Seite jeweils `Zugang verwenden` wählen, kontrollieren, dass Benutzername und Passwort korrekt übernommen werden, und anschließend bewusst auf `Anmelden` klicken.

| Account | Passwort | Erwartete Zuordnung | Erwarteter Ausgangszustand | Login / ID / Zustand |
|---|---|---|---|---|
| `sc901` | `demo901` | `SC901` | laufende Ausbildung, Mindeststunden fehlen | ☐ / ☐ / ☐ |
| `sc902` | `demo902` | `SC902` | Theorieprüfung freigeschaltet | ☐ / ☐ / ☐ |
| `sc903` | `demo903` | `SC903` | Praxisprüfung freigeschaltet | ☐ / ☐ / ☐ |
| `sc904` | `demo904` | `SC904` | Theorieprüfung nicht bestanden, Wiederholungsbedarf | ☐ / ☐ / ☐ |
| `sc905` | `demo905` | `SC905` | Praxisprüfung nicht bestanden, Wiederholungsbedarf | ☐ / ☐ / ☐ |
| `sc906` | `demo906` | `SC906` | Theorie und Praxis bestanden, Abschlussanfrage `ANGEFRAGT` | ☐ / ☐ / ☐ |
| `sc907` | `demo907` | `SC907` | Ausbildung `ABGESCHLOSSEN` | ☐ / ☐ / ☐ |
| `demo2` | `demo2` | keine Schüler-ID | Rolle `SCHUELERVERWALTUNG` | ☐ / ☐ / ☐ |

Für jeden Schülerlogin prüfen:

1. Name, Schüler-ID, Rolle und Ausbildungsstatus gehören zum erwarteten Konto.
2. Es gibt keine allgemeine Schülerauswahl und keine Daten eines zuvor angemeldeten Kontos.
3. Logout ausführen und erst danach das nächste Konto anmelden.
4. Nach mindestens einem Wechsel, beispielsweise `sc901` → Logout → `sc906`, `/api/auth/me` prüfen: Die neue Session darf nur `SC906` enthalten; das alte Token muss HTTP `401` liefern.

`demo2` ist kein Schülerkonto. Die Verwaltung muss alle Schüler sehen können, darf aber keine Schülerbuchung ausführen.

## Schülerrolle

Login:

```text
Benutzername: sc901
Passwort: demo901
Rolle: SCHUELER
Demo-Schüler: SC901 (Jonas Keller)
```

Tests in dieser Reihenfolge:

1. `Theorie anmelden` öffnen und eine Theoriestunde buchen:
   - Thema: `Präsentations-Smoke Theorie`
   - Termin: `20.11.2026`
   - Dauer: `60 Minuten`
   - Dozent: `Elias Schulz`
   - Erwartet: Buchung erfolgreich; Eintrag ist anschließend in der Theorieübersicht sichtbar.
2. `Praxis anmelden` öffnen und die sichere Vorauswahl kontrollieren:
   - Pilot: `P001`
   - Flugzeug: `FZ002`
   - Startflughafen: `EDDV`
   - Zielflughafen: `EDDV`
   - Termin: `21.11.2026, 08:00 Uhr`
   - Dauer: `60 Minuten`
   - Erwartet: Buchung erfolgreich; Eintrag ist anschließend in der Praxisübersicht sichtbar.
3. Im Praxisformular gezielt `FZ001` wählen und eine Buchung versuchen:
   - Pilot bleibt `P001`, Start und Ziel bleiben `EDDV`.
   - Erwartet: keine Buchung, sondern HTTP `409` mit Hinweis auf `in_wartung` beziehungsweise „nicht buchbar“.
   - Danach wieder `FZ002` auswählen, damit das Formular im sicheren Demo-Zustand bleibt.
4. `Prüfung anmelden` öffnen und die Theorieprüfung für `SC901` versuchen:
   - Nach der vorherigen 60-Minuten-Buchung hat `SC901` weiterhin weniger als die erforderlichen 10 Theoriestunden.
   - Erwartet: keine Anmeldung, sondern HTTP `409`; die Meldung nennt Mindeststunden und aktuellen Stand.
5. Die angezeigten Erfolgs- und Fehlermeldungen in der Ergebnistabelle unten notieren.

## Schülerverwaltung

Login:

```text
Benutzername: demo2
Passwort: demo2
Rolle: SCHUELERVERWALTUNG
```

Tests:

1. Prüfen, dass `Theorie anmelden` und `Praxis anmelden` im Menü nicht sichtbar sind.
2. Den direkten API-Zugriff serverseitig prüfen. Der automatisierte Smoke-Test sendet dafür mit dem Verwaltungstoken `POST /api/theorie/buchen`.
   - Erwartet: HTTP `403` und `Keine Berechtigung für diese Funktion.`
3. `Abschlussanfragen prüfen` öffnen und Anfrage `AA906` für Schüler `SC906` auswählen.
4. Prüfen:
   - Status vor Bestätigung: `ANGEFRAGT`
   - Theoriekriterium erfüllt: `Ja`
   - Praxiskriterium erfüllt: `Ja`
5. Abschluss bestätigen.
   - Erwartet: HTTP `200`; Anfrage und Gesamtstatus von `SC906` sind danach `ABGESCHLOSSEN`.

## Erwartete Ergebnisse

Die Spalte „Tatsächliches Ergebnis“ während der Präsentationsprobe ausfüllen.

| Fall | Nutzer | Datensatz | Erwarteter HTTP-Status | Erwartete UI-/API-Meldung | Tatsächliches Ergebnis |
|---|---|---|---:|---|---|
| 1. Theoriebuchung erfolgreich | `sc901` | `SC901`, 60 Minuten | `201` | `Theoriekurs gebucht.`; Buchung abrufbar | ☐ |
| 2. Praxisbuchung erfolgreich | `sc901` | `SC901`, `P001`, `FZ002`, `EDDV` → `EDDV` | `201` | `Praxisflugstunde gebucht.`; Buchung abrufbar | ☐ |
| 3. FZ001 blockiert | `sc901` | `SC901`, `P001`, `FZ001` | `409` | `Flugzeug FZ001 ist wegen Status 'in_wartung' nicht buchbar.` | ☐ |
| 4. Mindeststunden fehlen | `sc901` | `SC901`, Theorieprüfung | `409` | Mindestens 10 Theoriestunden erforderlich; aktueller Stand wird genannt | ☐ |
| 5. Verwaltungszugriff verboten | `demo2` | Schüleraktion für `SC901` | `403` | `Keine Berechtigung für diese Funktion.` | ☐ |
| 6. Abschluss bestätigt | `demo2` | `AA906` / `SC906` | `200` | `Abschlussanfrage bestaetigt.`; Status `ABGESCHLOSSEN` | ☐ |

Fehlerantworten müssen strukturierte JSON-Antworten mit `success: false`, `message` und `errors` sein. Stacktraces dürfen weder in der UI noch in der API-Antwort erscheinen.

## Automatisierte Backend-Tests

Die sechs fachlichen Fälle sowie alle acht aktuellen Konten, Fremdzugriffe und der Sessionwechsel sind zusätzlich in den bestehenden Backend-Tests abgedeckt. Sie verwenden für jeden Fall einen isolierten Demo-Datensatz und verändern das normale Demo-Volume nicht.

Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

macOS/Linux:

```bash
bash backend/run-tests.sh
```
