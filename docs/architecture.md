# Architektur

## 4-Schichtenmodell

```text
1. Praesentation
   Frontend-SPA und PHP-View-Layer

2. API-Schicht
   Java REST-Endpunkte, JSON-Antwortformat, Auth-Pruefung, CORS

3. Business-Schicht
   TheorieService, PraxisService, PruefungsService, AusbildungsstatusService

4. Datenzugriff
   Repository-Interfaces mit Oracle-JDBC- und Demo-Implementierung
```

## Frontend-SPA

Die SPA liegt in `frontend/assets/app.js` und wird ueber `frontend/index.php` oder `frontend/index.html` geladen. Sie nutzt keine Frameworks, sondern:

- HTML-Templates in JavaScript
- CSS fuer Layout, Tabs, Tabellen und BPMN-Lane-Schrittleisten
- `fetch()` ueber `frontend/assets/api.js`
- `localStorage` fuer Session-Token, Loginrolle und ausgewaehlten Schueler

Die Navigation erfolgt ohne Seitenneuladung ueber eine zentrale rollenbasierte Menue-Definition in `app.js`.

```text
SCHUELER:
  Main Menu / Dashboard
  Theorie anmelden
  Praxis anmelden
  Pruefung anmelden
  Abschluss anfragen
  Logout

SCHUELERVERWALTUNG:
  Main Menu / Dashboard
  Selbstverwaltung
    Uebersicht
    Schueler anlegen
    Schuelerdaten pruefen
    Ausbildungsvertrag pruefen
    Abschlussanfragen pruefen
  Logout
```

Rollenfremde Menuepunkte werden nicht gerendert. Backend-Endpunkte pruefen die Rolle zusaetzlich serverseitig und liefern bei falscher Rolle `403`.

## BPMN- und Rollenlogik

Die aktualisierte Datei `Ausbildungsverwaltung.bpmn` wird nicht direkt gerendert. Die WebApp bildet ihre fachlichen Lanes im Dashboard als Schrittleiste ab:

```text
Ausbilder              Anmeldung (Webapp)
Schülerverwaltung      Schülerdaten prüfen, Vertrag prüfen, Schüler anlegen, Abbruch
Prüfungsverwaltung     Abschlussanfrage prüfen, Abnahmekriterien prüfen, Abschluss bestätigen
```

Die WebApp kennt zwei Loginrollen: `SCHUELER` fuer den eigenen Ausbildungsprozess und `SCHUELERVERWALTUNG` fuer Verwaltungsfunktionen. Im Demo-Profil besitzt `SC901` bis `SC907` jeweils ein eigenes Schülerkonto (`sc901` bis `sc907`); `demo2/demo2` meldet als Schuelerverwaltung an. `demo/demo` bleibt ausschließlich als nicht empfohlener Legacy-Alias fuer `SC901` erhalten; Präsentation und neue Tests verwenden `sc901/demo901`. Die BPMN-Lane `Pruefungsverwaltung` wird im MVP als fachlicher Bereich der Schuelerverwaltung umgesetzt.

Für die Rolle `SCHUELER` ist die `schuelerId` der serverseitigen Session die verbindliche Identität. Das Frontend verwendet `/api/schueler/me`, `/api/status/me`, `/api/theorie/me`, `/api/praxis/me` und `/api/pruefung/me`; Schüleraktionen senden keine Schüler-ID. Kompatible ID-Routen bleiben serverseitig geschützt und liefern bei einer von der Session abweichenden ID `403`. Nur die Schülerverwaltung wählt über `/api/verwaltung/**` unterschiedliche Schüler aus.

Schueler koennen eigene Theorie-, Praxis- und Pruefungsanmeldungen ausfuehren und eine Abschlussanfrage stellen. Die Schuelerverwaltung kann Schuelerdaten und Vertraege pruefen, Schueler anlegen und Abschlussanfragen bestaetigen oder ablehnen. Die Schuelerverwaltung kann keine Theorie-, Praxis- oder Pruefungsanmeldung als Schueleraktion ausfuehren.

## PHP-View-Layer

`frontend/index.php` hat bewusst keine Business-Logik. Die Datei setzt nur die API-Basis-URL:

```php
$apiBase = getenv('API_BASE_URL') ?: 'http://localhost:8080/api';
```

Damit bleibt PHP ein View-Layer. Die Kommunikation mit dem Backend erfolgt ausschliesslich per JSON/REST.

## Java REST-API

Das Backend nutzt `com.sun.net.httpserver.HttpServer` und startet ueber:

```text
de.skyteam.flightschool.FlightSchoolApplication
```

Zentrale API-Klasse:

```text
backend/src/main/java/de/skyteam/flightschool/api/ApiHandler.java
```

Verantwortung:

- Routing unter `/api`
- Authentifizierung fuer schreibende und geschuetzte Endpunkte
- Request-Parsing
- Aufruf der Business-Services
- einheitliches `ApiResponse`-Format

Fehler werden zentral ueber `ErrorHandler` strukturiert beantwortet:

```text
400 Validierungsfehler
401 Login fehlt
403 falsche Rolle
404 Entitaet nicht gefunden
409 fachlicher Konflikt
500 unerwarteter Fehler
```

## Business-Services

Die Business-Services enthalten die Fachregeln und keine SQL-Statements:

```text
TheorieService
PraxisService
PruefungsService
AusbildungsstatusService
SchuelerService
AuthService
AbschlussService
```

Wichtige Regeln:

- Theoriepruefung ab 10 Theoriestunden.
- Praxispruefung ab 10 Flugstunden.
- Praxisbuchung prueft Fluglehrer, Flugzeug und Wartungsstatus.
- Nicht bestandene Pruefungen erzeugen Wiederholungsbedarf.
- Abschluss nur bei vorhandener Abschlussanfrage und bestandener Theorie sowie Praxis.
- Status `ABGESCHLOSSEN` wird erst nach Bestätigung durch die Schülerverwaltung gesetzt.

## Repository-Schicht

Die Repository-Interfaces kapseln den Datenzugriff:

```text
SchuelerRepository
AusbildungsVertragRepository
KursRepository
FlugRepository
PruefungRepository
PilotRepository
FlugzeugRepository
WartungRepository
AusbildungsStatusRepository
```

Business-Services haengen nur von diesen Interfaces ab. SQL ist vollstaendig in `repository/jdbc` gekapselt.

## Oracle-Modus

Oracle wird ueber `APP_PROFILE=oracle` aktiviert. Die JDBC-Implementierungen lesen mit expliziten SQL-Abfragen aus den bestehenden Tabellen. Fachliche Schreiboperationen rufen ueber `CallableStatement` das Package `SKYTEAM_WEBAPP_API` auf. Dadurch bleiben die Repository-Interfaces und Business-Services unveraendert, waehrend zusammengehoerige Tabellenupdates atomar in Oracle ausgefuehrt werden.

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
ABSCHLUSS_ANFRAGE
```

Das Package wird mit `database/migrations/stored-procedures.sql` installiert. Es wird nur vom Oracle-Profil verwendet; Demo-/InMemory-Repositories bleiben davon unberuehrt.

Konfiguration erfolgt ueber:

```text
DB_URL
DB_USER
DB_PASSWORD
DB_DRIVER
EXTRA_CLASSPATH
```

## Demo-Modus

Der Demo-Modus wird ueber `APP_PROFILE=demo` aktiviert oder automatisch genutzt, wenn kein Profil gesetzt ist. `APP_PROFILE=dev` bleibt als lokaler Alias erhalten.

Eigenschaften:

- keine externe Datenbank
- feste Demo-Daten fuer sieben BPMN-relevante Schuelerfaelle
- Datei-Persistenz fuer manuelle Demo-Aenderungen
- lokale Persistenz unter `backend/target/dev-data/flight-school-demo.properties` oder im Docker-Volume `backend-demo-data`

Die Demo-Implementierungen liegen unter:

```text
backend/src/main/java/de/skyteam/flightschool/repository/memory
```
