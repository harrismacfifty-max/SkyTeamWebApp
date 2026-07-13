package de.skyteam.flightschool.service;

import de.skyteam.flightschool.model.AuthUser;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.model.UserRole;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DemoAccountProvider {
    private final Map<String, DemoAccount> accounts;

    public DemoAccountProvider(String activeProfile, SchuelerRepository schuelerRepository) {
        if (!isDemoProfile(activeProfile)) {
            this.accounts = Map.of();
            return;
        }

        Map<String, DemoAccount> configuredAccounts = new LinkedHashMap<>();
        addStudentAccount(configuredAccounts, schuelerRepository, "sc901", "demo901", "SC901");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc902", "demo902", "SC902");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc903", "demo903", "SC903");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc904", "demo904", "SC904");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc905", "demo905", "SC905");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc906", "demo906", "SC906");
        addStudentAccount(configuredAccounts, schuelerRepository, "sc907", "demo907", "SC907");

        configuredAccounts.put("demo", new DemoAccount(
                "demo",
                studentUser(schuelerRepository, "demo", "SC901")
        ));
        configuredAccounts.put("demo2", new DemoAccount(
                "demo2",
                new AuthUser("demo2", "Demo Schülerverwaltung", UserRole.SCHUELERVERWALTUNG, null)
        ));
        this.accounts = Map.copyOf(configuredAccounts);
    }

    public Optional<AuthUser> authenticate(String username, String password) {
        DemoAccount account = accounts.get(username);
        if (account == null || !account.password().equals(password)) {
            return Optional.empty();
        }
        return Optional.of(account.user());
    }

    private static boolean isDemoProfile(String activeProfile) {
        return "demo".equalsIgnoreCase(activeProfile) || "dev".equalsIgnoreCase(activeProfile);
    }

    private static void addStudentAccount(
            Map<String, DemoAccount> accounts,
            SchuelerRepository schuelerRepository,
            String username,
            String password,
            String schuelerId
    ) {
        accounts.put(username, new DemoAccount(password, studentUser(schuelerRepository, username, schuelerId)));
    }

    private static AuthUser studentUser(SchuelerRepository schuelerRepository, String username, String schuelerId) {
        String displayName = schuelerRepository.findById(schuelerId)
                .map(DemoAccountProvider::displayName)
                .orElse("Demo Schüler " + schuelerId);
        return new AuthUser(username, displayName, UserRole.SCHUELER, schuelerId);
    }

    private static String displayName(Schueler schueler) {
        return (schueler.vorname() + " " + schueler.name()).trim();
    }

    private record DemoAccount(String password, AuthUser user) {
    }
}
