package de.skyteam.flightschool.dto;

public record SchuelerDto(
        String id,
        String vorname,
        String nachname,
        String ausbildungsVertragId,
        double theorieStunden,
        double flugStunden,
        String status
) {
}
