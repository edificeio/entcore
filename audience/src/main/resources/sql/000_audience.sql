CREATE SCHEMA audience;

CREATE TABLE audience.reactions (
    "id" BIGSERIAL NOT NULL PRIMARY KEY,
    "module" VARCHAR(36) NOT NULL,
    "resource_type" VARCHAR(64) NOT NULL,
    "resource_id" VARCHAR(36) NOT NULL,
    "profile" VARCHAR(12) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "reaction_date" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "reaction_type" VARCHAR(36) NOT NULL
)

ALTER TABLE audience.reactions ADD CONSTRAINT reactions_unique_constraint UNIQUE (
    module,
    resource_type,
    resource_id,
    user_id
)

CREATE TABLE audience.views (
    "id" BIGSERIAL NOT NULL PRIMARY KEY,
    "module" VARCHAR(36) NOT NULL,
    "resource_type" VARCHAR(64) NOT NULL,
    "resource_id" VARCHAR(36) NOT NULL,
    "profile" VARCHAR(9) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "last_view" TIMESTAMP NOT NULL,
    "counter" INT DEFAULT 0 NOT NULL
)

ALTER TABLE audience.views ADD CONSTRAINT views_unique_constraint UNIQUE (
    module,
    resource_type,
    resource_id,
    user_id
)

