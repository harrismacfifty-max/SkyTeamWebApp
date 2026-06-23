# Annahmen

- Der Demo-Modus ist der Standardmodus. Ohne `APP_PROFILE` startet die Anwendung mit Demo-Daten und ohne Oracle-Verbindung.
- `APP_PROFILE=dev` bleibt aus Kompatibilitätsgründen als Alias für den Demo-Modus erhalten.
- Der Oracle-Modus wird über `APP_PROFILE=oracle`, `DB_URL`, `DB_USER` und `DB_PASSWORD` konfiguriert. Es werden keine echten Zugangsdaten im Repository abgelegt.
- Die WebApp kennt bewusst nur zwei Demo-Rollen: `SCHUELER` und `SCHUELERVERWALTUNG`.
- `demo/demo` ist ein Schüler-Login und dem Demo-Schüler `SC901` zugeordnet. Dieser Benutzer darf nur den eigenen Ausbildungsprozess bearbeiten.
- `demo2/demo2` ist ein Login für die Schülerverwaltung. Diese Rolle darf Schülerdaten und Verträge prüfen, Schüler anlegen und Abschlussanfragen bearbeiten.
- Schülerverwaltung kann keine Theorie-, Praxis- oder Prüfungsanmeldung als Schüleraktion ausführen.
- Schüler können keine Verwaltungsfunktionen nutzen und keinen Abschluss selbst bestätigen.
- Die BPMN-Lane `Prüfungsverwaltung` wird im MVP als fachlicher Bereich der Rolle `SCHUELERVERWALTUNG` umgesetzt, nicht als separater Login.
- Eine Abschlussanfrage wird durch den Schüler gestellt und erst nach Prüfung der Abnahmekriterien durch die Schülerverwaltung wirksam.
- Für den Oracle-Modus werden bestehende Tabellen wie `SCHUELER`, `AUSBILDUNG_VERTRAG`, `KURSE`, `FLUG`, `PRUEFUNG`, `PILOT`, `FLUGZEUG` und `WARTUNG` genutzt. Destruktive Schemaänderungen sind nicht Teil des MVP.
- Falls für Abschlussanfragen eine eigene Oracle-Tabelle benötigt wird, liegt dafür ein optionales Migrationsskript unter `database/migrations/abschlussanfragen.sql`.
- Externe Systeme wie E-Mail, Zahlung, Kalenderintegration oder vollständiger Flughafenbetrieb sind aus dem Umfang ausgeschlossen.
