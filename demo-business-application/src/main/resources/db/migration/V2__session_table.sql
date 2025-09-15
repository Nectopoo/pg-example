CREATE TABLE user_session (
	id bigserial NOT NULL,
	user_id varchar(50) NULL,
	session_key varchar(50) NULL,
	CONSTRAINT user_session_pkey PRIMARY KEY (id)
);