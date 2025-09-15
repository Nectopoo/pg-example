create schema if not exists main;

CREATE TABLE IF NOT EXISTS main.country (
    id INTEGER PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    population INTEGER
);

create schema if not exists standin;

CREATE TABLE IF NOT EXISTS standin.country (
    id INTEGER PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    population INTEGER
);