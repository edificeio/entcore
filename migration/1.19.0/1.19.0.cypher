match (u:User) where has(u.login) and not(has(u.source)) set u.source = 'CLASS_PARAM';
