create table client
(
    id         bigserial primary key,
    first_name varchar(50),
    last_name  varchar(50)
);

create table account
(
    id             bigserial primary key,
    account_number bigint unique,
    client_id      bigint,
    account_type   int,
    amount         float8,
    foreign key (client_id) references client (id)
);

create table transaction
(
    id           bigserial primary key,
    date         timestamp,
    from_account bigint,
    amount       float8,
    foreign key (from_account) references account (id)
);

create table exchange_rate
(
    dollar double precision,
    euro   double precision,
    date   timestamp,
    primary key (dollar, euro)
);

create table users
(
    id       bigserial,
    username varchar(50) not null,
    email    varchar(50) unique,
    primary key (id)
);

create table student
(
    id       bigserial,
    name varchar(50) not null,
    address  varchar(50),
    primary key (id)
);

create procedure create_new_account(_account_number bigint, _client_id bigint, _account_type int)
    language sql
as
$$
insert into account (account_number, client_id, account_type, amount)
values (_account_number, _client_id, _account_type, 0)
$$;

CREATE OR REPLACE FUNCTION befo_insert()
    RETURNS trigger AS
$$
BEGIN
    new.first_name = LTRIM(new.first_name);
    new.last_name = LTRIM(new.last_name);
    RETURN NEW;
END;
$$
    LANGUAGE 'plpgsql';

CREATE TRIGGER trigger_val_before_insert
    BEFORE INSERT
    ON client
    FOR EACH ROW
EXECUTE PROCEDURE befo_insert();