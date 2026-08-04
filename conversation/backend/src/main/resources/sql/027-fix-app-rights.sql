-- rattrapage de l'existant
GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES     IN SCHEMA conversation TO "apps";
GRANT USAGE, SELECT, UPDATE              ON ALL SEQUENCES IN SCHEMA conversation TO "apps";
GRANT EXECUTE                            ON ALL FUNCTIONS IN SCHEMA conversation TO "apps";

-- pour tout ce qui sera créé ensuite, en remplaçant <role_migration>
-- par le rôle qui exécute réellement les DDL
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA conversation
  GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON TABLES TO "apps";
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA conversation
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "apps";
ALTER DEFAULT PRIVILEGES FOR ROLE "web-education" IN SCHEMA conversation
  GRANT EXECUTE ON FUNCTIONS TO "apps";