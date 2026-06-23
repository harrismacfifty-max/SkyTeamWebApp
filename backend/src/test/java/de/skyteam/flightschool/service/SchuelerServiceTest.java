package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.model.Schueler;
import java.util.Map;

final class SchuelerServiceTest {
    private SchuelerServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Schueler anlegen erzeugt Demo-Ausbildungsvertrag", () -> {
            try (TestContext context = TestContext.create()) {
                Schueler schueler = context.schuelerService.create(Map.of(
                        "vorname", "Test",
                        "name", "Verwaltung"
                ));
                AusbildungsVertrag vertrag = context.schuelerService.findVertragBySchuelerId(schueler.id());

                runner.assertEquals(schueler.ausbildungsVertragId(), vertrag.id(), "Schueler muss mit Vertrag verknuepft sein.");
                runner.assertEquals("Unterschrieben", vertrag.status(), "Demo-Vertrag muss Standardstatus erhalten.");
            }
        });

        runner.test("Schueler anlegen validiert Pflichtfelder", () -> {
            try (TestContext context = TestContext.create()) {
                runner.expectThrows(
                        ValidationException.class,
                        () -> context.schuelerService.create(Map.of("vorname", "OhneNachname"))
                );
            }
        });

        runner.test("Ausbildungsvertrag pruefen dokumentiert Pruefung", () -> {
            try (TestContext context = TestContext.create()) {
                AusbildungsVertrag vertrag = context.schuelerService.pruefeVertrag("SC901", Map.of(
                        "pruefer", "Demo Verwaltung",
                        "bemerkung", "Smoke"
                ));

                runner.assertTrue(vertrag.notiz().contains("Vertrag geprueft"), "Vertragsnotiz muss Pruefmarker enthalten.");
                runner.assertTrue(vertrag.notiz().contains("Demo Verwaltung"), "Vertragsnotiz muss Pruefer enthalten.");
            }
        });
    }
}
