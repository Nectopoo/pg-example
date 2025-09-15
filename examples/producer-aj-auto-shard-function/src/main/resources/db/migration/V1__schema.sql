CREATE TYPE schema1.my_country_type AS ENUM
    ('FULL_RECOGNIZED', 'PARTIALLY_RECOGNIZED', 'UNRECOGNIZED');

CREATE TABLE IF NOT EXISTS schema1.country (
    id INTEGER PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description text,
    population INTEGER,
    country_type schema1.my_country_type NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS schema1.users_id_sec START WITH 1;

CREATE TABLE IF NOT EXISTS schema1.users (
     id INTEGER PRIMARY KEY default nextval('schema1.users_id_sec'),
     username VARCHAR(128) NOT NULL,
     email TEXT NOT NULL,
     create_date timestamp NOT NULL
);
CREATE TYPE schema1.my_employee_type AS ENUM
    ('FULL', 'PARTIALLY');

CREATE TABLE IF NOT EXISTS schema1.employee (
                       id INTEGER PRIMARY KEY,
                       name VARCHAR(128) NOT NULL,
                       description text,
                       salary INTEGER,
                       employee_type schema1.my_employee_type NOT NULL
);

-- connect to main2 & standin2
-- CREATE TYPE schema2.my_country_type AS ENUM
--     ('FULL_RECOGNIZED', 'PARTIALLY_RECOGNIZED', 'UNRECOGNIZED');
--
-- CREATE TABLE IF NOT EXISTS schema2.country (
--     id INTEGER PRIMARY KEY,
--     name VARCHAR(128) NOT NULL,
--     description text,
--     population INTEGER,
--     country_type schema2.my_country_type NOT NULL
-- );
--
-- CREATE SEQUENCE IF NOT EXISTS schema2.users_id_sec START WITH 1;
--
-- CREATE TABLE IF NOT EXISTS schema2.users (
--      id INTEGER PRIMARY KEY default nextval('schema2.users_id_sec'),
--      username VARCHAR(128) NOT NULL,
--      email TEXT NOT NULL,
--      create_date timestamp NOT NULL
-- );
-- CREATE TYPE schema2.my_employee_type AS ENUM
--     ('FULL', 'PARTIALLY');
--
-- CREATE TABLE IF NOT EXISTS schema2.employee (
--                        id INTEGER PRIMARY KEY,
--                        name VARCHAR(128) NOT NULL,
--                        description text,
--                        salary INTEGER,
--                        employee_type schema2.my_employee_type NOT NULL
-- );

