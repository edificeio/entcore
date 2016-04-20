begin transaction
match (w:Widget)-[r]-n where w.name='calendar' delete r,w;
match (a:Application) where a.name='Timeline' set a.display=false;
commit
