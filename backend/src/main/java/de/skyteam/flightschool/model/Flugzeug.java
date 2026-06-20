package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Flugzeug(
        String id,
        LocalDateTime baujahr,
        String verfuegbarkeit,
        String status
) {
}

