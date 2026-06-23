package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.UnauthorizedException;
import de.skyteam.flightschool.model.AuthUser;
import de.skyteam.flightschool.model.LoginResponse;
import de.skyteam.flightschool.model.UserRole;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AuthService {
    private final Map<String, DemoAccount> accounts = Map.of(
            "demo", new DemoAccount("demo", new AuthUser("demo", "Demo Schüler", UserRole.SCHUELER, "SC901")),
            "demo2", new DemoAccount("demo2", new AuthUser("demo2", "Demo Schülerverwaltung", UserRole.SCHUELERVERWALTUNG, ""))
    );
    private final ConcurrentMap<String, AuthUser> activeSessions = new ConcurrentHashMap<>();

    public AuthService() {
    }

    public LoginResponse login(Map<String, String> data) {
        String requestedUsername = ServiceSupport.required(data, "username");
        String requestedPassword = ServiceSupport.required(data, "password");
        DemoAccount account = accounts.get(requestedUsername);
        if (account == null || !account.password().equals(requestedPassword)) {
            throw new UnauthorizedException("Ungueltige Zugangsdaten.");
        }
        String token = "session-" + UUID.randomUUID();
        activeSessions.put(token, account.user());
        return new LoginResponse(
                token,
                account.user().username(),
                account.user().displayName(),
                account.user().role().name(),
                account.user().schuelerId()
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

    private record DemoAccount(String password, AuthUser user) {
    }
}


