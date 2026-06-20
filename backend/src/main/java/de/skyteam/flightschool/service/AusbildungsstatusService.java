package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.AusbildungsStatusDto;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.AusbildungsStatus;
import de.skyteam.flightschool.model.AusbildungsStatusCode;
import de.skyteam.flightschool.repository.AusbildungsStatusRepository;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.util.Locale;

public final class AusbildungsstatusService {
    private final SchuelerRepository schuelerRepository;
    private final AusbildungsStatusRepository ausbildungsStatusRepository;
    private final AusbildungsVertragRepository ausbildungsVertragRepository;

    public AusbildungsstatusService(
            SchuelerRepository schuelerRepository,
            AusbildungsStatusRepository ausbildungsStatusRepository,
            AusbildungsVertragRepository ausbildungsVertragRepository
    ) {
        this.schuelerRepository = schuelerRepository;
        this.ausbildungsStatusRepository = ausbildungsStatusRepository;
        this.ausbildungsVertragRepository = ausbildungsVertragRepository;
    }

    public AusbildungsStatusDto status(String schuelerId) {
        requireSchueler(schuelerId);
        AusbildungsStatus status = ausbildungsStatusRepository.findBySchuelerId(schuelerId)
                .orElseThrow(() -> new NotFoundException("Ausbildungsstatus wurde nicht gefunden."));
        return toDto(status, berechneStatus(status));
    }

    public AusbildungsStatusDto schliesseAusbildungAb(String schuelerId) {
        requireSchueler(schuelerId);
        AusbildungsStatus status = ausbildungsStatusRepository.findBySchuelerId(schuelerId)
                .orElseThrow(() -> new NotFoundException("Ausbildungsstatus wurde nicht gefunden."));
        if (!status.theorieBestanden() || !status.praxisBestanden()) {
            throw new BusinessConflictException("Ausbildung kann nur abgeschlossen werden, wenn Theoriepruefung und Praxispruefung bestanden sind.");
        }
        if (status.ausbildungsVertragId() == null || status.ausbildungsVertragId().isBlank()) {
            throw new BusinessConflictException("Ausbildung kann nicht abgeschlossen werden: kein Ausbildungsvertrag vorhanden.");
        }
        ausbildungsVertragRepository.updateStatus(status.ausbildungsVertragId(), "Abgeschlossen");
        AusbildungsStatus updated = ausbildungsStatusRepository.findBySchuelerId(schuelerId).orElse(status);
        return toDto(updated, AusbildungsStatusCode.ABGESCHLOSSEN);
    }

    public AusbildungsStatusCode berechneStatus(AusbildungsStatus status) {
        if (status == null) {
            return AusbildungsStatusCode.NICHT_GESTARTET;
        }
        String vertragsStatus = normalize(status.vertragsStatus());
        if (vertragsStatus.contains("abgebrochen") || vertragsStatus.contains("gekuendigt") || vertragsStatus.contains("gekundigt")) {
            return AusbildungsStatusCode.ABGEBROCHEN;
        }
        if (vertragsStatus.contains("abgeschlossen")) {
            return AusbildungsStatusCode.ABGESCHLOSSEN;
        }
        boolean theorieBereit = status.theorieStunden() >= TrainingRules.MINDEST_THEORIESTUNDEN;
        boolean praxisBereit = status.flugStunden() >= TrainingRules.MINDEST_FLUGSTUNDEN;
        if (status.theorieStunden() <= 0.0 && status.flugStunden() <= 0.0) {
            return AusbildungsStatusCode.NICHT_GESTARTET;
        }
        if (theorieBereit && praxisBereit) {
            return AusbildungsStatusCode.PRUEFUNGEN_OFFEN;
        }
        if (praxisBereit) {
            return AusbildungsStatusCode.PRAXIS_BEREIT;
        }
        if (theorieBereit) {
            return AusbildungsStatusCode.THEORIE_BEREIT;
        }
        return AusbildungsStatusCode.AKTIV;
    }

    private AusbildungsStatusDto toDto(AusbildungsStatus status, AusbildungsStatusCode code) {
        boolean theorieFreigeschaltet = status.theorieStunden() >= TrainingRules.MINDEST_THEORIESTUNDEN;
        boolean praxisFreigeschaltet = status.flugStunden() >= TrainingRules.MINDEST_FLUGSTUNDEN;
        return new AusbildungsStatusDto(
                status.schuelerId(),
                code,
                status.vertragsStatus(),
                status.theorieStunden(),
                status.flugStunden(),
                theorieFreigeschaltet,
                praxisFreigeschaltet,
                status.theorieBestanden(),
                status.praxisBestanden(),
                theorieFreigeschaltet && praxisFreigeschaltet
        );
    }

    private void requireSchueler(String schuelerId) {
        if (schuelerId == null || schuelerId.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        schuelerRepository.findById(schuelerId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace("ß", "ss");
    }
}
