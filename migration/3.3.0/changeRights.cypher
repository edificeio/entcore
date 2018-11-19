MATCH (a:Action) 
WHERE a.name CONTAINS 'org.entcore.workspace.service.WorkspaceService' 
WITH COUNT(a.name) as oldNames
MATCH (a2:Action)
WHERE a2.name CONTAINS 'org.entcore.workspace.controllers.WorkspaceController' 
WITH COUNT(a2.name) as newNames,oldNames
MATCH (a3:Action) 
WHERE a3.name CONTAINS 'org.entcore.workspace.controllers.WorkspaceController' AND newNames > 0 AND oldNames > 0
DETACH DELETE a3
WITH COUNT(a3.name) as deleted
MATCH (a4:Action) WHERE a4.name CONTAINS 'org.entcore.workspace.service.WorkspaceService' 
SET a4.name = REPLACE(a4.name,'org.entcore.workspace.service.WorkspaceService' ,"org.entcore.workspace.controllers.WorkspaceController")
SET a4.migrated = true
RETURN a4.displayName, a4.name, a4.type