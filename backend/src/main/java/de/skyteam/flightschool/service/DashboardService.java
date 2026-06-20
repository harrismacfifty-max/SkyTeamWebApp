package de.skyteam.flightschool.service;

import de.skyteam.flightschool.model.Dashboard;
import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.repository.AircraftRepository;
import de.skyteam.flightschool.repository.LessonRepository;
import de.skyteam.flightschool.repository.StudentRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class DashboardService {
    private final StudentRepository students;
    private final AircraftRepository aircraft;
    private final LessonRepository lessons;

    public DashboardService(StudentRepository students, AircraftRepository aircraft, LessonRepository lessons) {
        this.students = students;
        this.aircraft = aircraft;
        this.lessons = lessons;
    }

    public Dashboard dashboard() {
        List<Lesson> allLessons = lessons.findAll();
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        List<Lesson> upcoming = allLessons.stream()
                .filter(lesson -> !lesson.scheduledAt().isBefore(now))
                .sorted(Comparator.comparing(Lesson::scheduledAt).thenComparingLong(Lesson::id))
                .limit(5)
                .toList();
        int plannedCount = (int) allLessons.stream()
                .filter(lesson -> "Geplant".equalsIgnoreCase(lesson.status()))
                .count();
        return new Dashboard(students.findAll().size(), aircraft.findAll().size(), plannedCount, upcoming);
    }
}


