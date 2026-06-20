package de.skyteam.flightschool.model;

public record Aircraft(
        long id,
        String registration,
        String model,
        String status,
        int totalHours
) {
}


