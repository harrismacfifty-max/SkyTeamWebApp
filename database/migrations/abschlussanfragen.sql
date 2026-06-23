-- Optionales Oracle-Schema fuer den BPMN-Abschlussprozess.
-- Nicht automatisch ausfuehren, wenn die Uni-Datenbank diese Tabelle nicht vorsieht.

create table ABSCHLUSS_ANFRAGE (
    ID_ABSCHLUSS_ANFRAGE varchar2(30) primary key,
    ID_SCHUELER varchar2(30) not null,
    STATUS varchar2(30) not null,
    BEGRUENDUNG varchar2(1000),
    ANGEFRAGT_AM timestamp not null,
    GEPRUEFT_AM timestamp,
    constraint FK_ABSCHLUSS_ANFRAGE_SCHUELER
        foreign key (ID_SCHUELER)
        references SCHUELER (ID_SCHUELER)
);

create index IDX_ABSCHLUSS_ANFRAGE_SCHUELER
    on ABSCHLUSS_ANFRAGE (ID_SCHUELER);

create index IDX_ABSCHLUSS_ANFRAGE_STATUS
    on ABSCHLUSS_ANFRAGE (STATUS);
