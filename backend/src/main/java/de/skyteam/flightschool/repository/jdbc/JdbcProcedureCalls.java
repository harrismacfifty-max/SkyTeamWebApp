package de.skyteam.flightschool.repository.jdbc;

/** Central Oracle package contract used by the JDBC write repositories. */
public final class JdbcProcedureCalls {
    public static final String PACKAGE_NAME = "SKYTEAM_WEBAPP_API";

    public static final String AUSBILDUNGSVERTRAG_SPEICHERN = call("AUSBILDUNGSVERTRAG_SPEICHERN", 6);
    public static final String AUSBILDUNGSVERTRAG_STATUS = call("AUSBILDUNGSVERTRAG_STATUS", 2);
    public static final String SCHUELER_SPEICHERN = call("SCHUELER_SPEICHERN", 9);
    public static final String SCHUELER_LOESCHEN = call("SCHUELER_LOESCHEN", 2);
    public static final String THEORIE_BUCHEN = call("THEORIE_BUCHEN", 6);
    public static final String THEORIE_STORNIEREN = call("THEORIE_STORNIEREN", 3);
    public static final String PRAXIS_BUCHEN = call("PRAXIS_BUCHEN", 9);
    public static final String PRAXIS_STORNIEREN = call("PRAXIS_STORNIEREN", 3);
    public static final String PRUEFUNG_ANMELDEN = call("PRUEFUNG_ANMELDEN", 6);
    public static final String PRUEFUNG_ERGEBNIS_SPEICHERN = call("PRUEFUNG_ERGEBNIS_SPEICHERN", 6);
    public static final String ABSCHLUSS_ANFRAGE_SPEICHERN = call("ABSCHLUSS_ANFRAGE_SPEICHERN", 6);

    private JdbcProcedureCalls() {
    }

    private static String call(String procedure, int parameterCount) {
        return "{ call " + PACKAGE_NAME + "." + procedure + "("
                + "?,".repeat(parameterCount).replaceAll(",$", "")
                + ") }";
    }
}
