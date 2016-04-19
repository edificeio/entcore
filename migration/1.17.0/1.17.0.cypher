match (w:Widget)-[r]-n where w.name='calendar' delete r,w;
