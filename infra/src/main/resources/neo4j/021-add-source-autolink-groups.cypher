match (g:ManualGroup)<-[r:IN]-(u:User) where has(g.autolinkUsersFromGroups) and has(r.updated) and not(has(r.source)) set r.source = 'AUTO';
