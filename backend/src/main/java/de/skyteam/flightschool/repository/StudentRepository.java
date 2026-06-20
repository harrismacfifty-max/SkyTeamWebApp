package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    List<Student> findAll();

    Optional<Student> findById(long id);

    Student create(Student student);
}


