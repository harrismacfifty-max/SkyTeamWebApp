package de.skyteam.flightschool.model;

public record AuthUser(
        String username,
        String displayName,
        UserRole role,
        String schuelerId
) {
}
