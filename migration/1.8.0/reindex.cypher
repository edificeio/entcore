begin transaction
match (u:User) set u.externalId = u.externalId;
match (u:Structure) set u.externalId = u.externalId;
match (u:Profile) set u.externalId = u.externalId;
commit
