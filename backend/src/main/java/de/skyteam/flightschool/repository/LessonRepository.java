package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Lesson;
import java.util.List;
import java.util.Optional;

public interface LessonRepository {
    List<Lesson> findAll();

    Optional<Lesson> findById(long id);

    Lesson create(Lesson lesson);
}


