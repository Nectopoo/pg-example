create table schema1.users
(
    id       bigserial,
    username varchar(50) not null,
    email    varchar(50) unique,
    create_date timestamp default now(),
    with_time_zone timestamp with time zone default now(),
    primary key (id)
);

create table schema1.student
(
    id       bigserial,
    name varchar(50) not null,
    address  varchar(50),
    primary key (id)
);

insert into schema1.users (username, email)
values ('Bob Johnson', 'bob_johnson@gmail.com'),
       ('John Johnson', 'john_johnson@gmail.com');
