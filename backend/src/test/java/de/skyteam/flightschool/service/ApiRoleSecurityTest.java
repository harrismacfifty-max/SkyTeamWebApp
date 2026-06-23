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

    private ApiRoleSecurityTest() {
    }

    static void run(TestRunner runner) {
        runner.test("API-Login liefert Rollen fuer Demo-Benutzer", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                ApiResult studentLogin = server.post("/auth/login", "", """
                        {"username":"demo","password":"demo"}
                        """);
                ApiResult managementLogin = server.post("/auth/login", "", """
                        {"username":"demo2","password":"demo2"}
                        """);

                runner.assertEquals(200, studentLogin.status(), "demo Login muss erfolgreich sein.");
                runner.assertTrue(studentLogin.body().contains("\"role\":\"SCHUELER\""), "demo muss SCHUELER liefern.");
                runner.assertTrue(studentLogin.body().contains("\"schuelerId\":\"SC901\""), "demo muss SC901 zugeordnet sein.");
                runner.assertEquals(200, managementLogin.status(), "demo2 Login muss erfolgreich sein.");
                runner.assertTrue(managementLogin.body().contains("\"role\":\"SCHUELERVERWALTUNG\""), "demo2 muss SCHUELERVERWALTUNG liefern.");
            }
        });

        runner.test("API-Schuelerrechte erlauben eigene Buchungen und sperren Verwaltung", () -> {
            try (TestContext context = TestContext.create();
                 ApiTestServer server = ApiTestServer.start(context)) {
                String token = server.login("demo", "demo");

                runner.assertEquals(201, server.post("/theorie/buchen", token, """
                        {"schuelerId":"SC901","thema":"API Theorie","termin":"2026-11-01","dauerMinuten":600,"dozent":"Test"}
                        """).status(), "Schueler muss eigene Theorie buchen duerfen.");
                runner.assertEquals(201, server.post("/praxis/buchen", token, """
                        {"schuelerId":"SC901","flugzeugId":"FZ002","fluglehrer":"P001","termin":"2026-11-02T08:00","dauerMinuten":600,"ausbildungsinhalt":"API Praxis"}
                        """).status(), "Schueler muss eigene Praxis buchen duerfen.");
                runner.assertEquals(201, server.post("/pruefung/theorie/anmelden", token, """
                        {"schuelerId":"SC901","pruefungsart":"Theoriepruefung","wunschtermin":"2026-11-10","pruefer":"P001"}
                        """).status(), "Schueler muss eigene Theoriepruefung anmelden duerfen.");
                runner.assertEquals(201, server.post("/pruefung/praxis/anmelden", token, """
                        {"schuelerId":"SC901","pruefungsart":"Praxispruefung","wunschtermin":"2026-11-11","pruefer":"P002"}
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

                runner.assertEquals(201, server.post("/verwaltung/schueler", token, """
                        {"vorname":"API","name":"Verwaltung"}
                        """).status(), "Verwaltung muss Schueler anlegen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/schueler/SC901", token).status(), "Verwaltung muss Schuelerdaten pruefen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/schueler/SC901/vertrag", token).status(), "Verwaltung muss Vertrag pruefen duerfen.");
                runner.assertEquals(200, server.get("/verwaltung/abschlussanfragen", token).status(), "Verwaltung muss Abschlussanfragen sehen duerfen.");
                runner.assertEquals(200, server.post("/verwaltung/abschlussanfragen/AA906/bestaetigen", token, "{}").status(), "Verwaltung muss bestaetigen duerfen.");

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
                String studentToken = server.login("demo", "demo");
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
                    new AuthService(),
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
