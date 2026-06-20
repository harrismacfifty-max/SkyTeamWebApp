package de.skyteam.flightschool.model;

import java.math.BigDecimal;

public record Pilot(
        String id,
        String lizenz,
        BigDecimal gehalt,
        String lehrer,
        String verfuegbar,
        String name,
        String vorname,
        String telefon,
        String email
) {
}

