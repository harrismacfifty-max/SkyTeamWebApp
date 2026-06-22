package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.dto.TheorieStornierungRequest;
import de.skyteam.flightschool.model.Kurs;

final class TheorieServiceTest {
    private TheorieServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Theorieprüfung erst ab Mindeststunden freigeschaltet", () -> {
            try (TestContext context = TestContext.create()) {
                runner.assertFalse(
                        context.theorieService.theoriePruefungFreigeschaltet("SC901"),
                        "SC901 hat weniger als die Mindest-Theoriestunden."
                );
                runner.assertTrue(
                        context.theorieService.theoriePruefungFreigeschaltet("SC902"),
                        "SC902 muss mit 12 Theoriestunden freigeschaltet sein."
                );
            }
        });

        runner.test("Theoriestunde kann storniert werden und reduziert Fortschritt", () -> {
            try (TestContext context = TestContext.create()) {
                double vorher = context.theorieService.theoriestunden("SC901");
                Kurs kurs = context.theorieService.bucheTheoriekurs(new TheorieBuchungRequest(
                        "SC901",
                        "Theorie - Storno Test",
                        "2026-10-30",
                        90,
                        "Elias Schulz",
                        "Testbuchung"
                ));

                runner.assertEquals(
                        Double.valueOf(vorher + 1.5),
                        Double.valueOf(context.theorieService.theoriestunden("SC901")),
                        "Buchung muss Theoriestunden erhoehen."
                );

                context.theorieService.storniereTheoriekurs(new TheorieStornierungRequest(
                        "SC901",
                        kurs.id(),
                        "Teststorno"
                ));

                runner.assertEquals(
                        Double.valueOf(vorher),
                        Double.valueOf(context.theorieService.theoriestunden("SC901")),
                        "Storno muss Theoriestunden wieder reduzieren."
                );
            }
        });
    }
}
