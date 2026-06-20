package de.skyteam.flightschool.config;

public record ApplicationConfig(
        String applicationName,
        String version
) {
    public static ApplicationConfig defaults() {
        return new ApplicationConfig("SkyTeam Flight School API", "0.2.0");
    }
}

