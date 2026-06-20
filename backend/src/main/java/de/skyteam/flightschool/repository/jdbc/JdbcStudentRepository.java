package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Student;
import de.skyteam.flightschool.repository.StudentRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcStudentRepository implements StudentRepository {
    private final JdbcConnectionFactory connections;

    public JdbcStudentRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Student> findAll() {
        String sql = "select id, first_name, last_name, email, phone, license_goal, status from fs_student order by last_name, first_name";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Student> students = new ArrayList<>();
            while (resultSet.next()) {
                students.add(map(resultSet));
            }
            return students;
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Optional<Student> findById(long id) {
        String sql = "select id, first_name, last_name, email, phone, license_goal, status from fs_student where id = ?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Student create(Student student) {
        String sql = """
                insert into fs_student (id, first_name, last_name, email, phone, license_goal, status)
                values (fs_student_seq.nextval, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.firstName());
            statement.setString(2, student.lastName());
            statement.setString(3, student.email());
            statement.setString(4, student.phone());
            statement.setString(5, student.licenseGoal());
            statement.setString(6, student.status());
            statement.executeUpdate();
            long id = currentSequenceValue(connection, "fs_student_seq");
            return new Student(
                    id,
                    student.firstName(),
                    student.lastName(),
                    student.email(),
                    student.phone(),
                    student.licenseGoal(),
                    student.status()
            );
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static Student map(ResultSet resultSet) throws SQLException {
        return new Student(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("license_goal"),
                resultSet.getString("status")
        );
    }

    private static long currentSequenceValue(Connection connection, String sequence) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select " + sequence + ".currval from dual")) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new SQLException("Sequence did not return a value: " + sequence);
        }
    }

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Database operation failed.", exception);
    }
}


