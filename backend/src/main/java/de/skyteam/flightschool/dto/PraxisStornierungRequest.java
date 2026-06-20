package de.skyteam.flightschool.dto;

public record PraxisStornierungRequest(
        String schuelerId,
        String flugId,
        String grund
) {
}

