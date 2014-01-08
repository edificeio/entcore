begin transaction
MATCH v WHERE (v:User OR v:ProfileGroup) SET v:Visible;
MATCH (u:User)-[:USERBOOK]->ub remove ub.userPreferencesBirthdayClass;
commit
