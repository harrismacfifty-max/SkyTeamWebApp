package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Flug(
        String id,
        String schuelerId,
        String flugzeugId,
        LocalDateTime datum,
        LocalDateTime startzeit,
        LocalDateTime endzeit,
        String startFlughafen,
        String zielFlughafen,
        String flugArt
) {
}

