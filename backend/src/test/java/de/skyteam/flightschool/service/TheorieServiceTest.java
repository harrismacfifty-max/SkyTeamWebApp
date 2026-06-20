package de.skyteam.flightschool.service;

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
    }
}
