DROP TEXT SEARCH CONFIGURATION IF EXISTS fr cascade;
-- specific configuration to language natively create vectors without accents (one configuration per supported language)
CREATE TEXT SEARCH CONFIGURATION  fr ( COPY = french ) ;
ALTER TEXT SEARCH CONFIGURATION fr ALTER MAPPING
FOR hword, hword_part, word WITH unaccent, french_stem;