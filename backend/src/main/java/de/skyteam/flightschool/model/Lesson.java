package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record Lesson(
        long id,
        long studentId,
        long aircraftId,
        String instructor,
        LocalDateTime scheduledAt,
        int durationMinutes,
        String trainingType,
        String status,
        String notes
) {
}


