package de.skyteam.flightschool.dto;

public record PruefungsErgebnisRequest(
        String pruefungId,
        String schuelerId,
        String pruefungsart,
        String datum,
        boolean bestanden,
        String ergebnisText,
        String notizen
) {
}
