# Demo-Skript: 5-Minuten-Vorfuehrung

Ziel: Den MVP ohne Oracle-Datenbank zeigen und den Bezug zwischen UI, API, Services und BPMN-Prozess verdeutlichen.

## Vorbereitung

Backend und Frontend starten:

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
.\start-skyteam.ps1
```

Login:

```text
demo / demo
```

Optionaler Reset auf Ausgangsdaten:

```powershell
Remove-Item .\backend\target\dev-data\flight-school-demo.properties
```

Danach Backend neu starten.

## Minute 0-1: Einstieg

1. Dashboard oeffnen.
2. Kurz zeigen:
   - Schuelerauswahl
   - Ausbildungsstatus
   - Theorie-/Praxisfortschritt
   - BPMN-Prozessanzeige
3. Erklaeren: Die Prozessanzeige wird aus `GET /api/status/{schuelerId}/gesamt` abgeleitet.

Empfohlener Schueler:

```text
SC901 Jonas Keller
```

## Demo-Fall 1: Schueler bucht Theoriekurs

1. Tab `Theorie` oeffnen.
2. Schueler `SC901` ausgewaehlt lassen.
3. Kursdaten eintragen:

```text
Thema: Theorie - Demo Navigation
Termin: beliebiges Datum
Dauer: 60
Dozent: Elias Schulz
```

4. `Theoriekurs buchen` klicken.
5. Tabelle und Fortschritt zeigen.

Technischer Bezug:

```text
POST /api/theorie/buchen
TheorieService
KursRepository
KURSE / SCHUELER.THEORIESTUNDE
```

## Demo-Fall 2: Schueler bucht Flugstunde

1. Tab `Praxis` oeffnen.
2. Gueltige Daten eintragen:

```text
Fluglehrer: P001
Flugzeug: FZ002
Start/Ziel: EDDV
Ausbildungsinhalt: Platzrunde Demo
```

3. `Flugstunde buchen` klicken.
4. Neue Flugstunde und aktualisierte Flugstunden zeigen.
5. Optional: Flugstunde ueber `Stornieren` wieder entfernen.

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
3. Ergebnis speichern:

```text
Pruefungsart: Theorie oder Praxis
Ergebnis: bestanden oder nicht bestanden
```

4. Bei `nicht bestanden` zeigen: Wiederholungsbedarf wird markiert.

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
5. Status `ABGESCHLOSSEN` zeigen.

Technischer Bezug:

```text
POST /api/ausbildung/SC906/abschliessen
AusbildungsstatusService
AusbildungsVertragRepository
AUSBILDUNG_VERTRAG.STATUS
```

## Abschluss der Vorfuehrung

Kurz zusammenfassen:

- Frontend ist eine SPA ohne Framework.
- PHP ist nur View-Layer.
- Java REST-API kapselt alle Fachaktionen.
- Services enthalten die Fachregeln.
- Repositories kapseln Oracle-SQL oder Demo-Persistenz.
- BPMN-Prozess ist im Dashboard nachvollziehbar abgebildet.
