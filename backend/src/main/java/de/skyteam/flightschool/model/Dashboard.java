package de.skyteam.flightschool.model;

import java.util.List;

public record Dashboard(
        int studentCount,
        int aircraftCount,
        int plannedLessonCount,
        List<Lesson> upcomingLessons
) {
}


