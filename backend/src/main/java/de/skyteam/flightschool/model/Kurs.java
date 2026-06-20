package de.skyteam.flightschool.model;

public record Kurs(
        String id,
        String schuelerId,
        String typ,
        String lehrer,
        String tag
) {
}

