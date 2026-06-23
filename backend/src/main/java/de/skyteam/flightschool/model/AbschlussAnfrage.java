package de.skyteam.flightschool.model;

import java.time.LocalDateTime;

public record AbschlussAnfrage(
        String id,
        String schuelerId,
        AbschlussAnfrageStatus status,
        String begruendung,
        LocalDateTime angefragtAm,
        LocalDateTime geprueftAm
) {
}
