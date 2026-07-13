package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.AbschlussAnfrageDto;
import de.skyteam.flightschool.dto.AusbildungsStatusDto;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.AbschlussAnfrage;
import de.skyteam.flightschool.model.AbschlussAnfrageStatus;
import de.skyteam.flightschool.model.AusbildungsStatusCode;
import de.skyteam.flightschool.repository.AbschlussAnfrageRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.time.LocalDateTime;
import java.util.List;

public final class AbschlussService {
    private final AbschlussAnfrageRepository abschlussAnfragen;
    private final SchuelerRepository schuelerRepository;
    private final AusbildungsstatusService ausbildungsstatusService;

    public AbschlussService(
            AbschlussAnfrageRepository abschlussAnfragen,
            SchuelerRepository schuelerRepository,
            AusbildungsstatusService ausbildungsstatusService
    ) {
        this.abschlussAnfragen = abschlussAnfragen;
        this.schuelerRepository = schuelerRepository;
        this.ausbildungsstatusService = ausbildungsstatusService;
    }

    public AbschlussAnfrageDto requestAbschluss(String schuelerId) {
        requireSchueler(schuelerId);
        if (ausbildungsstatusService.status(schuelerId).status() == AusbildungsStatusCode.ABGESCHLOSSEN) {
            throw new BusinessConflictException("Ausbildung ist bereits abgeschlossen; eine neue Abschlussanfrage ist nicht möglich.");
        }
        return abschlussAnfragen.findLatestBySchuelerId(schuelerId)
                .filter(this::isAktiv)
                .map(this::toDto)
                .orElseGet(() -> toDto(abschlussAnfragen.save(new AbschlussAnfrage(
                        abschlussAnfragen.nextId(),
                        schuelerId,
                        AbschlussAnfrageStatus.ANGEFRAGT,
                        "",
                        LocalDateTime.now(),
                        null
                ))));
    }

    public AbschlussAnfrageDto getMeineAbschlussanfrage(String schuelerId) {
        requireSchueler(schuelerId);
        return abschlussAnfragen.findLatestBySchuelerId(schuelerId)
                .map(this::toDto)
                .orElseGet(() -> toDto(new AbschlussAnfrage(
                        "",
                        schuelerId,
                        AbschlussAnfrageStatus.KEINE_ANFRAGE,
                        "",
                        null,
                        null
                )));
    }

    public List<AbschlussAnfrageDto> findOffeneAbschlussanfragen() {
        return abschlussAnfragen.findOffene().stream()
                .map(this::toDto)
                .toList();
    }

    public List<AbschlussAnfrageDto> findAlleAbschlussanfragen() {
        return abschlussAnfragen.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public AbschlussAnfrageDto findAbschlussanfrage(String anfrageId) {
        return toDto(requireAnfrage(anfrageId));
    }

    public boolean pruefeTheorieKriterien(String schuelerId) {
        return ausbildungsstatusService.status(schuelerId).theorieBestanden();
    }

    public boolean pruefePraxisKriterien(String schuelerId) {
        return ausbildungsstatusService.status(schuelerId).praxisBestanden();
    }

    public AbschlussAnfrageDto bestaetigeAbschluss(String anfrageId) {
        AbschlussAnfrage anfrage = requireAnfrage(anfrageId);
        if (!isOffen(anfrage)) {
            throw new BusinessConflictException("Abschlussanfrage ist nicht offen.");
        }
        if (!pruefeTheorieKriterien(anfrage.schuelerId()) || !pruefePraxisKriterien(anfrage.schuelerId())) {
            abschlussAnfragen.save(new AbschlussAnfrage(
                    anfrage.id(),
                    anfrage.schuelerId(),
                    AbschlussAnfrageStatus.IN_PRUEFUNG,
                    "Abnahmekriterien nicht erfuellt.",
                    anfrage.angefragtAm(),
                    LocalDateTime.now()
            ));
            throw new BusinessConflictException("Abschluss kann nicht bestaetigt werden: Abnahmekriterien sind nicht erfuellt.");
        }
        ausbildungsstatusService.schliesseAusbildungAb(anfrage.schuelerId());
        return toDto(abschlussAnfragen.save(new AbschlussAnfrage(
                anfrage.id(),
                anfrage.schuelerId(),
                AbschlussAnfrageStatus.ABGESCHLOSSEN,
                "Abschluss bestaetigt.",
                anfrage.angefragtAm(),
                LocalDateTime.now()
        )));
    }

    public AbschlussAnfrageDto lehneAbschlussAb(String anfrageId, String begruendung) {
        AbschlussAnfrage anfrage = requireAnfrage(anfrageId);
        if (!isOffen(anfrage)) {
            throw new BusinessConflictException("Abschlussanfrage ist nicht offen.");
        }
        String text = begruendung == null || begruendung.isBlank()
                ? "Abschlussanfrage abgelehnt."
                : begruendung.trim();
        return toDto(abschlussAnfragen.save(new AbschlussAnfrage(
                anfrage.id(),
                anfrage.schuelerId(),
                AbschlussAnfrageStatus.ABGELEHNT,
                text,
                anfrage.angefragtAm(),
                LocalDateTime.now()
        )));
    }

    private AbschlussAnfrage requireAnfrage(String anfrageId) {
        if (anfrageId == null || anfrageId.isBlank()) {
            throw new ValidationException("abschlussAnfrageId ist erforderlich.");
        }
        return abschlussAnfragen.findById(anfrageId)
                .orElseThrow(() -> new NotFoundException("Abschlussanfrage wurde nicht gefunden."));
    }

    private void requireSchueler(String schuelerId) {
        if (schuelerId == null || schuelerId.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        schuelerRepository.findById(schuelerId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    private AbschlussAnfrageDto toDto(AbschlussAnfrage anfrage) {
        boolean theorie = false;
        boolean praxis = false;
        if (anfrage.schuelerId() != null && !anfrage.schuelerId().isBlank()) {
            AusbildungsStatusDto status = ausbildungsstatusService.status(anfrage.schuelerId());
            theorie = status.theorieBestanden();
            praxis = status.praxisBestanden();
        }
        return new AbschlussAnfrageDto(
                anfrage.id(),
                anfrage.schuelerId(),
                anfrage.status(),
                anfrage.begruendung(),
                anfrage.angefragtAm(),
                anfrage.geprueftAm(),
                theorie,
                praxis,
                theorie && praxis && isOffen(anfrage)
        );
    }

    private boolean isAktiv(AbschlussAnfrage anfrage) {
        return anfrage.status() == AbschlussAnfrageStatus.ANGEFRAGT
                || anfrage.status() == AbschlussAnfrageStatus.IN_PRUEFUNG
                || anfrage.status() == AbschlussAnfrageStatus.BESTAETIGT
                || anfrage.status() == AbschlussAnfrageStatus.ABGESCHLOSSEN;
    }

    private boolean isOffen(AbschlussAnfrage anfrage) {
        return anfrage.status() == AbschlussAnfrageStatus.ANGEFRAGT
                || anfrage.status() == AbschlussAnfrageStatus.IN_PRUEFUNG;
    }
}
