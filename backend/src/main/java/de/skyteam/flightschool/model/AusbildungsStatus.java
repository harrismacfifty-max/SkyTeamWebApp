package de.skyteam.flightschool.model;

public record AusbildungsStatus(
        String schuelerId,
        String ausbildungsVertragId,
        String vertragsStatus,
        double theorieStunden,
        double flugStunden,
        boolean theorieBestanden,
        boolean praxisBestanden,
        boolean pruefungsreif
) {
}

