package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.LoginResponse;
import java.util.Map;

final class AuthServiceTest {
    private AuthServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Alle Demo-Schuelerkonten liefern ihre eigene Zuordnung", () -> {
            try (TestContext context = TestContext.create()) {
                AuthService authService = demoAuthService(context);
                String[][] accounts = {
                        {"sc901", "demo901", "SC901", "Jonas Keller"},
                        {"sc902", "demo902", "SC902", "Nico Berger"},
                        {"sc903", "demo903", "SC903", "Mina Sommer"},
                        {"sc904", "demo904", "SC904", "Mara Seidel"},
                        {"sc905", "demo905", "SC905", "Lea Wagner"},
                        {"sc906", "demo906", "SC906", "Oskar Lange"},
                        {"sc907", "demo907", "SC907", "Ella Hartmann"}
                };

                for (String[] account : accounts) {
                    LoginResponse response = authService.login(Map.of("username", account[0], "password", account[1]));
                    runner.assertEquals(account[0], response.username(), "Username muss erhalten bleiben.");
                    runner.assertEquals("SCHUELER", response.role(), account[0] + " muss Schuelerrolle erhalten.");
                    runner.assertEquals(account[2], response.schuelerId(), account[0] + " muss dem richtigen Schueler zugeordnet sein.");
                    runner.assertEquals(account[3], response.displayName(), "Anzeigename muss aus den Demo-Schuelerdaten stammen.");
                    runner.assertTrue(authService.currentUser(response.token()).isPresent(), "Session muss aktiv sein.");
                }
            }
        });

        runner.test("Demo2-Login liefert Schuelerverwaltungsrolle ohne Schueler-ID", () -> {
            try (TestContext context = TestContext.create()) {
                AuthService authService = demoAuthService(context);
                LoginResponse response = authService.login(Map.of("username", "demo2", "password", "demo2"));

                runner.assertEquals("demo2", response.username(), "Username muss erhalten bleiben.");
                runner.assertEquals("SCHUELERVERWALTUNG", response.role(), "demo2 muss Verwaltungsrolle erhalten.");
                runner.assertEquals(null, response.schuelerId(), "Verwaltungsrolle ist keinem Schueler fest zugeordnet.");
            }
        });

        runner.test("Demo-Login bleibt dokumentierter Legacy-Alias fuer SC901", () -> {
            try (TestContext context = TestContext.create()) {
                AuthService authService = demoAuthService(context);
                LoginResponse response = authService.login(Map.of("username", "demo", "password", "demo"));

                runner.assertEquals("demo", response.username(), "Legacy-Username muss erhalten bleiben.");
                runner.assertEquals("SCHUELER", response.role(), "Legacy-Alias muss Schuelerrolle erhalten.");
                runner.assertEquals("SC901", response.schuelerId(), "Legacy-Alias muss eindeutig SC901 zugeordnet bleiben.");
                runner.assertEquals("Jonas Keller", response.displayName(), "Legacy-Alias muss denselben Anzeigenamen wie SC901 liefern.");
            }
        });

        runner.test("Ungueltige Zugangsdaten werden ohne Detailpreisgabe abgelehnt", () -> {
            try (TestContext context = TestContext.create()) {
                AuthService authService = demoAuthService(context);
                UnauthorizedException wrongPassword = runner.expectThrows(
                        UnauthorizedException.class,
                        () -> authService.login(Map.of("username", "sc901", "password", "falsch"))
                );
                UnauthorizedException unknownUser = runner.expectThrows(
                        UnauthorizedException.class,
                        () -> authService.login(Map.of("username", "unbekannt", "password", "demo901"))
                );

                runner.assertEquals("Ungueltige Zugangsdaten.", wrongPassword.getMessage(), "Falsches Passwort darf kein Detail verraten.");
                runner.assertEquals(wrongPassword.getMessage(), unknownUser.getMessage(), "Unbekannter Benutzer und falsches Passwort muessen dieselbe Meldung liefern.");
            }
        });

        runner.test("Oracle-Profil aktiviert keine Demo-Konten", () -> {
            try (TestContext context = TestContext.create()) {
                AuthService authService = new AuthService(new DemoAccountProvider("oracle", context.schuelerRepository));
                runner.expectThrows(
                        UnauthorizedException.class,
                        () -> authService.login(Map.of("username", "sc901", "password", "demo901"))
                );
                runner.expectThrows(
                        UnauthorizedException.class,
                        () -> authService.login(Map.of("username", "demo2", "password", "demo2"))
                );
            }
        });
    }

    private static AuthService demoAuthService(TestContext context) {
        return new AuthService(new DemoAccountProvider("demo", context.schuelerRepository));
    }
}
