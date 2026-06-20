package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.repository.AircraftRepository;
import java.util.List;
import java.util.Map;

public final class AircraftService {
    private final AircraftRepository aircraftRepository;

    public AircraftService(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public List<Aircraft> list() {
        return aircraftRepository.findAll();
    }

    public Aircraft create(Map<String, String> data) {
        String registration = ServiceSupport.required(data, "registration").toUpperCase();
        boolean duplicateRegistration = aircraftRepository.findAll().stream()
                .anyMatch(aircraft -> aircraft.registration().equalsIgnoreCase(registration));
        if (duplicateRegistration) {
            throw new BusinessConflictException("Flugzeug mit dieser Kennung existiert bereits.");
        }

        Aircraft aircraft = new Aircraft(
                0,
                registration,
                ServiceSupport.required(data, "model"),
                ServiceSupport.optional(data, "status", "Verfuegbar"),
                ServiceSupport.optionalInt(data, "totalHours", 0)
        );
        return aircraftRepository.create(aircraft);
    }
}


