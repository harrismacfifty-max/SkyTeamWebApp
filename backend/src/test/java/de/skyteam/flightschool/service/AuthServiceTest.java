package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.LoginResponse;
import java.util.Map;

final class AuthServiceTest {
    private AuthServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Demo-Login liefert Schuelerrolle", () -> {
            AuthService authService = new AuthService();
            LoginResponse response = authService.login(Map.of("username", "demo", "password", "demo"));

            runner.assertEquals("demo", response.username(), "Username muss erhalten bleiben.");
            runner.assertEquals("SCHUELER", response.role(), "demo muss Schuelerrolle erhalten.");
            runner.assertEquals("SC901", response.schuelerId(), "Demo-Schueler muss SC901 zugeordnet sein.");
            runner.assertTrue(authService.currentUser(response.token()).isPresent(), "Session muss aktiv sein.");
        });

        runner.test("Demo2-Login liefert Schuelerverwaltungsrolle", () -> {
            AuthService authService = new AuthService();
            LoginResponse response = authService.login(Map.of("username", "demo2", "password", "demo2"));

            runner.assertEquals("demo2", response.username(), "Username muss erhalten bleiben.");
            runner.assertEquals("SCHUELERVERWALTUNG", response.role(), "demo2 muss Verwaltungsrolle erhalten.");
            runner.assertEquals("", response.schuelerId(), "Verwaltungsrolle ist keinem Schueler fest zugeordnet.");
        });

        runner.test("Ungueltiger Login wird abgelehnt", () -> {
            AuthService authService = new AuthService();
            runner.expectThrows(
                    UnauthorizedException.class,
                    () -> authService.login(Map.of("username", "demo", "password", "falsch"))
            );
        });
    }
}
