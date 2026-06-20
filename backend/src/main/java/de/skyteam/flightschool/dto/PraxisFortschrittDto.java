package de.skyteam.flightschool.dto;

public record PraxisFortschrittDto(
        String schuelerId,
        double flugStunden,
        double mindestFlugStunden,
        boolean praxisPruefungFreigeschaltet
) {
}

