package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.AusbildungsStatusDto;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.model.AusbildungsStatusCode;

final class AusbildungsstatusServiceTest {
    private AusbildungsstatusServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Nicht bestandene Prüfung blockiert Abschluss", () -> {
            try (TestContext context = TestContext.create()) {
                BusinessConflictException exception = runner.expectThrows(
                        BusinessConflictException.class,
                        () -> context.ausbildungsstatusService.schliesseAusbildungAb("SC905")
                );
                runner.assertEquals(409, exception.statusCode(), "Blockierter Abschluss muss HTTP 409 liefern.");
            }
        });

        runner.test("Bestandene Theorie und bestandene Praxis erlauben Abschluss", () -> {
            try (TestContext context = TestContext.create()) {
                AusbildungsStatusDto before = context.ausbildungsstatusService.status("SC906");
                runner.assertTrue(before.theorieBestanden(), "SC906 muss Theorie bestanden haben.");
                runner.assertTrue(before.praxisBestanden(), "SC906 muss Praxis bestanden haben.");
                context.ausbildungsstatusService.schliesseAusbildungAb("SC906");
            }
        });

        runner.test("Abschluss setzt Ausbildungsstatus auf ABGESCHLOSSEN", () -> {
            try (TestContext context = TestContext.create()) {
                AusbildungsStatusDto completed = context.ausbildungsstatusService.schliesseAusbildungAb("SC906");
                runner.assertEquals(
                        AusbildungsStatusCode.ABGESCHLOSSEN,
                        completed.status(),
                        "Abschluss muss Status ABGESCHLOSSEN liefern."
                );
                runner.assertEquals(
                        AusbildungsStatusCode.ABGESCHLOSSEN,
                        context.ausbildungsstatusService.status("SC906").status(),
                        "Folgeabfrage muss ABGESCHLOSSEN liefern."
                );
            }
        });

        runner.test("Ungültige Schüler-ID liefert 404", () -> {
            try (TestContext context = TestContext.create()) {
                NotFoundException exception = runner.expectThrows(
                        NotFoundException.class,
                        () -> context.ausbildungsstatusService.status("SC999999")
                );
                runner.assertEquals(404, exception.statusCode(), "Ungültige Schüler-ID muss HTTP 404 liefern.");
            }
        });
    }
}
