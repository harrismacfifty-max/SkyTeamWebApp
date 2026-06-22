package de.skyteam.flightschool.dto;

public record TheorieStornierungResponse(
        String schuelerId,
        String kursId,
        boolean storniert,
        String message
) {
}
