# Demo-Skript: 5-Minuten-Vorfuehrung

Ziel: Den MVP ohne Oracle-Datenbank zeigen und den Bezug zwischen UI, API, Services und BPMN-Prozess verdeutlichen.

## Vorbereitung

Backend und Frontend starten:

Empfohlen ueber Docker Compose, auf Windows, macOS und Linux gleich:

```bash
cd <projektverzeichnis>
docker compose up --build
```

Frontend oeffnen:

```text
http://localhost:8081
```

Optionaler lokaler Start ueber Windows PowerShell:

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1
```

Optionaler lokaler Start ueber macOS/Linux Terminal:

```bash
cd <projektverzeichnis>
chmod +x scripts/start.sh
./scripts/start.sh
```

Login:

```text
demo / demo
```

Optionaler Reset auf Ausgangsdaten:

Docker Compose:

```bash
docker compose down -v
```

Lokaler PowerShell-Start:

```powershell
Remove-Item .\backend\target\dev-data\flight-school-demo.properties
```

Lokaler macOS/Linux-Start:

```bash
rm -f ./backend/target/dev-data/flight-school-demo.properties
```

Danach Backend neu starten.

## Minute 0-1: Einstieg

1. Frontend oeffnen und einloggen.
2. Logo oben links, aktive Tabs und Demo-Login kurz zeigen.
3. Dashboard oeffnen.
4. Kurz zeigen:
   - Statusuebersicht fuer Theorie, Praxis, Pruefung und Ausbildung
   - Schuelerauswahl
   - Ausbildungsstatus
   - Theorie-/Praxisfortschritt
   - BPMN-Prozessanzeige
5. Erklaeren: Die Prozessanzeige wird aus `GET /api/status/{schuelerId}/gesamt` abgeleitet.

Empfohlener Schueler:

```text
SC901 Jonas Keller
```

## Minute 1: Schueler suchen und Komfortfunktionen zeigen

1. Tab `Schueler` oeffnen.
2. Im Suchfeld nach `Keller`, `Nico` oder einem Status filtern.
3. Einen Schueler aus der Tabelle auswaehlen.
4. Rechtsklick oder Drei-Punkte-Button in der Schuelerzeile zeigen.
5. Kontextaktion `Theorie oeffnen` oder `Praxis oeffnen` auswaehlen.

Technischer Bezug:

```text
GET /api/schueler
GET /api/status/{schuelerId}/gesamt
Frontend-Komfort: Suche, Status-Badges, Kontextmenue
```

## Optional: Planungsansicht zeigen

1. Tab `Planung` oeffnen.
2. Offene Theorieanfrage per Drag-and-Drop in `Geplante Theoriestunden` ziehen.
3. Bestaetigung anzeigen und speichern.
4. Geplante Theoriestunde wieder per Drag-and-Drop zurueck in `Offene Theorieanfragen` ziehen und Storno bestaetigen.
5. Optional: Offene Praxisanfrage in `Geplante Flugstunden` ziehen.
6. Ungueltigen Drop, z.B. Theorieanfrage auf `Pruefungen`, kurz zeigen: Die UI verhindert fachlich falsche Verschiebungen.

Technischer Bezug:

```text
POST /api/theorie/buchen
POST /api/theorie/stornieren
POST /api/praxis/buchen
Planungsansicht nutzt vorhandene REST-Endpunkte und bricht den normalen Buchungsprozess nicht.
```

## Demo-Fall 1: Schueler bucht Theoriekurs

1. Tab `Theorie` oeffnen.
2. Schueler `SC901` ausgewaehlt lassen.
3. Status-Badge zeigen: Theorie offen oder bereit.
4. Kursdaten eintragen:

```text
Thema: Theorie - Demo Navigation
Termin: beliebiges Datum
Dauer: 60
Dozent: Elias Schulz
```

5. `Theoriekurs buchen` klicken.
6. Ladehinweis und danach Tabelle/Fortschritt zeigen.
7. Optional: Kurs ueber `Stornieren`, Kontextmenue oder Planungsboard wieder entfernen.

Technischer Bezug:

```text
POST /api/theorie/buchen
POST /api/theorie/stornieren
TheorieService
KursRepository
KURSE / SCHUELER.THEORIESTUNDE
```

## Demo-Fall 2: Schueler bucht Flugstunde

1. Tab `Praxis` oeffnen.
2. Fluglehrer- und Flugzeugsuche kurz zeigen.
3. Gueltige Daten eintragen:

```text
Fluglehrer: P001
Flugzeug: FZ002
Start/Ziel: EDDV
Ausbildungsinhalt: Platzrunde Demo
```

4. `Flugstunde buchen` klicken.
5. Neue Flugstunde, aktualisierte Flugstunden und Status-Badge zeigen.
6. Optional: Flugstunde ueber `Stornieren` oder Kontextmenue wieder entfernen.

Technischer Bezug:

```text
POST /api/praxis/buchen
PraxisService
PilotRepository / FlugzeugRepository / WartungRepository / FlugRepository
PILOT / FLUGZEUG / WARTUNG / FLUG
```

Optionaler Fehlerfall:

```text
Flugzeug FZ001 -> 409 wegen Wartung
Flugzeug FZ004 -> 409 wegen gesperrtem Flugzeug
Pilot P004 -> 409 wegen nicht verfuegbarem Fluglehrer
```

## Demo-Fall 3: Pruefung anmelden

1. Schueler `SC902 Nico Berger` waehlen.
2. Tab `Theorie` oeffnen.
3. Zeigen: Theoriepruefung ist wegen 12 Theoriestunden freigeschaltet.
4. Theoriepruefung anmelden.

Alternativ Praxis:

```text
SC903 Mina Sommer
```

Technischer Bezug:

```text
POST /api/pruefung/theorie/anmelden
POST /api/pruefung/praxis/anmelden
PruefungsService
PruefungRepository
PRUEFUNG
```

## Demo-Fall 4: Pruefungsergebnis speichern

1. Tab `Pruefungen` oeffnen.
2. Eine Pruefung auswaehlen.
3. Status-Badge `Angemeldet`, `Bestanden` oder `Nicht bestanden` zeigen.
4. Ergebnis speichern:

```text
Pruefungsart: Theorie oder Praxis
Ergebnis: bestanden oder nicht bestanden
```

5. Bei `nicht bestanden` zeigen: Wiederholungsbedarf wird markiert.

Technischer Bezug:

```text
POST /api/pruefung/ergebnis
PruefungsService
PruefungRepository
PRUEFUNG
```

## Demo-Fall 5: Ausbildung abschliessen

1. Schueler `SC906 Oskar Lange` waehlen.
2. Dashboard zeigen: Theorie und Praxis sind bestanden, Status ist noch nicht `ABGESCHLOSSEN`.
3. Tab `Abschluss` oeffnen.
4. `Ausbildung abschliessen` klicken.
5. Ladehinweis und danach Status `ABGESCHLOSSEN` zeigen.

Technischer Bezug:

```text
POST /api/ausbildung/SC906/abschliessen
AusbildungsstatusService
AusbildungsVertragRepository
AUSBILDUNG_VERTRAG.STATUS
```

## Abschluss der Vorfuehrung

Kurz zusammenfassen:

- Logo und Header zeigen die SkyTeam-Branding-Datei aus `frontend/assets/logo.png`.
- Frontend ist eine SPA ohne Framework.
- PHP ist nur View-Layer.
- Java REST-API kapselt alle Fachaktionen.
- Services enthalten die Fachregeln.
- Repositories kapseln Oracle-SQL oder Demo-Persistenz.
- BPMN-Prozess ist im Dashboard nachvollziehbar abgebildet.
- Komfortfunktionen: Suche, Status-Badges, Kontextmenue und Drag-and-Drop-Planung.
