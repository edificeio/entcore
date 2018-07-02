MATCH (u:User) WHERE head(u.profiles) IN ['Student', 'Relative', 'Guest'] SET u.displayName = u.lastName + ' ' + u. firstName;
MATCH (u:User) WHERE head(u.profiles) IN ['Teacher', 'Personnel'] AND u.displayName = u.firstName + ' ' + u.lastName SET u.displayName = u.lastName + ' ' + u. firstName;
