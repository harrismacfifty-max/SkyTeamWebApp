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

- `SchuelerRepository`: liest `SCHUELER`; Speichern und kaskadiertes Loeschen laufen ueber das Procedure-Package.
- `AusbildungsVertragRepository`: liest `AUSBILDUNG_VERTRAG`; Speichern und Statuswechsel laufen ueber das Procedure-Package.
- `KursRepository`: liest Theorieeintraege aus `KURSE`; Buchen und Stornieren laufen ueber das Procedure-Package und aktualisieren `SCHUELER.THEORIESTUNDE` atomar.
- `FlugRepository`: liest Praxisfluege aus `FLUG`; Buchen und Stornieren laufen ueber das Procedure-Package, pflegen `FLUG_UND_PILOT` und aktualisieren `SCHUELER.FLUGSTUNDE` atomar.
- `PruefungRepository`: liest `PRUEFUNG`; Anmeldung und Ergebnis werden ueber das Procedure-Package gespeichert. Der lesbare Status bleibt in `TYP` als `... - bestanden` oder `... - nicht bestanden` erhalten, Details stehen in `NOTIZ`.
- `AbschlussAnfrageRepository`: liest `ABSCHLUSS_ANFRAGE`; Anlegen und Statuswechsel laufen ueber das Procedure-Package.
- `PilotRepository`: liest `PILOT`.
- `FlugzeugRepository`: liest `FLUGZEUG`.
- `WartungRepository`: liest `WARTUNG` und `WARTUNG_UND_FLUGZEUG`.
- `AusbildungsStatusRepository`: aggregiert `SCHUELER`, `AUSBILDUNG_VERTRAG` und `PRUEFUNG`.

## Stored Procedures installieren

Die WebApp verwendet im Oracle-Profil das Package `SKYTEAM_WEBAPP_API`. Es ist auf die aktuellen Repository-Vertraege zugeschnitten und kollidiert deshalb nicht mit eventuell bereits vorhandenen gleichnamigen Standalone-Procedures.

Nach dem Basisschema werden die Migrationen in dieser Reihenfolge als Schema-Eigentuemer ausgefuehrt:

```sql
@database/migrations/abschlussanfragen.sql
@database/migrations/stored-procedures.sql
```

Danach muessen Package-Spezifikation und Package-Body gueltig sein:

```sql
select object_name, object_type, status
from user_objects
where object_name = 'SKYTEAM_WEBAPP_API';

select line, position, text
from user_errors
where name = 'SKYTEAM_WEBAPP_API'
order by sequence;
```

Das Package enthaelt bewusst kein `COMMIT` oder `ROLLBACK`. Transaktionsgrenzen bleiben beim JDBC-Aufruf. Oracle-Fehler aus `raise_application_error` werden von der API in verstaendliche HTTP-Fehler (400, 404 oder 409) uebersetzt.

Das vorhandene Basisschema enthaelt keine Sequenzen fuer neue IDs. Die Procedures erzeugen die String-IDs fuer Kurse, Fluege und Pruefungen unter einer Tabellensperre anhand der vorhandenen Praefixe. IDs fuer Schueler, Vertraege und Abschlussanfragen werden weiterhin vom bestehenden Service-Vertrag vorgegeben.

Die Business-Services nutzen ausschliesslich Repository-Interfaces. SQL bleibt in `repository/jdbc`.
