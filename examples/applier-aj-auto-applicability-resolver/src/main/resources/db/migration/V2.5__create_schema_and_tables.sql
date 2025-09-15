-- переключиться на БД standin
create schema if not exists schema1;

CREATE TABLE IF NOT EXISTS schema1.country (
    id INTEGER PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    population INTEGER
);

CREATE SEQUENCE IF NOT EXISTS schema1.users_id_sec START WITH 1;

CREATE TABLE IF NOT EXISTS schema1.users (
    id INTEGER PRIMARY KEY default nextval('schema1.users_id_sec'),
    username VARCHAR(128) NOT NULL,
    country_id integer REFERENCES schema1.country(id) NOT NULL
);
