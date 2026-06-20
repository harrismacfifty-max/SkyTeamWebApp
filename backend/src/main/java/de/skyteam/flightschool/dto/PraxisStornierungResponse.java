package de.skyteam.flightschool.dto;

public record PraxisStornierungResponse(
        String schuelerId,
        String flugId,
        boolean storniert,
        String message
) {
}

