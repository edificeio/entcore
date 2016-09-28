begin transaction
match (u:User) where has(u.relative) and LENGTH(u.relative) > 2 set u.oldrelative = u.relative;
match (u:User) where has(u.relative) and LENGTH(u.relative) > 2 set u.relative = null;
match (u:User) where head(u.profiles) = 'Student' and u.source = 'AAF' set u.checksum = '0';
commit

