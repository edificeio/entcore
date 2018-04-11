MATCH (u:User) WHERE head(u.profiles) IN ['Student', 'Relative', 'Guest'] SET u.displayName = u.lastName + ' ' + u. firstName;
