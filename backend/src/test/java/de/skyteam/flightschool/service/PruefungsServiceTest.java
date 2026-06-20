package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.model.Pruefung;

final class PruefungsServiceTest {
    private PruefungsServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Theorieprüfung kann angemeldet werden", () -> {
            try (TestContext context = TestContext.create()) {
                Pruefung pruefung = context.pruefungsService.meldeTheoriePruefungAn(new PruefungAnmeldungRequest(
                        "SC902",
                        "Theoriepruefung",
                        "2026-10-12",
                        "P001",
                        "Test"
                ));
                runner.assertEquals("SC902", pruefung.schuelerId(), "Prüfung muss dem Schüler zugeordnet sein.");
                runner.assertTrue(pruefung.typ().contains("Theorie"), "Prüfungsart muss Theorie enthalten.");
            }
        });

        runner.test("Praxisprüfung kann angemeldet werden", () -> {
            try (TestContext context = TestContext.create()) {
                Pruefung pruefung = context.pruefungsService.meldePraxisPruefungAn(new PruefungAnmeldungRequest(
                        "SC903",
                        "Praxispruefung",
                        "2026-10-13",
                        "P002",
                        "Test"
                ));
                runner.assertEquals("SC903", pruefung.schuelerId(), "Prüfung muss dem Schüler zugeordnet sein.");
                runner.assertTrue(pruefung.typ().contains("Praxis"), "Prüfungsart muss Praxis enthalten.");
            }
        });
    }
}
