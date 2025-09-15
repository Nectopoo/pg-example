create table schema1.address (
    id bigint primary key ,
    postal_code varchar(128),
    user_id integer references schema1.users(id)
);
