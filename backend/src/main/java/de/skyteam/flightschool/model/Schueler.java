package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Schueler(
        String id,
        String ausbildungsVertragId,
        LocalDateTime startzeit,
        LocalDateTime endzeit,
        double flugStunden,
        double theorieStunden,
        String notiz,
        String name,
        String vorname
) {
}

