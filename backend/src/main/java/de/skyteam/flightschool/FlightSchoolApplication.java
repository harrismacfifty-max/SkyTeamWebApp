package de.skyteam.flightschool;

import com.sun.net.httpserver.HttpServer;
import de.skyteam.flightschool.api.ApiHandler;
import de.skyteam.flightschool.config.ApplicationConfig;
import de.skyteam.flightschool.repository.RepositoryProvider;
import de.skyteam.flightschool.service.AbschlussService;
import de.skyteam.flightschool.service.AircraftService;
import de.skyteam.flightschool.service.AuthService;
import de.skyteam.flightschool.service.AusbildungsstatusService;
import de.skyteam.flightschool.service.DashboardService;
import de.skyteam.flightschool.service.DemoAccountProvider;
import de.skyteam.flightschool.service.LessonService;
import de.skyteam.flightschool.service.PraxisService;
import de.skyteam.flightschool.service.PruefungsService;
import de.skyteam.flightschool.service.SchuelerService;
import de.skyteam.flightschool.service.StudentService;
import de.skyteam.flightschool.service.TheorieService;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class FlightSchoolApplication {
    private FlightSchoolApplication() {
    }

    public static void main(String[] args) throws Exception {
        try {
            start();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("SkyTeam Flight School API konnte nicht gestartet werden.");
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private static void start() throws Exception {
        int port = readPort();
        RepositoryProvider repositories = RepositoryProvider.fromEnvironment();

        StudentService studentService = new StudentService(repositories.students());
        AircraftService aircraftService = new AircraftService(repositories.aircraft());
        LessonService lessonService = new LessonService(
                repositories.lessons(),
                repositories.students(),
                repositories.aircraft()
        );
        DashboardService dashboardService = new DashboardService(
                repositories.students(),
                repositories.aircraft(),
                repositories.lessons()
        );
        SchuelerService schuelerService = new SchuelerService(
                repositories.schueler(),
                repositories.ausbildungsVertraege()
        );
        TheorieService theorieService = new TheorieService(repositories.schueler(), repositories.kurse());
        PraxisService praxisService = new PraxisService(
                repositories.schueler(),
                repositories.fluege(),
                repositories.piloten(),
                repositories.flugzeuge(),
                repositories.wartungen()
        );
        PruefungsService pruefungsService = new PruefungsService(
                repositories.schueler(),
                repositories.pruefungen(),
                theorieService,
                praxisService
        );
        AusbildungsstatusService ausbildungsstatusService = new AusbildungsstatusService(
                repositories.schueler(),
                repositories.ausbildungsStatus(),
                repositories.ausbildungsVertraege()
        );
        AbschlussService abschlussService = new AbschlussService(
                repositories.abschlussAnfragen(),
                repositories.schueler(),
                ausbildungsstatusService
        );

        ApiHandler apiHandler = new ApiHandler(
                ApplicationConfig.defaults(),
                repositories.profile(),
                repositories.databaseMode(),
                repositories::databaseReachable,
                new AuthService(new DemoAccountProvider(repositories.profile(), repositories.schueler())),
                studentService,
                aircraftService,
                lessonService,
                dashboardService,
                schuelerService,
                theorieService,
                praxisService,
                pruefungsService,
                ausbildungsstatusService,
                abschlussService
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", apiHandler);
        server.setExecutor(Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors())));
        server.start();

        System.out.printf(
                "SkyTeam Flight School API listening on http://localhost:%d/api (%s profile)%n",
                port,
                repositories.profile()
        );
    }

    private static int readPort() {
        String configured = System.getProperty("server.port");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("PORT");
        }
        if (configured == null || configured.isBlank()) {
            return 8080;
        }
        return Integer.parseInt(configured);
    }
}


