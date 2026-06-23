package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SchuelerService {
    private final SchuelerRepository schuelerRepository;
    private final AusbildungsVertragRepository ausbildungsVertragRepository;

    public SchuelerService(
            SchuelerRepository schuelerRepository,
            AusbildungsVertragRepository ausbildungsVertragRepository
    ) {
        this.schuelerRepository = schuelerRepository;
        this.ausbildungsVertragRepository = ausbildungsVertragRepository;
    }

    public List<Schueler> findAll() {
        return schuelerRepository.findAll();
    }

    public Schueler findById(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        return schuelerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    public Schueler create(Map<String, String> body) {
        String name = required(body, "name");
        String vorname = required(body, "vorname");
        String schuelerId = optional(body, "id", "");
        if (schuelerId.isBlank()) {
            schuelerId = schuelerRepository.nextId();
        }
        if (schuelerRepository.findById(schuelerId).isPresent()) {
            throw new ValidationException("SchuelerId existiert bereits.");
        }

        String vertragId = optional(body, "ausbildungsVertragId", "");
        if (vertragId.isBlank()) {
            vertragId = ausbildungsVertragRepository.nextId();
        }

        LocalDateTime start = optionalDateTime(body, "startzeit", LocalDate.now().atStartOfDay());
        LocalDateTime end = optionalDateTime(body, "endzeit", start.plusYears(2));
        double theorieStunden = optionalDouble(body, "theorieStunden", 0.0);
        double flugStunden = optionalDouble(body, "flugStunden", 0.0);
        String notiz = optional(body, "notiz", "Manuell angelegter Demo-Schueler");

        AusbildungsVertrag vertrag = new AusbildungsVertrag(
                vertragId,
                optional(body, "schuleId", "S001"),
                start,
                end,
                optional(body, "vertragsStatus", "Unterschrieben"),
                notiz
        );
        Schueler schueler = new Schueler(
                schuelerId,
                vertragId,
                start,
                end,
                flugStunden,
                theorieStunden,
                notiz,
                name,
                vorname
        );

        ausbildungsVertragRepository.save(vertrag);
        return schuelerRepository.save(schueler);
    }

    public void delete(String id) {
        findById(id);
        if (!schuelerRepository.deleteById(id)) {
            throw new NotFoundException("Schueler wurde nicht gefunden.");
        }
    }

    public AusbildungsVertrag findVertragBySchuelerId(String id) {
        findById(id);
        return ausbildungsVertragRepository.findBySchuelerId(id)
                .orElseThrow(() -> new NotFoundException("Ausbildungsvertrag wurde nicht gefunden."));
    }

    public AusbildungsVertrag pruefeVertrag(String schuelerId, Map<String, String> body) {
        AusbildungsVertrag current = findVertragBySchuelerId(schuelerId);
        String pruefer = optional(body, "pruefer", "Schuelerverwaltung");
        String bemerkung = optional(body, "bemerkung", "Vertrag fachlich geprueft");
        String marker = "Vertrag geprueft durch " + pruefer + ": " + bemerkung;
        String notiz = appendMarker(current.notiz(), marker);
        return ausbildungsVertragRepository.save(new AusbildungsVertrag(
                current.id(),
                current.schuleId(),
                current.startzeit(),
                current.endzeit(),
                current.status(),
                notiz
        ));
    }

    private static String appendMarker(String current, String marker) {
        if (current == null || current.isBlank()) {
            return marker;
        }
        if (current.contains(marker)) {
            return current;
        }
        return current + " | " + marker;
    }

    private static String required(Map<String, String> body, String field) {
        if (body == null) {
            throw new ValidationException(field + " ist erforderlich.");
        }
        String value = body.get(field);
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " ist erforderlich.");
        }
        return value.trim();
    }

    private static String optional(Map<String, String> body, String field, String fallback) {
        if (body == null) {
            return fallback;
        }
        String value = body.get(field);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static double optionalDouble(Map<String, String> body, String field, double fallback) {
        String value = optional(body, field, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0.0) {
                throw new ValidationException(field + " darf nicht negativ sein.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ValidationException(field + " muss eine Zahl sein.");
        }
    }

    private static LocalDateTime optionalDateTime(Map<String, String> body, String field, LocalDateTime fallback) {
        String value = optional(body, field, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value).atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } catch (RuntimeException exception) {
            throw new ValidationException(field + " muss ein gueltiges Datum sein.");
        }
    }
}
