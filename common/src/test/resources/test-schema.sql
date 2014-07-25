BEGIN;

CREATE TABLE members (
	id VARCHAR(36) NOT NULL PRIMARY KEY
);

CREATE TABLE users (
	username VARCHAR(255)
) INHERITS (members);

CREATE TABLE groups (
	name VARCHAR(255)
) INHERITS (members);

CREATE TABLE resources (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(255),
	owner VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT NOW(),
	modified TIMESTAMP NOT NULL DEFAULT NOW(),
	visibility VARCHAR(9),
	CONSTRAINT owner_fk FOREIGN KEY(owner) REFERENCES members(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE shares (
	member_id VARCHAR(36) NOT NULL,
	resource_id BIGINT NOT NULL,
	action VARCHAR(255) NOT NULL,
	CONSTRAINT share PRIMARY KEY (member_id, resource_id, action),
	CONSTRAINT member_fk FOREIGN KEY(member_id) REFERENCES members(id) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT resource_fk FOREIGN KEY(resource_id) REFERENCES resources(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE scripts (
	filename VARCHAR(255) NOT NULL PRIMARY KEY,
	passed TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tests (
	name VARCHAR(128) NOT NULL,
	number INT
) INHERITS (resources);

CREATE FUNCTION merge_users(key VARCHAR, data VARCHAR) RETURNS VOID AS
$$
BEGIN
    LOOP
        UPDATE users SET username = data WHERE id = key;
        IF found THEN
            RETURN;
        END IF;
        BEGIN
            INSERT INTO users(id,username) VALUES (key, data);
            RETURN;
        EXCEPTION WHEN unique_violation THEN
        END;
    END LOOP;
END;
$$
LANGUAGE plpgsql;

COMMIT;