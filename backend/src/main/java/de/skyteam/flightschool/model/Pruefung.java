package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Pruefung(
        String id,
        String schuelerId,
        LocalDateTime datum,
        String typ
) {
}

