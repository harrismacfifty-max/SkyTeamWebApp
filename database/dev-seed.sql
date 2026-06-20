insert into fs_student (id, first_name, last_name, email, phone, license_goal, status)
values (fs_student_seq.nextval, 'Lena', 'Hoffmann', 'lena.hoffmann@example.com', '+49 201 1001', 'PPL(A)', 'Aktiv');

insert into fs_student (id, first_name, last_name, email, phone, license_goal, status)
values (fs_student_seq.nextval, 'Jonas', 'Richter', 'jonas.richter@example.com', '+49 201 1002', 'LAPL(A)', 'Aktiv');

insert into fs_aircraft (id, registration, model, status, total_hours)
values (fs_aircraft_seq.nextval, 'D-ESKY', 'Cessna 172', 'Verfuegbar', 1240);

insert into fs_aircraft (id, registration, model, status, total_hours)
values (fs_aircraft_seq.nextval, 'D-ETEA', 'Piper PA-28', 'Wartung geplant', 980);

insert into fs_lesson (
    id, student_id, aircraft_id, instructor, scheduled_at, duration_minutes, training_type, status, notes
) values (
    fs_lesson_seq.nextval, 1, 1, 'M. Keller', current_timestamp + interval '1' day, 90, 'Platzrunden', 'Geplant', 'Vorflugkontrolle einplanen'
);

insert into fs_lesson (
    id, student_id, aircraft_id, instructor, scheduled_at, duration_minutes, training_type, status, notes
) values (
    fs_lesson_seq.nextval, 2, 2, 'S. Weber', current_timestamp + interval '3' day, 120, 'Navigation', 'Geplant', 'Route EDLE-EDLN vorbereiten'
);

commit;

