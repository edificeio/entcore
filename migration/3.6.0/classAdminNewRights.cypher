MATCH (role:Role { name: 'Directory - test paramClass'}), (dest:Action {name: 'org.entcore.directory.controllers.DirectoryController|allowClassAdminUnlinkUsers' })
MERGE (role)-[:AUTHORIZE]->(dest)
RETURN role,dest;