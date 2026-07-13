package de.skyteam.flightschool.service;

import de.skyteam.flightschool.repository.jdbc.JdbcProcedureCalls;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StoredProcedureContractTest {
    private StoredProcedureContractTest() {
    }

    static void run(TestRunner runner) {
        runner.test("Oracle-Schreibzugriffe verwenden das zentrale Procedure-Package", () -> {
            Map<String, String> calls = calls();
            for (Map.Entry<String, String> entry : calls.entrySet()) {
                runner.assertTrue(
                        entry.getValue().startsWith("{ call " + JdbcProcedureCalls.PACKAGE_NAME + "." + entry.getKey() + "("),
                        "CallableStatement muss " + entry.getKey() + " aus dem zentralen Package aufrufen."
                );
            }
        });

        runner.test("Oracle-Procedure-Aufrufe haben die erwartete Parameterzahl", () -> {
            for (Map.Entry<String, String> entry : calls().entrySet()) {
                long actual = entry.getValue().chars().filter(character -> character == '?').count();
                runner.assertEquals(expectedArities().get(entry.getKey()).longValue(), actual,
                        "Parameterzahl fuer " + entry.getKey() + " muss zum Package passen.");
            }
        });

        runner.test("Oracle-Procedure-Migration deckt alle Java-Aufrufe ab", () -> {
            String sql = Files.readString(findMigration()).toUpperCase();
            runner.assertTrue(sql.contains("CREATE OR REPLACE PACKAGE " + JdbcProcedureCalls.PACKAGE_NAME),
                    "Package-Spezifikation fehlt.");
            runner.assertTrue(sql.contains("CREATE OR REPLACE PACKAGE BODY " + JdbcProcedureCalls.PACKAGE_NAME),
                    "Package-Body fehlt.");
            String specification = sql.substring(0, sql.indexOf("END " + JdbcProcedureCalls.PACKAGE_NAME + ";"));
            for (String procedure : calls().keySet()) {
                runner.assertTrue(sql.contains("PROCEDURE " + procedure + "("),
                        "Procedure " + procedure + " fehlt in der Migration.");
                Matcher declaration = Pattern.compile(
                        "PROCEDURE\\s+" + procedure + "\\s*\\((.*?)\\);",
                        Pattern.DOTALL
                ).matcher(specification);
                runner.assertTrue(declaration.find(), "Package-Spezifikation fuer " + procedure + " fehlt.");
                long actual = declaration.group(1).lines()
                        .map(String::strip)
                        .filter(line -> line.startsWith("P_"))
                        .count();
                runner.assertEquals(expectedArities().get(procedure).longValue(), actual,
                        "SQL-Parameterzahl fuer " + procedure + " muss zum Java-Aufruf passen.");
            }
        });

        runner.test("Oracle-Procedures ueberlassen Java die Transaktionsgrenze", () -> {
            String sql = Files.readString(findMigration()).toUpperCase();
            runner.assertFalse(sql.contains("COMMIT;"), "Die Migration darf kein fest eingebautes COMMIT enthalten.");
            runner.assertFalse(sql.contains("ROLLBACK;"), "Die Migration darf kein fest eingebautes ROLLBACK enthalten.");
        });
    }

    private static Map<String, String> calls() {
        Map<String, String> calls = new LinkedHashMap<>();
        calls.put("AUSBILDUNGSVERTRAG_SPEICHERN", JdbcProcedureCalls.AUSBILDUNGSVERTRAG_SPEICHERN);
        calls.put("AUSBILDUNGSVERTRAG_STATUS", JdbcProcedureCalls.AUSBILDUNGSVERTRAG_STATUS);
        calls.put("SCHUELER_SPEICHERN", JdbcProcedureCalls.SCHUELER_SPEICHERN);
        calls.put("SCHUELER_LOESCHEN", JdbcProcedureCalls.SCHUELER_LOESCHEN);
        calls.put("THEORIE_BUCHEN", JdbcProcedureCalls.THEORIE_BUCHEN);
        calls.put("THEORIE_STORNIEREN", JdbcProcedureCalls.THEORIE_STORNIEREN);
        calls.put("PRAXIS_BUCHEN", JdbcProcedureCalls.PRAXIS_BUCHEN);
        calls.put("PRAXIS_STORNIEREN", JdbcProcedureCalls.PRAXIS_STORNIEREN);
        calls.put("PRUEFUNG_ANMELDEN", JdbcProcedureCalls.PRUEFUNG_ANMELDEN);
        calls.put("PRUEFUNG_ERGEBNIS_SPEICHERN", JdbcProcedureCalls.PRUEFUNG_ERGEBNIS_SPEICHERN);
        calls.put("ABSCHLUSS_ANFRAGE_SPEICHERN", JdbcProcedureCalls.ABSCHLUSS_ANFRAGE_SPEICHERN);
        return calls;
    }

    private static Map<String, Integer> expectedArities() {
        return Map.ofEntries(
                Map.entry("AUSBILDUNGSVERTRAG_SPEICHERN", 6),
                Map.entry("AUSBILDUNGSVERTRAG_STATUS", 2),
                Map.entry("SCHUELER_SPEICHERN", 9),
                Map.entry("SCHUELER_LOESCHEN", 2),
                Map.entry("THEORIE_BUCHEN", 6),
                Map.entry("THEORIE_STORNIEREN", 3),
                Map.entry("PRAXIS_BUCHEN", 9),
                Map.entry("PRAXIS_STORNIEREN", 3),
                Map.entry("PRUEFUNG_ANMELDEN", 6),
                Map.entry("PRUEFUNG_ERGEBNIS_SPEICHERN", 6),
                Map.entry("ABSCHLUSS_ANFRAGE_SPEICHERN", 6)
        );
    }

    private static Path findMigration() {
        Path[] candidates = {
                Path.of("database", "migrations", "stored-procedures.sql"),
                Path.of("..", "database", "migrations", "stored-procedures.sql")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("database/migrations/stored-procedures.sql wurde nicht gefunden.");
    }
}
