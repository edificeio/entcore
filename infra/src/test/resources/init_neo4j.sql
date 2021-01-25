CREATE SCHEMA events;
CREATE TABLE events.neo4j_change_events (
    id uuid not null PRIMARY KEY,
    date timestamp without time zone not null,
    profile character varying(9),
    module character varying(64),
    event_type character varying(16),
    state character varying(20),
    url character varying(256),
    starting BOOLEAN,
    node_type character varying(16),
    hostname character varying(256),
    platform_id character varying(36),
    user_id character varying(36)
);
CREATE SCHEMA mail;
CREATE TABLE mail.mail_events(
    id uuid PRIMARY KEY,
    date timestamp without time zone not null,
    profile character varying(9),
    module character varying(64),
    platform_id character varying(36),
    platform_url character varying(300),
    user_id character varying(36),
    receivers jsonb not null,
    from_mail character varying(325) not null,
    from_name text,
    cc jsonb,
    bcc jsonb,
    subject text not null,
    headers jsonb,
    body text not null,
    attempt integer DEFAULT 0,
    attempt_reason text,
    attempt_at timestamp without time zone,
    status smallint DEFAULT 0,
    read boolean DEFAULT false,
    priority smallint DEFAULT 0,
    sent_with character varying(100)
);
CREATE TABLE mail.attachments_events (
    id uuid PRIMARY KEY,
    date timestamp without time zone not null,
    mail_id uuid not null,
    name text not null,
    content text not null
);