package de.skyteam.flightschool.model;

public record Kurs(
        String id,
        String schuelerId,
        String typ,
        String lehrer,
        String tag,
        int dauerMinuten
) {
    public Kurs(String id, String schuelerId, String typ, String lehrer, String tag) {
        this(id, schuelerId, typ, lehrer, tag, 60);
    }
}
