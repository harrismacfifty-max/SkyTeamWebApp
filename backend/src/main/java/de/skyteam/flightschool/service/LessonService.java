package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.repository.AircraftRepository;
import de.skyteam.flightschool.repository.LessonRepository;
import de.skyteam.flightschool.repository.StudentRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public final class LessonService {
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final AircraftRepository aircraftRepository;

    public LessonService(
            LessonRepository lessonRepository,
            StudentRepository studentRepository,
            AircraftRepository aircraftRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.studentRepository = studentRepository;
        this.aircraftRepository = aircraftRepository;
    }

    public List<Lesson> list() {
        return lessonRepository.findAll();
    }

    public Lesson create(Map<String, String> data) {
        long studentId = ServiceSupport.requiredLong(data, "studentId");
        long aircraftId = ServiceSupport.requiredLong(data, "aircraftId");
        studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
        aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new NotFoundException("Flugzeug wurde nicht gefunden."));

        int durationMinutes = ServiceSupport.optionalInt(data, "durationMinutes", 60);
        if (durationMinutes < 15) {
            throw new ValidationException("durationMinutes muss mindestens 15 sein.");
        }

        Lesson lesson = new Lesson(
                0,
                studentId,
                aircraftId,
                ServiceSupport.required(data, "instructor"),
                parseDateTime(ServiceSupport.required(data, "scheduledAt")),
                durationMinutes,
                ServiceSupport.required(data, "trainingType"),
                ServiceSupport.optional(data, "status", "Geplant"),
                ServiceSupport.optional(data, "notes", "")
        );
        return lessonRepository.create(lesson);
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ValidationException("scheduledAt muss im Format yyyy-MM-ddTHH:mm vorliegen.");
        }
    }
}


