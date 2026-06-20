package de.skyteam.flightschool.model;

public record Student(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseGoal,
        String status
) {
}


