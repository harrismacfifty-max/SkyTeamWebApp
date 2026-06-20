package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Wartung(
        String id,
        LocalDateTime datum,
        String typ,
        String notiz,
        String buchungId,
        String flugzeugId
) {
}

