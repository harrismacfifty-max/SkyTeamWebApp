create table fs_student (
    id number(10) primary key,
    first_name varchar2(80) not null,
    last_name varchar2(80) not null,
    email varchar2(160) not null,
    phone varchar2(60),
    license_goal varchar2(40) default 'PPL(A)' not null,
    status varchar2(30) default 'Aktiv' not null,
    created_at timestamp default current_timestamp not null
);

create sequence fs_student_seq start with 1 increment by 1 nocache;

create table fs_aircraft (
    id number(10) primary key,
    registration varchar2(20) not null unique,
    model varchar2(80) not null,
    status varchar2(40) default 'Verfuegbar' not null,
    total_hours number(8) default 0 not null,
    created_at timestamp default current_timestamp not null
);

create sequence fs_aircraft_seq start with 1 increment by 1 nocache;

create table fs_lesson (
    id number(10) primary key,
    student_id number(10) not null,
    aircraft_id number(10) not null,
    instructor varchar2(120) not null,
    scheduled_at timestamp not null,
    duration_minutes number(4) not null,
    training_type varchar2(80) not null,
    status varchar2(30) default 'Geplant' not null,
    notes varchar2(1000),
    created_at timestamp default current_timestamp not null,
    constraint fk_fs_lesson_student foreign key (student_id) references fs_student(id),
    constraint fk_fs_lesson_aircraft foreign key (aircraft_id) references fs_aircraft(id)
);

create sequence fs_lesson_seq start with 1 increment by 1 nocache;

