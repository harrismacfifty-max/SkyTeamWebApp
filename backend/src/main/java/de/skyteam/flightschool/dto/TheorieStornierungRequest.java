package de.skyteam.flightschool.dto;

public record TheorieStornierungRequest(
        String schuelerId,
        String kursId,
        String grund
) {
}
