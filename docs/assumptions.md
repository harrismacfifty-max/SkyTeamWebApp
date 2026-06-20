# Annahmen

- Der Zielordner war leer und noch kein Git-Repository. Daher wurde lokal ein Repository initialisiert und direkt der Branch `flight-school-webapp-mvp` angelegt.
- Das MVP bildet den Kernprozess einer Flugschule ab: Schueler verwalten, Flugzeuge verwalten, Ausbildungstermine planen, Dashboard anzeigen.
- Login ist bewusst einfach gehalten und prueft nur, ob Benutzername und Passwort nicht leer sind. Rollen, Rechte, Passwortspeicherung und Sessions sind nicht Teil dieses MVP.
- Das Standardprofil `dev` nutzt In-Memory-Daten, damit lokal keine Oracle-Installation erforderlich ist.
- Das Profil `oracle` nutzt JDBC-Repositories. Fuer die fachliche Flugschul-Datenzugriffsschicht werden die bestehenden Tabellen aus dem gelieferten SQL-Skript verwendet, insbesondere `SCHUELER`, `AUSBILDUNG_VERTRAG`, `KURSE`, `FLUG`, `PRUEFUNG`, `PILOT`, `FLUGZEUG` und `WARTUNG`.
- Da die vorhandene Tabelle `PRUEFUNG` keine eigene Ergebnisspalte besitzt, wird das Ergebnis ohne Schemaaenderung im vorhandenen Feld `TYP` codiert.
- Externe Systeme wie E-Mail, Zahlung, Kalenderintegration oder Flughafenbetrieb sind aus dem Umfang ausgeschlossen.
