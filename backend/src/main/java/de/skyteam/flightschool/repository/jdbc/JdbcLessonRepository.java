package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.repository.LessonRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcLessonRepository implements LessonRepository {
    private final JdbcConnectionFactory connections;

    public JdbcLessonRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Lesson> findAll() {
        String sql = """
                select id, student_id, aircraft_id, instructor, scheduled_at, duration_minutes,
                       training_type, status, notes
                from fs_lesson
                order by scheduled_at, id
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Lesson> lessons = new ArrayList<>();
            while (resultSet.next()) {
                lessons.add(map(resultSet));
            }
            return lessons;
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Optional<Lesson> findById(long id) {
        String sql = """
                select id, student_id, aircraft_id, instructor, scheduled_at, duration_minutes,
                       training_type, status, notes
                from fs_lesson
                where id = ?
                """;
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
    public Lesson create(Lesson lesson) {
        String sql = """
                insert into fs_lesson (
                    id, student_id, aircraft_id, instructor, scheduled_at, duration_minutes,
                    training_type, status, notes
                )
                values (fs_lesson_seq.nextval, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lesson.studentId());
            statement.setLong(2, lesson.aircraftId());
            statement.setString(3, lesson.instructor());
            statement.setTimestamp(4, Timestamp.valueOf(lesson.scheduledAt()));
            statement.setInt(5, lesson.durationMinutes());
            statement.setString(6, lesson.trainingType());
            statement.setString(7, lesson.status());
            statement.setString(8, lesson.notes());
            statement.executeUpdate();
            long id = currentSequenceValue(connection, "fs_lesson_seq");
            return new Lesson(
                    id,
                    lesson.studentId(),
                    lesson.aircraftId(),
                    lesson.instructor(),
                    lesson.scheduledAt(),
                    lesson.durationMinutes(),
                    lesson.trainingType(),
                    lesson.status(),
                    lesson.notes()
            );
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static Lesson map(ResultSet resultSet) throws SQLException {
        return new Lesson(
                resultSet.getLong("id"),
                resultSet.getLong("student_id"),
                resultSet.getLong("aircraft_id"),
                resultSet.getString("instructor"),
                resultSet.getTimestamp("scheduled_at").toLocalDateTime(),
                resultSet.getInt("duration_minutes"),
                resultSet.getString("training_type"),
                resultSet.getString("status"),
                resultSet.getString("notes")
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


