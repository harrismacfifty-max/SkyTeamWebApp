package de.skyteam.flightschool.model;

public record LoginResponse(
        String token,
        String displayName
) {
}


