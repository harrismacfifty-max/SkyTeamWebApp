# SkyTeam Flight School WebApp

## Kurzbeschreibung

SkyTeam Flight School ist eine browserbasierte Single-Page-Application zur Verwaltung eines vereinfachten Flugschulprozesses. Der MVP bildet die Rollen `Schüler` und `Schülerverwaltung`, Theorie-/Praxisanmeldungen, Prüfungsanmeldungen, Abschlussanfragen und eine BPMN-nahe Prozessanzeige nach den Lanes `Ausbilder`, `Schülerverwaltung` und `Prüfungsverwaltung` ab.

Die Anwendung kann ohne Oracle-Datenbank im Demo-Modus vorgefuehrt werden. Manuelle Demo-Aenderungen wie neue Schueler oder gebuchte/stornierte Theorie- und Praxisstunden werden im Docker-Compose-Start in einem lokalen Docker-Volume gespeichert.

## Architektur

- Frontend-SPA mit HTML, CSS, Vanilla JavaScript und `fetch()`
- PHP-View-Layer ueber `./frontend/index.php`
- Java REST-API unter `/api`
- Business-Services fuer Theorie, Praxis, Pruefung und Ausbildungsstatus
- Repository-Schicht mit Oracle-JDBC oder Demo-Modus
- Oracle-Datenbank oder dateibasierter Demo-Modus

Weitere Details stehen in [docs/architecture.md](docs/architecture.md).

## Voraussetzungen

- Git
- Docker Desktop oder Docker Engine mit Docker Compose
- optional Java JDK 17 oder neuer fuer lokalen Start ohne Docker
- optional PHP fuer lokalen Start ohne Docker
- optional Oracle-Zugang und Oracle JDBC-Treiber

## Start im Demo-Modus

Der empfohlene Startweg ist auf Windows, macOS und Linux gleich:

```bash
cd <projektverzeichnis>
docker compose up --build
```

Danach sind erreichbar:

```text
Frontend: http://localhost:8081
Backend:  http://localhost:8080/api/health
```

Stoppen:

```bash
docker compose down
```

## Demo-Daten vor einer Präsentation zurücksetzen

Den Reset bewusst im Projektverzeichnis ausführen. Der direkte Docker-Ablauf lautet:

```bash
docker compose down -v
docker compose up --build
```

Unter Windows steht dafür ein PowerShell-Skript bereit:

```powershell
.\scripts\reset-demo.ps1
```

Unter macOS und Linux kann das Bash-Skript verwendet werden:

```bash
chmod +x scripts/reset-demo.sh
./scripts/reset-demo.sh
```

Der Reset löscht alle im Demo-Modus angelegten oder geänderten Schüler, Buchungen, Prüfungsanmeldungen und Abschlussbestätigungen. Er entfernt nur das lokale Docker-Compose-Volume des Demo-Modus; externe Oracle-Daten werden nicht gelöscht. Nach dem Neustart müssen der Health-Endpoint sowie die Logins `sc901/demo901` und `demo2/demo2` getestet werden.

## Start mit Oracle

Oracle ist optional und wird nicht als Container erzwungen. Fuer eine externe Oracle-Datenbank werden Umgebungsvariablen genutzt.
Details stehen in [docs/oracle-setup.md](docs/oracle-setup.md). Im Oracle-Profil werden Schreiboperationen ueber das Package `SKYTEAM_WEBAPP_API` ausgefuehrt. Nach dem Basisschema sind dafuer einmalig `database/migrations/abschlussanfragen.sql` und danach `database/migrations/stored-procedures.sql` zu installieren.

Beispiel mit Shell-Variablen:

```bash
cd <projektverzeichnis>
APP_PROFILE=oracle \
DB_URL="jdbc:oracle:thin:@//host.docker.internal:1521/XEPDB1" \
DB_USER="SKYTEAM" \
DB_PASSWORD="<passwort>" \
docker compose up --build
```

Alternativ eine lokale `.env` aus der Vorlage anlegen:

```bash
cp .env.example .env
```

Dann `.env` lokal befuellen:

```text
APP_PROFILE=oracle
DB_URL=jdbc:oracle:thin:@//host.docker.internal:1521/XEPDB1
DB_USER=SKYTEAM
DB_PASSWORD=<passwort>
```

Start:

```bash
docker compose up --build
```

Falls ein Oracle JDBC-Treiber benoetigt wird, muss er zusaetzlich ins Backend-Image oder per separatem Mount in den Container gebracht und ueber `EXTRA_CLASSPATH` referenziert werden. Echte Zugangsdaten gehoeren nur in die lokale `.env`, nicht ins Repository.

## Start ueber Docker Compose

Die YAML-Konfiguration liegt in `./compose.yaml`.

Services:

```text
backend   build: ./backend, Port 8080
frontend  build: ./frontend, Port 8081 -> Container-Port 80
```

Standardstart:

```bash
docker compose up --build
```

Stoppen:

```bash
docker compose down
```

## Lokaler Start ohne Docker

Docker Compose bleibt der bevorzugte Weg. Ohne Docker stehen plattformbezogene Startskripte unter `./scripts` bereit.

Windows PowerShell:

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1
```

macOS/Linux Terminal:

```bash
cd <projektverzeichnis>
chmod +x scripts/start.sh
./scripts/start.sh
```

Die lokalen Skripte starten das Backend im Demo-Modus auf Port `8080` und das Frontend, falls PHP installiert ist, auf Port `8000`. Ohne PHP wird `./frontend/index.html` als Fallback geoeffnet.

Die Skripte koennen Docker Compose auch explizit ausloesen:

```bash
cd <projektverzeichnis>
./scripts/start.sh --docker
```

```powershell
cd <projektverzeichnis>
.\scripts\start.ps1 -Docker
```

## Projektstruktur

```text
./compose.yaml       Docker-Compose-Start fuer Backend und Frontend
./backend            Java REST-API, Services, Repositories, Tests, Dockerfile
./frontend           index.php, index.html, assets, Dockerfile
./database           Oracle-kompatible SQL-Skripte
./docs               API, Architektur, BPMN-Mapping, Demo-Skript, Smoke-Test
./scripts            Plattformneutrale lokale Startskripte
./assets             Desktop-Icon-Dateien
./tools              Hilfsskripte fuer Desktop-Icons
./.env.example       Vorlage fuer lokale Umgebungsvariablen
```

## Logo austauschen

Das WebApp-Logo liegt unter `./frontend/assets/logo.png` und wird oben links im Header rund dargestellt. Zum Austauschen die Datei durch ein neues PNG ersetzen; das Bild sollte quadratisch sein, damit es im Kreis nicht verzerrt wirkt. Das Favicon liegt optional unter `./frontend/assets/favicon.ico`.

## Demo-Login

Die Demo-Schüler besitzen jeweils ein eigenes Konto. Das Verwaltungskonto ist in derselben Übersicht aufgeführt:

| Account | Passwort | Schüler | Zustand |
|---|---|---|---|
| `sc901` | `demo901` | `SC901` | laufende Ausbildung, Mindeststunden fehlen |
| `sc902` | `demo902` | `SC902` | Theorieprüfung freigeschaltet |
| `sc903` | `demo903` | `SC903` | Praxisprüfung freigeschaltet |
| `sc904` | `demo904` | `SC904` | Theorieprüfung nicht bestanden |
| `sc905` | `demo905` | `SC905` | Praxisprüfung nicht bestanden |
| `sc906` | `demo906` | `SC906` | Theorie und Praxis bestanden, Abschluss angefragt |
| `sc907` | `demo907` | `SC907` | Ausbildung abgeschlossen |
| `demo2` | `demo2` | Schülerverwaltung | Verwaltungsfunktionen |

Jedes Konto `sc901` bis `sc907` ist serverseitig fest mit genau dem gleichnamigen Schülerdatensatz `SC901` bis `SC907` verknüpft. Schüler können ausschließlich die eigene Ausbildung laden und verändern; manipulierte Zugriffe auf andere Schüler werden mit HTTP `403` abgewiesen. `demo2` ist kein Schülerkonto, besitzt keine Schüler-ID und verwendet ausschließlich die Rolle `SCHUELERVERWALTUNG`.

Der frühere Zugang `demo` / `demo` bleibt nur als ausdrücklich dokumentierter Legacy-Alias für `SC901` erhalten. Für Präsentation, Dokumentation und neue Tests wird stattdessen `sc901` / `demo901` verwendet.

Das Login ist bewusst einfach gehalten. Es gibt keine externe Authentifizierung und kein JWT. Rollen werden serverseitig über die InMemory-Session geprüft. Die Konten werden nur in den Profilen `demo` und `dev` aktiviert; im Profil `oracle` sind sie nicht verfügbar.

## Rollen und Menüs

Schüler, beispielsweise `sc901/demo901`, sehen nur Funktionen für ihren jeweils zugeordneten Ausbildungsprozess:

- Main Menu / Dashboard
- Theorie anmelden
- Praxis anmelden
- Prüfung anmelden
- Abschluss anfragen
- Logout

Schüler dürfen eigene Theorieeinheiten, Praxiseinheiten und Prüfungen anmelden. Sie können eine Abschlussanfrage stellen und deren Status sehen. Sie können keine Schüler anlegen, keine Schülerdaten anderer Personen verwalten und keinen Abschluss selbst bestätigen.

Die Schüler-ID wird für Schülerzugriffe verbindlich aus der serverseitigen Session übernommen. Das Frontend verwendet dafür `/api/schueler/me`, `/api/status/me`, `/api/theorie/me`, `/api/praxis/me` und `/api/pruefung/me` und sendet bei Schüleraktionen keine Schüler-ID. Manipulierte fremde IDs in Pfad, Query oder JSON werden serverseitig mit HTTP `403` abgewiesen.

Schülerverwaltung (`demo2/demo2`) sieht nur Verwaltungsfunktionen:

- Main Menu / Dashboard
- Selbstverwaltung
- Schüler anlegen
- Schülerdaten prüfen
- Ausbildungsvertrag prüfen
- Abschlussanfragen prüfen/bestätigen
- Logout

Die Schülerverwaltung kann keine Theorie-, Praxis- oder Prüfungsanmeldung als Schüleraktion ausführen. Eine Abschlussanfrage wird durch den Schüler gestellt und wird erst nach Prüfung und Bestätigung durch die Schülerverwaltung wirksam.

## BPMN-Bezug

Die aktuelle BPMN-Version `Ausbildungsverwaltung.bpmn` wird im Dashboard als einfache Prozessanzeige abgebildet. Relevante BPMN-Punkte:

- Anmeldung (Webapp)
- Schülerdaten prüfen
- Ausbildungsvertrag prüfen
- Schüler anlegen
- Beantragung des Schülers vorhanden?
- Abnahmekriterien Theorie überprüfen
- Abnahmekriterien Praxis überprüfen
- Schüler Abschluss bestätigen

Details stehen in [docs/bpmn-mapping.md](docs/bpmn-mapping.md).

## Demo-Modus

`APP_PROFILE=demo` nutzt feste Beispieldaten und benoetigt keine Oracle-Verbindung. `APP_PROFILE=dev` wird aus Kompatibilitaetsgruenden weiterhin als Alias akzeptiert.

Enthaltene Demo-Faelle:

```text
SC901  Laufende Ausbildung, Mindeststunden noch offen
SC902  Genug Theoriestunden, Theoriepruefung offen
SC903  Genug Flugstunden, Praxispruefung offen
SC904  Theoriepruefung nicht bestanden
SC905  Praxispruefung nicht bestanden
SC906  Theorie und Praxis bestanden, Abschluss angefragt
SC907  Ausbildung abgeschlossen
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

GET    /api/verwaltung/schueler
GET    /api/verwaltung/schueler/{id}
POST   /api/verwaltung/schueler
GET    /api/verwaltung/schueler/{id}/vertrag
POST   /api/verwaltung/schueler/{id}/vertrag/pruefen

GET    /api/status/{schuelerId}/gesamt
GET    /api/theorie/{schuelerId}
POST   /api/theorie/buchen
POST   /api/theorie/stornieren
GET    /api/praxis/{schuelerId}
POST   /api/praxis/buchen
POST   /api/praxis/stornieren

GET    /api/pruefung/{schuelerId}
POST   /api/pruefung/theorie/anmelden
POST   /api/pruefung/praxis/anmelden
POST   /api/pruefung/ergebnis

GET    /api/abschluss/meine-anfrage
POST   /api/abschluss/anfragen
GET    /api/verwaltung/abschlussanfragen
GET    /api/verwaltung/abschlussanfragen/alle
GET    /api/verwaltung/abschlussanfragen/{id}
POST   /api/verwaltung/abschlussanfragen/{id}/bestaetigen
POST   /api/verwaltung/abschlussanfragen/{id}/ablehnen
```

Alle Antworten nutzen das einheitliche `ApiResponse`-Format:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "errors": []
}
```

Weitere Beispiele stehen in [docs/api.md](docs/api.md) und [docs/smoke-test.md](docs/smoke-test.md).
`GET /api/health` zeigt zusaetzlich `activeProfile`, `databaseMode`, `databaseReachable` und `timestamp`.

## Qualitaetspruefung

Die Startskripte pruefen automatisch, ob Textdateien typische falsch dekodierte UTF-8-Zeichenfolgen enthalten. Manuell kann die Pruefung so gestartet werden:

Windows PowerShell:

```powershell
cd <projektverzeichnis>
.\scripts\check-encoding.ps1
```

macOS/Linux Terminal:

```bash
cd <projektverzeichnis>
chmod +x scripts/check-encoding.sh
./scripts/check-encoding.sh
```

## Bekannte Einschraenkungen

- MVP-Login mit acht sichtbaren Demo-Konten und einem Legacy-Alias, einfache Rollenpruefung, kein JWT, keine externe Authentifizierung.
- Keine E-Mail, keine Zahlung, keine externen Systeme.
- Demo-Modus ist dateibasiert und nicht fuer parallele Mehrbenutzer-Szenarien gedacht.
- Oracle-Modus ist vorbereitet, benoetigt aber einen passenden Oracle JDBC-Treiber und ein zur SQL-Struktur passendes Schema.
- BPMN wird bewusst als HTML/CSS-Schrittleiste visualisiert, nicht mit einer BPMN-Rendering-Library.
