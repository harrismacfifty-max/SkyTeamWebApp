package de.skyteam.flightschool.dto;

public record TheorieBuchungRequest(
        String schuelerId,
        String thema,
        String termin,
        int dauerMinuten,
        String dozent,
        String notizen
) {
}
