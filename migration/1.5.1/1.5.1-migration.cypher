begin transaction
match (a:Application) where a.scope = '' set a.scope = [''];
commit

