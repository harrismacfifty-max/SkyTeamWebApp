# SkyTeam Flight School MVP

## Kurzbeschreibung

SkyTeam Flight School ist eine browserbasierte Single-Page-Application zur Verwaltung eines vereinfachten Flugschulprozesses. Der MVP bildet Schuelerauswahl, Theorieausbildung, Praxisausbildung, Pruefungen, Abschluss und eine BPMN-nahe Prozessanzeige ab.

Die Anwendung ist fuer eine Vorfuehrung ohne Oracle-Datenbank vorbereitet. Im Dev-Modus nutzt das Backend feste Demo-Daten und speichert manuelle Demo-Aenderungen lokal in einer Datei.

## Architekturuebersicht

```text
Frontend SPA      HTML/CSS/Vanilla JS, fetch(), Tabs und Prozessanzeige
PHP View-Layer    frontend/index.php setzt nur die API-Basis-URL
Java REST-API     HTTP-Server mit JSON-Endpunkten unter /api
Business-Services Theorie, Praxis, Pruefung, Ausbildungsstatus
Repository-Layer  Oracle-JDBC oder Dev-In-Memory mit Datei-Persistenz
```

Details stehen in [docs/architecture.md](docs/architecture.md).

## Verwendete Technologien

- HTML, CSS, Vanilla JavaScript
- PHP nur als einfacher View-Layer fuer `index.php`
- Java 17+ REST-Backend mit `com.sun.net.httpserver.HttpServer`
- JSON ueber REST
- Oracle-kompatible JDBC-Repository-Schicht
- Dev-/Demo-Modus ohne externe Datenbank
- PowerShell-Launcher fuer lokale Vorfuehrung

## Schnellstart

Am einfachsten aus dem Projektverzeichnis:

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
.\start-skyteam.ps1
```

Der Launcher prueft Port `8080`, startet das Backend bei Bedarf im Dev-Modus und oeffnet das Frontend. Wenn PHP nicht installiert ist, wird automatisch `frontend/index.html` genutzt.

Desktop-Shortcut:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\create-desktop-icon.ps1
powershell -ExecutionPolicy Bypass -File .\create-desktop-shortcut.ps1
```

## Start Backend

Dev-/Demo-Modus:

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
$env:APP_PROFILE = "dev"
powershell -ExecutionPolicy Bypass -File .\backend\run.ps1
```

Healthcheck:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Tests:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

## Start Frontend

Mit PHP:

```powershell
cd C:\Users\pasca\Desktop\SkyTeamWebApp
php -S localhost:8000 -t frontend
```

Oeffnen:

```text
http://localhost:8000
```

Ohne PHP:

```powershell
Start-Process .\frontend\index.html
```

## Demo-Login

```text
Benutzername: demo
Passwort: demo
```

Das Login ist bewusst einfach gehalten. Es gibt keine Rollenpruefung und keine externe Authentifizierung.

## Demo-Modus

Der Demo-Modus ist Standard, wenn `APP_PROFILE` fehlt oder `dev` ist. Er benoetigt keine Oracle-Verbindung.

Enthaltene Demo-Faelle:

```text
SC901  Laufende Ausbildung, Mindeststunden noch offen
SC902  Genug Theoriestunden, Theoriepruefung offen
SC903  Genug Flugstunden, Praxispruefung offen
SC904  Theoriepruefung nicht bestanden
SC905  Praxispruefung nicht bestanden
SC906  Theorie und Praxis bestanden, bereit fuer Abschluss
SC907  Ausbildung abgeschlossen
```

Demo-Daten enthalten Ausbildungsvertraege, Theoriekurse, Flugstunden, Pruefungen, Piloten, Flugzeuge und Wartungsstatus.

Manuelle Demo-Aenderungen, z.B. neue Schueler oder gebuchte/stornierte Flugstunden, werden gespeichert unter:

```text
backend/target/dev-data/flight-school-demo.properties
```

Reset auf Ausgangsdaten:

```powershell
Remove-Item .\backend\target\dev-data\flight-school-demo.properties
```

Danach Backend neu starten.

## Oracle-Konfiguration

Oracle wird ueber Umgebungsvariablen aktiviert:

```powershell
$env:APP_PROFILE = "oracle"
$env:DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:DB_USER = "skyteam"
$env:DB_PASSWORD = "<passwort>"
$env:DB_DRIVER = "oracle.jdbc.OracleDriver"
$env:EXTRA_CLASSPATH = "C:\pfad\zu\ojdbc.jar"
powershell -ExecutionPolicy Bypass -File .\backend\run.ps1
```

Im Produktiv-/Oracle-Profil werden keine Zugangsdaten im Code erwartet. `DB_PASSWORD` kommt aus der Umgebung. Die Repository-Schicht nutzt die bestehenden Tabellen:

```text
SCHUELER
AUSBILDUNG_VERTRAG
KURSE
FLUG
PRUEFUNG
PILOT
FLUGZEUG
WARTUNG
SCHUELER_UND_PILOT
FLUG_UND_PILOT
WARTUNG_UND_FLUGZEUG
```

## Wichtigste Endpunkte

```text
GET    /api/health
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me

GET    /api/schueler
POST   /api/schueler
GET    /api/schueler/{id}
DELETE /api/schueler/{id}

GET    /api/status/{schuelerId}/gesamt
GET    /api/theorie/{schuelerId}
POST   /api/theorie/buchen
GET    /api/praxis/{schuelerId}
POST   /api/praxis/buchen
POST   /api/praxis/stornieren

GET    /api/pruefung/{schuelerId}
POST   /api/pruefung/theorie/anmelden
POST   /api/pruefung/praxis/anmelden
POST   /api/pruefung/ergebnis
POST   /api/ausbildung/{schuelerId}/abschliessen
```

Alle Antworten nutzen:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "errors": []
}
```

Weitere Beispiele stehen in [docs/api.md](docs/api.md) und [docs/smoke-test.md](docs/smoke-test.md).

## Vorfuehrung

5-Minuten-Ablauf:

```text
docs/demo-script.md
```

BPMN-Zuordnung:

```text
docs/bpmn-mapping.md
```

## Bekannte Einschraenkungen

- MVP-Login mit Demo-Benutzer, keine Rollen, kein JWT, keine externe Authentifizierung.
- Keine E-Mail, keine Zahlung, keine externen Systeme.
- Dev-Modus ist dateibasiert und nicht fuer parallele Mehrbenutzer-Szenarien gedacht.
- PHP ist optional; ohne PHP wird `frontend/index.html` direkt geoeffnet.
- Oracle-Modus ist vorbereitet, benoetigt aber einen passenden Oracle JDBC-Treiber und ein zur vorhandenen SQL-Struktur passendes Schema.
- BPMN wird bewusst als HTML/CSS-Schrittleiste visualisiert, nicht mit einer BPMN-Rendering-Library.

## Projektstruktur

```text
backend/    Java REST-API, Services, Repositories, Tests
frontend/   index.php, index.html, assets/app.js, api.js, styles.css
database/   Oracle-kompatible SQL-Skripte
docs/       API, Architektur, BPMN-Mapping, Demo-Skript, Smoke-Test
assets/     Desktop-Icon
```
