# BPMN-Mapping

Dieses Mapping bezieht sich auf die aktualisierte Datei `Ausbildungsverwaltung.bpmn`. Die WebApp rendert das BPMN nicht direkt, sondern zeigt die fachlichen Lanes im Dashboard als HTML/CSS-Schrittleiste mit den Zuständen `offen`, `aktiv`, `erledigt` und `blockiert`.

Statusquellen:

```text
GET /api/status/{schuelerId}/gesamt
GET /api/abschluss/meine-anfrage
GET /api/verwaltung/abschlussanfragen
```

Die BPMN-Datei enthält drei Lanes:

- `Ausbilder`
- `Schülerverwaltung`
- `Prüfungsverwaltung`

Die WebApp kennt zwei Loginrollen: `SCHUELER` für den eigenen Ausbildungsprozess und `SCHUELERVERWALTUNG` für Verwaltungsfunktionen. Die BPMN-Lane `Prüfungsverwaltung` ist im MVP ein fachlicher Bereich der Schülerverwaltung, nicht ein separater Login.

## Mapping-Tabelle

| BPMN-Lane | BPMN-Aktivität | Rolle in der WebApp | UI-Bereich | API-Endpunkt | Service | Repository/Tabelle | Status im MVP |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Ausbilder | Anmeldung (Webapp) | Schüler oder Schülerverwaltung | Login-Ansicht, Header-Loginstatus | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` | `AuthService` | In-Memory-Session, keine DB-Tabelle | Umgesetzt; `demo/demo` = `SCHUELER`, `demo2/demo2` = `SCHUELERVERWALTUNG` |
| Schülerverwaltung | Schülerdaten prüfen | Schülerverwaltung | Menü `Selbstverwaltung`, Bereich `Schülerdaten prüfen` | `GET /api/verwaltung/schueler`, `GET /api/verwaltung/schueler/{id}` | `SchuelerService` | `SchuelerRepository` / `SCHUELER` | Umgesetzt; Suche, Filter und Detailansicht vorhanden |
| Schülerverwaltung | Ausbildungsvertrag prüfen | Schülerverwaltung | Menü `Selbstverwaltung`, Bereich `Ausbildungsvertrag prüfen` | `GET /api/verwaltung/schueler/{id}/vertrag`, `POST /api/verwaltung/schueler/{id}/vertrag/pruefen` | `SchuelerService`, `AusbildungsstatusService` | `AusbildungsVertragRepository` / `AUSBILDUNG_VERTRAG` | Umgesetzt; Prüfung kann im Demo-Modus dokumentiert werden |
| Schülerverwaltung | Schüler anlegen | Schülerverwaltung | Menü `Selbstverwaltung`, Unterpunkt `Schüler anlegen` | `POST /api/verwaltung/schueler` | `SchuelerService` | `SchuelerRepository`, `AusbildungsVertragRepository` / `SCHUELER`, `AUSBILDUNG_VERTRAG` | Umgesetzt im Demo-Modus mit Persistenz; Oracle-Repository vorbereitet |
| Schülerverwaltung | Abbruch | Schülerverwaltung | Dashboard-Prozessanzeige, Statusanzeige | `GET /api/status/{schuelerId}/gesamt` | `AusbildungsstatusService` | `AUSBILDUNG_VERTRAG` | Teilweise umgesetzt; Status `ABGEBROCHEN` wird angezeigt, separate Abbruch-Aktion ist offen |
| Schülerverwaltung | Schüler erfolgreich angelegt | Schülerverwaltung | Dashboard-Prozessanzeige, Schülerliste | `GET /api/verwaltung/schueler/{id}`, `GET /api/status/{schuelerId}/gesamt` | `SchuelerService`, `AusbildungsstatusService` | `SCHUELER`, `AUSBILDUNG_VERTRAG` | Umgesetzt; vorhandener Schüler mit Vertrag gilt als erfolgreich angelegt |
| Prüfungsverwaltung | Beantragung des Schülers vorhanden? | Schüler, Schülerverwaltung | Menü `Abschluss anfragen` bzw. `Abschlussanfragen prüfen`, Dashboard-Prozessanzeige | `POST /api/abschluss/anfragen`, `GET /api/abschluss/meine-anfrage`, `GET /api/verwaltung/abschlussanfragen` | `AbschlussService` | `AbschlussAnfrageRepository` / optional `ABSCHLUSS_ANFRAGE` | Umgesetzt als echte Abschlussanfrage; Demo-Modus persistiert Anfragen im In-Memory-Speicher |
| Prüfungsverwaltung | Nein | Schülerverwaltung | Dashboard-Prozessanzeige, Verwaltungsliste | `GET /api/verwaltung/abschlussanfragen` | `AbschlussService` | `AbschlussAnfrageRepository` / optional `ABSCHLUSS_ANFRAGE` | Umgesetzt; ohne vorhandene Anfrage gibt es keinen bestätigbaren Verwaltungseintrag |
| Prüfungsverwaltung | Abnahmekriterien Theorie überprüfen | Schülerverwaltung | Dashboard, Abschlussanfragen prüfen | `GET /api/verwaltung/abschlussanfragen/{id}` | `AbschlussService`, `AusbildungsstatusService`, `PruefungsService` | `PruefungRepository`, `KursRepository` / `PRUEFUNG`, `KURSE` | Umgesetzt; Kriterium ist bestandene Theorieprüfung |
| Prüfungsverwaltung | Abnahmekriterien Praxis überprüfen | Schülerverwaltung | Dashboard, Abschlussanfragen prüfen | `GET /api/verwaltung/abschlussanfragen/{id}` | `AbschlussService`, `AusbildungsstatusService`, `PruefungsService` | `PruefungRepository`, `FlugRepository` / `PRUEFUNG`, `FLUG` | Umgesetzt; Kriterium ist bestandene Praxisprüfung |
| Prüfungsverwaltung | Schüler Abschluss bestätigen | Schülerverwaltung | Menü `Abschlussanfragen prüfen`, Button `Bestätigen` | `POST /api/verwaltung/abschlussanfragen/{id}/bestaetigen` | `AbschlussService`, `AusbildungsstatusService` | `AbschlussAnfrageRepository`, `AusbildungsVertragRepository` / optional `ABSCHLUSS_ANFRAGE`, `AUSBILDUNG_VERTRAG` | Umgesetzt; nur mit vorhandener Anfrage und erfüllten Abnahmekriterien möglich |
| Prüfungsverwaltung | Bestätigt | Schülerverwaltung | Dashboard, Abschlussansicht | `GET /api/status/{schuelerId}/gesamt`, `GET /api/verwaltung/abschlussanfragen/{id}` | `AbschlussService`, `AusbildungsstatusService` | `ABSCHLUSS_ANFRAGE`, `AUSBILDUNG_VERTRAG` | Umgesetzt; Anfrage wird `ABGESCHLOSSEN`, Ausbildungsvertrag wird `ABGESCHLOSSEN` |

## UI- und Menübezug

- Dashboard: zeigt die drei BPMN-Lanes `Ausbilder`, `Schülerverwaltung` und `Prüfungsverwaltung`.
- Schülerrolle: sieht `Main Menu / Dashboard`, `Theorie anmelden`, `Praxis anmelden`, `Prüfung anmelden`, `Abschluss anfragen`.
- Schülerverwaltung: sieht `Main Menu / Dashboard`, `Selbstverwaltung`, `Schüler anlegen`, `Schülerdaten prüfen`, `Ausbildungsvertrag prüfen`, `Abschlussanfragen prüfen`.
- Menüpunkt `Abschluss anfragen`: Schüler stellt die eigene Anfrage und sieht Statuswerte wie `KEINE_ANFRAGE`, `ANGEFRAGT`, `ABGELEHNT` oder `ABGESCHLOSSEN`.
- Menüpunkt `Abschlussanfragen prüfen`: Schülerverwaltung sieht nur vorhandene offene Anfragen, prüft Abnahmekriterien und bestätigt oder lehnt ab.
- Menüpunkte `Theorie anmelden`, `Praxis anmelden` und `Prüfung anmelden`: sind Schülerfunktionen und keine eigenen Lanes im aktualisierten BPMN.
- Die Planungsansicht bleibt eine unterstützende Demo-Komfortfunktion und ist nicht Teil der aktualisierten BPMN-Lanes.

## Statusableitung

| UI-Zustand | Ableitung |
| --- | --- |
| `offen` | Schritt ist im aktuellen Schülerkontext noch nicht erreicht. |
| `aktiv` | Der Schritt ist der nächste sinnvolle Schritt im BPMN-Ablauf. |
| `erledigt` | Status, Datensatz oder Ergebnis ist erfolgreich vorhanden. |
| `blockiert` | Fachregel verhindert Fortsetzung, z.B. fehlende Abschlussanfrage oder nicht erfüllte Abnahmekriterien. |

Der Abschluss wird erst als `Bestätigt` markiert, wenn eine Abschlussanfrage vorhanden ist, Theorie- und Praxis-Abnahmekriterien erfüllt sind und die Schülerverwaltung bestätigt hat. Bestandene Theorie und Praxis allein schließen die Ausbildung nicht mehr direkt ab.
