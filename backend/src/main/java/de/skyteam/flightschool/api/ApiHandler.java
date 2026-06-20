package de.skyteam.flightschool.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.skyteam.flightschool.config.ApplicationConfig;
import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.dto.PraxisStornierungRequest;
import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.error.ErrorHandler;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.model.Student;
import de.skyteam.flightschool.service.AircraftService;
import de.skyteam.flightschool.service.AuthService;
import de.skyteam.flightschool.service.AusbildungsstatusService;
import de.skyteam.flightschool.service.DashboardService;
import de.skyteam.flightschool.service.LessonService;
import de.skyteam.flightschool.service.PraxisService;
import de.skyteam.flightschool.service.PruefungsService;
import de.skyteam.flightschool.service.SchuelerService;
import de.skyteam.flightschool.service.StudentService;
import de.skyteam.flightschool.service.TheorieService;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public final class ApiHandler implements HttpHandler {
    private final ApplicationConfig config;
    private final AuthService authService;
    private final StudentService studentService;
    private final AircraftService aircraftService;
    private final LessonService lessonService;
    private final DashboardService dashboardService;
    private final SchuelerService schuelerService;
    private final TheorieService theorieService;
    private final PraxisService praxisService;
    private final PruefungsService pruefungsService;
    private final AusbildungsstatusService ausbildungsstatusService;

    public ApiHandler(
            ApplicationConfig config,
            AuthService authService,
            StudentService studentService,
            AircraftService aircraftService,
            LessonService lessonService,
            DashboardService dashboardService,
            SchuelerService schuelerService,
            TheorieService theorieService,
            PraxisService praxisService,
            PruefungsService pruefungsService,
            AusbildungsstatusService ausbildungsstatusService
    ) {
        this.config = config;
        this.authService = authService;
        this.studentService = studentService;
        this.aircraftService = aircraftService;
        this.lessonService = lessonService;
        this.dashboardService = dashboardService;
        this.schuelerService = schuelerService;
        this.theorieService = theorieService;
        this.praxisService = praxisService;
        this.pruefungsService = pruefungsService;
        this.ausbildungsstatusService = ausbildungsstatusService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpSupport.handlePreflight(exchange)) {
            return;
        }

        try {
            route(exchange);
        } catch (Exception exception) {
            ErrorHandler.handle(exchange, exception);
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = normalize(exchange.getRequestURI().getPath());

        if ("GET".equals(method) && "/api/health".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("API erreichbar.", ApiJson.health()));
            return;
        }

        if ("GET".equals(method) && "/api/version".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Version ermittelt.", ApiJson.version(config)));
            return;
        }

        if ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/login".equals(path))) {
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Login erfolgreich.", ApiJson.login(authService.login(body))));
            return;
        }

        if ("GET".equals(method) && "/api/auth/me".equals(path)) {
            String username = authService.currentUser(bearerToken(exchange))
                    .orElseThrow(() -> new UnauthorizedException("Login erforderlich."));
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Benutzer angemeldet.", ApiJson.authUser(username)));
            return;
        }

        if ("POST".equals(method) && "/api/auth/logout".equals(path)) {
            boolean loggedOut = authService.logout(bearerToken(exchange));
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Logout erfolgreich.", ApiJson.logout(loggedOut)));
            return;
        }

        if (!isAuthorized(exchange)) {
            throw new UnauthorizedException("Login erforderlich.");
        }

        if ("GET".equals(method) && "/api/dashboard".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Dashboard geladen.", ApiJson.dashboard(dashboardService.dashboard())));
            return;
        }

        String[] segments = segments(path);

        if ("GET".equals(method) && "/api/schueler".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.schuelerList(schuelerService.findAll())));
            return;
        }

        if ("POST".equals(method) && "/api/schueler".equals(path)) {
            HttpSupport.sendResponse(exchange, 201, ApiJson.success(
                    "Schueler angelegt.",
                    ApiJson.schueler(schuelerService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange))))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "schueler".equals(segments[1])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.schueler(schuelerService.findById(segments[2]))));
            return;
        }

        if (segments.length == 3 && "DELETE".equals(method) && "schueler".equals(segments[1])) {
            schuelerService.delete(segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geloescht.", ApiJson.deleted(segments[2])));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "status".equals(segments[1])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildungsstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "gesamt".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Gesamtstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "theorie".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theoriestatus geladen.",
                    ApiJson.theorieFortschritt(theorieService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "praxis".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisstatus geladen.",
                    ApiJson.praxisFortschritt(praxisService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "theorie".equals(segments[1])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theorie geladen.",
                    ApiJson.theorieDetails(theorieService.fortschritt(segments[2]), theorieService.kurse(segments[2]))
            ));
            return;
        }

        if ("POST".equals(method) && "/api/theorie/buchen".equals(path)) {
            Kurs kurs = theorieService.bucheTheoriekurs(theorieRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriekurs gebucht.", ApiJson.kurs(kurs)));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "praxis".equals(segments[1])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxis geladen.",
                    ApiJson.praxisDetails(praxisService.fortschritt(segments[2]), praxisService.fluege(segments[2]))
            ));
            return;
        }

        if ("POST".equals(method) && "/api/praxis/buchen".equals(path)) {
            Flug flug = praxisService.bucheFlugstunde(praxisRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxisflugstunde gebucht.", ApiJson.flug(flug)));
            return;
        }

        if ("POST".equals(method) && "/api/praxis/stornieren".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisflugstunde storniert.",
                    ApiJson.praxisStornierung(praxisService.storniereFlugstunde(praxisStornierungRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)))))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "pruefung".equals(segments[1])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Pruefungen geladen.", ApiJson.pruefungen(pruefungsService.pruefungen(segments[2]))));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/theorie/anmelden".equals(path)) {
            Pruefung pruefung = pruefungsService.meldeTheoriePruefungAn(pruefungRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Theoriepruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriepruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/praxis/anmelden".equals(path)) {
            Pruefung pruefung = pruefungsService.meldePraxisPruefungAn(pruefungRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Praxispruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxispruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/ergebnis".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Pruefungsergebnis gespeichert.",
                    ApiJson.pruefungsErgebnisStatus(pruefungsService.speichereErgebnis(pruefungsErgebnisRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)))))
            ));
            return;
        }

        if (segments.length == 4 && "POST".equals(method) && "ausbildung".equals(segments[1]) && "abschliessen".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildung abgeschlossen.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.schliesseAusbildungAb(segments[2]))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/students".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.students(studentService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/students".equals(path)) {
            Student student = studentService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange)));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Schueler angelegt.", ApiJson.student(student)));
            return;
        }

        if ("GET".equals(method) && "/api/aircraft".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Flugzeuge geladen.", ApiJson.aircraftList(aircraftService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/aircraft".equals(path)) {
            Aircraft aircraft = aircraftService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange)));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Flugzeug angelegt.", ApiJson.aircraft(aircraft)));
            return;
        }

        if ("GET".equals(method) && "/api/lessons".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Ausbildungstermine geladen.", ApiJson.lessons(lessonService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/lessons".equals(path)) {
            Lesson lesson = lessonService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange)));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Ausbildungstermin angelegt.", ApiJson.lesson(lesson)));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "theorie".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theoriefortschritt geladen.",
                    ApiJson.theorieFortschritt(theorieService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "theorie".equals(segments[3]) && "buchungen".equals(segments[4])) {
            Kurs kurs = theorieService.bucheTheoriekurs(theorieRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriekurs gebucht.", ApiJson.kurs(kurs)));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "praxis".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisfortschritt geladen.",
                    ApiJson.praxisFortschritt(praxisService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "praxis".equals(segments[3]) && "buchungen".equals(segments[4])) {
            Flug flug = praxisService.bucheFlugstunde(praxisRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxisflugstunde gebucht.", ApiJson.flug(flug)));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "pruefungen".equals(segments[3]) && "theorie".equals(segments[4])) {
            Pruefung pruefung = pruefungsService.meldeTheoriePruefungAn(pruefungRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Theoriepruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriepruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "pruefungen".equals(segments[3]) && "praxis".equals(segments[4])) {
            Pruefung pruefung = pruefungsService.meldePraxisPruefungAn(pruefungRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Praxispruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxispruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefungen/ergebnis".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Pruefungsergebnis gespeichert.",
                    ApiJson.pruefungsErgebnisStatus(pruefungsService.speichereErgebnis(pruefungsErgebnisRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)))))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "status".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildungsstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "POST".equals(method) && "schueler".equals(segments[1]) && "abschluss".equals(segments[3])) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildung abgeschlossen.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.schliesseAusbildungAb(segments[2]))
            ));
            return;
        }

        throw new NotFoundException("Endpoint nicht gefunden.");
    }

    private boolean isAuthorized(HttpExchange exchange) {
        return authService.isAuthenticated(bearerToken(exchange));
    }

    private static String bearerToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private static String normalize(String path) {
        if (path.length() > 4 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static String[] segments(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.split("/");
    }

    private static TheorieBuchungRequest theorieRequest(String schuelerId, Map<String, String> body) {
        return new TheorieBuchungRequest(
                schuelerId,
                required(body, "thema"),
                required(body, "termin"),
                requiredInt(body, "dauerMinuten"),
                required(body, "dozent"),
                optional(body, "notizen", optional(body, "notes", ""))
        );
    }

    private static TheorieBuchungRequest theorieRequest(Map<String, String> body) {
        return theorieRequest(required(body, "schuelerId"), body);
    }

    private static PraxisBuchungRequest praxisRequest(String schuelerId, Map<String, String> body) {
        return new PraxisBuchungRequest(
                schuelerId,
                required(body, "flugzeugId"),
                required(body, "fluglehrer"),
                required(body, "termin"),
                requiredInt(body, "dauerMinuten"),
                required(body, "ausbildungsinhalt"),
                optional(body, "notizen", optional(body, "notes", ""))
        );
    }

    private static PraxisBuchungRequest praxisRequest(Map<String, String> body) {
        return praxisRequest(required(body, "schuelerId"), body);
    }

    private static PraxisStornierungRequest praxisStornierungRequest(Map<String, String> body) {
        return new PraxisStornierungRequest(
                required(body, "schuelerId"),
                required(body, "flugId"),
                optional(body, "grund", "")
        );
    }

    private static PruefungAnmeldungRequest pruefungRequest(String schuelerId, Map<String, String> body, String fallbackArt) {
        return new PruefungAnmeldungRequest(
                schuelerId,
                optional(body, "pruefungsart", fallbackArt),
                required(body, "wunschtermin"),
                optional(body, "pruefer", ""),
                optional(body, "bemerkung", optional(body, "notizen", ""))
        );
    }

    private static PruefungAnmeldungRequest pruefungRequest(Map<String, String> body, String fallbackArt) {
        return pruefungRequest(required(body, "schuelerId"), body, fallbackArt);
    }

    private static PruefungsErgebnisRequest pruefungsErgebnisRequest(Map<String, String> body) {
        return new PruefungsErgebnisRequest(
                required(body, "pruefungId"),
                required(body, "schuelerId"),
                optional(body, "pruefungsart", "Pruefung"),
                required(body, "datum"),
                requiredBoolean(body, "bestanden"),
                optional(body, "ergebnisText", ""),
                optional(body, "notizen", "")
        );
    }

    private static String required(Map<String, String> body, String field) {
        String value = body.get(field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " ist erforderlich.");
        }
        return value.trim();
    }

    private static String optional(Map<String, String> body, String field, String fallback) {
        String value = body.get(field);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static int optionalInt(Map<String, String> body, String field, int fallback) {
        String value = body.get(field);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " muss eine Zahl sein.");
        }
    }

    private static int requiredInt(Map<String, String> body, String field) {
        String value = required(body, field);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " muss eine Zahl sein.");
        }
    }

    private static boolean requiredBoolean(Map<String, String> body, String field) {
        String value = required(body, field).toLowerCase(Locale.ROOT);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(field + " muss true oder false sein.");
    }
}


