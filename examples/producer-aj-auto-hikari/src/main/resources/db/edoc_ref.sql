create table rrko_edoc_ref
(
    id                 bigint not null
        constraint rrko_edoc_ref_pk
            primary key,
    edoc_class_name    varchar(255),
    edoc_id            bigint,
    client_snapshot_id bigint,
    branch_snapshot_id bigint
);

create index edoc_ref_branch_snapshot_id_id_idx
    on rrko_edoc_ref (branch_snapshot_id, id);

create index edoc_ref_client_snapshot_id_id_idx
    on rrko_edoc_ref (client_snapshot_id, id);

create index edoc_ref_edoc_id_idx
    on rrko_edoc_ref (edoc_id);

CREATE SEQUENCE IF NOT EXISTS edoc_ref_id_seq INCREMENT 50 START WITH 1;

