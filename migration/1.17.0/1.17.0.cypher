begin transaction
match (w:Widget)-[r]-n where w.name='calendar' delete r,w;
match (a:Application) where a.name='Timeline' set a.display=false;
match (a:Application) where a.name='Directory' set a.display=false;
match (u:User)-[:PREFERS]->(uac: UserAppConf) WHERE u.activationCode IS NULL AND uac.timeline is not null AND NOT(uac.timeline =~ "^\\{.*") REMOVE uac.timeline;
commit
