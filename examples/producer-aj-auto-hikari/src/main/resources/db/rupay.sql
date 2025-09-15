create table rrko_ru_payment
(
    id                                   bigint                    not null
        constraint rrko_ru_payment_pk
            primary key,
    edoc_ref_id                          bigint                    not null
        constraint rrko_ru_payment_edoc_ref_fk
            references rrko_edoc_ref,
    archive_date                         timestamp with time zone,
    branch_ext_id                        bigint,
    branch_snapshot                      jsonb,
    channel                              integer,
    client_ext_id                        bigint,
    client_snapshot                      jsonb,
    create_date                          timestamp with time zone,
    delete_date                          timestamp with time zone,
    document_number                      varchar(255),
    last_modify_date                     timestamp with time zone,
    note_from_bank_author                bigint,
    note_from_bank_comment               text,
    note_from_bank_create_date           timestamp(6) with time zone,
    note_from_bank_fio                   varchar(255),
    note_from_receiver                   varchar(255),
    receive_date                         timestamp with time zone,
    send_date                            timestamp with time zone,
    status_action                        varchar(255),
    status_base                          varchar(255),
    status_comment                       varchar(300),
    type_id                              bigint,
    last_modify_user_ext_id              varchar(38),
    last_modify_user_snapshot            jsonb,
    amount                               numeric(19, 2),
    authorization_attempt                integer,
    authorization_information            varchar(255),
    budget_type                          integer,
    budget_customs_code                  varchar(255),
    budget_kbk                           varchar(255),
    budget_oktmo                         varchar(255),
    budget_payer_status                  varchar(255),
    budget_reason_code                   varchar(255),
    budget_reason_document_date          varchar(255),
    budget_reason_document_number        varchar(255),
    budget_tax_period_code               varchar(255),
    commission                           numeric(19, 2),
    commission_type                      varchar(255),
    debit_date                           timestamp(6) with time zone,
    document_date                        timestamp with time zone,
    expected_execution_date              timestamp(6) with time zone,
    fraud_comment                        text,
    fraud_external_ip_address            varchar(255),
    fraud_internal_ip_address            varchar(255),
    fraud_mac_address                    varchar(255),
    fraud_state                          varchar(255),
    import_id                            bigint,
    operation_type                       varchar(255),
    payment_code                         varchar(255),
    payment_ground_description           varchar(255),
    payment_ground_nds                   numeric(19, 2),
    payment_ground_nds_calculation       varchar(255),
    payment_ground_nds_percent           varchar(255),
    payment_ground_operation_code        varchar(255),
    payment_priority                     integer,
    payment_type                         varchar(255),
    payment_type_code                    integer,
    recipient_official                   varchar(255),
    sender_official                      varchar(255),
    uin_uip                              varchar(255),
    payer_account                        varchar(255),
    payer_additional_information         varchar(255),
    payer_address                        varchar(255),
    payer_inn_kio                        varchar(255),
    payer_kpp                            varchar(255),
    payer_name                           text,
    payer_bank_address                   varchar(255),
    payer_bank_bic                       varchar(255),
    payer_bank_corr_account              varchar(255),
    payer_bank_name                      varchar(255),
    receiver_account                     varchar(255),
    receiver_inn_kio                     varchar(255),
    receiver_kpp                         varchar(255),
    receiver_name                        text,
    receiver_bank_address                varchar(255),
    receiver_bank_bic                    varchar(255),
    receiver_bank_corr_account           varchar(255),
    receiver_bank_name                   varchar(255),
    exporting_to_abs                     boolean,
    cabs_doc_ref                         varchar(50)
        unique,
    cabs_doc_ref_ext                     varchar(50),
    fraud_doc_ref_ext                    varchar(50),
    fraud_http_accept                    varchar(256),
    fraud_http_accept_chars              varchar(256),
    fraud_http_accept_encoding           varchar(256),
    fraud_http_accept_language           varchar(256),
    fraud_http_referer                   varchar(256),
    fraud_user_agent                     varchar(256),
    fraud_device_print                   varchar(4096),
    cut_off_time_extend_bic              boolean,
    cut_off_time_extend_account          boolean,
    optlock                              integer     default 0,
    fraud_user_id                        varchar(255),
    document_number_as_num               bigint,
    read                                 boolean,
    import_date                          timestamp with time zone,
    res_field_23                         varchar(255),
    import_session_id                    bigint,
    source_id                            bigint,
    status_bank_backend_response_message jsonb,
    fraud_user_login                     varchar(255),
    loan_accept                          varchar(1),
    note_bank_employee_author            bigint,
    note_bank_employee_comment           text,
    note_bank_employee_create_date       timestamp(6) with time zone,
    note_bank_employee_fio               varchar(255),
    note_fraud_author                    bigint,
    note_fraud_comment                   text,
    note_fraud_create_date               timestamp(6) with time zone,
    note_fraud_fio                       varchar(255),
    message_for_bank                     varchar(4000),
    message_for_bank_code                varchar(50),
    rates_confirmed                      boolean,
    status_extended                      jsonb,
    cold_data                            boolean     default false,
    budget_ext_id                        bigint,
    budget_name                          varchar(255),
    kesr_code                            varchar(64),
    kesr_code_name                       varchar(1024),
    is_exported                          boolean     default false,
    income_type_code                     varchar(35),
    recovery_amount                      numeric(19, 2),
    sign_bsk                             varchar(30),
    uuid                                 varchar(50)
        unique,
    value_date                           timestamp(6) with time zone,
    abs_type_ext                         varchar(255),
    supporting_document_ids              bigint[],
    manual_acceptance_end_date           timestamp(6) with time zone,
    manual_acceptance_official           varchar(255),
    registry_ref_doc_id                  bigint,
    registry_date                        timestamp with time zone,
    registry_number                      varchar(255),
    edk_doc_ref_ext                      varchar(38),
    credit_register                      varchar(40) default 'Не обрабатывается'::character varying,
    role_template_signature              varchar(255),
    manual_accept_results                smallint    default 0,
    registry_payer_account               varchar(255),
    registry_bic                         varchar(255),
    create_type                          varchar(20),
    budget_control_items                 jsonb,
    payment_type_int                     varchar(255),
    budget_reason_code_int               varchar(255),
    migrated                             boolean,
    registry_client_id                   bigint,
    payer_name_int                       varchar(255),
    payer_bank_name_int                  varchar(255),
    payer_address_int                    varchar(255),
    receiver_bank_name_int               varchar(255),
    contractor_rule_id                   bigint,
    contractor_rule_version              integer,
    contractor_zok_number                varchar(255),
    contractor_zok_date                  date,
    contractor_restriction               varchar(40),
    signature_session_id                 varchar(40),
    contractor_restriction_check_date    date,
    source_name                          varchar(50),
    fine_edoc_id                         bigint,
    payment_message_type                 varchar(50),
    visible_for_acceptor                 boolean     default false not null,
    contractor_restriction_check_code    varchar(255),
    contractor_restriction_check_hint    varchar(255),
    contractor_restriction_check_params  bytea,
    part_document_ids                    jsonb,
    part_document_amount                 numeric(19, 2),
    mac_address                          varchar(255),
    contractor_id                        bigint,
    registry_doc_type                    integer,
    registry_sign_time                   timestamp(6) with time zone,
    special_accept                       boolean,
    registry_edoc_ref_id                 bigint,
    params                               jsonb,
    card_indexes                         jsonb,
    registry_signatures                  jsonb,
    localized_status_base                varchar(100),
    smb_document_id                      varchar(40)
        unique,
    payer_account_id                     bigint,
    account_number                       varchar(50),
    payer_account_key                    varchar(200),
    delivered_date                       timestamp with time zone,
    receiver_type                        varchar(50),
    budget_need_sections                 boolean     default false not null,
    budget_sections                      jsonb,
    payer_account_ext_id                 bigint
);

comment on table rrko_ru_payment is 'Ruble payment entity';

comment on column rrko_ru_payment.id is 'Electronic document id';

comment on column rrko_ru_payment.edoc_ref_id is 'Electronic document reference id';

comment on column rrko_ru_payment.archive_date is 'Archive date';

comment on column rrko_ru_payment.branch_ext_id is 'External id of branch';

comment on column rrko_ru_payment.branch_snapshot is 'Branch object snapshot';

comment on column rrko_ru_payment.channel is 'Channel';

comment on column rrko_ru_payment.client_ext_id is 'External id of client';

comment on column rrko_ru_payment.client_snapshot is 'Client object snapshot';

comment on column rrko_ru_payment.create_date is 'Created at';

comment on column rrko_ru_payment.delete_date is 'Deleted at';

comment on column rrko_ru_payment.document_number is 'Electronic document number';

comment on column rrko_ru_payment.last_modify_date is 'Last modify at';

comment on column rrko_ru_payment.note_from_bank_author is 'Note from bank - author';

comment on column rrko_ru_payment.note_from_bank_comment is 'Note from bank - comment';

comment on column rrko_ru_payment.note_from_bank_create_date is 'Note from bank - created at';

comment on column rrko_ru_payment.note_from_bank_fio is 'Note from bank - fio';

comment on column rrko_ru_payment.note_from_receiver is 'Note from receiver';

comment on column rrko_ru_payment.receive_date is 'Received at';

comment on column rrko_ru_payment.send_date is 'Sended at';

comment on column rrko_ru_payment.status_action is 'Status action';

comment on column rrko_ru_payment.status_base is 'Base status name';

comment on column rrko_ru_payment.status_comment is 'Status comment';

comment on column rrko_ru_payment.type_id is 'Type id';

comment on column rrko_ru_payment.last_modify_user_ext_id is 'External id of last modify user';

comment on column rrko_ru_payment.last_modify_user_snapshot is 'Snapshot of last modify user';

comment on column rrko_ru_payment.amount is 'Amount';

comment on column rrko_ru_payment.authorization_attempt is 'Authorization attempt';

comment on column rrko_ru_payment.authorization_information is 'Authorization information';

comment on column rrko_ru_payment.budget_type is 'Budget type';

comment on column rrko_ru_payment.budget_customs_code is 'Budget custom code';

comment on column rrko_ru_payment.budget_kbk is 'Budget kbk';

comment on column rrko_ru_payment.budget_oktmo is 'Budget oktmo';

comment on column rrko_ru_payment.budget_payer_status is 'Budget payer status';

comment on column rrko_ru_payment.budget_reason_code is 'Budget reason code';

comment on column rrko_ru_payment.budget_reason_document_date is 'Budget reason doc date';

comment on column rrko_ru_payment.budget_reason_document_number is 'Budget reason doc number';

comment on column rrko_ru_payment.budget_tax_period_code is 'Budget tax period code';

comment on column rrko_ru_payment.commission is 'Commission';

comment on column rrko_ru_payment.commission_type is 'Commission type';

comment on column rrko_ru_payment.debit_date is 'Debit date';

comment on column rrko_ru_payment.document_date is 'Document date';

comment on column rrko_ru_payment.expected_execution_date is 'Expected execution date';

comment on column rrko_ru_payment.fraud_comment is 'FRAUD comment';

comment on column rrko_ru_payment.fraud_external_ip_address is 'FRAUD external ip';

comment on column rrko_ru_payment.fraud_internal_ip_address is 'FRAUD internal ip';

comment on column rrko_ru_payment.fraud_mac_address is 'FRAUD mac address';

comment on column rrko_ru_payment.fraud_state is 'FRAUD state';

comment on column rrko_ru_payment.import_id is 'Import id';

comment on column rrko_ru_payment.operation_type is 'Operation type';

comment on column rrko_ru_payment.payment_code is 'Payment code';

comment on column rrko_ru_payment.payment_ground_description is 'Payment ground description';

comment on column rrko_ru_payment.payment_ground_nds is 'Payment ground NDS';

comment on column rrko_ru_payment.payment_ground_nds_calculation is 'Payment ground NDS calculation type';

comment on column rrko_ru_payment.payment_ground_nds_percent is 'Payment ground NDS percent';

comment on column rrko_ru_payment.payment_ground_operation_code is 'Payment ground currency operation code';

comment on column rrko_ru_payment.payment_priority is 'Payment priority';

comment on column rrko_ru_payment.payment_type is 'Payment type';

comment on column rrko_ru_payment.payment_type_code is 'Payment type code';

comment on column rrko_ru_payment.recipient_official is 'Executive in charge of the sender initials';

comment on column rrko_ru_payment.sender_official is 'Responsible officer of the recipient initials';

comment on column rrko_ru_payment.uin_uip is 'UIN code';

comment on column rrko_ru_payment.payer_account is 'Payer account';

comment on column rrko_ru_payment.payer_additional_information is 'Payer account';

comment on column rrko_ru_payment.payer_address is 'Payer account';

comment on column rrko_ru_payment.payer_inn_kio is 'Payer inn/kio';

comment on column rrko_ru_payment.payer_kpp is 'Payer kpp';

comment on column rrko_ru_payment.payer_name is 'Payer name';

comment on column rrko_ru_payment.payer_bank_address is 'Ruble payment payer bank address';

comment on column rrko_ru_payment.payer_bank_bic is 'Ruble payment payer bank bic';

comment on column rrko_ru_payment.payer_bank_corr_account is 'Ruble payment payer bank correspondent account';

comment on column rrko_ru_payment.payer_bank_name is 'Ruble payment payer bank name';

comment on column rrko_ru_payment.receiver_account is 'Receiver account';

comment on column rrko_ru_payment.receiver_inn_kio is 'Receiver inn/kio';

comment on column rrko_ru_payment.receiver_kpp is 'Receiver kpp';

comment on column rrko_ru_payment.receiver_name is 'Receiver name';

comment on column rrko_ru_payment.receiver_bank_address is 'Ruble payment receiver bank address';

comment on column rrko_ru_payment.receiver_bank_bic is 'Ruble payment receiver bank bic';

comment on column rrko_ru_payment.receiver_bank_corr_account is 'Ruble payment receiver bank correspondent account';

comment on column rrko_ru_payment.receiver_bank_name is 'Ruble payment receiver bank name';

comment on column rrko_ru_payment.note_bank_employee_author is 'Note from bank employee - author';

comment on column rrko_ru_payment.note_bank_employee_comment is 'Note from bank employee - comment';

comment on column rrko_ru_payment.note_bank_employee_create_date is 'Note from bank employee - created at';

comment on column rrko_ru_payment.note_bank_employee_fio is 'Note from bank employee- fio';

comment on column rrko_ru_payment.note_fraud_author is 'Note from fraud - author';

comment on column rrko_ru_payment.note_fraud_comment is 'Note from fraud - comment';

comment on column rrko_ru_payment.note_fraud_create_date is 'Note from fraud - created at';

comment on column rrko_ru_payment.note_fraud_fio is 'Note from fraud - fio';

comment on column rrko_ru_payment.message_for_bank is 'Message for bank text';

comment on column rrko_ru_payment.message_for_bank_code is 'Message for bank code';

comment on column rrko_ru_payment.rates_confirmed is 'Flag representing accept of bank rates';

comment on column rrko_ru_payment.budget_control_items is 'Items of budget control';

comment on column rrko_ru_payment.payment_type_int is 'Payment type international';

comment on column rrko_ru_payment.budget_reason_code_int is 'Budget reason code international';

comment on column rrko_ru_payment.migrated is 'Migrated flag';

comment on column rrko_ru_payment.registry_client_id is 'Registry client id';

comment on column rrko_ru_payment.payer_name_int is 'Payer name international';

comment on column rrko_ru_payment.payer_bank_name_int is 'Payer bank name international';

comment on column rrko_ru_payment.payer_address_int is 'Payer address international';

comment on column rrko_ru_payment.receiver_bank_name_int is 'Receiver bank name international';

comment on column rrko_ru_payment.contractor_rule_id is 'Идентификатор правила ДиСК';

comment on column rrko_ru_payment.contractor_rule_version is 'Версия правила ДиСК';

comment on column rrko_ru_payment.contractor_zok_number is 'Номер заявления на ограничение контрагента, ЭД ЗОК';

comment on column rrko_ru_payment.contractor_zok_date is 'Дата заявления на ограничение контрагента, ЭД ЗОК';

comment on column rrko_ru_payment.contractor_restriction is 'Принадлежность контрагента к «черному» или «белому» спискам';

comment on column rrko_ru_payment.signature_session_id is 'Signature session id';

comment on column rrko_ru_payment.contractor_restriction_check_date is 'Дата проверки ограничений по справочнику ДиСК';

comment on column rrko_ru_payment.source_name is 'Краткое наименованием внешней системы, откуда был получен документ';

comment on column rrko_ru_payment.fine_edoc_id is 'Идентификатор EDoc штрафов';

comment on column rrko_ru_payment.payment_message_type is 'Subtype of the ruble payment order received via the Х2Х channel';

comment on column rrko_ru_payment.contractor_restriction_check_code is 'Код сообщения БК контрагента по справочнику ДиСК';

comment on column rrko_ru_payment.contractor_restriction_check_hint is 'Подсказка БК контрагента по справочнику ДиСК';

comment on column rrko_ru_payment.contractor_restriction_check_params is 'Параметры БК контрагента по справочнику ДиСК';

comment on column rrko_ru_payment.mac_address is 'MAC address';

comment on column rrko_ru_payment.params is 'Context with additional parameters';

comment on column rrko_ru_payment.registry_signatures is 'Registry signatures';

comment on column rrko_ru_payment.localized_status_base is 'Localized status';

comment on column rrko_ru_payment.smb_document_id is 'Document id from SMB system';

comment on column rrko_ru_payment.payer_account_key is 'Payer account business key';

comment on column rrko_ru_payment.delivered_date is 'Date and time of transition to DELIVERED status';

comment on column rrko_ru_payment.receiver_type is 'Receiver type INTERNAL or EXTERNAL';

comment on column rrko_ru_payment.budget_need_sections is 'Budget need sections';

comment on column rrko_ru_payment.budget_sections is 'Budget sections';

comment on column rrko_ru_payment.payer_account_ext_id is 'Payer account ext id';


create index branch_ext_id_index
    on rrko_ru_payment (branch_ext_id);

create index delivered_date_index
    on rrko_ru_payment (delivered_date);

create index payer_account_id_index
    on rrko_ru_payment (payer_account_id);

create index edoc_entity_id_idx
    on rrko_ru_payment (uuid, type_id, client_ext_id);

create unique index rrko_empty_payer_account_key_index
    on rrko_ru_payment (id)
    where (payer_account_key IS NULL);

create index fix_rrko_ru_payment_last_modify_date_id_partial_3idx
    on rrko_ru_payment (last_modify_date, id)
    where ((cold_data = false) AND ((status_base)::text = ANY
                                    (ARRAY [('test.dev.lifecycle.model.RejectedStatus#REJECTED'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXPORT_ABS_ERROR'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXPORT_RSA_ERROR'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CUT_OFF_TIME_FAILED'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WAITING_FOR_VALUE_DATE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#SUSPECT_FRAUD'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IN_PROCESS_FRAUD'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WAITING_FOR_CONFIRMATION_FRAUD'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#BUDGET_CONTROL_SENDING_ERROR'::character varying)::text, ('test.dev.lifecycle.model.NewEdocStatus#NEW'::character varying)::text, ('test.dev.lifecycle.model.PartlySignedStatus#PARTLY_SIGNED'::character varying)::text, ('test.dev.lifecycle.model.GenericStatus#SIGNED'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#INCORRECT_SIGNATURE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#INCORRECT_DETAILS'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_BANK'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_CLIENT'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#BC_DENIED_ACCEPTED'::character varying)::text])));

create index rrko_payer_account_key_index
    on rrko_ru_payment (client_ext_id, payer_account_key);

create index rrko_ru_payment_cabs_doc_ref_ext_idx
    on rrko_ru_payment (cabs_doc_ref_ext);

create index rrko_ru_payment_amount_idx
    on rrko_ru_payment (amount);

create index rrko_ru_payment_cabs_doc_ref_idx
    on rrko_ru_payment (cabs_doc_ref);

create index rrko_ru_payment_check_document_number_for_unique_rule_idx
    on rrko_ru_payment (document_number, client_ext_id)
    where ((delete_date IS NULL) AND ((status_base)::text <> 'test.dev.lifecycle.model.RejectedStatus#REJECTED'::text) AND
           ((status_base)::text <> ALL
            (ARRAY [('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DELETED'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#NONE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORTING'::character varying)::text])) AND
           ((status_base)::text <> ALL
            (ARRAY [('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#TEMPLATE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DRAFT'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORT_ERROR'::character varying)::text])));

create index rrko_ru_payment_cl_ext_id_with_status_idx
    on rrko_ru_payment (client_ext_id, last_modify_date)
    where ((status_base)::text = ANY
           ('{test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXECUTED,test.dev.lifecycle.model.RejectedStatus#REJECTED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WRONG_E_SIGNATURE,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DETAILS_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DENIED_ACCEPT,test.dev.lifecycle.model.GenericStatus#RECALLED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_CLIENT,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_BANK,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DENY_AF,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DENY_AML,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#BC_DENIED_ACCEPTED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXECUTED_TRANSFER,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXECUTED_NOT_TRANSFER,test.dev.lifecycle.model.LoadingByH2HStatus#SIGNMODEL_VALIDATION_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#ACCEPT_DENIED_BY_BANK,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#NOT_EXECUTED_BY_EXTSYS,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#ACCESS_DENIED_MANUAL_BC,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#ACCESS_DENIED_AUTO_BC,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#REMOVED_FROM_ACCEPT,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#REMOVED_FROM_ACCEPT_BC,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#NOT_EXECUTED_BY_ABS,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXECUTED_ABS,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DELETED,test.dev.lifecycle.model.LoadingByH2HStatus#LOAD_ERROR}'::text[]));

create index rrko_ru_payment_client_ext_id_document_date_id_statusfilter_idx
    on rrko_ru_payment (client_ext_id asc, document_date desc, id desc)
    where ((delete_date IS NULL) AND ((status_base)::text = ANY
                                      (ARRAY [('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DRAFT'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORT_ERROR'::character varying)::text, ('test.dev.lifecycle.model.NewEdocStatus#NEW'::character varying)::text])));

create index rrko_ru_payment_client_ext_id_is_allowed_statuses_idx
    on rrko_ru_payment (client_ext_id, status_base, delete_date)
    where ((delete_date IS NULL) AND ((status_base)::text <> ALL
                                      (ARRAY [('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#NONE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORTING'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DELETED'::character varying)::text])));

create index rrko_ru_payment_client_ext_id_idx
    on rrko_ru_payment (client_ext_id);

create index rrko_ru_payment_document_date_idx
    on rrko_ru_payment (document_date);

create index rrko_ru_payment_document_number_document_date_partial_idx
    on rrko_ru_payment (document_number asc, document_date desc)
    where ((delete_date IS NULL) AND ((status_base)::text <> ALL
                                      (ARRAY [('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DELETED'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#NONE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORTING'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#TEMPLATE'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DRAFT'::character varying)::text, ('test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IMPORT_ERROR'::character varying)::text])));

create index rrko_ru_payment_document_number_idx
    on rrko_ru_payment (document_number);

create index rrko_ru_payment_fraud_doc_ref_ext_idx
    on rrko_ru_payment (fraud_doc_ref_ext);

create unique index rrko_ru_payment_id_last_modify_date_idx
    on rrko_ru_payment (id, last_modify_date);

create index rrko_ru_payment_id_with_status_idx
    on rrko_ru_payment (id, last_modify_date)
    where ((status_base)::text = ANY
           ('{test.dev.lifecycle.model.RejectedStatus#REJECTED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXPORT_ABS_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#EXPORT_RSA_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CUT_OFF_TIME_FAILED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WAITING_FOR_VALUE_DATE,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#SUSPECT_FRAUD,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#IN_PROCESS_FRAUD,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WAITING_FOR_CONFIRMATION_FRAUD,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#REVIEW_AML,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#BUDGET_CONTROL_SENDING_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#FOR_ACCEPT_BY_BANK,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#PARTLY_ACCEPTED_BY_BANK,test.dev.lifecycle.model.NewEdocStatus#NEW,test.dev.lifecycle.model.PartlySignedStatus#PARTLY_SIGNED,test.dev.lifecycle.model.GenericStatus#SIGNED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#WRONG_E_SIGNATURE,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DETAILS_ERROR,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_BANK,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#CONFIRMED_FRAUD_BY_CLIENT,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DENY_AF,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#DENY_AML,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#BC_DENIED_ACCEPTED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#FOR_ACCEPT,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#PARTLY_ACCEPTED,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#FOR_ACCEPT_BC,test.dev.dbo.rupayment.domain.rupayment.entity.type.RuPaymentStatus#PARTLY_ACCEPTED_BC}'::text[]));

create index rrko_ru_payment_import_session_id_idx
    on rrko_ru_payment (import_session_id);

create index rrko_ru_payment_last_modify_date_idx
    on rrko_ru_payment (last_modify_date);

create index rrko_ru_payment_payer_account_idx
    on rrko_ru_payment (payer_account);

create index rrko_ru_payment_payer_bank_bic_idx
    on rrko_ru_payment (payer_bank_bic);

create index rrko_ru_payment_status_and_last_modify_2date_idx
    on rrko_ru_payment (status_base, id, last_modify_date);

create index rrko_ru_payment_status_and_last_modify_date_idx
    on rrko_ru_payment (status_base, last_modify_date);

create index rrko_ru_payment_status_base_idx
    on rrko_ru_payment (status_base);

create index ru_payment_client_ext_id_status_base_delete_date_idx
    on rrko_ru_payment (client_ext_id, status_base, delete_date)
    where (delete_date IS NULL);

create index ru_payment_edoc_ref_id_idx
    on rrko_ru_payment (edoc_ref_id);

CREATE SEQUENCE IF NOT EXISTS rupay_id_seq INCREMENT 50 START WITH 1;
