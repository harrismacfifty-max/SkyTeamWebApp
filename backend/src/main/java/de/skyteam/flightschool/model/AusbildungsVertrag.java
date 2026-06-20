package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record AusbildungsVertrag(
        String id,
        String schuleId,
        LocalDateTime startzeit,
        LocalDateTime endzeit,
        String status,
        String notiz
) {
}

