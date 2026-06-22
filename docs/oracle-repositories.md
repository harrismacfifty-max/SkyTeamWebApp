# Oracle-Repository-Schicht

Die Datenzugriffsschicht kapselt SQL in `backend/src/main/java/de/skyteam/flightschool/repository`. Business-Services enthalten keine SQL-Statements.

## Profile

- `APP_PROFILE=demo`: nutzt In-Memory-Repositories mit Beispielobjekten aus dem Flugschulprozess.
- `APP_PROFILE=dev`: Alias fuer den Demo-Modus.
- `APP_PROFILE=oracle`: nutzt JDBC-Repositories gegen die bestehende Oracle-Datenbank.

## Verbindung

Das Oracle-Profil erwartet diese Umgebungsvariablen:

Windows PowerShell:

```powershell
cd <projektverzeichnis>
$env:APP_PROFILE = "oracle"
$env:DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:DB_USER = "SKYTEAM"
$env:DB_PASSWORD = "<passwort>"
$env:DB_DRIVER = "oracle.jdbc.OracleDriver"
$env:EXTRA_CLASSPATH = ".\lib\ojdbc.jar"
powershell -ExecutionPolicy Bypass -File .\backend\run.ps1
```

macOS/Linux Terminal:

```bash
cd <projektverzeichnis>
export APP_PROFILE=oracle
export DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
export DB_USER="SKYTEAM"
export DB_PASSWORD="<passwort>"
export DB_DRIVER="oracle.jdbc.OracleDriver"
export EXTRA_CLASSPATH="./lib/ojdbc.jar"
mkdir -p ./backend/target/classes
find ./backend/src/main/java -name "*.java" > ./backend/target/sources.list
javac -encoding UTF-8 -d ./backend/target/classes @./backend/target/sources.list
java -Dserver.port=8080 -cp "./backend/target/classes:${EXTRA_CLASSPATH}" de.skyteam.flightschool.FlightSchoolApplication
```

Docker Compose:

```bash
cd <projektverzeichnis>
APP_PROFILE=oracle \
DB_URL="jdbc:oracle:thin:@//host.docker.internal:1521/XEPDB1" \
DB_USER="SKYTEAM" \
DB_PASSWORD="<passwort>" \
DB_DRIVER="oracle.jdbc.OracleDriver" \
EXTRA_CLASSPATH="/app/lib/ojdbc.jar" \
docker compose up --build
```

Der Oracle JDBC-Treiber muss fuer Docker zusaetzlich ins Backend-Image oder per separatem Mount in den Container gebracht und ueber `EXTRA_CLASSPATH` referenziert werden. Echte Zugangsdaten gehoeren nur in eine lokale `.env`, nicht ins Repository.

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
