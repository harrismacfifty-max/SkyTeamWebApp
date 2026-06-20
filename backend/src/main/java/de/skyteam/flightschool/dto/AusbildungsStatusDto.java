package de.skyteam.flightschool.dto;

import de.skyteam.flightschool.model.AusbildungsStatusCode;

public record AusbildungsStatusDto(
        String schuelerId,
        AusbildungsStatusCode status,
        String vertragsStatus,
        double theorieStunden,
        double flugStunden,
        boolean theoriePruefungFreigeschaltet,
        boolean praxisPruefungFreigeschaltet,
        boolean theorieBestanden,
        boolean praxisBestanden,
        boolean pruefungsreif
) {
}
