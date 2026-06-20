# Oracle-Repository-Schicht

Die Datenzugriffsschicht kapselt SQL in `backend/src/main/java/de/skyteam/flightschool/repository`. Business-Services enthalten keine SQL-Statements.

## Profile

- `APP_PROFILE=dev`: nutzt In-Memory-Repositories mit Beispielobjekten aus dem Flugschulprozess.
- `APP_PROFILE=oracle`: nutzt JDBC-Repositories gegen die bestehende Oracle-Datenbank.

## Verbindung

Das Oracle-Profil erwartet diese Umgebungsvariablen:

```powershell
$env:APP_PROFILE = "oracle"
$env:DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:DB_USER = "SKYTEAM"
$env:DB_PASSWORD = "<passwort>"
$env:DB_DRIVER = "oracle.jdbc.OracleDriver"
$env:EXTRA_CLASSPATH = "C:\pfad\zu\ojdbc.jar"
```

## Repositories

- `SchuelerRepository`: liest `SCHUELER`.
- `AusbildungsVertragRepository`: liest und aktualisiert `AUSBILDUNG_VERTRAG`, verknuepft ueber `SCHUELER.ID_AUSBILDUNG_VERTRAG`.
- `KursRepository`: liest und erzeugt Theorieeintraege in `KURSE`, aktualisiert `SCHUELER.THEORIESTUNDE`.
- `FlugRepository`: liest und erzeugt Praxisfluege in `FLUG`, verknuepft Piloten ueber `FLUG_UND_PILOT`, aktualisiert `SCHUELER.FLUGSTUNDE`.
- `PruefungRepository`: liest und erzeugt `PRUEFUNG`; Ergebnisse werden ohne Schemaaenderung im bestehenden Feld `TYP` als `... - bestanden` oder `... - nicht bestanden` gespeichert.
- `PilotRepository`: liest `PILOT`.
- `FlugzeugRepository`: liest `FLUGZEUG`.
- `WartungRepository`: liest `WARTUNG` und `WARTUNG_UND_FLUGZEUG`.
- `AusbildungsStatusRepository`: aggregiert `SCHUELER`, `AUSBILDUNG_VERTRAG` und `PRUEFUNG`.

## Hinweis

Das vorhandene SQL-Skript enthaelt keine separate Ergebnisspalte fuer Pruefungen und keine Sequenzen fuer neue IDs. Deshalb generiert die JDBC-Schicht neue String-IDs anhand vorhandener Praefixe und speichert Pruefungsergebnisse im bestehenden `PRUEFUNG.TYP`.

Die Business-Services nutzen ausschliesslich Repository-Interfaces. SQL bleibt in `repository/jdbc`.
