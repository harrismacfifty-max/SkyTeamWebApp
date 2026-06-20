package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Student;
import de.skyteam.flightschool.repository.StudentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryStudentRepository implements StudentRepository {
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Student> students = new ConcurrentHashMap<>();

    public InMemoryStudentRepository() {
        create(new Student(0, "Lena", "Hoffmann", "lena.hoffmann@example.com", "+49 201 1001", "PPL(A)", "Aktiv"));
        create(new Student(0, "Jonas", "Richter", "jonas.richter@example.com", "+49 201 1002", "LAPL(A)", "Aktiv"));
    }

    @Override
    public List<Student> findAll() {
        return students.values().stream()
                .sorted(Comparator.comparing(Student::lastName).thenComparing(Student::firstName))
                .toList();
    }

    @Override
    public Optional<Student> findById(long id) {
        return Optional.ofNullable(students.get(id));
    }

    @Override
    public Student create(Student studentToCreate) {
        long id = ids.incrementAndGet();
        Student saved = new Student(
                id,
                studentToCreate.firstName(),
                studentToCreate.lastName(),
                studentToCreate.email(),
                studentToCreate.phone(),
                studentToCreate.licenseGoal(),
                studentToCreate.status()
        );
        students.put(id, saved);
        return saved;
    }
}


