# Checkliste für die Abschlusspräsentation

Die Punkte in dieser Reihenfolge abhaken. Ein vollständiger Testlauf verändert Demo-Daten; vor einem erneuten Lauf wieder mit dem Reset beginnen.

## Vor dem Vortragsbeginn

- [ ] Im Projektverzeichnis `docker compose down -v` ausgeführt.
- [ ] Anschließend `docker compose up --build` vollständig gestartet.
- [ ] Frontend unter `http://localhost:8081` geöffnet und ohne leere oder defekte Ansicht geladen.
- [ ] `GET http://localhost:8080/api/health` liefert HTTP `200`.
- [ ] Im Header stehen `API: online`, `Profil: Demo` und `Datenbankmodus: InMemory`.
- [ ] Anwendung bleibt vor dem Vortragsbeginn gestartet.
- [ ] Backup-Screenshots von Schülerdashboard, Praxisformular, Verwaltungsübersicht und Abschlussanfrage `SC906` liegen lokal bereit.

## Demo-Konten vorab prüfen

- [ ] `sc901` / `demo901` öffnet ausschließlich `SC901`.
- [ ] `sc902` / `demo902` öffnet ausschließlich `SC902`.
- [ ] `sc903` / `demo903` öffnet ausschließlich `SC903`.
- [ ] `sc904` / `demo904` öffnet ausschließlich `SC904` und zeigt Theorie-Wiederholungsbedarf.
- [ ] `sc905` / `demo905` öffnet ausschließlich `SC905` und zeigt Praxis-Wiederholungsbedarf.
- [ ] `sc906` / `demo906` öffnet ausschließlich `SC906` und zeigt Anfrage `AA906` als `ANGEFRAGT`.
- [ ] `sc907` / `demo907` öffnet ausschließlich `SC907` als `ABGESCHLOSSEN`.
- [ ] `demo2` / `demo2` öffnet die Schülerverwaltung ohne eigene Schüler-ID.
- [ ] Nach jedem Test Logout ausgeführt; beim nächsten Login erscheinen keine Daten des vorherigen Kontos.

## Demo-Fall SC901

- [ ] Login `sc901` / `demo901` erfolgreich.
- [ ] Nur Schülerfunktionen sind sichtbar; Verwaltungsfunktionen fehlen.
- [ ] Testfall 1: Theoriebuchung für `SC901` erfolgreich und anschließend sichtbar.
- [ ] Testfall 2: Praxisbuchung mit `P001`, `FZ002` und `EDDV` → `EDDV` erfolgreich.
- [ ] Testfall 3: Praxisbuchung mit `FZ001` wird mit verständlichem Wartungshinweis blockiert.
- [ ] Nach dem Negativtest wieder `FZ002` ausgewählt oder das Praxisformular vollständig zurückgesetzt.
- [ ] Testfall 4: Prüfungsanmeldung ohne Mindeststunden wird blockiert; die Meldung nennt die fehlenden Voraussetzungen.

## Zustandswechsel im Vortrag

- [ ] Logout von `sc901`, danach Login `sc903` / `demo903`.
- [ ] `SC903` und die freigeschaltete Praxisprüfung sind sichtbar; SC901-Daten fehlen.
- [ ] Logout von `sc903`, danach Login `sc906` / `demo906`.
- [ ] Für `SC906` sind Theorie und Praxis bestanden und Abschlussanfrage `AA906` steht auf `ANGEFRAGT`.
- [ ] Eine zweite identische Abschlussanfrage kann nicht erzeugt werden.
- [ ] Optional: `sc907` / `demo907` zeigt den Endzustand `ABGESCHLOSSEN` und deaktivierte neue Ausbildungsaktionen.

## Schülerverwaltung

- [ ] Vor dem Verwaltungslogin vom letzten Schülerkonto abgemeldet.
- [ ] Login `demo2` / `demo2` erfolgreich.
- [ ] `demo2` wird ohne Schüler-ID und als Rolle `SCHUELERVERWALTUNG` angezeigt.
- [ ] Nur Verwaltungsfunktionen sind sichtbar; Theorie-, Praxis- und Prüfungsanmeldung fehlen.
- [ ] Verwaltungsübersicht zeigt Schüler-ID, Name, Abschlussstatus sowie Theorie und Praxis als `Ja`/`Nein`.
- [ ] Testfall 5: Direkte Theorie- oder Praxisbuchung mit Verwaltungsrolle liefert HTTP `403` und `Keine Berechtigung für diese Funktion.`
- [ ] Demo-Datensatz `SC906` beziehungsweise Anfrage `AA906` ist vor der Bestätigung offen.
- [ ] Für `SC906` sind Theorie- und Praxiskriterium jeweils erfüllt.
- [ ] Testfall 6: Abschlussanfrage `AA906` erfolgreich bestätigt; Gesamtstatus von `SC906` ist `ABGESCHLOSSEN`.

## Letzte Sichtprüfung

- [ ] Keine JavaScript-Fehler in der Browserkonsole.
- [ ] Keine rohen Stacktraces oder technischen Interna in der Oberfläche.
- [ ] Buttons sind während laufender Requests deaktiviert.
- [ ] Erfolgs- und Fehlermeldungen sind sichtbar und verständlich.
- [ ] Browserfenster, Zoomstufe und gewünschte Startansicht sind für den Vortrag vorbereitet.
