package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.error.BusinessConflictException;

final class PraxisServiceTest {
    private PraxisServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Praxisprüfung erst ab Mindestflugstunden freigeschaltet", () -> {
            try (TestContext context = TestContext.create()) {
                runner.assertFalse(
                        context.praxisService.praxisPruefungFreigeschaltet("SC901"),
                        "SC901 hat weniger als die Mindest-Flugstunden."
                );
                runner.assertTrue(
                        context.praxisService.praxisPruefungFreigeschaltet("SC903"),
                        "SC903 muss mit 12 Flugstunden freigeschaltet sein."
                );
            }
        });

        runner.test("Praxisbuchung scheitert bei nicht verfügbarem Flugzeug", () -> {
            try (TestContext context = TestContext.create()) {
                BusinessConflictException exception = runner.expectThrows(
                        BusinessConflictException.class,
                        () -> context.praxisService.bucheFlugstunde(new PraxisBuchungRequest(
                                "SC901",
                                "FZ004",
                                "P001",
                                "2026-10-20T10:00",
                                60,
                                "Testflug",
                                ""
                        ))
                );
                runner.assertEquals(409, exception.statusCode(), "Fachlicher Konflikt muss HTTP 409 liefern.");
            }
        });

        runner.test("Praxisbuchung scheitert bei Wartungsstatus", () -> {
            try (TestContext context = TestContext.create()) {
                BusinessConflictException exception = runner.expectThrows(
                        BusinessConflictException.class,
                        () -> context.praxisService.bucheFlugstunde(new PraxisBuchungRequest(
                                "SC901",
                                "FZ001",
                                "P001",
                                "2026-10-20T10:00",
                                60,
                                "Testflug",
                                ""
                        ))
                );
                runner.assertEquals(409, exception.statusCode(), "Wartungsstatus muss HTTP 409 liefern.");
            }
        });
    }
}
