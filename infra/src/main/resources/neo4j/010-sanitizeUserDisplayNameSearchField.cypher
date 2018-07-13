match (u:User) where u.displayNameSearchField contains ' ' set u.displayNameSearchField = replace(u.displayNameSearchField, ' ','');
