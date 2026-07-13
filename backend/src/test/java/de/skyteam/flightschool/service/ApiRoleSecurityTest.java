package de.skyteam.flightschool.service;

import com.sun.net.httpserver.HttpServer;
import de.skyteam.flightschool.api.ApiHandler;
import de.skyteam.flightschool.config.ApplicationConfig;
import de.skyteam.flightschool.repository.memory.InMemoryAircraftRepository;
import de.skyteam.flightschool.repository.memory.InMemoryLessonRepository;
import de.skyteam.flightschool.repository.memory.InMemoryStudentRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ApiRoleSecurityTest {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final String[][] STUDENT_ACCOUNTS = {
            {"sc901", "demo901", "SC901", "Jonas Keller"},
            {"sc902", "demo902", "SC902", "Nico Berger"},
            {"sc903", "demo903", "SC903", "Mina Sommer"},
            {"sc904", "demo904", "SC904", "Mara Seidel"},
            {"sc905", "demo905", "SC905", "Lea Wagner"},
            {"sc906", "demo906", "SC906", "Oskar Lange"},
            {"sc907", "demo907", "SC907", "Ella Hartmann"}
    };

    private ApiRoleSecurityTest() {
    }

    static void run(TestRunner runner) {
        for (String[] account : STUDENT_ACCOUNTS) {
            runner.test("API-Login " + account[0] + " liefert " + account[2], () -> {
                try (TestContext context = TestContext.create();
                     ApiTestServer server = ApiTestServer.start(context)) {
                    ApiResult login = server.post("/auth/login", "", "{\"username\":\"" + account[0] + "\",\"password\":\"" + account[1] + "\"}");
                    runner.assertEquals(200, login.status(), account[0] + " Login muss erfolgreich sein.");
                    runner.assertTrue(login.body().contains("\"username\":\"" + account[0] + "\""), "Login muss Username liefern.");
                    runner.assertTrue(login.body().contains("\"displayName\":\"" + account[3] + "\""), "Login muss Anzeigenamen aus Demo-Daten liefern.");
                    runner.assertTrue(login.body().contains("\"role\":\"SCHUELER\""), "Login muss SCHUELER liefern.");
                    runner.assertTrue(login.body().contains("\"schuelerId\":\"" + account[2] + "\""), "Login muss richtige Schueler-ID liefern.");

                    String token = extractJsonString(login.body(), "token");
                    ApiResult me = server.get("/auth/me", token);
                    runner.assertEquals(200, me.status(), "/auth/me muss fuer " + account[0] + " erfolgreich sein.");
                    runner.assertTrue(me.body().contains("\"authenticated\":true"), "/auth/me muss Authentifizierung bestaetigen.");
                    runner.assertTrue(me.body().contains("\"username\":\"" + account[0] + "\""), "/auth/me muss Username liefern.");
                    runner.assertTrue(me.body().contains("\"displayName\":\"" + account[3] + "\""), "/auth/me muss Anzeigenamen liefern.");
                    runner.assertTrue(me.body().contains("\"role\":\"SCHUELER\""), "/auth/me muss SCHUELER liefern.");
                    runner.assertTrue(me.body().contains("\"schuelerId\":\"" + account[2] + "\""), "/auth/me muss richtige Schueler-ID liefern.");
                }
            });
        }

        runner.test("API-Login demo2 liefert Schuelerverwaltung ohne Schueler-ID", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                ApiResult managementLogin = server.post("/auth/login", "", """
                        {"username":"demo2","password":"demo2"}
                        """);
                runner.assertEquals(200, managementLogin.status(), "demo2 Login muss erfolgreich sein.");
                runner.assertTrue(managementLogin.body().contains("\"role\":\"SCHUELERVERWALTUNG\""), "demo2 muss SCHUELERVERWALTUNG liefern.");
                runner.assertTrue(managementLogin.body().contains("\"schuelerId\":null"), "demo2 darf keine Schueler-ID haben.");

                ApiResult managementMe = server.get("/auth/me", extractJsonString(managementLogin.body(), "token"));
                runner.assertEquals(200, managementMe.status(), "/auth/me muss fuer demo2 erfolgreich sein.");
                runner.assertTrue(managementMe.body().contains("\"authenticated\":true"), "/auth/me muss Authentifizierung bestaetigen.");
                runner.assertTrue(managementMe.body().contains("\"role\":\"SCHUELERVERWALTUNG\""), "/auth/me muss Verwaltungsrolle liefern.");
                runner.assertTrue(managementMe.body().contains("\"schuelerId\":null"), "/auth/me darf fuer Verwaltung keine Schueler-ID liefern.");
            }
        });

        runner.test("API-Login behaelt dokumentierten Legacy-Alias", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                ApiResult legacyLogin = server.post("/auth/login", "", """
                        {"username":"demo","password":"demo"}
                        """);
                runner.assertEquals(200, legacyLogin.status(), "Dokumentierter Legacy-Alias muss funktionieren.");
                runner.assertTrue(legacyLogin.body().contains("\"schuelerId\":\"SC901\""), "Legacy-Alias muss eindeutig SC901 zugeordnet sein.");
            }
        });

        runner.test("API-Login lehnt falsches Passwort und unbekannten Benutzer ab", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                ApiResult wrongPassword = server.post("/auth/login", "", """
                        {"username":"sc901","password":"falsch"}
                        """);
                ApiResult unknownUser = server.post("/auth/login", "", """
                        {"username":"unbekannt","password":"demo901"}
                        """);
                runner.assertEquals(401, wrongPassword.status(), "Falsches Passwort muss HTTP 401 liefern.");
                runner.assertEquals(401, unknownUser.status(), "Unbekannter Benutzer muss HTTP 401 liefern.");
                runner.assertTrue(wrongPassword.body().contains("Ungueltige Zugangsdaten."), "401 muss verstaendliche Sammelmeldung liefern.");
                runner.assertTrue(unknownUser.body().contains("Ungueltige Zugangsdaten."), "401 darf keinen Benutzernamen-Hinweis liefern.");
            }
        });

        runner.test("API-me-Endpunkte verwenden fuer sc901 und sc906 nur die Session-Zuordnung", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String[][] accounts = {
                        {"sc901", "demo901", "SC901", "SC902"},
                        {"sc906", "demo906", "SC906", "SC907"}
                };
                String[] ownDataPaths = {
                        "/schueler/me",
                        "/status/me",
                        "/theorie/me",
                        "/praxis/me",
                        "/abschluss/meine-anfrage"
                };

                for (String[] account : accounts) {
                    String token = server.login(account[0], account[1]);
                    for (String path : ownDataPaths) {
                        ApiResult result = server.get(path, token);
                        String idField = "/schueler/me".equals(path) ? "id" : "schuelerId";
                        runner.assertEquals(200, result.status(), path + " muss fuer " + account[0] + " erfolgreich sein.");
                        runner.assertTrue(result.body().contains("\"" + idField + "\":\"" + account[2] + "\""), path + " muss eigene Schueler-ID liefern.");
                        runner.assertFalse(result.body().contains("\"" + idField + "\":\"" + account[3] + "\""), path + " darf keine fremde Schueler-ID liefern.");
                    }

                    ApiResult exams = server.get("/pruefung/me", token);
                    runner.assertEquals(200, exams.status(), "/pruefung/me muss fuer " + account[0] + " erfolgreich sein.");
                    runner.assertFalse(exams.body().contains("\"schuelerId\":\"" + account[3] + "\""), "/pruefung/me darf keine fremden Pruefungen liefern.");

                    ApiResult ownBooking = server.post("/theorie/buchen", token, """
                            {"thema":"Sessiongebundene Buchung","termin":"2026-12-10","dauerMinuten":60,"dozent":"Test"}
                            """);
                    runner.assertEquals(201, ownBooking.status(), "Buchung ohne Schueler-ID muss fuer " + account[0] + " erfolgreich sein.");
                    runner.assertTrue(ownBooking.body().contains("\"schuelerId\":\"" + account[2] + "\""), "Buchung muss Session-Schueler-ID verwenden.");
                    runner.assertFalse(ownBooking.body().contains("\"schuelerId\":\"" + account[3] + "\""), "Buchung darf keine fremde Schueler-ID verwenden.");
                }
            }
        });

        runner.test("API blockiert manipulierte Pfad Query und JSON-Schueler-IDs", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String sc901Token = server.login("sc901", "demo901");
                String sc906Token = server.login("sc906", "demo906");

                runner.assertEquals(200, server.get("/schueler/SC901", sc901Token).status(), "sc901 muss den eigenen Datensatz SC901 laden duerfen.");
                runner.assertEquals(200, server.get("/schueler/SC906", sc906Token).status(), "sc906 muss den eigenen Datensatz SC906 laden duerfen.");

                String[] sc901ForeignPaths = {
                        "/schueler/SC902",
                        "/status/SC902",
                        "/status/SC902/gesamt",
                        "/theorie/SC902",
                        "/praxis/SC902",
                        "/pruefung/SC902",
                        "/schueler/SC902/theorie",
                        "/schueler/SC902/praxis",
                        "/schueler/SC902/status"
                };
                for (String path : sc901ForeignPaths) {
                    runner.assertEquals(403, server.get(path, sc901Token).status(), "sc901 muss fuer fremden Pfad " + path + " HTTP 403 erhalten.");
                }
                runner.assertEquals(403, server.get("/schueler/SC907", sc906Token).status(), "sc906 darf SC907 nicht laden.");
                runner.assertEquals(403, server.get("/status/SC907/gesamt", sc906Token).status(), "sc906 darf SC907 nicht ueber Status-Pfad laden.");
                runner.assertEquals(403, server.get("/status/me?schuelerId=SC902", sc901Token).status(), "Fremde schuelerId im Query muss HTTP 403 liefern.");
                runner.assertEquals(403, server.get("/schueler/me?studentId=SC902", sc901Token).status(), "Fremde studentId im Query muss HTTP 403 liefern.");

                runner.assertEquals(403, server.post("/theorie/buchen", sc901Token, """
                        {"schuelerId":"SC902","thema":"Manipuliert","termin":"2026-12-01","dauerMinuten":60,"dozent":"Test"}
                        """).status(), "Fremde schuelerId in Theoriebuchung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/theorie/buchen", sc901Token, """
                        {"studentId":"SC902","thema":"Manipuliert","termin":"2026-12-01","dauerMinuten":60,"dozent":"Test"}
                        """).status(), "Fremde studentId in Theoriebuchung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/praxis/buchen", sc901Token, """
                        {"schuelerId":"SC902","flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-12-02T08:00","dauerMinuten":60,"ausbildungsinhalt":"Manipuliert"}
                        """).status(), "Fremde schuelerId in Praxisbuchung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/pruefung/theorie/anmelden", sc901Token, """
                        {"schuelerId":"SC902","wunschtermin":"2026-12-03"}
                        """).status(), "Fremde schuelerId in Theoriepruefung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/pruefung/praxis/anmelden", sc901Token, """
                        {"schuelerId":"SC902","wunschtermin":"2026-12-04"}
                        """).status(), "Fremde schuelerId in Praxispruefung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/theorie/stornieren", sc901Token, """
                        {"schuelerId":"SC902","kursId":"KTB903"}
                        """).status(), "Fremde schuelerId in Theoriestornierung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/praxis/stornieren", sc901Token, """
                        {"schuelerId":"SC902","flugId":"FL903"}
                        """).status(), "Fremde schuelerId in Praxisstornierung muss HTTP 403 liefern.");
                runner.assertEquals(403, server.post("/abschluss/anfragen", sc901Token, """
                        {"schuelerId":"SC906"}
                        """).status(), "Fremde schuelerId in Abschlussanfrage muss HTTP 403 liefern.");
                runner.assertEquals(403, server.get("/verwaltung/abschlussanfragen/AA906", sc901Token).status(), "Schueler darf fremde Abschlussanfrage nicht oeffnen.");

                ApiResult ownTheory = server.get("/theorie/me", sc901Token);
                runner.assertFalse(ownTheory.body().contains("Manipuliert"), "Blockierte Fremdbuchung darf keine Daten anlegen.");
            }
        });

        runner.test("API-Schuelerrechte erlauben eigene Buchungen und sperren Verwaltung", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc901", "demo901");

                runner.assertEquals(201, server.post("/theorie/buchen", token, """
                        {"thema":"API Theorie","termin":"2026-11-01","dauerMinuten":600,"dozent":"Test"}
                        """).status(), "Schueler muss eigene Theorie buchen duerfen.");
                runner.assertEquals(201, server.post("/praxis/buchen", token, """
                        {"flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-11-02T08:00","dauerMinuten":600,"ausbildungsinhalt":"API Praxis"}
                        """).status(), "Schueler muss eigene Praxis buchen duerfen.");
                runner.assertEquals(201, server.post("/pruefung/theorie/anmelden", token, """
                        {"pruefungsart":"Theoriepruefung","wunschtermin":"2026-11-10","pruefer":"P001"}
                        """).status(), "Schueler muss eigene Theoriepruefung anmelden duerfen.");
                runner.assertEquals(201, server.post("/pruefung/praxis/anmelden", token, """
                        {"pruefungsart":"Praxispruefung","wunschtermin":"2026-11-11","pruefer":"P002"}
                        """).status(), "Schueler muss eigene Praxispruefung anmelden duerfen.");
                runner.assertEquals(201, server.post("/abschluss/anfragen", token, "{}").status(), "Schueler muss Abschluss anfragen duerfen.");

                runner.assertEquals(403, server.post("/verwaltung/schueler", token, """
                        {"vorname":"Nicht","name":"Erlaubt"}
                        """).status(), "Schueler darf keinen Schueler anlegen.");
                runner.assertEquals(403, server.post("/verwaltung/abschlussanfragen/AA906/bestaetigen", token, "{}").status(), "Schueler darf Abschlussanfragen nicht bestaetigen.");
                runner.assertEquals(403, server.post("/theorie/buchen", token, """
                        {"schuelerId":"SC902","thema":"Fremd","termin":"2026-11-01","dauerMinuten":60,"dozent":"Test"}
                        """).status(), "Schueler darf nicht fuer fremde Schueler buchen.");
                runner.assertEquals(403, server.post("/pruefung/ergebnis", token, """
                        {"pruefungId":"PRT904","schuelerId":"SC904","pruefungsart":"Theoriepruefung","datum":"2026-11-12","bestanden":true,"ergebnisText":"Nicht erlaubt"}
                        """).status(), "Schueler darf keine Pruefungsergebnisse speichern.");
            }
        });

        runner.test("API-Verwaltungsrechte erlauben Verwaltung und sperren Schueleraktionen", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("demo2", "demo2");

                ApiResult overview = server.get("/verwaltung/schueler", token);
                runner.assertEquals(200, overview.status(), "Verwaltung muss die Schueleruebersicht laden duerfen.");
                runner.assertTrue(overview.body().contains("\"id\":\"SC901\""), "Uebersicht muss SC901 enthalten.");
                runner.assertTrue(overview.body().contains("\"id\":\"SC907\""), "Uebersicht muss SC907 enthalten.");

                runner.assertEquals(201, server.post("/verwaltung/schueler", token, """
                        {"vorname":"API","name":"Verwaltung"}
                        """).status(), "Verwaltung muss Schueler anlegen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/schueler/SC901", token).status(), "Verwaltung muss Schuelerdaten pruefen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/schueler/SC901/vertrag", token).status(), "Verwaltung muss Vertrag pruefen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/schueler/SC906", token).status(), "Verwaltung muss verschiedene Schueler lesen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/abschlussanfragen", token).status(), "Verwaltung muss Abschlussanfragen sehen duerfen.");
                runner.assertEquals(200, server.post("/verwaltung/abschlussanfragen/AA906/bestaetigen", token, "{}").status(), "Verwaltung muss bestaetigen duerfen.");

                runner.assertEquals(403, server.get("/schueler/me", token).status(), "Verwaltung darf keinen Schueler-me-Endpunkt verwenden.");
                runner.assertEquals(403, server.get("/status/me", token).status(), "Verwaltung darf keinen Status-me-Endpunkt verwenden.");

                runner.assertEquals(403, server.post("/theorie/buchen", token, """
                        {"schuelerId":"SC901","thema":"Nicht erlaubt","termin":"2026-11-01","dauerMinuten":60,"dozent":"Test"}
                        """).status(), "Verwaltung darf keine Theorie buchen.");
                runner.assertEquals(403, server.post("/praxis/buchen", token, """
                        {"schuelerId":"SC901","flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-11-02T08:00","dauerMinuten":60,"ausbildungsinhalt":"Nicht erlaubt"}
                        """).status(), "Verwaltung darf keine Praxis buchen.");
                runner.assertEquals(403, server.post("/pruefung/theorie/anmelden", token, """
                        {"schuelerId":"SC902","pruefungsart":"Theoriepruefung","wunschtermin":"2026-11-10"}
                        """).status(), "Verwaltung darf keine Theoriepruefung anmelden.");
                runner.assertEquals(403, server.post("/pruefung/praxis/anmelden", token, """
                        {"schuelerId":"SC903","pruefungsart":"Praxispruefung","wunschtermin":"2026-11-11"}
                        """).status(), "Verwaltung darf keine Praxispruefung anmelden.");
            }
        });

        runner.test("API-Abschlusslogik liefert 404 und 409 fuer Fehlerfaelle", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String studentToken = server.login("sc901", "demo901");
                String managementToken = server.login("demo2", "demo2");

                runner.assertEquals(404, server.get("/verwaltung/abschlussanfragen/AA404", managementToken).status(), "Nicht vorhandene Anfrage muss 404 liefern.");
                runner.assertEquals(404, server.post("/verwaltung/abschlussanfragen/AA404/bestaetigen", managementToken, "{}").status(), "Nicht vorhandene Bestaetigung muss 404 liefern.");

                ApiResult openBefore = server.get("/verwaltung/abschlussanfragen", managementToken);
                runner.assertTrue(openBefore.body().contains("\"id\":\"AA906\""), "Ohne neue Anfrage darf nur die Demo-Anfrage bestaetigbar sichtbar sein.");

                ApiResult missingCriteria = server.post("/abschluss/anfragen", studentToken, "{}");
                String requestId = extractJsonString(missingCriteria.body(), "id");
                runner.assertEquals(409, server.post("/verwaltung/abschlussanfragen/" + requestId + "/bestaetigen", managementToken, "{}").status(), "Fehlende Abnahmekriterien muessen 409 liefern.");

                ApiResult confirmed = server.post("/verwaltung/abschlussanfragen/AA906/bestaetigen", managementToken, "{}");
                runner.assertEquals(200, confirmed.status(), "Erfuellte Anfrage muss bestaetigbar sein.");
                runner.assertTrue(confirmed.body().contains("\"status\":\"ABGESCHLOSSEN\""), "Bestaetigung muss Anfrage abschliessen.");
                runner.assertTrue(server.get("/status/SC906/gesamt", managementToken).body().contains("\"status\":\"ABGESCHLOSSEN\""), "Bestaetigung muss Ausbildungsstatus setzen.");
            }
        });

        runner.test("Demo-Zustaende erlauben eigene Aktionen fuer SC902 SC903 und SC906", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String sc902Token = server.login("sc902", "demo902");
                String sc903Token = server.login("sc903", "demo903");
                String sc906Token = server.login("sc906", "demo906");

                ApiResult sc902Status = server.get("/status/me", sc902Token);
                runner.assertEquals(200, sc902Status.status(), "SC902-Status muss abrufbar sein.");
                runner.assertTrue(sc902Status.body().contains("\"status\":\"THEORIE_BEREIT\""), "SC902 muss im Demo-Ausgangszustand THEORIE_BEREIT sein.");
                runner.assertTrue(sc902Status.body().contains("\"theoriePruefungFreigeschaltet\":true"), "SC902 muss fuer die Theoriepruefung freigeschaltet sein.");
                ApiResult theoryExam = server.post("/pruefung/theorie/anmelden", sc902Token, """
                        {"pruefungsart":"Theoriepruefung","wunschtermin":"2026-12-15","pruefer":"P001"}
                        """);
                runner.assertEquals(201, theoryExam.status(), "SC902 muss die eigene Theoriepruefung anmelden duerfen.");
                runner.assertTrue(theoryExam.body().contains("\"schuelerId\":\"SC902\""), "Theoriepruefung muss SC902 zugeordnet sein.");

                ApiResult sc903Status = server.get("/status/me", sc903Token);
                runner.assertEquals(200, sc903Status.status(), "SC903-Status muss abrufbar sein.");
                runner.assertTrue(sc903Status.body().contains("\"status\":\"PRAXIS_BEREIT\""), "SC903 muss im Demo-Ausgangszustand PRAXIS_BEREIT sein.");
                runner.assertTrue(sc903Status.body().contains("\"praxisPruefungFreigeschaltet\":true"), "SC903 muss fuer die Praxispruefung freigeschaltet sein.");
                ApiResult practiceExam = server.post("/pruefung/praxis/anmelden", sc903Token, """
                        {"pruefungsart":"Praxispruefung","wunschtermin":"2026-12-16","pruefer":"P002"}
                        """);
                runner.assertEquals(201, practiceExam.status(), "SC903 muss die eigene Praxispruefung anmelden duerfen.");
                runner.assertTrue(practiceExam.body().contains("\"schuelerId\":\"SC903\""), "Praxispruefung muss SC903 zugeordnet sein.");

                ApiResult completionRequest = server.get("/abschluss/meine-anfrage", sc906Token);
                runner.assertEquals(200, completionRequest.status(), "SC906 muss die eigene Abschlussanfrage lesen duerfen.");
                runner.assertTrue(completionRequest.body().contains("\"id\":\"AA906\""), "SC906 muss die bestehende Demo-Anfrage AA906 sehen.");
                runner.assertTrue(completionRequest.body().contains("\"schuelerId\":\"SC906\""), "AA906 muss SC906 zugeordnet sein.");
                runner.assertTrue(completionRequest.body().contains("\"status\":\"ANGEFRAGT\""), "AA906 muss im Ausgangszustand ANGEFRAGT sein.");
            }
        });

        runner.test("SC907 darf nach abgeschlossener Ausbildung keine neue Abschlussanfrage stellen", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc907", "demo907");
                ApiResult status = server.get("/status/me", token);
                ApiResult request = server.post("/abschluss/anfragen", token, "{}");

                runner.assertEquals(200, status.status(), "SC907-Status muss abrufbar sein.");
                runner.assertTrue(status.body().contains("\"status\":\"ABGESCHLOSSEN\""), "SC907 muss im Demo-Ausgangszustand abgeschlossen sein.");
                runner.assertEquals(409, request.status(), "SC907 darf keine neue Abschlussanfrage erzeugen.");
                runner.assertTrue(request.body().contains("Ausbildung ist bereits abgeschlossen"), "Konflikt muss den abgeschlossenen Status erklaeren.");
                assertNoStackTrace(runner, request, "Abschlussanfrage fuer SC907");
            }
        });

        runner.test("Sessionwechsel von sc901 zu sc906 entfernt die alte Session", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String sc901Token = server.login("sc901", "demo901");
                ApiResult sc901Me = server.get("/auth/me", sc901Token);
                runner.assertTrue(sc901Me.body().contains("\"schuelerId\":\"SC901\""), "Erste Session muss SC901 enthalten.");

                ApiResult logout = server.post("/auth/logout", sc901Token, "{}");
                runner.assertEquals(200, logout.status(), "Logout von sc901 muss erfolgreich sein.");
                runner.assertTrue(logout.body().contains("\"authenticated\":false"), "Logout-Antwort muss die beendete Authentifizierung zeigen.");
                runner.assertEquals(401, server.get("/auth/me", sc901Token).status(), "Abgemeldete SC901-Session darf nicht weiter verwendbar sein.");

                String sc906Token = server.login("sc906", "demo906");
                runner.assertFalse(sc901Token.equals(sc906Token), "Neuer Login muss ein neues Session-Token erzeugen.");
                ApiResult sc906Me = server.get("/auth/me", sc906Token);
                runner.assertEquals(200, sc906Me.status(), "/auth/me muss fuer die neue Session erfolgreich sein.");
                runner.assertTrue(sc906Me.body().contains("\"authenticated\":true"), "Neue Session muss authentifiziert sein.");
                runner.assertTrue(sc906Me.body().contains("\"username\":\"sc906\""), "Neue Session muss sc906 enthalten.");
                runner.assertTrue(sc906Me.body().contains("\"schuelerId\":\"SC906\""), "Neue Session muss ausschliesslich SC906 zugeordnet sein.");
                runner.assertFalse(sc906Me.body().contains("SC901"), "Neue Session darf keine SC901-Daten enthalten.");
            }
        });

        runner.test("Praesentations-Smoke: erfolgreiche Theoriebuchung ist danach abrufbar", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc901", "demo901");

                ApiResult booking = server.post("/theorie/buchen", token, """
                        {"thema":"Praesentations-Smoke Theorie","termin":"2026-11-20","dauerMinuten":60,"dozent":"Elias Schulz"}
                        """);
                ApiResult bookings = server.get("/theorie/me", token);

                runner.assertEquals(201, booking.status(), "Gueltige Theoriebuchung muss HTTP 201 liefern.");
                runner.assertTrue(booking.body().contains("\"success\":true"), "Theoriebuchung muss als erfolgreich markiert sein.");
                runner.assertEquals(200, bookings.status(), "Theoriebuchungen muessen abrufbar sein.");
                runner.assertTrue(bookings.body().contains("Praesentations-Smoke Theorie"), "Neue Theoriebuchung muss im Abruf enthalten sein.");
            }
        });

        runner.test("Praesentations-Smoke: erfolgreiche Praxisbuchung nutzt P001 FZ002 und EDDV", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc901", "demo901");

                ApiResult booking = server.post("/praxis/buchen", token, """
                        {"flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-11-21T08:00","dauerMinuten":60,"ausbildungsinhalt":"Praesentations-Smoke Praxis","startFlughafen":"EDDV","zielFlughafen":"EDDV"}
                        """);
                ApiResult bookings = server.get("/praxis/me", token);

                runner.assertEquals(201, booking.status(), "Gueltige Praxisbuchung muss HTTP 201 liefern.");
                runner.assertTrue(booking.body().contains("\"success\":true"), "Praxisbuchung muss als erfolgreich markiert sein.");
                runner.assertTrue(booking.body().contains("\"flugzeugId\":\"FZ002\""), "Praxisbuchung muss FZ002 verwenden.");
                runner.assertTrue(booking.body().contains("\"startFlughafen\":\"EDDV\""), "Startflughafen muss EDDV sein.");
                runner.assertTrue(booking.body().contains("\"zielFlughafen\":\"EDDV\""), "Zielflughafen muss EDDV sein.");
                runner.assertEquals(200, bookings.status(), "Praxisbuchungen muessen abrufbar sein.");
                runner.assertTrue(bookings.body().contains("Praesentations-Smoke Praxis"), "Neue Praxisbuchung muss im Abruf enthalten sein.");
            }
        });

        runner.test("Praesentations-Smoke: FZ001 liefert Wartungskonflikt", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc901", "demo901");

                ApiResult result = server.post("/praxis/buchen", token, """
                        {"flugzeugId":"FZ001","fluglehrer":"P001","termin":"2026-11-21T10:00","dauerMinuten":60,"ausbildungsinhalt":"Negativtest Wartung","startFlughafen":"EDDV","zielFlughafen":"EDDV"}
                        """);

                runner.assertEquals(409, result.status(), "FZ001 muss einen fachlichen Konflikt liefern.");
                runner.assertTrue(result.body().contains("FZ001"), "Fehlermeldung muss das blockierte Flugzeug nennen.");
                runner.assertTrue(
                        result.body().toLowerCase().contains("wartung") || result.body().toLowerCase().contains("nicht buchbar"),
                        "Fehlermeldung muss Wartungs- oder Buchungsstatus erklaeren."
                );
                assertNoStackTrace(runner, result, "FZ001-Konflikt");
            }
        });

        runner.test("Praesentations-Smoke: fehlende Mindeststunden blockieren Pruefungsanmeldung", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("sc901", "demo901");

                ApiResult result = server.post("/pruefung/theorie/anmelden", token, """
                        {"pruefungsart":"Theoriepruefung","wunschtermin":"2026-11-25","pruefer":"P001"}
                        """);

                runner.assertEquals(409, result.status(), "SC901 muss wegen fehlender Mindeststunden blockiert werden.");
                runner.assertTrue(result.body().contains("mindestens 10.0 Theoriestunden erforderlich"), "Fehlermeldung muss die fehlenden Voraussetzungen erklaeren.");
                runner.assertTrue(result.body().contains("aktuell 4.0"), "Fehlermeldung muss den aktuellen Stand nennen.");
                assertNoStackTrace(runner, result, "Pruefungsanmeldung ohne Mindeststunden");
            }
        });

        runner.test("Praesentations-Smoke: Verwaltung erhaelt fuer Schuelerbuchung HTTP 403", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("demo2", "demo2");

                ApiResult result = server.post("/theorie/buchen", token, """
                        {"schuelerId":"SC901","thema":"Nicht erlaubt","termin":"2026-11-20","dauerMinuten":60,"dozent":"Elias Schulz"}
                        """);

                runner.assertEquals(403, result.status(), "Schuelerverwaltung darf keine Schuelerbuchung ausfuehren.");
                runner.assertTrue(result.body().contains("Keine Berechtigung für diese Funktion."), "403-Antwort muss die vorgesehene Meldung liefern.");
                assertNoStackTrace(runner, result, "unerlaubter Verwaltungszugriff");
            }
        });

        runner.test("Praesentations-Smoke: SC906 kann erfolgreich abgeschlossen werden", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("demo2", "demo2");

                ApiResult openRequest = server.get("/verwaltung/abschlussanfragen/AA906", token);
                runner.assertEquals(200, openRequest.status(), "Demo-Abschlussanfrage AA906 muss abrufbar sein.");
                runner.assertTrue(openRequest.body().contains("\"schuelerId\":\"SC906\""), "AA906 muss zu SC906 gehoeren.");
                runner.assertTrue(openRequest.body().contains("\"status\":\"ANGEFRAGT\""), "AA906 muss vor der Bestaetigung offen sein.");
                runner.assertTrue(openRequest.body().contains("\"theorieKriterienErfuellt\":true"), "Theoriekriterium muss erfuellt sein.");
                runner.assertTrue(openRequest.body().contains("\"praxisKriterienErfuellt\":true"), "Praxiskriterium muss erfuellt sein.");

                ApiResult confirmed = server.post("/verwaltung/abschlussanfragen/AA906/bestaetigen", token, "{}");
                ApiResult status = server.get("/status/SC906/gesamt", token);

                runner.assertEquals(200, confirmed.status(), "AA906 muss erfolgreich bestaetigt werden.");
                runner.assertTrue(confirmed.body().contains("\"status\":\"ABGESCHLOSSEN\""), "Abschlussanfrage muss danach abgeschlossen sein.");
                runner.assertEquals(200, status.status(), "Gesamtstatus von SC906 muss abrufbar sein.");
                runner.assertTrue(status.body().contains("\"status\":\"ABGESCHLOSSEN\""), "SC906 muss danach den Gesamtstatus ABGESCHLOSSEN haben.");
            }
        });
    }

    private static void assertNoStackTrace(TestRunner runner, ApiResult result, String caseName) {
        String body = result.body().toLowerCase();
        runner.assertFalse(body.contains("exception") || body.contains("stacktrace") || body.contains("\tat "), caseName + " darf keinen Stacktrace ausliefern.");
    }

    private static String extractJsonString(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Field not found in JSON response: " + field + " / " + json);
        }
        return matcher.group(1);
    }

    private record ApiResult(int status, String body) {
    }

    private static final class ApiTestServer implements AutoCloseable {
        private final HttpServer server;
        private final HttpClient client;
        private final String baseUrl;

        private ApiTestServer(HttpServer server) {
            this.server = server;
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            this.baseUrl = "http://localhost:" + server.getAddress().getPort() + "/api";
        }

        static ApiTestServer start(TestContext context) throws IOException {
            InMemoryStudentRepository students = new InMemoryStudentRepository();
            InMemoryAircraftRepository aircraft = new InMemoryAircraftRepository();
            InMemoryLessonRepository lessons = new InMemoryLessonRepository();
            StudentService studentService = new StudentService(students);
            AircraftService aircraftService = new AircraftService(aircraft);
            LessonService lessonService = new LessonService(lessons, students, aircraft);
            DashboardService dashboardService = new DashboardService(students, aircraft, lessons);

            ApiHandler handler = new ApiHandler(
                    ApplicationConfig.defaults(),
                    "demo",
                    "in-memory",
                    () -> true,
                    new AuthService(new DemoAccountProvider("demo", context.schuelerRepository)),
                    studentService,
                    aircraftService,
                    lessonService,
                    dashboardService,
                    context.schuelerService,
                    context.theorieService,
                    context.praxisService,
                    context.pruefungsService,
                    context.ausbildungsstatusService,
                    context.abschlussService
            );

            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api", handler);
            server.start();
            return new ApiTestServer(server);
        }

        String login(String username, String password) throws IOException, InterruptedException {
            ApiResult result = post("/auth/login", "", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
            if (result.status() != 200) {
                throw new AssertionError("Login failed: " + result.status() + " / " + result.body());
            }
            Matcher matcher = TOKEN_PATTERN.matcher(result.body());
            if (!matcher.find()) {
                throw new AssertionError("Login response did not contain token: " + result.body());
            }
            return matcher.group(1);
        }

        ApiResult get(String path, String token) throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5))
                    .GET();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResult(response.statusCode(), response.body());
        }

        ApiResult post(String path, String token, String body) throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResult(response.statusCode(), response.body());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
