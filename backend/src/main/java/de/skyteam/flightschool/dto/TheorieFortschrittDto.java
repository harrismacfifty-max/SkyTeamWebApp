package de.skyteam.flightschool.dto;

public record TheorieFortschrittDto(
        String schuelerId,
        double theorieStunden,
        double mindestTheorieStunden,
        boolean theoriePruefungFreigeschaltet
) {
}

