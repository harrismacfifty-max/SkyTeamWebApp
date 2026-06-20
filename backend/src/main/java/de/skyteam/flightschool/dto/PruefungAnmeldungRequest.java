package de.skyteam.flightschool.dto;

public record PruefungAnmeldungRequest(
        String schuelerId,
        String pruefungsart,
        String wunschtermin,
        String pruefer,
        String bemerkung
) {
}
