match (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(pg:ProfileGroup) where not(has(pg.structureName)) OR pg.structureName <> s.name set pg.structureName = s.name;
match (s:Structure)<-[:DEPENDS]-(pg:FunctionalGroup) where not(has(pg.structureName)) OR pg.structureName <> s.name set pg.structureName = s.name;
