CREATE SCHEMA test;

CREATE TABLE test.users (
	id VARCHAR(36) NOT NULL PRIMARY KEY,
	username VARCHAR(255)
);

CREATE TABLE test.groups (
	id VARCHAR(36) NOT NULL PRIMARY KEY,
	name VARCHAR(255)
);

CREATE TABLE test.members (
	id VARCHAR(36) NOT NULL PRIMARY KEY,
	user_id VARCHAR(36),
	group_id VARCHAR(36),
	CONSTRAINT user_fk FOREIGN KEY(user_id) REFERENCES test.users(id) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT group_fk FOREIGN KEY(group_id) REFERENCES test.groups(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE test.shares (
	member_id VARCHAR(36) NOT NULL,
	resource_id BIGINT NOT NULL,
	action VARCHAR(255) NOT NULL,
	CONSTRAINT share PRIMARY KEY (member_id, resource_id, action),
	CONSTRAINT member_fk FOREIGN KEY(member_id) REFERENCES test.members(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE test.scripts (
	filename VARCHAR(255) NOT NULL PRIMARY KEY,
	passed TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE FUNCTION test.merge_users(key VARCHAR, data VARCHAR) RETURNS VOID AS
$$
BEGIN
    LOOP
        UPDATE test.users SET username = data WHERE id = key;
        IF found THEN
            RETURN;
        END IF;
        BEGIN
            INSERT INTO test.users(id,username) VALUES (key, data);
            RETURN;
        EXCEPTION WHEN unique_violation THEN
        END;
    END LOOP;
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION test.insert_users_members() RETURNS TRIGGER AS $$
    BEGIN
		IF (TG_OP = 'INSERT') THEN
            INSERT INTO test.members (id, user_id) VALUES (NEW.id, NEW.id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION test.insert_groups_members() RETURNS TRIGGER AS $$
    BEGIN
		IF (TG_OP = 'INSERT') THEN
            INSERT INTO test.members (id, group_id) VALUES (NEW.id, NEW.id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_trigger
AFTER INSERT ON test.users
    FOR EACH ROW EXECUTE PROCEDURE test.insert_users_members();

CREATE TRIGGER groups_trigger
AFTER INSERT ON test.groups
    FOR EACH ROW EXECUTE PROCEDURE test.insert_groups_members();

CREATE TYPE test.share_tuple as (member_id VARCHAR(36), action VARCHAR(255));

CREATE TABLE test.tests (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(255),
	owner VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT NOW(),
	modified TIMESTAMP NOT NULL DEFAULT NOW(),
	visibility VARCHAR(9),
    number INT,
	CONSTRAINT owner_fk FOREIGN KEY(owner) REFERENCES test.users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE test.shares ADD CONSTRAINT resource_fk FOREIGN KEY(resource_id) REFERENCES test.tests(id) ON UPDATE CASCADE ON DELETE CASCADE;