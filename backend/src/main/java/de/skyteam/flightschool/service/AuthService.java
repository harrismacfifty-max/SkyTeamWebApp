package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.AuthUser;
import de.skyteam.flightschool.model.LoginResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AuthService {
    private final DemoAccountProvider accountProvider;
    private final ConcurrentMap<String, AuthUser> activeSessions = new ConcurrentHashMap<>();

    public AuthService(DemoAccountProvider accountProvider) {
        this.accountProvider = Objects.requireNonNull(accountProvider, "accountProvider");
    }

    public LoginResponse login(Map<String, String> data) {
        String requestedUsername = ServiceSupport.required(data, "username");
        String requestedPassword = ServiceSupport.required(data, "password");
        AuthUser user = accountProvider.authenticate(requestedUsername, requestedPassword)
                .orElseThrow(() -> new UnauthorizedException("Ungueltige Zugangsdaten."));
        String token = "session-" + UUID.randomUUID();
        activeSessions.put(token, user);
        return new LoginResponse(
                token,
                user.username(),
                user.displayName(),
                user.role().name(),
                user.schuelerId()
        );
    }

    public boolean logout(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return activeSessions.remove(token) != null;
    }

    public Optional<AuthUser> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSessions.get(token));
    }

    public boolean isAuthenticated(String token) {
        return currentUser(token).isPresent();
    }
}


