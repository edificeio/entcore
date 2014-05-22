begin transaction
MATCH (pg:ProfileGroup) WHERE pg.name =~ '.*_ELEVE' SET pg.name = replace(pg.name, '_ELEVE', '-Student');
MATCH (pg:ProfileGroup) WHERE pg.name =~ '.*_ENSEIGNANT' SET pg.name = replace(pg.name, '_ENSEIGNANT', '-Teacher');
MATCH (pg:ProfileGroup) WHERE pg.name =~ '.*_PERSRELELEVE' SET pg.name = replace(pg.name, '_PERSRELELEVE', '-Relative');
commit
