begin transaction
match (a:Application) where a.scope = '' set a.scope = [''];
MATCH n WHERE n:User OR n:School OR n:Class OR n:ProfileGroup REMOVE n.health, n.mood, n.picture, n.motto;
commit

