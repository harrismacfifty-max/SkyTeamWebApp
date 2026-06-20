package de.skyteam.flightschool.dto;

public record PraxisBuchungRequest(
        String schuelerId,
        String flugzeugId,
        String fluglehrer,
        String termin,
        int dauerMinuten,
        String ausbildungsinhalt,
        String notizen
) {
}
