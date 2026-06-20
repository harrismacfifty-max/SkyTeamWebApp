package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.LoginResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AuthService {
    private static final String DEFAULT_USERNAME = "demo";
    private static final String DEFAULT_PASSWORD = "demo";
    private final String username;
    private final String password;
    private final ConcurrentMap<String, String> activeSessions = new ConcurrentHashMap<>();

    public AuthService() {
        this.username = optionalEnv("AUTH_USERNAME", DEFAULT_USERNAME);
        this.password = optionalEnv("AUTH_PASSWORD", DEFAULT_PASSWORD);
    }

    public LoginResponse login(Map<String, String> data) {
        String requestedUsername = ServiceSupport.required(data, "username");
        String requestedPassword = ServiceSupport.required(data, "password");
        if (!username.equals(requestedUsername) || !password.equals(requestedPassword)) {
            throw new UnauthorizedException("Ungueltige Zugangsdaten.");
        }
        String token = "session-" + UUID.randomUUID();
        activeSessions.put(token, requestedUsername);
        return new LoginResponse(token, requestedUsername);
    }

    public boolean logout(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return activeSessions.remove(token) != null;
    }

    public Optional<String> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSessions.get(token));
    }

    public boolean isAuthenticated(String token) {
        return currentUser(token).isPresent();
    }

    private static String optionalEnv(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}


