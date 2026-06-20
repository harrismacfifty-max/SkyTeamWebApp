package de.skyteam.flightschool.api;

import de.skyteam.flightschool.config.ApplicationConfig;
import de.skyteam.flightschool.dto.ApiResponse;
import de.skyteam.flightschool.dto.AusbildungsStatusDto;
import de.skyteam.flightschool.dto.PraxisFortschrittDto;
import de.skyteam.flightschool.dto.PraxisStornierungResponse;
import de.skyteam.flightschool.dto.PruefungsErgebnisStatusDto;
import de.skyteam.flightschool.dto.TheorieFortschrittDto;
import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.model.Dashboard;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.model.Lesson;
import de.skyteam.flightschool.model.LoginResponse;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.model.Student;
import java.util.List;

public final class ApiJson {
    private ApiJson() {
    }

    public static String response(ApiResponse<?> response) {
        return JsonUtil.object(JsonUtil.fields(
                "success", response.success(),
                "message", response.message(),
                "data", response.data(),
                "errors", response.errors()
        ));
    }

    public static ApiResponse<JsonUtil.RawJson> success(String message, String dataJson) {
        return ApiResponse.success(message, JsonUtil.raw(dataJson));
    }

    public static String health() {
        return JsonUtil.object(JsonUtil.fields("status", "ok"));
    }

    public static String version(ApplicationConfig config) {
        return JsonUtil.object(JsonUtil.fields(
                "name", config.applicationName(),
                "version", config.version()
        ));
    }

    public static String theorieFortschritt(TheorieFortschrittDto dto) {
        return JsonUtil.object(JsonUtil.fields(
                "schuelerId", dto.schuelerId(),
                "theorieStunden", dto.theorieStunden(),
                "mindestTheorieStunden", dto.mindestTheorieStunden(),
                "theoriePruefungFreigeschaltet", dto.theoriePruefungFreigeschaltet()
        ));
    }

    public static String praxisFortschritt(PraxisFortschrittDto dto) {
        return JsonUtil.object(JsonUtil.fields(
                "schuelerId", dto.schuelerId(),
                "flugStunden", dto.flugStunden(),
                "mindestFlugStunden", dto.mindestFlugStunden(),
                "praxisPruefungFreigeschaltet", dto.praxisPruefungFreigeschaltet()
        ));
    }

    public static String ausbildungsStatus(AusbildungsStatusDto dto) {
        return JsonUtil.object(JsonUtil.fields(
                "schuelerId", dto.schuelerId(),
                "status", dto.status().name(),
                "vertragsStatus", dto.vertragsStatus(),
                "theorieStunden", dto.theorieStunden(),
                "flugStunden", dto.flugStunden(),
                "theoriePruefungFreigeschaltet", dto.theoriePruefungFreigeschaltet(),
                "praxisPruefungFreigeschaltet", dto.praxisPruefungFreigeschaltet(),
                "theorieBestanden", dto.theorieBestanden(),
                "praxisBestanden", dto.praxisBestanden(),
                "pruefungsreif", dto.pruefungsreif()
        ));
    }

    public static String kurs(Kurs kurs) {
        return JsonUtil.object(JsonUtil.fields(
                "id", kurs.id(),
                "schuelerId", kurs.schuelerId(),
                "typ", kurs.typ(),
                "lehrer", kurs.lehrer(),
                "tag", kurs.tag()
        ));
    }

    public static String flug(Flug flug) {
        return JsonUtil.object(JsonUtil.fields(
                "id", flug.id(),
                "schuelerId", flug.schuelerId(),
                "flugzeugId", flug.flugzeugId(),
                "datum", flug.datum(),
                "startzeit", flug.startzeit(),
                "endzeit", flug.endzeit(),
                "startFlughafen", flug.startFlughafen(),
                "zielFlughafen", flug.zielFlughafen(),
                "flugArt", flug.flugArt()
        ));
    }

    public static String pruefung(Pruefung pruefung) {
        return JsonUtil.object(JsonUtil.fields(
                "id", pruefung.id(),
                "schuelerId", pruefung.schuelerId(),
                "datum", pruefung.datum(),
                "typ", pruefung.typ()
        ));
    }

    public static String pruefungsErgebnisStatus(PruefungsErgebnisStatusDto dto) {
        return JsonUtil.object(JsonUtil.fields(
                "pruefungId", dto.pruefungId(),
                "bestanden", dto.bestanden(),
                "wiederholungsbedarf", dto.wiederholungsbedarf(),
                "message", dto.message()
        ));
    }

    public static String praxisStornierung(PraxisStornierungResponse response) {
        return JsonUtil.object(JsonUtil.fields(
                "schuelerId", response.schuelerId(),
                "flugId", response.flugId(),
                "storniert", response.storniert(),
                "message", response.message()
        ));
    }

    public static String schuelerList(List<Schueler> schueler) {
        return JsonUtil.arrayOfJson(schueler.stream().map(ApiJson::schueler).toList());
    }

    public static String schueler(Schueler schueler) {
        return JsonUtil.object(JsonUtil.fields(
                "id", schueler.id(),
                "ausbildungsVertragId", schueler.ausbildungsVertragId(),
                "startzeit", schueler.startzeit(),
                "endzeit", schueler.endzeit(),
                "flugStunden", schueler.flugStunden(),
                "theorieStunden", schueler.theorieStunden(),
                "notiz", schueler.notiz(),
                "name", schueler.name(),
                "vorname", schueler.vorname()
        ));
    }

    public static String kurse(List<Kurs> kurse) {
        return JsonUtil.arrayOfJson(kurse.stream().map(ApiJson::kurs).toList());
    }

    public static String fluege(List<Flug> fluege) {
        return JsonUtil.arrayOfJson(fluege.stream().map(ApiJson::flug).toList());
    }

    public static String pruefungen(List<Pruefung> pruefungen) {
        return JsonUtil.arrayOfJson(pruefungen.stream().map(ApiJson::pruefung).toList());
    }

    public static String theorieDetails(TheorieFortschrittDto fortschritt, List<Kurs> kurse) {
        return JsonUtil.object(JsonUtil.fields(
                "fortschritt", JsonUtil.raw(theorieFortschritt(fortschritt)),
                "kurse", JsonUtil.raw(kurse(kurse))
        ));
    }

    public static String praxisDetails(PraxisFortschrittDto fortschritt, List<Flug> fluege) {
        return JsonUtil.object(JsonUtil.fields(
                "fortschritt", JsonUtil.raw(praxisFortschritt(fortschritt)),
                "fluege", JsonUtil.raw(fluege(fluege))
        ));
    }

    public static String login(LoginResponse response) {
        return JsonUtil.object(JsonUtil.fields(
                "token", response.token(),
                "displayName", response.displayName()
        ));
    }

    public static String authUser(String username) {
        return JsonUtil.object(JsonUtil.fields(
                "authenticated", true,
                "username", username,
                "displayName", username
        ));
    }

    public static String logout(boolean loggedOut) {
        return JsonUtil.object(JsonUtil.fields(
                "authenticated", false,
                "loggedOut", loggedOut
        ));
    }

    public static String deleted(String id) {
        return JsonUtil.object(JsonUtil.fields(
                "id", id,
                "deleted", true
        ));
    }

    public static String dashboard(Dashboard dashboard) {
        String upcomingLessons = JsonUtil.arrayOfJson(dashboard.upcomingLessons().stream()
                .map(ApiJson::lesson)
                .toList());
        return JsonUtil.object(JsonUtil.fields(
                "studentCount", dashboard.studentCount(),
                "aircraftCount", dashboard.aircraftCount(),
                "plannedLessonCount", dashboard.plannedLessonCount(),
                "upcomingLessons", JsonUtil.raw(upcomingLessons)
        ));
    }

    public static String students(List<Student> students) {
        return JsonUtil.arrayOfJson(students.stream().map(ApiJson::student).toList());
    }

    public static String student(Student student) {
        return JsonUtil.object(JsonUtil.fields(
                "id", student.id(),
                "firstName", student.firstName(),
                "lastName", student.lastName(),
                "email", student.email(),
                "phone", student.phone(),
                "licenseGoal", student.licenseGoal(),
                "status", student.status()
        ));
    }

    public static String aircraftList(List<Aircraft> aircraft) {
        return JsonUtil.arrayOfJson(aircraft.stream().map(ApiJson::aircraft).toList());
    }

    public static String aircraft(Aircraft aircraft) {
        return JsonUtil.object(JsonUtil.fields(
                "id", aircraft.id(),
                "registration", aircraft.registration(),
                "model", aircraft.model(),
                "status", aircraft.status(),
                "totalHours", aircraft.totalHours()
        ));
    }

    public static String lessons(List<Lesson> lessons) {
        return JsonUtil.arrayOfJson(lessons.stream().map(ApiJson::lesson).toList());
    }

    public static String lesson(Lesson lesson) {
        return JsonUtil.object(JsonUtil.fields(
                "id", lesson.id(),
                "studentId", lesson.studentId(),
                "aircraftId", lesson.aircraftId(),
                "instructor", lesson.instructor(),
                "scheduledAt", lesson.scheduledAt().toString(),
                "durationMinutes", lesson.durationMinutes(),
                "trainingType", lesson.trainingType(),
                "status", lesson.status(),
                "notes", lesson.notes()
        ));
    }
}


