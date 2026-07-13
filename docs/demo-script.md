# Demo-Skript: Abschlusspräsentation

Ziel: Im Demo-Modus mehrere fachlich unterschiedliche Schülerzustände und anschließend die Schülerverwaltung zeigen. Jedes Schülerkonto ist fest mit dem angegebenen Schülerdatensatz verbunden und sieht ausschließlich die eigene Ausbildung.

## Vorbereitung

Vor der Präsentation:

1. Demo-Daten im Projektverzeichnis bewusst zurücksetzen:

   ```bash
   docker compose down -v
   docker compose up --build
   ```

   Alternativ unter Windows `.\scripts\reset-demo.ps1` oder unter macOS/Linux `./scripts/reset-demo.sh` ausführen.

2. Anwendung vollständig starten lassen und warten, bis Frontend und Backend erreichbar sind:

   ```text
   Frontend: http://localhost:8081
   Backend:  http://localhost:8080/api/health
   ```

3. Health-Endpoint prüfen. Erwartet sind `activeProfile: demo`, ein erreichbares Backend und im Header `API: online`, `Profil: Demo`, `Datenbankmodus: InMemory`:

   ```bash
   curl http://localhost:8080/api/health
   ```

4. Die für den Vortrag empfohlenen Logins `sc901/demo901`, `sc903/demo903`, `sc906/demo906` und `demo2/demo2` kurz testen und jeweils wieder abmelden.
5. In der Schülerverwaltung prüfen, dass die Anfrage `AA906` für `SC906 Oskar Lange` offen ist und den Status `ANGEFRAGT` hat.

Der Reset löscht nur die persistenten Daten des lokalen Demo-Modus. Oracle-Daten werden nicht berührt.

## Demo-Fall 1: Laufende Ausbildung und blockierte Prüfung

Login: `sc901` / `demo901` → `SC901 Jonas Keller`

1. Im Dashboard Name, Schüler-ID `SC901`, Rolle `SCHUELER` und Ausbildungsstatus `AKTIV` zeigen.
2. Darauf hinweisen, dass keine Schülerauswahl und keine Verwaltungsfunktion sichtbar ist.
3. Fehlende Mindeststunden in Theorie und Praxis zeigen.
4. `Prüfung anmelden` öffnen beziehungsweise die deaktivierte Anmeldung zeigen.
5. Erwartung: Die Prüfungsanmeldung ist wegen fehlender Mindeststunden blockiert; ein direkter API-Versuch liefert HTTP `409` mit verständlicher Meldung.
6. Optional die sichere Praxisvorauswahl `P001`, `FZ002`, `EDDV` → `EDDV` zeigen und eine Praxisstunde buchen.
7. Optionaler Negativfall: `FZ001` auswählen. Die Buchung bleibt wegen Wartung fachlich blockiert; anschließend wieder `FZ002` auswählen.

Kernaussage: Das Konto kann nur `SC901` sehen und Aktionen werden weiterhin serverseitig fachlich geprüft.

## Demo-Fall 2: Praxisprüfung freigeschaltet

Logout, danach Login: `sc903` / `demo903` → `SC903 Mina Sommer`

1. Prüfen, dass nach dem Kontowechsel ausschließlich `SC903` angezeigt wird.
2. Den Ausbildungsstatus `PRAXIS_BEREIT` und die erreichten Praxis-Mindeststunden zeigen.
3. `Prüfung anmelden` öffnen.
4. Zeigen, dass die Praxisprüfung freigeschaltet und die Theorieprüfung noch nicht freigeschaltet ist.
5. Optional eine eigene Praxisprüfungsanmeldung durchführen.

Kernaussage: Freigaben werden aus dem fachlichen Zustand des aktuell angemeldeten Schülers abgeleitet.

## Demo-Fall 3: Bestandene Prüfungen und offene Abschlussanfrage

Logout, danach Login: `sc906` / `demo906` → `SC906 Oskar Lange`

1. Prüfen, dass keine Daten von SC903 mehr sichtbar sind.
2. Im Dashboard zeigen:
   - Theorie bestanden
   - Praxis bestanden
   - Abschlussanfrage `AA906`
   - Anfragestatus `ANGEFRAGT`
3. `Abschluss anfragen` öffnen und den bestehenden Status zeigen.
4. Darauf hinweisen, dass keine zweite identische Abschlussanfrage erzeugt werden kann.

Kernaussage: Der Schüler kann den Abschluss nur anfragen. Bestätigt wird er durch die Schülerverwaltung.

## Demo-Fall 4: Abschluss durch die Schülerverwaltung bestätigen

Logout, danach Login: `demo2` / `demo2` → Rolle `SCHUELERVERWALTUNG`

1. Zeigen, dass `demo2` kein Schülerkonto ist und eine Übersicht über alle Schüler besitzt.
2. In der Verwaltungsübersicht Schüler-ID, Name, Abschlussstatus sowie Theorie- und Praxiskriterium zeigen.
3. Prüfen, dass Schüleraktionen wie Theorie-, Praxis- und Prüfungsanmeldung nicht sichtbar sind. Direkte Schüleraktionen liefern serverseitig HTTP `403`.
4. `Abschlussanfragen prüfen` öffnen.
5. Anfrage `AA906` für `SC906 Oskar Lange` auswählen.
6. Status `ANGEFRAGT`, Theoriekriterium `Ja` und Praxiskriterium `Ja` zeigen.
7. `Bestätigen` klicken.
8. Prüfen, dass Anfrage und Ausbildungsstatus anschließend `ABGESCHLOSSEN` sind.

Wenn `AA906` bereits bestätigt wurde, vor einem erneuten Vortrag wieder den Demo-Reset ausführen.

## Optionaler Endzustand: Ausbildung abgeschlossen

Login: `sc907` / `demo907` → `SC907 Ella Hartmann`

1. Ausbildungsstatus `ABGESCHLOSSEN` zeigen.
2. Zeigen, dass neue Buchungen, Prüfungsanmeldungen und Abschlussanfragen deaktiviert sind.
3. Darauf hinweisen, dass auch ein direkter Versuch, eine neue Abschlussanfrage zu stellen, serverseitig mit HTTP `409` blockiert wird.

## Technischer Bezug

```text
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout

GET  /api/schueler/me
GET  /api/status/me
GET  /api/theorie/me
GET  /api/praxis/me
GET  /api/pruefung/me
GET  /api/abschluss/meine-anfrage

GET  /api/verwaltung/schueler
GET  /api/verwaltung/abschlussanfragen/{id}
POST /api/verwaltung/abschlussanfragen/{id}/bestaetigen
```

## Abschluss

Kurz zusammenfassen:

- Sieben Schülerkonten zeigen sieben definierte Ausbildungszustände.
- Jedes Schülerkonto ist serverseitig auf den eigenen Datensatz begrenzt.
- Schülerverwaltung und Schüleraktionen sind rollenbasiert getrennt.
- Fachliche Konflikte liefern verständliche Antworten statt Stacktraces.
- Der Demo-Modus funktioniert ohne Oracle; Oracle bleibt über `APP_PROFILE=oracle` vorbereitet.
