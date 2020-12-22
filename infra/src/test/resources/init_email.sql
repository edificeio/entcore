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