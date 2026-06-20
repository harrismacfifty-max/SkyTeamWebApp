package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.model.Student;
import de.skyteam.flightschool.repository.StudentRepository;
import java.util.List;
import java.util.Map;

public final class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> list() {
        return studentRepository.findAll();
    }

    public Student create(Map<String, String> data) {
        String email = ServiceSupport.required(data, "email");
        boolean duplicateEmail = studentRepository.findAll().stream()
                .anyMatch(student -> student.email().equalsIgnoreCase(email));
        if (duplicateEmail) {
            throw new BusinessConflictException("Schueler mit dieser E-Mail existiert bereits.");
        }

        Student student = new Student(
                0,
                ServiceSupport.required(data, "firstName"),
                ServiceSupport.required(data, "lastName"),
                email,
                ServiceSupport.optional(data, "phone", ""),
                ServiceSupport.optional(data, "licenseGoal", "PPL(A)"),
                ServiceSupport.optional(data, "status", "Aktiv")
        );
        return studentRepository.create(student);
    }
}


