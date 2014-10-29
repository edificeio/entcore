begin transaction
MATCH (g:ProfileGroup) where NOT(g:Group) SET g:Group;
commit
