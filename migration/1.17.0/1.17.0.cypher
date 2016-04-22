begin transaction
match (w:Widget)-[r]-n where w.name='calendar' delete r,w;
match (a:Application) where a.name='Timeline' set a.display=false;
commit

begin transaction
match (u:User)-[:PREFERS]->(uac: UserAppConf) WHERE u.activationCode IS NULL AND uac.timeline is not null AND NOT(uac.timeline =~ "^\\{.*") REMOVE uac.timeline;
commit
