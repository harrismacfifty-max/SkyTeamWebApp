package de.skyteam.flightschool.dto;

public record PruefungsErgebnisStatusDto(
        String pruefungId,
        boolean bestanden,
        boolean wiederholungsbedarf,
        String message
) {
}

