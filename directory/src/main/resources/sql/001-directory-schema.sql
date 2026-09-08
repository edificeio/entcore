CREATE SCHEMA IF NOT EXISTS directory;

GRANT USAGE ON SCHEMA directory TO "apps";
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA directory GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON TABLES TO "apps";
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA directory GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "apps";
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA directory GRANT EXECUTE ON FUNCTIONS TO "apps";

CREATE TABLE IF NOT EXISTS directory.user_link (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" VARCHAR(36) NOT NULL,
	"position" SMALLINT NOT NULL,
    "name" VARCHAR(256),
    "url" TEXT NOT NULL,
    CONSTRAINT user_link_position_range CHECK ("position" BETWEEN 0 AND 9),
    CONSTRAINT user_link_user_position_key UNIQUE ("user_id", "position")
);

COMMENT ON COLUMN directory.user_link."position" IS 'Index de capacite, pas un ordre d''affichage : le CHECK 0..9 et la contrainte UNIQUE (user_id, position) sont ce qui plafonne un utilisateur a 10 liens. L''affichage est trie alphabetiquement sur name.';

CREATE TABLE IF NOT EXISTS directory.scripts ("filename" VARCHAR(255) NOT NULL PRIMARY KEY,
         "passed" TIMESTAMP NOT NULL DEFAULT NOW()
);
