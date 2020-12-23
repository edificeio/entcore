CREATE SCHEMA events;
CREATE TABLE events.access_events (
    id uuid not null PRIMARY KEY,
    date timestamp without time zone not null,
    profile character varying(9),
    module character varying(64),
    event_type character varying(16),
    ua character varying(256),
    device_type character varying(32),
    platform_id character varying(36),
    user_id character varying(36)
);