create schema if not exists schema1;

CREATE TYPE schema1.my_country_type AS ENUM
    ('FULL_RECOGNIZED', 'PARTIALLY_RECOGNIZED', 'UNRECOGNIZED');

CREATE TABLE IF NOT EXISTS schema1.country (
    id INTEGER PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description text,
    population INTEGER,
    country_type schema1.my_country_type NOT NULL,
    amount numeric
);

CREATE SEQUENCE IF NOT EXISTS schema1.users_id_sec START WITH 1;

CREATE TABLE IF NOT EXISTS schema1.users (
     id INTEGER PRIMARY KEY default nextval('schema1.users_id_sec'),
     username VARCHAR(128) NOT NULL,
     email TEXT NOT NULL,
     create_date timestamp NOT NULL,
     amount numeric
);

