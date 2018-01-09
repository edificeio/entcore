CREATE EXTENSION IF NOT EXISTS unaccent;
DROP TEXT SEARCH CONFIGURATION IF EXISTS conversation.fr cascade;
CREATE TEXT SEARCH CONFIGURATION  conversation.fr ( COPY = french ) ;
ALTER TEXT SEARCH CONFIGURATION conversation.fr ALTER MAPPING
FOR hword, hword_part, word WITH unaccent, french_stem;

ALTER TABLE conversation.messages ADD language VARCHAR(2) NOT NULL DEFAULT('fr');

ALTER TABLE conversation.messages ADD COLUMN text_searchable tsvector;

UPDATE conversation.messages SET text_searchable =
setweight(to_tsvector(language::regconfig, coalesce(subject,'')), 'A') ||
setweight(to_tsvector(language::regconfig, coalesce(regexp_replace(body, '<[^>]*>',' ','g'),'')), 'B') ||
setweight(to_tsvector(language::regconfig, coalesce(regexp_replace("displayNames"::TEXT, '\"[a-z0-9-]{17,36}',' ','g'),'')), 'B');


CREATE INDEX idx_message_textsearch ON conversation.messages USING gin(text_searchable);

CREATE FUNCTION conversation.text_searchable_trigger() RETURNS trigger AS $$
begin
  new.text_searchable := setweight(to_tsvector(new.language::regconfig, coalesce(new.subject,'')), 'A') ||
  setweight(to_tsvector(new.language::regconfig, coalesce(regexp_replace(new.body, '<[^>]*>',' ','g'),'')), 'B') ||
  setweight(to_tsvector(new.language::regconfig, coalesce(regexp_replace(new."displayNames"::TEXT, '\"[a-z0-9-]{17,36}',' ','g'),'')), 'B');
  return new;
end
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvector_update_trigger BEFORE INSERT OR UPDATE
ON conversation.messages FOR EACH ROW EXECUTE PROCEDURE conversation.text_searchable_trigger();