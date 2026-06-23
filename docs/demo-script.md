# Demo-Skript: 5-Minuten-Vorführung

Ziel: Den MVP im Demo-Modus ohne Oracle zeigen und dabei klar zwischen den Rollen `Schüler` und `Schülerverwaltung` unterscheiden.

## Vorbereitung

Empfohlener Start über Docker Compose:

```bash
cd <projektverzeichnis>
docker compose up --build
```

Danach öffnen:

```text
Frontend: http://localhost:8081
Backend:  http://localhost:8080/api/health
```

Lokale Alternativen ohne Docker:

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1
```

```bash
cd <projektverzeichnis>
chmod +x scripts/start.sh
./scripts/start.sh
```

Demo-Logins:

```text
Schüler:             demo  / demo
Schülerverwaltung:   demo2 / demo2
```

Optionaler Reset der Demo-Daten:

```bash
docker compose down -v
docker compose up --build
```

## Teil 1: Schüler, ca. 2 Minuten

1. Mit `demo/demo` anmelden.
2. `Main Menu / Dashboard` zeigen.
3. Begrüßung mit eigenem Schülernamen, Ausbildungsstatus, Theorie-Fortschritt, Praxis-Fortschritt, Prüfungsstatus und Abschlussanfrage-Status zeigen.
4. Prüfen: Verwaltungsfunktionen wie `Schüler anlegen`, `Schülerdaten prüfen`, `Ausbildungsvertrag prüfen` und `Abschlussanfragen prüfen` sind nicht sichtbar.
5. `Theorie anmelden` öffnen und eine eigene Theoriestunde buchen.
6. `Praxis anmelden` öffnen und eine eigene Flugstunde buchen. Geeignete Demo-Werte:

```text
Fluglehrer: P001
Flugzeug: FZ002
Start/Ziel: EDDV
```

7. `Prüfung anmelden` öffnen. Falls beim eigenen Demo-Schüler noch Mindeststunden fehlen, die fachliche Meldung zeigen.
8. `Abschluss anfragen` öffnen und die eigene Abschlussanfrage stellen oder den aktuellen Anfragezustand zeigen.

Technischer Bezug:

```text
POST /api/theorie/buchen
POST /api/praxis/buchen
POST /api/pruefung/theorie/anmelden
POST /api/pruefung/praxis/anmelden
POST /api/abschluss/anfragen
GET  /api/abschluss/meine-anfrage
```

Wichtige Aussage für die Vorführung: Der Schüler kann den Abschluss nur anfragen. Wirksam wird er erst durch die Schülerverwaltung.

## Teil 2: Schülerverwaltung, ca. 3 Minuten

1. Abmelden und mit `demo2/demo2` anmelden.
2. `Main Menu / Dashboard` zeigen.
3. Verwaltungskennzahlen zeigen: Anzahl Schüler, offene Abschlussanfragen, abgelehnte und bestätigte Abschlussanfragen.
4. BPMN-Prozesspunkte im Verwaltungsdashboard zeigen:

```text
Schülerdaten prüfen
Ausbildungsvertrag prüfen
Schüler anlegen
Beantragung des Schülers vorhanden?
Abnahmekriterien Theorie überprüfen
Abnahmekriterien Praxis überprüfen
Schüler Abschluss bestätigen
```

5. `Selbstverwaltung` öffnen.
6. `Schüler anlegen` öffnen und einen Demo-Schüler anlegen.
7. `Schülerdaten prüfen` öffnen, nach einem Schüler suchen und Details anzeigen.
8. `Ausbildungsvertrag prüfen` öffnen und einen Vertrag als geprüft markieren.
9. `Abschlussanfragen prüfen` öffnen.
10. Demo-Anfrage `AA906` für `SC906 Oskar Lange` öffnen.
11. Theorie- und Praxis-Abnahmekriterien zeigen.
12. `Bestätigen` klicken und damit den BPMN-Schritt `Schüler Abschluss bestätigen` ausführen.
13. Prüfen: Theorie-, Praxis- und Prüfungsanmeldung sind für `demo2/demo2` nicht sichtbar.

Technischer Bezug:

```text
GET  /api/verwaltung/schueler
POST /api/verwaltung/schueler
GET  /api/verwaltung/schueler/{id}
GET  /api/verwaltung/schueler/{id}/vertrag
POST /api/verwaltung/schueler/{id}/vertrag/pruefen
GET  /api/verwaltung/abschlussanfragen
GET  /api/verwaltung/abschlussanfragen/{id}
POST /api/verwaltung/abschlussanfragen/{id}/bestaetigen
POST /api/verwaltung/abschlussanfragen/{id}/ablehnen
```

Wenn `AA906` bereits bestätigt wurde, die Demo-Daten zurücksetzen oder eine neue Abschlussanfrage über den Schülerlauf stellen. Nicht erfüllte Abnahmekriterien können über `Ablehnen` als fachlicher Gegenfall gezeigt werden.

## Abschluss

Kurz zusammenfassen:

- Logo und Header nutzen `frontend/assets/logo.png`.
- Die Navigation ist rollenbasiert.
- Schüler führen nur eigene Ausbildungsaktionen aus.
- Schülerverwaltung führt Verwaltungs- und Abschlussprüfungen aus.
- Backend schützt die Rollen zusätzlich serverseitig mit `403`.
- BPMN wird bewusst als HTML/CSS-Prozessanzeige statt als BPMN-Renderer dargestellt.
- Demo-Modus funktioniert ohne Oracle; Oracle bleibt über `APP_PROFILE=oracle` vorbereitet.
