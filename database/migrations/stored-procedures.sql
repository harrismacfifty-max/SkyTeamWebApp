-- Oracle-Schreibschnittstelle fuer die SkyTeam-WebApp.
--
-- Voraussetzungen:
--   1. Das abgestimmte SkyTeam-Basisschema ist installiert.
--   2. database/migrations/abschlussanfragen.sql wurde ausgefuehrt.
--
-- Die Procedures enthalten absichtlich kein COMMIT. Die aufrufende Anwendung
-- bestimmt damit weiterhin die Transaktionsgrenze.

begin
    execute immediate 'alter table PRUEFUNG add NOTIZ varchar2(4000)';
exception
    when others then
        -- ORA-01430: Die Spalte ist bereits vorhanden.
        if sqlcode != -1430 then
            raise;
        end if;
end;
/

create or replace package SKYTEAM_WEBAPP_API as
    procedure AUSBILDUNGSVERTRAG_SPEICHERN(
        p_id in varchar2,
        p_schule_id in varchar2,
        p_startzeit in timestamp,
        p_endzeit in timestamp,
        p_status in varchar2,
        p_notiz in varchar2
    );

    procedure AUSBILDUNGSVERTRAG_STATUS(
        p_id in varchar2,
        p_status in varchar2
    );

    procedure SCHUELER_SPEICHERN(
        p_id in varchar2,
        p_vertrag_id in varchar2,
        p_startzeit in timestamp,
        p_endzeit in timestamp,
        p_flugstunden in number,
        p_theoriestunden in number,
        p_notiz in varchar2,
        p_name in varchar2,
        p_vorname in varchar2
    );

    procedure SCHUELER_LOESCHEN(
        p_schueler_id in varchar2,
        p_geloescht out number
    );

    procedure THEORIE_BUCHEN(
        p_schueler_id in varchar2,
        p_typ in varchar2,
        p_lehrer in varchar2,
        p_tag in varchar2,
        p_dauer_minuten in number,
        p_kurs_id out varchar2
    );

    procedure THEORIE_STORNIEREN(
        p_schueler_id in varchar2,
        p_kurs_id in varchar2,
        p_geloescht out number
    );

    procedure PRAXIS_BUCHEN(
        p_schueler_id in varchar2,
        p_flugzeug_id in varchar2,
        p_pilot_id in varchar2,
        p_startzeit in timestamp,
        p_dauer_minuten in number,
        p_start_flughafen in varchar2,
        p_ziel_flughafen in varchar2,
        p_flug_art in varchar2,
        p_flug_id out varchar2
    );

    procedure PRAXIS_STORNIEREN(
        p_schueler_id in varchar2,
        p_flug_id in varchar2,
        p_geloescht out number
    );

    procedure PRUEFUNG_ANMELDEN(
        p_schueler_id in varchar2,
        p_typ in varchar2,
        p_datum in timestamp,
        p_pruefer in varchar2,
        p_notiz in varchar2,
        p_pruefung_id out varchar2
    );

    procedure PRUEFUNG_ERGEBNIS_SPEICHERN(
        p_pruefung_id in varchar2,
        p_typ in varchar2,
        p_datum in timestamp,
        p_bestanden in number,
        p_ergebnis in varchar2,
        p_notiz in varchar2
    );

    procedure ABSCHLUSS_ANFRAGE_SPEICHERN(
        p_id in varchar2,
        p_schueler_id in varchar2,
        p_status in varchar2,
        p_begruendung in varchar2,
        p_angefragt_am in timestamp,
        p_geprueft_am in timestamp
    );
end SKYTEAM_WEBAPP_API;
/

create or replace package body SKYTEAM_WEBAPP_API as
    procedure SCHUELER_PRUEFEN(p_schueler_id in varchar2) is
        v_anzahl number;
    begin
        select count(*) into v_anzahl
        from SCHUELER
        where ID_SCHUELER = p_schueler_id;

        if v_anzahl = 0 then
            raise_application_error(-20010, 'Schueler wurde nicht gefunden.');
        end if;
    end SCHUELER_PRUEFEN;

    procedure AUSBILDUNGSVERTRAG_SPEICHERN(
        p_id in varchar2,
        p_schule_id in varchar2,
        p_startzeit in timestamp,
        p_endzeit in timestamp,
        p_status in varchar2,
        p_notiz in varchar2
    ) is
    begin
        if p_id is null or p_schule_id is null then
            raise_application_error(-20001, 'Vertrags-ID und Schul-ID sind erforderlich.');
        end if;

        merge into AUSBILDUNG_VERTRAG target
        using (
            select p_id ID_AUSBILDUNG_VERTRAG,
                   p_schule_id ID_SCHULE,
                   p_startzeit STARTZEIT,
                   p_endzeit ENDZEIT,
                   p_status STATUS,
                   p_notiz NOTIZ
            from dual
        ) source
        on (target.ID_AUSBILDUNG_VERTRAG = source.ID_AUSBILDUNG_VERTRAG)
        when matched then update set
            target.ID_SCHULE = source.ID_SCHULE,
            target.STARTZEIT = source.STARTZEIT,
            target.ENDZEIT = source.ENDZEIT,
            target.STATUS = source.STATUS,
            target.NOTIZ = source.NOTIZ
        when not matched then insert (
            ID_AUSBILDUNG_VERTRAG, ID_SCHULE, STARTZEIT, ENDZEIT, STATUS, NOTIZ
        ) values (
            source.ID_AUSBILDUNG_VERTRAG, source.ID_SCHULE, source.STARTZEIT,
            source.ENDZEIT, source.STATUS, source.NOTIZ
        );
    end AUSBILDUNGSVERTRAG_SPEICHERN;

    procedure AUSBILDUNGSVERTRAG_STATUS(
        p_id in varchar2,
        p_status in varchar2
    ) is
    begin
        if p_status is null then
            raise_application_error(-20002, 'Ein Vertragsstatus ist erforderlich.');
        end if;

        update AUSBILDUNG_VERTRAG
        set STATUS = p_status
        where ID_AUSBILDUNG_VERTRAG = p_id;

        if sql%rowcount = 0 then
            raise_application_error(-20011, 'Ausbildungsvertrag wurde nicht gefunden.');
        end if;
    end AUSBILDUNGSVERTRAG_STATUS;

    procedure SCHUELER_SPEICHERN(
        p_id in varchar2,
        p_vertrag_id in varchar2,
        p_startzeit in timestamp,
        p_endzeit in timestamp,
        p_flugstunden in number,
        p_theoriestunden in number,
        p_notiz in varchar2,
        p_name in varchar2,
        p_vorname in varchar2
    ) is
    begin
        if p_id is null or p_vertrag_id is null or p_name is null or p_vorname is null then
            raise_application_error(-20003, 'Schueler-ID, Vertrag, Name und Vorname sind erforderlich.');
        end if;

        merge into SCHUELER target
        using (
            select p_id ID_SCHUELER,
                   p_vertrag_id ID_AUSBILDUNG_VERTRAG,
                   p_startzeit STARTZEIT,
                   p_endzeit ENDZEIT,
                   nvl(p_flugstunden, 0) FLUGSTUNDE,
                   nvl(p_theoriestunden, 0) THEORIESTUNDE,
                   p_notiz NOTIZ,
                   p_name NAME,
                   p_vorname VORNAME
            from dual
        ) source
        on (target.ID_SCHUELER = source.ID_SCHUELER)
        when matched then update set
            target.ID_AUSBILDUNG_VERTRAG = source.ID_AUSBILDUNG_VERTRAG,
            target.STARTZEIT = source.STARTZEIT,
            target.ENDZEIT = source.ENDZEIT,
            target.FLUGSTUNDE = source.FLUGSTUNDE,
            target.THEORIESTUNDE = source.THEORIESTUNDE,
            target.NOTIZ = source.NOTIZ,
            target.NAME = source.NAME,
            target.VORNAME = source.VORNAME
        when not matched then insert (
            ID_SCHUELER, ID_AUSBILDUNG_VERTRAG, STARTZEIT, ENDZEIT,
            FLUGSTUNDE, THEORIESTUNDE, NOTIZ, NAME, VORNAME
        ) values (
            source.ID_SCHUELER, source.ID_AUSBILDUNG_VERTRAG, source.STARTZEIT,
            source.ENDZEIT, source.FLUGSTUNDE, source.THEORIESTUNDE,
            source.NOTIZ, source.NAME, source.VORNAME
        );
    end SCHUELER_SPEICHERN;

    procedure SCHUELER_LOESCHEN(
        p_schueler_id in varchar2,
        p_geloescht out number
    ) is
        v_vertrag_id AUSBILDUNG_VERTRAG.ID_AUSBILDUNG_VERTRAG%type;
    begin
        begin
            select ID_AUSBILDUNG_VERTRAG into v_vertrag_id
            from SCHUELER
            where ID_SCHUELER = p_schueler_id;
        exception
            when no_data_found then
                p_geloescht := 0;
                return;
        end;

        delete from ABSCHLUSS_ANFRAGE where ID_SCHUELER = p_schueler_id;
        delete from PRUEFUNG where ID_SCHUELER = p_schueler_id;
        delete from FLUG_UND_PILOT
        where ID_FLUG in (select ID_FLUG from FLUG where ID_SCHUELER = p_schueler_id);
        delete from FLUG where ID_SCHUELER = p_schueler_id;
        delete from KURSE where ID_SCHUELER = p_schueler_id;
        delete from SCHUELER_UND_PILOT where ID_SCHUELER = p_schueler_id;
        delete from SCHUELER where ID_SCHUELER = p_schueler_id;
        p_geloescht := sql%rowcount;

        if v_vertrag_id is not null then
            delete from AUSBILDUNG_VERTRAG where ID_AUSBILDUNG_VERTRAG = v_vertrag_id;
        end if;
    end SCHUELER_LOESCHEN;

    procedure THEORIE_BUCHEN(
        p_schueler_id in varchar2,
        p_typ in varchar2,
        p_lehrer in varchar2,
        p_tag in varchar2,
        p_dauer_minuten in number,
        p_kurs_id out varchar2
    ) is
    begin
        SCHUELER_PRUEFEN(p_schueler_id);
        if p_typ is null or p_lehrer is null or p_tag is null or nvl(p_dauer_minuten, 0) <= 0 then
            raise_application_error(-20012, 'Thema, Lehrer, Termin und positive Dauer sind erforderlich.');
        end if;

        lock table KURSE in exclusive mode;
        select 'KTB' || lpad(nvl(max(to_number(regexp_substr(ID_KURS, '[0-9]+$'))), 0) + 1, 6, '0')
        into p_kurs_id
        from KURSE
        where ID_KURS like 'KTB%';

        insert into KURSE (TYP, LEHRER, TAG, ID_KURS, ID_SCHUELER)
        values (p_typ, p_lehrer, p_tag, p_kurs_id, p_schueler_id);

        update SCHUELER
        set THEORIESTUNDE = nvl(THEORIESTUNDE, 0) + (p_dauer_minuten / 60)
        where ID_SCHUELER = p_schueler_id;
    end THEORIE_BUCHEN;

    procedure THEORIE_STORNIEREN(
        p_schueler_id in varchar2,
        p_kurs_id in varchar2,
        p_geloescht out number
    ) is
    begin
        delete from KURSE
        where ID_KURS = p_kurs_id
          and ID_SCHUELER = p_schueler_id;
        p_geloescht := sql%rowcount;

        if p_geloescht > 0 then
            update SCHUELER
            set THEORIESTUNDE = greatest(0, nvl(THEORIESTUNDE, 0) - 1)
            where ID_SCHUELER = p_schueler_id;
        end if;
    end THEORIE_STORNIEREN;

    procedure PRAXIS_BUCHEN(
        p_schueler_id in varchar2,
        p_flugzeug_id in varchar2,
        p_pilot_id in varchar2,
        p_startzeit in timestamp,
        p_dauer_minuten in number,
        p_start_flughafen in varchar2,
        p_ziel_flughafen in varchar2,
        p_flug_art in varchar2,
        p_flug_id out varchar2
    ) is
        v_verfuegbarkeit FLUGZEUG.VERFUEGBARKEIT%type;
        v_status FLUGZEUG.STATUS%type;
        v_pilot_lehrer PILOT.LEHRER%type;
        v_pilot_verfuegbar PILOT.VERFUEGBAR%type;
    begin
        SCHUELER_PRUEFEN(p_schueler_id);
        if p_startzeit is null or nvl(p_dauer_minuten, 0) <= 0 then
            raise_application_error(-20022, 'Startzeit und positive Flugdauer sind erforderlich.');
        end if;

        begin
            select VERFUEGBARKEIT, STATUS into v_verfuegbarkeit, v_status
            from FLUGZEUG
            where ID_FLUGZEUG = p_flugzeug_id
            for update;
        exception
            when no_data_found then
                raise_application_error(-20020, 'Flugzeug wurde nicht gefunden.');
        end;

        if lower(trim(nvl(v_verfuegbarkeit, 'nein'))) not in
                ('ja', 'yes', 'true', '1', 'verfuegbar')
           or lower(nvl(v_status, '')) like '%wartung%'
           or lower(nvl(v_status, '')) like '%gesperrt%'
           or lower(nvl(v_status, '')) like '%blockiert%' then
            raise_application_error(-20023, 'Flugzeug ist wegen Wartung oder fehlender Verfuegbarkeit blockiert.');
        end if;

        begin
            select LEHRER, VERFUEGBAR into v_pilot_lehrer, v_pilot_verfuegbar
            from PILOT
            where ID_PILOT = p_pilot_id
            for update;
        exception
            when no_data_found then
                raise_application_error(-20021, 'Pilot wurde nicht gefunden.');
        end;

        if lower(trim(nvl(v_pilot_lehrer, 'nein'))) not in ('ja', 'yes', 'true', '1')
           or lower(trim(nvl(v_pilot_verfuegbar, 'nein'))) not in ('ja', 'yes', 'true', '1') then
            raise_application_error(-20023, 'Pilot ist nicht als verfuegbarer Fluglehrer buchbar.');
        end if;

        lock table FLUG in exclusive mode;
        select 'FL' || lpad(nvl(max(to_number(regexp_substr(ID_FLUG, '[0-9]+$'))), 0) + 1, 6, '0')
        into p_flug_id
        from FLUG
        where ID_FLUG like 'FL%';

        insert into FLUG (
            ID_FLUG, ID_SCHUELER, ID_FLUGZEUG, DATUM, STARTZEIT, ENDZEIT,
            START_FLUGHAFEN, ZIEL_FLUGHAFEN, FLUG_ART
        ) values (
            p_flug_id, p_schueler_id, p_flugzeug_id, p_startzeit, p_startzeit,
            p_startzeit + numtodsinterval(p_dauer_minuten, 'MINUTE'),
            p_start_flughafen, p_ziel_flughafen, p_flug_art
        );

        insert into FLUG_UND_PILOT (ID_FLUG, ID_PILOT)
        values (p_flug_id, p_pilot_id);

        update SCHUELER
        set FLUGSTUNDE = nvl(FLUGSTUNDE, 0) + (p_dauer_minuten / 60)
        where ID_SCHUELER = p_schueler_id;
    end PRAXIS_BUCHEN;

    procedure PRAXIS_STORNIEREN(
        p_schueler_id in varchar2,
        p_flug_id in varchar2,
        p_geloescht out number
    ) is
        v_dauer_stunden number;
    begin
        begin
            select greatest(0, (ENDZEIT - STARTZEIT) * 24)
            into v_dauer_stunden
            from FLUG
            where ID_FLUG = p_flug_id
              and ID_SCHUELER = p_schueler_id;
        exception
            when no_data_found then
                p_geloescht := 0;
                return;
        end;

        delete from FLUG_UND_PILOT where ID_FLUG = p_flug_id;
        delete from FLUG
        where ID_FLUG = p_flug_id
          and ID_SCHUELER = p_schueler_id;
        p_geloescht := sql%rowcount;

        if p_geloescht > 0 then
            update SCHUELER
            set FLUGSTUNDE = greatest(0, nvl(FLUGSTUNDE, 0) - v_dauer_stunden)
            where ID_SCHUELER = p_schueler_id;
        end if;
    end PRAXIS_STORNIEREN;

    procedure PRUEFUNG_ANMELDEN(
        p_schueler_id in varchar2,
        p_typ in varchar2,
        p_datum in timestamp,
        p_pruefer in varchar2,
        p_notiz in varchar2,
        p_pruefung_id out varchar2
    ) is
        v_notiz varchar2(4000);
    begin
        SCHUELER_PRUEFEN(p_schueler_id);
        if p_typ is null or p_datum is null then
            raise_application_error(-20031, 'Pruefungsart und Termin sind erforderlich.');
        end if;

        lock table PRUEFUNG in exclusive mode;
        select 'PRB' || lpad(nvl(max(to_number(regexp_substr(ID_PRUEFUNG, '[0-9]+$'))), 0) + 1, 6, '0')
        into p_pruefung_id
        from PRUEFUNG
        where ID_PRUEFUNG like 'PRB%';

        v_notiz := trim(
            case when p_pruefer is not null then 'Pruefer: ' || p_pruefer end
            || case when p_pruefer is not null and p_notiz is not null then ' | ' end
            || p_notiz
        );

        insert into PRUEFUNG (DATUM, TYP, ID_PRUEFUNG, ID_SCHUELER, NOTIZ)
        values (p_datum, p_typ, p_pruefung_id, p_schueler_id, v_notiz);
    end PRUEFUNG_ANMELDEN;

    procedure PRUEFUNG_ERGEBNIS_SPEICHERN(
        p_pruefung_id in varchar2,
        p_typ in varchar2,
        p_datum in timestamp,
        p_bestanden in number,
        p_ergebnis in varchar2,
        p_notiz in varchar2
    ) is
        v_status varchar2(50);
        v_notiz varchar2(4000);
    begin
        if p_typ is null or nvl(p_bestanden, -1) not in (0, 1) then
            raise_application_error(-20032, 'Pruefungsart und gueltiges Ergebnis sind erforderlich.');
        end if;

        v_status := substr(p_typ || case when p_bestanden = 1 then ' - bestanden' else ' - nicht bestanden' end, 1, 50);
        v_notiz := trim(
            case when p_ergebnis is not null then 'Ergebnis: ' || p_ergebnis end
            || case when p_ergebnis is not null and p_notiz is not null then ' | ' end
            || p_notiz
        );

        update PRUEFUNG
        set TYP = v_status,
            DATUM = p_datum,
            NOTIZ = v_notiz
        where ID_PRUEFUNG = p_pruefung_id;

        if sql%rowcount = 0 then
            raise_application_error(-20030, 'Pruefung wurde nicht gefunden.');
        end if;
    end PRUEFUNG_ERGEBNIS_SPEICHERN;

    procedure ABSCHLUSS_ANFRAGE_SPEICHERN(
        p_id in varchar2,
        p_schueler_id in varchar2,
        p_status in varchar2,
        p_begruendung in varchar2,
        p_angefragt_am in timestamp,
        p_geprueft_am in timestamp
    ) is
    begin
        SCHUELER_PRUEFEN(p_schueler_id);
        if p_id is null or p_status is null or p_angefragt_am is null then
            raise_application_error(-20041, 'Anfrage-ID, Status und Anfragezeitpunkt sind erforderlich.');
        end if;

        merge into ABSCHLUSS_ANFRAGE target
        using (
            select p_id ID_ABSCHLUSS_ANFRAGE,
                   p_schueler_id ID_SCHUELER,
                   p_status STATUS,
                   p_begruendung BEGRUENDUNG,
                   p_angefragt_am ANGEFRAGT_AM,
                   p_geprueft_am GEPRUEFT_AM
            from dual
        ) source
        on (target.ID_ABSCHLUSS_ANFRAGE = source.ID_ABSCHLUSS_ANFRAGE)
        when matched then update set
            target.ID_SCHUELER = source.ID_SCHUELER,
            target.STATUS = source.STATUS,
            target.BEGRUENDUNG = source.BEGRUENDUNG,
            target.ANGEFRAGT_AM = source.ANGEFRAGT_AM,
            target.GEPRUEFT_AM = source.GEPRUEFT_AM
        when not matched then insert (
            ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG,
            ANGEFRAGT_AM, GEPRUEFT_AM
        ) values (
            source.ID_ABSCHLUSS_ANFRAGE, source.ID_SCHUELER, source.STATUS,
            source.BEGRUENDUNG, source.ANGEFRAGT_AM, source.GEPRUEFT_AM
        );
    end ABSCHLUSS_ANFRAGE_SPEICHERN;
end SKYTEAM_WEBAPP_API;
/

show errors package SKYTEAM_WEBAPP_API;
show errors package body SKYTEAM_WEBAPP_API;
