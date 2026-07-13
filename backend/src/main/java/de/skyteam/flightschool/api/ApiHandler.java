package de.skyteam.flightschool.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.skyteam.flightschool.config.ApplicationConfig;
import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.dto.PraxisStornierungRequest;
import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.dto.TheorieStornierungRequest;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.ErrorHandler;
import de.skyteam.flightschool.error.ForbiddenException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.model.AuthUser;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.model.Student;
import de.skyteam.flightschool.model.UserRole;
import de.skyteam.flightschool.service.AircraftService;
import de.skyteam.flightschool.service.AbschlussService;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class ApiHandler implements HttpHandler {
    private final ApplicationConfig config;
    private final String activeProfile;
    private final String databaseMode;
    private final BooleanSupplier databaseReachable;
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
    private final AbschlussService abschlussService;

    public ApiHandler(
            ApplicationConfig config,
            String activeProfile,
            String databaseMode,
            BooleanSupplier databaseReachable,
            AuthService authService,
            StudentService studentService,
            AircraftService aircraftService,
            LessonService lessonService,
            DashboardService dashboardService,
            SchuelerService schuelerService,
            TheorieService theorieService,
            PraxisService praxisService,
            PruefungsService pruefungsService,
            AusbildungsstatusService ausbildungsstatusService,
            AbschlussService abschlussService
    ) {
        this.config = config;
        this.activeProfile = activeProfile;
        this.databaseMode = databaseMode;
        this.databaseReachable = databaseReachable;
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
        this.abschlussService = abschlussService;
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
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "API erreichbar.",
                    ApiJson.health(activeProfile, databaseMode, databaseReachable.getAsBoolean())
            ));
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
            AuthUser user = authService.currentUser(bearerToken(exchange))
                    .orElseThrow(() -> new UnauthorizedException("Login erforderlich."));
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Benutzer angemeldet.", ApiJson.authUser(user)));
            return;
        }

        if ("POST".equals(method) && "/api/auth/logout".equals(path)) {
            boolean loggedOut = authService.logout(bearerToken(exchange));
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Logout erfolgreich.", ApiJson.logout(loggedOut)));
            return;
        }

        AuthUser currentUser = requireAuthenticated(exchange);
        requireQuerySchuelerAccess(exchange, currentUser);

        if ("GET".equals(method) && "/api/dashboard".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Dashboard geladen.", ApiJson.dashboard(dashboardService.dashboard())));
            return;
        }

        String[] segments = segments(path);

        if ("GET".equals(method) && "/api/schueler/me".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            String schuelerId = requireOwnSchuelerId(currentUser);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Eigener Schuelerdatensatz geladen.",
                    ApiJson.schueler(schuelerService.findById(schuelerId))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/status/me".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            String schuelerId = requireOwnSchuelerId(currentUser);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Eigener Ausbildungsstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(schuelerId))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/theorie/me".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            String schuelerId = requireOwnSchuelerId(currentUser);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Eigene Theorie geladen.",
                    ApiJson.theorieDetails(theorieService.fortschritt(schuelerId), theorieService.kurse(schuelerId))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/praxis/me".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            String schuelerId = requireOwnSchuelerId(currentUser);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Eigene Praxis geladen.",
                    ApiJson.praxisDetails(praxisService.fortschritt(schuelerId), praxisService.fluege(schuelerId))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/pruefung/me".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            String schuelerId = requireOwnSchuelerId(currentUser);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Eigene Pruefungen geladen.",
                    ApiJson.pruefungen(pruefungsService.pruefungen(schuelerId))
            ));
            return;
        }

        if ("POST".equals(method) && "/api/abschluss/anfragen".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            String schuelerId = requireSessionSchuelerId(currentUser, body);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success(
                    "Abschlussanfrage gestellt.",
                    ApiJson.abschlussAnfrageDto(abschlussService.requestAbschluss(schuelerId))
            ));
            return;
        }

        if ("GET".equals(method) && "/api/abschluss/meine-anfrage".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Abschlussanfrage geladen.",
                    ApiJson.abschlussAnfrageDto(abschlussService.getMeineAbschlussanfrage(requireOwnSchuelerId(currentUser)))
            ));
            return;
        }

        if (segments.length >= 2 && "verwaltung".equals(segments[1])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);

            if (segments.length == 3 && "GET".equals(method) && "abschlussanfragen".equals(segments[2])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Abschlussanfragen geladen.",
                        ApiJson.abschlussAnfrageDtos(abschlussService.findOffeneAbschlussanfragen())
                ));
                return;
            }

            if (segments.length == 4 && "GET".equals(method) && "abschlussanfragen".equals(segments[2]) && "alle".equals(segments[3])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Alle Abschlussanfragen geladen.",
                        ApiJson.abschlussAnfrageDtos(abschlussService.findAlleAbschlussanfragen())
                ));
                return;
            }

            if (segments.length == 4 && "GET".equals(method) && "abschlussanfragen".equals(segments[2])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Abschlussanfrage geladen.",
                        ApiJson.abschlussAnfrageDto(abschlussService.findAbschlussanfrage(segments[3]))
                ));
                return;
            }

            if (segments.length == 5 && "POST".equals(method) && "abschlussanfragen".equals(segments[2]) && "bestaetigen".equals(segments[4])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Abschlussanfrage bestaetigt.",
                        ApiJson.abschlussAnfrageDto(abschlussService.bestaetigeAbschluss(segments[3]))
                ));
                return;
            }

            if (segments.length == 5 && "POST".equals(method) && "abschlussanfragen".equals(segments[2]) && "ablehnen".equals(segments[4])) {
                Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Abschlussanfrage abgelehnt.",
                        ApiJson.abschlussAnfrageDto(abschlussService.lehneAbschlussAb(segments[3], body.get("begruendung")))
                ));
                return;
            }

            if (segments.length == 3 && "GET".equals(method) && "schueler".equals(segments[2])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Verwaltungsschueler geladen.",
                        ApiJson.schuelerList(schuelerService.findAll())
                ));
                return;
            }

            if (segments.length == 3 && "POST".equals(method) && "schueler".equals(segments[2])) {
                HttpSupport.sendResponse(exchange, 201, ApiJson.success(
                        "Schueler angelegt.",
                        ApiJson.schueler(schuelerService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange))))
                ));
                return;
            }

            if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[2])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Schuelerdaten geladen.",
                        ApiJson.schueler(schuelerService.findById(segments[3]))
                ));
                return;
            }

            if (segments.length == 5 && "GET".equals(method) && "schueler".equals(segments[2]) && "vertrag".equals(segments[4])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Ausbildungsvertrag geladen.",
                        ApiJson.ausbildungsVertrag(schuelerService.findVertragBySchuelerId(segments[3]))
                ));
                return;
            }

            if (segments.length == 6 && "POST".equals(method) && "schueler".equals(segments[2]) && "vertrag".equals(segments[4]) && "pruefen".equals(segments[5])) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Ausbildungsvertrag wurde geprueft.",
                        ApiJson.ausbildungsVertrag(schuelerService.pruefeVertrag(segments[3], JsonUtil.parseObject(HttpSupport.readBody(exchange))))
                ));
                return;
            }

            throw new NotFoundException("Verwaltungsendpoint nicht gefunden.");
        }

        if ("GET".equals(method) && "/api/schueler".equals(path)) {
            if (currentUser.role() == UserRole.SCHUELER) {
                HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                        "Schueler geladen.",
                        ApiJson.schuelerList(List.of(schuelerService.findById(requireOwnSchuelerId(currentUser)))))
                );
                return;
            }
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.schuelerList(schuelerService.findAll())));
            return;
        }

        if ("POST".equals(method) && "/api/schueler".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success(
                    "Schueler angelegt.",
                    ApiJson.schueler(schuelerService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange))))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "schueler".equals(segments[1])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.schueler(schuelerService.findById(segments[2]))));
            return;
        }

        if (segments.length == 3 && "DELETE".equals(method) && "schueler".equals(segments[1])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            schuelerService.delete(segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geloescht.", ApiJson.deleted(segments[2])));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "status".equals(segments[1])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildungsstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "gesamt".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Gesamtstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "theorie".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theoriestatus geladen.",
                    ApiJson.theorieFortschritt(theorieService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "status".equals(segments[1]) && "praxis".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisstatus geladen.",
                    ApiJson.praxisFortschritt(praxisService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "theorie".equals(segments[1])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theorie geladen.",
                    ApiJson.theorieDetails(theorieService.fortschritt(segments[2]), theorieService.kurse(segments[2]))
            ));
            return;
        }

        if ("POST".equals(method) && "/api/theorie/buchen".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            TheorieBuchungRequest request = theorieRequest(requireSessionSchuelerId(currentUser, body), body);
            Kurs kurs = theorieService.bucheTheoriekurs(request);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriekurs gebucht.", ApiJson.kurs(kurs)));
            return;
        }

        if ("POST".equals(method) && "/api/theorie/stornieren".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            TheorieStornierungRequest request = theorieStornierungRequest(requireSessionSchuelerId(currentUser, body), body);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theoriekurs storniert.",
                    ApiJson.theorieStornierung(theorieService.storniereTheoriekurs(request))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "praxis".equals(segments[1])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxis geladen.",
                    ApiJson.praxisDetails(praxisService.fortschritt(segments[2]), praxisService.fluege(segments[2]))
            ));
            return;
        }

        if ("POST".equals(method) && "/api/praxis/buchen".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            PraxisBuchungRequest request = praxisRequest(requireSessionSchuelerId(currentUser, body), body);
            Flug flug = praxisService.bucheFlugstunde(request);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxisflugstunde gebucht.", ApiJson.flug(flug)));
            return;
        }

        if ("POST".equals(method) && "/api/praxis/stornieren".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            PraxisStornierungRequest request = praxisStornierungRequest(requireSessionSchuelerId(currentUser, body), body);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisflugstunde storniert.",
                    ApiJson.praxisStornierung(praxisService.storniereFlugstunde(request))
            ));
            return;
        }

        if (segments.length == 3 && "GET".equals(method) && "pruefung".equals(segments[1])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Pruefungen geladen.", ApiJson.pruefungen(pruefungsService.pruefungen(segments[2]))));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/theorie/anmelden".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            PruefungAnmeldungRequest request = pruefungRequest(requireSessionSchuelerId(currentUser, body), body, "Theoriepruefung");
            Pruefung pruefung = pruefungsService.meldeTheoriePruefungAn(request);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriepruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/praxis/anmelden".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            Map<String, String> body = JsonUtil.parseObject(HttpSupport.readBody(exchange));
            PruefungAnmeldungRequest request = pruefungRequest(requireSessionSchuelerId(currentUser, body), body, "Praxispruefung");
            Pruefung pruefung = pruefungsService.meldePraxisPruefungAn(request);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxispruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefung/ergebnis".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Pruefungsergebnis gespeichert.",
                    ApiJson.pruefungsErgebnisStatus(pruefungsService.speichereErgebnis(pruefungsErgebnisRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)))))
            ));
            return;
        }

        if (segments.length == 4 && "POST".equals(method) && "ausbildung".equals(segments[1]) && "abschliessen".equals(segments[3])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            throw new BusinessConflictException("Abschluss muss ueber eine vorhandene Abschlussanfrage bestaetigt werden.");
        }

        if ("GET".equals(method) && "/api/abschlussanfragen".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Abschlussanfragen geladen.",
                    ApiJson.abschlussAnfrageDtos(abschlussService.findOffeneAbschlussanfragen())
            ));
            return;
        }

        if (segments.length == 3 && "POST".equals(method) && "abschlussanfragen".equals(segments[1])) {
            requireRole(currentUser, UserRole.SCHUELER);
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 201, ApiJson.success(
                    "Abschlussanfrage gestellt.",
                    ApiJson.abschlussAnfrageDto(abschlussService.requestAbschluss(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "POST".equals(method) && "abschlussanfragen".equals(segments[1]) && "bestaetigen".equals(segments[3])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            throw new BusinessConflictException("Bitte /api/verwaltung/abschlussanfragen/{id}/bestaetigen mit Anfrage-ID verwenden.");
        }

        if (segments.length == 4 && "POST".equals(method) && "abschlussanfragen".equals(segments[1]) && "ablehnen".equals(segments[3])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            throw new BusinessConflictException("Bitte /api/verwaltung/abschlussanfragen/{id}/ablehnen mit Anfrage-ID verwenden.");
        }

        if ("GET".equals(method) && "/api/students".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Schueler geladen.", ApiJson.students(studentService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/students".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            Student student = studentService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange)));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Schueler angelegt.", ApiJson.student(student)));
            return;
        }

        if ("GET".equals(method) && "/api/aircraft".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Flugzeuge geladen.", ApiJson.aircraftList(aircraftService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/aircraft".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            Aircraft aircraft = aircraftService.create(JsonUtil.parseObject(HttpSupport.readBody(exchange)));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Flugzeug angelegt.", ApiJson.aircraft(aircraft)));
            return;
        }

        if ("GET".equals(method) && "/api/lessons".equals(path)) {
            HttpSupport.sendResponse(exchange, 200, ApiJson.success("Ausbildungstermine geladen.", ApiJson.lessons(lessonService.list())));
            return;
        }

        if ("POST".equals(method) && "/api/lessons".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELER);
            throw new BusinessConflictException("Dieser alte Termin-Endpunkt ist fuer Buchungen gesperrt. Bitte /api/theorie/buchen oder /api/praxis/buchen verwenden.");
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "theorie".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Theoriefortschritt geladen.",
                    ApiJson.theorieFortschritt(theorieService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "theorie".equals(segments[3]) && "buchungen".equals(segments[4])) {
            requireRole(currentUser, UserRole.SCHUELER);
            requireSchuelerReadAccess(currentUser, segments[2]);
            Kurs kurs = theorieService.bucheTheoriekurs(theorieRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriekurs gebucht.", ApiJson.kurs(kurs)));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "praxis".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Praxisfortschritt geladen.",
                    ApiJson.praxisFortschritt(praxisService.fortschritt(segments[2]))
            ));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "praxis".equals(segments[3]) && "buchungen".equals(segments[4])) {
            requireRole(currentUser, UserRole.SCHUELER);
            requireSchuelerReadAccess(currentUser, segments[2]);
            Flug flug = praxisService.bucheFlugstunde(praxisRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange))));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxisflugstunde gebucht.", ApiJson.flug(flug)));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "pruefungen".equals(segments[3]) && "theorie".equals(segments[4])) {
            requireRole(currentUser, UserRole.SCHUELER);
            requireSchuelerReadAccess(currentUser, segments[2]);
            Pruefung pruefung = pruefungsService.meldeTheoriePruefungAn(pruefungRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Theoriepruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Theoriepruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if (segments.length == 5 && "POST".equals(method) && "schueler".equals(segments[1]) && "pruefungen".equals(segments[3]) && "praxis".equals(segments[4])) {
            requireRole(currentUser, UserRole.SCHUELER);
            requireSchuelerReadAccess(currentUser, segments[2]);
            Pruefung pruefung = pruefungsService.meldePraxisPruefungAn(pruefungRequest(segments[2], JsonUtil.parseObject(HttpSupport.readBody(exchange)), "Praxispruefung"));
            HttpSupport.sendResponse(exchange, 201, ApiJson.success("Praxispruefung angemeldet.", ApiJson.pruefung(pruefung)));
            return;
        }

        if ("POST".equals(method) && "/api/pruefungen/ergebnis".equals(path)) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Pruefungsergebnis gespeichert.",
                    ApiJson.pruefungsErgebnisStatus(pruefungsService.speichereErgebnis(pruefungsErgebnisRequest(JsonUtil.parseObject(HttpSupport.readBody(exchange)))))
            ));
            return;
        }

        if (segments.length == 4 && "GET".equals(method) && "schueler".equals(segments[1]) && "status".equals(segments[3])) {
            requireSchuelerReadAccess(currentUser, segments[2]);
            HttpSupport.sendResponse(exchange, 200, ApiJson.success(
                    "Ausbildungsstatus berechnet.",
                    ApiJson.ausbildungsStatus(ausbildungsstatusService.status(segments[2]))
            ));
            return;
        }

        if (segments.length == 4 && "POST".equals(method) && "schueler".equals(segments[1]) && "abschluss".equals(segments[3])) {
            requireRole(currentUser, UserRole.SCHUELERVERWALTUNG);
            throw new BusinessConflictException("Abschluss muss ueber eine vorhandene Abschlussanfrage bestaetigt werden.");
        }

        throw new NotFoundException("Endpoint nicht gefunden.");
    }

    private AuthUser requireAuthenticated(HttpExchange exchange) {
        return authService.currentUser(bearerToken(exchange))
                .orElseThrow(() -> new UnauthorizedException("Login erforderlich."));
    }

    private static void requireRole(AuthUser user, UserRole role) {
        if (user.role() != role) {
            throw new ForbiddenException("Keine Berechtigung für diese Funktion.");
        }
    }

    private static void requireSchuelerReadAccess(AuthUser user, String schuelerId) {
        if (user.role() == UserRole.SCHUELERVERWALTUNG) {
            return;
        }
        if (user.role() == UserRole.SCHUELER && schuelerId != null && schuelerId.equals(user.schuelerId())) {
            return;
        }
        throw new ForbiddenException("Keine Berechtigung für diese Funktion.");
    }

    private static String requireOwnSchuelerId(AuthUser user) {
        if (user.schuelerId() == null || user.schuelerId().isBlank()) {
            throw new ForbiddenException("Keine Berechtigung für diese Funktion.");
        }
        return user.schuelerId();
    }

    private static String requireSessionSchuelerId(AuthUser user, Map<String, String> body) {
        String sessionSchuelerId = requireOwnSchuelerId(user);
        requireMatchingSchuelerId(sessionSchuelerId, body.get("schuelerId"));
        requireMatchingSchuelerId(sessionSchuelerId, body.get("studentId"));
        return sessionSchuelerId;
    }

    private static void requireQuerySchuelerAccess(HttpExchange exchange, AuthUser user) {
        if (user.role() != UserRole.SCHUELER) {
            return;
        }
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return;
        }
        String sessionSchuelerId = requireOwnSchuelerId(user);
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (!"schuelerId".equalsIgnoreCase(key) && !"studentId".equalsIgnoreCase(key)) {
                continue;
            }
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            requireMatchingSchuelerId(sessionSchuelerId, value);
        }
    }

    private static void requireMatchingSchuelerId(String sessionSchuelerId, String requestedSchuelerId) {
        if (requestedSchuelerId == null || requestedSchuelerId.isBlank()) {
            return;
        }
        if (!sessionSchuelerId.equals(requestedSchuelerId.trim())) {
            throw new ForbiddenException("Keine Berechtigung für diese Funktion.");
        }
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

    private static PraxisStornierungRequest praxisStornierungRequest(String schuelerId, Map<String, String> body) {
        return new PraxisStornierungRequest(
                schuelerId,
                required(body, "flugId"),
                optional(body, "grund", "")
        );
    }

    private static TheorieStornierungRequest theorieStornierungRequest(String schuelerId, Map<String, String> body) {
        return new TheorieStornierungRequest(
                schuelerId,
                required(body, "kursId"),
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


