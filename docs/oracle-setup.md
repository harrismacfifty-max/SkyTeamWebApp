# Oracle-Setup

Die WebApp startet standardmaessig im Demo-Modus und benoetigt dann keine Datenbank. Fuer den Betrieb gegen eine echte Oracle-Datenbank wird das Profil `oracle` ueber Umgebungsvariablen aktiviert.

## Benoetigte ENV-Variablen

```text
APP_PROFILE=oracle
DB_URL=jdbc:oracle:thin:@//<host>:<port>/<service>
DB_USER=<username>
DB_PASSWORD=<password>
```

Optional:

```text
DB_DRIVER=oracle.jdbc.OracleDriver
EXTRA_CLASSPATH=./lib/ojdbc.jar
```

`DB_DRIVER` und `EXTRA_CLASSPATH` sind nur noetig, wenn der Oracle JDBC-Treiber nicht bereits im Klassenpfad bzw. im Container-Image verfuegbar ist.

## Windows PowerShell

```powershell
cd <projektverzeichnis>
$env:APP_PROFILE = "oracle"
$env:DB_URL = "jdbc:oracle:thin:@//<host>:<port>/<service>"
$env:DB_USER = "<username>"
$env:DB_PASSWORD = "<password>"
$env:DB_DRIVER = "oracle.jdbc.OracleDriver"
$env:EXTRA_CLASSPATH = ".\lib\ojdbc.jar"
.\scripts\start.ps1
```

## macOS/Linux Terminal

```bash
cd <projektverzeichnis>
export APP_PROFILE=oracle
export DB_URL="jdbc:oracle:thin:@//<host>:<port>/<service>"
export DB_USER="<username>"
export DB_PASSWORD="<password>"
export DB_DRIVER="oracle.jdbc.OracleDriver"
export EXTRA_CLASSPATH="./lib/ojdbc.jar"
chmod +x scripts/start.sh
./scripts/start.sh
```

## Docker Compose .env

Lokale Datei aus Vorlage erzeugen:

```bash
cp .env.example .env
```

Beispielinhalt:

```text
APP_PROFILE=oracle
DB_URL=jdbc:oracle:thin:@//<host>:<port>/<service>
DB_USER=<username>
DB_PASSWORD=<password>
```

Start:

```bash
docker compose up --build
```

Wenn der Oracle JDBC-Treiber nicht im Backend-Image enthalten ist, muss er separat bereitgestellt und ueber `EXTRA_CLASSPATH` eingebunden werden.

## Healthcheck

```text
GET /api/health
```

Beispielantwort im Oracle-Modus:

```json
{
  "success": true,
  "message": "API erreichbar.",
  "data": {
    "status": "ok",
    "activeProfile": "oracle",
    "databaseMode": "oracle",
    "databaseReachable": true,
    "timestamp": "2026-06-22T10:30:00+02:00"
  },
  "errors": []
}
```

Wenn `APP_PROFILE=oracle` gesetzt ist, aber `DB_URL`, `DB_USER` oder `DB_PASSWORD` fehlen, bricht das Backend beim Start mit einer klaren Fehlermeldung ab.

## Sicherheit

Keine echten Passwoerter committen. Zugangsdaten gehoeren nur in lokale Umgebungsvariablen oder in eine lokale `.env`, die durch `.gitignore` vom Repository ausgeschlossen ist.
