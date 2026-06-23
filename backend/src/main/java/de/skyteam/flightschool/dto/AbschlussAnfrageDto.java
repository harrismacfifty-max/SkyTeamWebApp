package de.skyteam.flightschool.dto;

import de.skyteam.flightschool.model.AbschlussAnfrageStatus;
import java.time.LocalDateTime;

public record AbschlussAnfrageDto(
        String id,
        String schuelerId,
        AbschlussAnfrageStatus status,
        String begruendung,
        LocalDateTime angefragtAm,
        LocalDateTime geprueftAm,
        boolean theorieKriterienErfuellt,
        boolean praxisKriterienErfuellt,
        boolean bestaetigungMoeglich
) {
}
