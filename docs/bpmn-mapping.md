# BPMN-Mapping

Die BPMN-Logik wird im MVP nicht mit einer BPMN-Rendering-Library visualisiert. Stattdessen zeigt das Dashboard eine HTML/CSS-Schrittleiste mit den Zustaenden `offen`, `aktiv`, `erledigt` und `blockiert`.

Statusquelle:

```text
GET /api/status/{schuelerId}/gesamt
```

## Ausbildungsverwaltung

| BPMN-Schritt | UI-Funktion | API-Endpunkt | Service | Tabelle / Repository |
| --- | --- | --- | --- | --- |
| Ausbildungsanfrage / Ausbildung starten | Schueleransicht, Schueler anlegen | `POST /api/schueler` | `SchuelerService` | `SCHUELER`, `AUSBILDUNG_VERTRAG` / `SchuelerRepository`, `AusbildungsVertragRepository` |
| Schuelerdaten pruefen | Schuelerliste und Detaildaten | `GET /api/schueler/{id}` | `SchuelerService` | `SCHUELER` / `SchuelerRepository` |
| Ausbildungsvertrag pruefen | Dashboard, Abschlussansicht | `GET /api/status/{id}/gesamt` | `AusbildungsstatusService` | `AUSBILDUNG_VERTRAG` / `AusbildungsStatusRepository` |
| Ausbildungsplan anlegen | Dashboard, Theorie-/Praxisdaten sichtbar | `GET /api/theorie/{id}`, `GET /api/praxis/{id}` | `TheorieService`, `PraxisService` | `KURSE`, `FLUG` / `KursRepository`, `FlugRepository` |
| Ausbildungsabschluss dokumentieren | Tab Abschluss, Button `Ausbildung abschliessen` | `POST /api/ausbildung/{id}/abschliessen` | `AusbildungsstatusService` | `AUSBILDUNG_VERTRAG` / `AusbildungsVertragRepository` |

## Theorieausbildung

| BPMN-Schritt | UI-Funktion | API-Endpunkt | Service | Tabelle / Repository |
| --- | --- | --- | --- | --- |
| Theoriekurs buchen | Tab Theorie, Formular `Neuer Theoriekurs` | `POST /api/theorie/buchen` | `TheorieService` | `KURSE`, `SCHUELER` / `KursRepository` |
| Theoriekurs durchfuehren | Kurs erscheint in Theorietabelle | `GET /api/theorie/{id}` | `TheorieService` | `KURSE` / `KursRepository` |
| Theoriestunden erfassen | Fortschrittskarte Theorie | `GET /api/status/{id}/theorie` | `TheorieService` | `SCHUELER.THEORIESTUNDE` / `KursRepository` |
| Restliche Theoriestunden pruefen | Dashboard-Prozessanzeige | `GET /api/status/{id}/gesamt` | `AusbildungsstatusService` | `SCHUELER` / `AusbildungsStatusRepository` |
| Theoriepruefung durchfuehren | Button `Theoriepruefung anmelden` | `POST /api/pruefung/theorie/anmelden` | `PruefungsService` | `PRUEFUNG` / `PruefungRepository` |
| Theorieergebnis speichern | Tab Pruefungen, Ergebnisformular | `POST /api/pruefung/ergebnis` | `PruefungsService` | `PRUEFUNG` / `PruefungRepository` |
| Theorie wiederholen oder abschliessen | Prozessanzeige und Pruefungsstatus | `GET /api/status/{id}/gesamt` | `AusbildungsstatusService`, `PruefungsService` | `PRUEFUNG` / `PruefungRepository` |

## Praxisausbildung

| BPMN-Schritt | UI-Funktion | API-Endpunkt | Service | Tabelle / Repository |
| --- | --- | --- | --- | --- |
| Neue Flugstunde buchen | Tab Praxis, Formular `Neue Flugstunde` | `POST /api/praxis/buchen` | `PraxisService` | `FLUG` / `FlugRepository` |
| Fluglehrer pruefen | Validierung bei Praxisbuchung | `POST /api/praxis/buchen` | `PraxisService` | `PILOT` / `PilotRepository` |
| Flugzeug pruefen | Validierung bei Praxisbuchung | `POST /api/praxis/buchen` | `PraxisService` | `FLUGZEUG` / `FlugzeugRepository` |
| Wartungsstatus pruefen | Validierung bei Praxisbuchung | `POST /api/praxis/buchen` | `PraxisService` | `WARTUNG`, `WARTUNG_UND_FLUGZEUG` / `WartungRepository` |
| Check-in durchfuehren | Im MVP als Teil der gebuchten Flugstunde abgebildet | `GET /api/praxis/{id}` | `PraxisService` | `FLUG` / `FlugRepository` |
| Ausbildungsflug durchfuehren | Flugstunde in Praxistabelle | `GET /api/praxis/{id}` | `PraxisService` | `FLUG` / `FlugRepository` |
| Flugstunden erfassen | Fortschrittskarte Praxis | `GET /api/status/{id}/praxis` | `PraxisService` | `SCHUELER.FLUGSTUNDE` / `FlugRepository` |
| Praxispruefung durchfuehren | Button `Praxispruefung anmelden` | `POST /api/pruefung/praxis/anmelden` | `PruefungsService` | `PRUEFUNG` / `PruefungRepository` |
| Praxisergebnis speichern | Tab Pruefungen, Ergebnisformular | `POST /api/pruefung/ergebnis` | `PruefungsService` | `PRUEFUNG` / `PruefungRepository` |
| Praxis wiederholen oder abschliessen | Prozessanzeige und Abschlussstatus | `GET /api/status/{id}/gesamt` | `AusbildungsstatusService`, `PruefungsService` | `PRUEFUNG`, `AUSBILDUNG_VERTRAG` |

## Statusableitung

| UI-Zustand | Ableitung |
| --- | --- |
| `offen` | Schritt ist noch nicht erreicht. |
| `aktiv` | Mindestdaten liegen vor, der naechste Prozessschritt ist ausfuehrbar. |
| `erledigt` | Status oder Ergebnis ist erfolgreich vorhanden. |
| `blockiert` | Fachregel verhindert Fortsetzung, z.B. fehlende Mindeststunden oder nicht bestandene Pruefung. |

Der Abschluss wird erst als erledigt markiert, wenn der Gesamtstatus `ABGESCHLOSSEN` ist. Bestandene Theorie und Praxis allein bedeuten nur: Abschluss ist ausfuehrbar.
