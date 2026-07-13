package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.AbschlussAnfrageDto;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.model.AbschlussAnfrageStatus;

final class AbschlussServiceTest {
    private AbschlussServiceTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Schueler kann eigene Abschlussanfrage stellen", () -> {
            try (TestContext context = TestContext.create()) {
                AbschlussAnfrageDto anfrage = context.abschlussService.requestAbschluss("SC901");

                runner.assertEquals("SC901", anfrage.schuelerId(), "Anfrage muss dem Schueler gehoeren.");
                runner.assertEquals(AbschlussAnfrageStatus.ANGEFRAGT, anfrage.status(), "Neue Anfrage muss ANGEFRAGT sein.");
                runner.assertFalse(anfrage.bestaetigungMoeglich(), "SC901 erfuellt die Abnahmekriterien noch nicht.");
            }
        });

        runner.test("Eigene Abschlussanfrage liefert KEINE_ANFRAGE ohne Antrag", () -> {
            try (TestContext context = TestContext.create()) {
                AbschlussAnfrageDto anfrage = context.abschlussService.getMeineAbschlussanfrage("SC902");

                runner.assertEquals(AbschlussAnfrageStatus.KEINE_ANFRAGE, anfrage.status(), "Ohne Antrag muss KEINE_ANFRAGE geliefert werden.");
            }
        });

        runner.test("Abgeschlossene Ausbildung kann keine neue Abschlussanfrage stellen", () -> {
            try (TestContext context = TestContext.create()) {
                BusinessConflictException exception = runner.expectThrows(
                        BusinessConflictException.class,
                        () -> context.abschlussService.requestAbschluss("SC907")
                );

                runner.assertTrue(exception.getMessage().contains("bereits abgeschlossen"), "Fehlermeldung muss den abgeschlossenen Status erklaeren.");
                runner.assertEquals(
                        AbschlussAnfrageStatus.KEINE_ANFRAGE,
                        context.abschlussService.getMeineAbschlussanfrage("SC907").status(),
                        "Blockierter Versuch darf keine Anfrage anlegen."
                );
            }
        });

        runner.test("Verwaltung sieht offene Abschlussanfragen", () -> {
            try (TestContext context = TestContext.create()) {
                runner.assertTrue(
                        context.abschlussService.findOffeneAbschlussanfragen().stream().anyMatch(anfrage -> "SC906".equals(anfrage.schuelerId())),
                        "Demo-Anfrage fuer SC906 muss sichtbar sein."
                );
            }
        });

        runner.test("Abschlussbestaetigung scheitert bei fehlenden Abnahmekriterien", () -> {
            try (TestContext context = TestContext.create()) {
                AbschlussAnfrageDto anfrage = context.abschlussService.requestAbschluss("SC901");

                runner.expectThrows(
                        BusinessConflictException.class,
                        () -> context.abschlussService.bestaetigeAbschluss(anfrage.id())
                );
            }
        });

        runner.test("Abschlussbestaetigung schliesst Ausbildung ab", () -> {
            try (TestContext context = TestContext.create()) {
                AbschlussAnfrageDto bestaetigt = context.abschlussService.bestaetigeAbschluss("AA906");

                runner.assertEquals(AbschlussAnfrageStatus.ABGESCHLOSSEN, bestaetigt.status(), "Bestaetigung muss Anfrage abschliessen.");
                runner.assertEquals("ABGESCHLOSSEN", context.ausbildungsstatusService.status("SC906").status().name(), "Ausbildung muss abgeschlossen sein.");
            }
        });
    }
}
