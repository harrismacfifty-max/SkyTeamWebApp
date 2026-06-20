package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.repository.LessonRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryLessonRepository implements LessonRepository {
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Lesson> lessons = new ConcurrentHashMap<>();

    public InMemoryLessonRepository() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        create(new Lesson(0, 1, 1, "M. Keller", tomorrow, 90, "Platzrunden", "Geplant", "Vorflugkontrolle einplanen"));
        create(new Lesson(0, 2, 2, "S. Weber", tomorrow.plusDays(2), 120, "Navigation", "Geplant", "Route EDLE-EDLN vorbereiten"));
    }

    @Override
    public List<Lesson> findAll() {
        return lessons.values().stream()
                .sorted(Comparator.comparing(Lesson::scheduledAt).thenComparingLong(Lesson::id))
                .toList();
    }

    @Override
    public Optional<Lesson> findById(long id) {
        return Optional.ofNullable(lessons.get(id));
    }

    @Override
    public Lesson create(Lesson lessonToCreate) {
        long id = ids.incrementAndGet();
        Lesson saved = new Lesson(
                id,
                lessonToCreate.studentId(),
                lessonToCreate.aircraftId(),
                lessonToCreate.instructor(),
                lessonToCreate.scheduledAt(),
                lessonToCreate.durationMinutes(),
                lessonToCreate.trainingType(),
                lessonToCreate.status(),
                lessonToCreate.notes()
        );
        lessons.put(id, saved);
        return saved;
    }
}


