package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.model.Flugzeug;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.model.Pilot;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.model.Wartung;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

public final class DevFlightSchoolData {
    final Map<String, Schueler> schueler = new LinkedHashMap<>();
    final Map<String, AusbildungsVertrag> vertraege = new LinkedHashMap<>();
    final Map<String, Kurs> kurse = new LinkedHashMap<>();
    final Map<String, Flug> fluege = new LinkedHashMap<>();
    final Map<String, Pruefung> pruefungen = new LinkedHashMap<>();
    final Map<String, Pilot> piloten = new LinkedHashMap<>();
    final Map<String, Flugzeug> flugzeuge = new LinkedHashMap<>();
    final Map<String, Wartung> wartungen = new LinkedHashMap<>();
    final Map<String, String> wartungFlugzeug = new LinkedHashMap<>();
    private int kursSeq = 950;
    private int flugSeq = 950;
    private int pruefungSeq = 950;
    private int schuelerSeq = 907;
    private int vertragSeq = 907;
    private final Path persistenceFile;

    public DevFlightSchoolData() {
        this.persistenceFile = configuredPersistenceFile();
        seedPiloten();
        seedFlugzeugeUndWartung();
        seedDemoSchueler();
        loadPersistedState();
    }

    private static Path configuredPersistenceFile() {
        String configured = System.getProperty("dev.data.file");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("DEV_DATA_FILE");
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        return Paths.get("backend", "target", "dev-data", "flight-school-demo.properties")
                .toAbsolutePath()
                .normalize();
    }

    private void seedPiloten() {
        piloten.put("P001", new Pilot("P001", "FI(A)", new BigDecimal("5200.00"), "Ja", "Ja", "Mueller", "Max", "01610001", "max.mueller@skyteam.de"));
        piloten.put("P002", new Pilot("P002", "FI(A)", new BigDecimal("5100.00"), "Ja", "Ja", "Schmidt", "Erika", "01610002", "erika.schmidt@skyteam.de"));
        piloten.put("P003", new Pilot("P003", "PPL(A)", new BigDecimal("0.00"), "Nein", "Ja", "Weber", "Lina", "01610003", "lina.weber@skyteam.de"));
        piloten.put("P004", new Pilot("P004", "FI(A)", new BigDecimal("5000.00"), "Ja", "Nein", "Neumann", "Tom", "01610004", "tom.neumann@skyteam.de"));
    }

    private void seedFlugzeugeUndWartung() {
        flugzeuge.put("FZ001", new Flugzeug("FZ001", LocalDateTime.of(2023, 2, 27, 0, 0), "Ja", "in_wartung"));
        flugzeuge.put("FZ002", new Flugzeug("FZ002", LocalDateTime.of(2005, 9, 23, 0, 0), "Ja", "einsatzbereit"));
        flugzeuge.put("FZ003", new Flugzeug("FZ003", LocalDateTime.of(2015, 6, 15, 0, 0), "Ja", "einsatzbereit"));
        flugzeuge.put("FZ004", new Flugzeug("FZ004", LocalDateTime.of(2001, 4, 10, 0, 0), "Nein", "gesperrt"));

        wartungen.put("W901", new Wartung("W901", LocalDateTime.of(2026, 9, 20, 0, 0), "offen", "100h Kontrolle faellig", null, "FZ001"));
        wartungFlugzeug.put("W901", "FZ001");
        wartungen.put("W902", new Wartung("W902", LocalDateTime.of(2026, 8, 18, 0, 0), "abgeschlossen", "Jahresnachpruefung erledigt", null, "FZ002"));
        wartungFlugzeug.put("W902", "FZ002");
    }

    private void seedDemoSchueler() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2028, 9, 1, 0, 0);

        demoCase(
                "SC901",
                "AV901",
                "Keller",
                "Jonas",
                4.0,
                3.0,
                "Demo 1: laufende Ausbildung, Mindeststunden noch offen.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB901", "SC901", "Theorie - Grundlagen der Aerodynamik", "Elias Schulz", "2026-09-05");
        addKurs("KTB902", "SC901", "Theorie - Luftrecht Einstieg", "Elias Schulz", "2026-09-06");
        addFlug("FL901", "SC901", "FZ002", "2026-09-08T10:00", "2026-09-08T11:30", "EDDV", "EDDV", "Platzrunde");
        addFlug("FL902", "SC901", "FZ003", "2026-09-09T09:00", "2026-09-09T10:30", "EDDV", "EDDV", "Start und Landung");

        demoCase(
                "SC902",
                "AV902",
                "Berger",
                "Nico",
                12.0,
                4.0,
                "Demo 2: genug Theoriestunden, Theoriepruefung noch offen.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB903", "SC902", "Theorie - Navigation", "Elias Schulz", "2026-09-10");
        addKurs("KTB904", "SC902", "Theorie - Meteorologie", "Elias Schulz", "2026-09-11");
        addFlug("FL903", "SC902", "FZ002", "2026-09-12T10:00", "2026-09-12T12:00", "EDDV", "EDDV", "Schulflug");
        addFlug("FL904", "SC902", "FZ003", "2026-09-13T10:00", "2026-09-13T12:00", "EDDV", "EDDV", "Schulflug");

        demoCase(
                "SC903",
                "AV903",
                "Sommer",
                "Mina",
                5.0,
                12.0,
                "Demo 3: genug Flugstunden, Praxispruefung noch offen.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB905", "SC903", "Theorie - Technik Grundlagen", "Elias Schulz", "2026-09-14");
        addFlug("FL905", "SC903", "FZ002", "2026-09-15T08:00", "2026-09-15T10:00", "EDDV", "EDDV", "Platzrunde");
        addFlug("FL906", "SC903", "FZ003", "2026-09-16T08:00", "2026-09-16T10:00", "EDDV", "EDVE", "Ueberlandflug");
        addFlug("FL907", "SC903", "FZ002", "2026-09-17T08:00", "2026-09-17T10:00", "EDDV", "EDDV", "Notverfahren");

        demoCase(
                "SC904",
                "AV904",
                "Seidel",
                "Mara",
                14.0,
                6.0,
                "Demo 4: Theoriepruefung nicht bestanden, Wiederholung erforderlich.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB906", "SC904", "Theorie - Luftrecht Vertiefung", "Elias Schulz", "2026-09-18");
        addKurs("KTB907", "SC904", "Theorie - Navigation Vertiefung", "Elias Schulz", "2026-09-19");
        addFlug("FL908", "SC904", "FZ002", "2026-09-20T08:00", "2026-09-20T10:00", "EDDV", "EDDV", "Schulflug");
        addPruefung("PRB901", "SC904", "2026-09-21T00:00", "Theoriepruefung Navigation - nicht bestanden");

        demoCase(
                "SC905",
                "AV905",
                "Wagner",
                "Lea",
                13.0,
                15.0,
                "Demo 5: Praxispruefung nicht bestanden, Wiederholung erforderlich.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB908", "SC905", "Theorie - Pruefungsvorbereitung", "Elias Schulz", "2026-09-22");
        addFlug("FL909", "SC905", "FZ003", "2026-09-23T09:00", "2026-09-23T11:00", "EDDV", "EDDV", "Platzrunde");
        addFlug("FL910", "SC905", "FZ002", "2026-09-24T09:00", "2026-09-24T12:00", "EDDV", "EDVE", "Ueberlandflug");
        addPruefung("PRB902", "SC905", "2026-09-25T00:00", "Theoriepruefung Luftrecht - bestanden");
        addPruefung("PRB903", "SC905", "2026-09-26T00:00", "Praxispruefung Start und Landung - nicht bestanden");

        demoCase(
                "SC906",
                "AV906",
                "Lange",
                "Oskar",
                16.0,
                18.0,
                "Demo 6: Theorie und Praxis bestanden, bereit fuer Abschluss.",
                "Unterschrieben",
                start,
                end
        );
        addKurs("KTB909", "SC906", "Theorie - Komplettkurs", "Elias Schulz", "2026-09-27");
        addFlug("FL911", "SC906", "FZ002", "2026-09-28T10:00", "2026-09-28T13:00", "EDDV", "EDVE", "Ueberlandflug");
        addFlug("FL912", "SC906", "FZ003", "2026-09-29T10:00", "2026-09-29T13:00", "EDVE", "EDDV", "Pruefungsvorbereitung");
        addPruefung("PRB904", "SC906", "2026-10-01T00:00", "Theoriepruefung Luftrecht - bestanden");
        addPruefung("PRB905", "SC906", "2026-10-03T00:00", "Praxispruefung Start und Landung - bestanden");

        demoCase(
                "SC907",
                "AV907",
                "Hartmann",
                "Ella",
                18.0,
                20.0,
                "Demo 7: Ausbildung abgeschlossen.",
                "Abgeschlossen",
                start,
                end
        );
        addKurs("KTB910", "SC907", "Theorie - Abschlusskurs", "Elias Schulz", "2026-10-04");
        addFlug("FL913", "SC907", "FZ002", "2026-10-05T09:00", "2026-10-05T12:00", "EDDV", "EDDV", "Abschlussflug");
        addPruefung("PRB906", "SC907", "2026-10-06T00:00", "Theoriepruefung Navigation - bestanden");
        addPruefung("PRB907", "SC907", "2026-10-07T00:00", "Praxispruefung Ueberlandflug - bestanden");
    }

    private void demoCase(
            String schuelerId,
            String vertragId,
            String name,
            String vorname,
            double theorieStunden,
            double flugStunden,
            String notiz,
            String vertragsStatus,
            LocalDateTime start,
            LocalDateTime end
    ) {
        vertraege.put(vertragId, new AusbildungsVertrag(vertragId, "S001", start, end, vertragsStatus, notiz));
        schueler.put(schuelerId, new Schueler(
                schuelerId,
                vertragId,
                start,
                end,
                flugStunden,
                theorieStunden,
                notiz,
                name,
                vorname
        ));
    }

    private void addKurs(String id, String schuelerId, String typ, String lehrer, String tag) {
        kurse.put(id, new Kurs(id, schuelerId, typ, lehrer, tag));
    }

    private void addFlug(
            String id,
            String schuelerId,
            String flugzeugId,
            String startzeit,
            String endzeit,
            String startFlughafen,
            String zielFlughafen,
            String flugArt
    ) {
        LocalDateTime start = LocalDateTime.parse(startzeit);
        fluege.put(id, new Flug(
                id,
                schuelerId,
                flugzeugId,
                start,
                start,
                LocalDateTime.parse(endzeit),
                startFlughafen,
                zielFlughafen,
                flugArt
        ));
    }

    private void addPruefung(String id, String schuelerId, String datum, String typ) {
        pruefungen.put(id, new Pruefung(id, schuelerId, LocalDateTime.parse(datum), typ));
    }

    synchronized String nextKursId() {
        return "KTB" + (++kursSeq);
    }

    synchronized String nextFlugId() {
        return "FL" + (++flugSeq);
    }

    synchronized String nextPruefungId() {
        return "PRB" + (++pruefungSeq);
    }

    synchronized String nextSchuelerId() {
        return "SC" + (++schuelerSeq);
    }

    synchronized String nextAusbildungsVertragId() {
        return "AV" + (++vertragSeq);
    }

    synchronized void addTheorieStunden(String schuelerId, double hours) {
        Schueler current = schueler.get(schuelerId);
        if (current == null) {
            return;
        }
        schueler.put(schuelerId, new Schueler(
                current.id(),
                current.ausbildungsVertragId(),
                current.startzeit(),
                current.endzeit(),
                current.flugStunden(),
                current.theorieStunden() + hours,
                current.notiz(),
                current.name(),
                current.vorname()
        ));
    }

    synchronized void addFlugStunden(String schuelerId, double hours) {
        Schueler current = schueler.get(schuelerId);
        if (current == null) {
            return;
        }
        schueler.put(schuelerId, new Schueler(
                current.id(),
                current.ausbildungsVertragId(),
                current.startzeit(),
                current.endzeit(),
                current.flugStunden() + hours,
                current.theorieStunden(),
                current.notiz(),
                current.name(),
                current.vorname()
        ));
    }

    synchronized void persist() {
        Properties properties = new Properties();
        properties.setProperty("seq.kurs", String.valueOf(kursSeq));
        properties.setProperty("seq.flug", String.valueOf(flugSeq));
        properties.setProperty("seq.pruefung", String.valueOf(pruefungSeq));
        properties.setProperty("seq.schueler", String.valueOf(schuelerSeq));
        properties.setProperty("seq.vertrag", String.valueOf(vertragSeq));

        properties.setProperty("schueler.ids", joinIds(schueler));
        schueler.forEach((id, value) -> {
            String prefix = "schueler." + id + ".";
            properties.setProperty(prefix + "vertrag", value.ausbildungsVertragId());
            properties.setProperty(prefix + "start", text(value.startzeit()));
            properties.setProperty(prefix + "end", text(value.endzeit()));
            properties.setProperty(prefix + "flugStunden", String.valueOf(value.flugStunden()));
            properties.setProperty(prefix + "theorieStunden", String.valueOf(value.theorieStunden()));
            properties.setProperty(prefix + "notiz", safe(value.notiz()));
            properties.setProperty(prefix + "name", safe(value.name()));
            properties.setProperty(prefix + "vorname", safe(value.vorname()));
        });

        properties.setProperty("vertraege.ids", joinIds(vertraege));
        vertraege.forEach((id, value) -> {
            String prefix = "vertrag." + id + ".";
            properties.setProperty(prefix + "schuleId", safe(value.schuleId()));
            properties.setProperty(prefix + "start", text(value.startzeit()));
            properties.setProperty(prefix + "end", text(value.endzeit()));
            properties.setProperty(prefix + "status", safe(value.status()));
            properties.setProperty(prefix + "notiz", safe(value.notiz()));
        });

        properties.setProperty("kurse.ids", joinIds(kurse));
        kurse.forEach((id, value) -> {
            String prefix = "kurs." + id + ".";
            properties.setProperty(prefix + "schuelerId", value.schuelerId());
            properties.setProperty(prefix + "typ", safe(value.typ()));
            properties.setProperty(prefix + "lehrer", safe(value.lehrer()));
            properties.setProperty(prefix + "tag", safe(value.tag()));
        });

        properties.setProperty("fluege.ids", joinIds(fluege));
        fluege.forEach((id, value) -> {
            String prefix = "flug." + id + ".";
            properties.setProperty(prefix + "schuelerId", value.schuelerId());
            properties.setProperty(prefix + "flugzeugId", value.flugzeugId());
            properties.setProperty(prefix + "datum", text(value.datum()));
            properties.setProperty(prefix + "startzeit", text(value.startzeit()));
            properties.setProperty(prefix + "endzeit", text(value.endzeit()));
            properties.setProperty(prefix + "startFlughafen", safe(value.startFlughafen()));
            properties.setProperty(prefix + "zielFlughafen", safe(value.zielFlughafen()));
            properties.setProperty(prefix + "flugArt", safe(value.flugArt()));
        });

        properties.setProperty("pruefungen.ids", joinIds(pruefungen));
        pruefungen.forEach((id, value) -> {
            String prefix = "pruefung." + id + ".";
            properties.setProperty(prefix + "schuelerId", value.schuelerId());
            properties.setProperty(prefix + "datum", text(value.datum()));
            properties.setProperty(prefix + "typ", safe(value.typ()));
        });

        properties.setProperty("piloten.ids", joinIds(piloten));
        piloten.forEach((id, value) -> {
            String prefix = "pilot." + id + ".";
            properties.setProperty(prefix + "lizenz", safe(value.lizenz()));
            properties.setProperty(prefix + "gehalt", String.valueOf(value.gehalt()));
            properties.setProperty(prefix + "lehrer", safe(value.lehrer()));
            properties.setProperty(prefix + "verfuegbar", safe(value.verfuegbar()));
            properties.setProperty(prefix + "name", safe(value.name()));
            properties.setProperty(prefix + "vorname", safe(value.vorname()));
            properties.setProperty(prefix + "telefon", safe(value.telefon()));
            properties.setProperty(prefix + "email", safe(value.email()));
        });

        properties.setProperty("flugzeuge.ids", joinIds(flugzeuge));
        flugzeuge.forEach((id, value) -> {
            String prefix = "flugzeug." + id + ".";
            properties.setProperty(prefix + "baujahr", text(value.baujahr()));
            properties.setProperty(prefix + "verfuegbarkeit", safe(value.verfuegbarkeit()));
            properties.setProperty(prefix + "status", safe(value.status()));
        });

        properties.setProperty("wartungen.ids", joinIds(wartungen));
        wartungen.forEach((id, value) -> {
            String prefix = "wartung." + id + ".";
            properties.setProperty(prefix + "datum", text(value.datum()));
            properties.setProperty(prefix + "typ", safe(value.typ()));
            properties.setProperty(prefix + "notiz", safe(value.notiz()));
            properties.setProperty(prefix + "buchungId", safe(value.buchungId()));
            properties.setProperty(prefix + "flugzeugId", safe(value.flugzeugId()));
        });

        try {
            Files.createDirectories(persistenceFile.getParent());
            try (OutputStream output = Files.newOutputStream(persistenceFile)) {
                properties.store(output, "SkyTeam Flight School dev data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Dev-Daten konnten nicht gespeichert werden: " + persistenceFile, exception);
        }
    }

    private void loadPersistedState() {
        if (!Files.exists(persistenceFile)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(persistenceFile)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Dev-Daten konnten nicht geladen werden: " + persistenceFile, exception);
        }

        schueler.clear();
        vertraege.clear();
        kurse.clear();
        fluege.clear();
        pruefungen.clear();
        piloten.clear();
        flugzeuge.clear();
        wartungen.clear();
        wartungFlugzeug.clear();

        kursSeq = intProperty(properties, "seq.kurs", 950);
        flugSeq = intProperty(properties, "seq.flug", 950);
        pruefungSeq = intProperty(properties, "seq.pruefung", 950);
        schuelerSeq = intProperty(properties, "seq.schueler", 907);
        vertragSeq = intProperty(properties, "seq.vertrag", 907);

        for (String id : ids(properties, "vertraege.ids")) {
            String prefix = "vertrag." + id + ".";
            vertraege.put(id, new AusbildungsVertrag(
                    id,
                    property(properties, prefix + "schuleId", "S001"),
                    dateTime(properties, prefix + "start"),
                    dateTime(properties, prefix + "end"),
                    property(properties, prefix + "status", "Unterschrieben"),
                    property(properties, prefix + "notiz", "")
            ));
        }

        for (String id : ids(properties, "schueler.ids")) {
            String prefix = "schueler." + id + ".";
            schueler.put(id, new Schueler(
                    id,
                    property(properties, prefix + "vertrag", ""),
                    dateTime(properties, prefix + "start"),
                    dateTime(properties, prefix + "end"),
                    doubleProperty(properties, prefix + "flugStunden", 0.0),
                    doubleProperty(properties, prefix + "theorieStunden", 0.0),
                    property(properties, prefix + "notiz", ""),
                    property(properties, prefix + "name", ""),
                    property(properties, prefix + "vorname", "")
            ));
        }

        for (String id : ids(properties, "kurse.ids")) {
            String prefix = "kurs." + id + ".";
            kurse.put(id, new Kurs(
                    id,
                    property(properties, prefix + "schuelerId", ""),
                    property(properties, prefix + "typ", ""),
                    property(properties, prefix + "lehrer", ""),
                    property(properties, prefix + "tag", "")
            ));
        }

        for (String id : ids(properties, "fluege.ids")) {
            String prefix = "flug." + id + ".";
            LocalDateTime startzeit = dateTime(properties, prefix + "startzeit");
            fluege.put(id, new Flug(
                    id,
                    property(properties, prefix + "schuelerId", ""),
                    property(properties, prefix + "flugzeugId", ""),
                    dateTime(properties, prefix + "datum"),
                    startzeit,
                    dateTime(properties, prefix + "endzeit"),
                    property(properties, prefix + "startFlughafen", "EDDV"),
                    property(properties, prefix + "zielFlughafen", "EDDV"),
                    property(properties, prefix + "flugArt", "")
            ));
        }

        for (String id : ids(properties, "pruefungen.ids")) {
            String prefix = "pruefung." + id + ".";
            pruefungen.put(id, new Pruefung(
                    id,
                    property(properties, prefix + "schuelerId", ""),
                    dateTime(properties, prefix + "datum"),
                    property(properties, prefix + "typ", "")
            ));
        }

        for (String id : ids(properties, "piloten.ids")) {
            String prefix = "pilot." + id + ".";
            piloten.put(id, new Pilot(
                    id,
                    property(properties, prefix + "lizenz", ""),
                    new BigDecimal(property(properties, prefix + "gehalt", "0.00")),
                    property(properties, prefix + "lehrer", "Nein"),
                    property(properties, prefix + "verfuegbar", "Nein"),
                    property(properties, prefix + "name", ""),
                    property(properties, prefix + "vorname", ""),
                    property(properties, prefix + "telefon", ""),
                    property(properties, prefix + "email", "")
            ));
        }

        for (String id : ids(properties, "flugzeuge.ids")) {
            String prefix = "flugzeug." + id + ".";
            flugzeuge.put(id, new Flugzeug(
                    id,
                    dateTime(properties, prefix + "baujahr"),
                    property(properties, prefix + "verfuegbarkeit", "Nein"),
                    property(properties, prefix + "status", "")
            ));
        }

        for (String id : ids(properties, "wartungen.ids")) {
            String prefix = "wartung." + id + ".";
            Wartung wartung = new Wartung(
                    id,
                    dateTime(properties, prefix + "datum"),
                    property(properties, prefix + "typ", ""),
                    property(properties, prefix + "notiz", ""),
                    emptyToNull(property(properties, prefix + "buchungId", "")),
                    property(properties, prefix + "flugzeugId", "")
            );
            wartungen.put(id, wartung);
            wartungFlugzeug.put(id, wartung.flugzeugId());
        }
    }

    private static String joinIds(Map<String, ?> values) {
        StringJoiner joiner = new StringJoiner(",");
        values.keySet().forEach(joiner::add);
        return joiner.toString();
    }

    private static String[] ids(Properties properties, String key) {
        String value = properties.getProperty(key, "");
        if (value.isBlank()) {
            return new String[0];
        }
        return value.split(",");
    }

    private static String property(Properties properties, String key, String fallback) {
        return properties.getProperty(key, fallback);
    }

    private static int intProperty(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private static double doubleProperty(Properties properties, String key, double fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(value.trim());
    }

    private static LocalDateTime dateTime(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim());
    }

    private static String text(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
