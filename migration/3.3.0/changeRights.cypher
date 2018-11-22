begin transaction

MATCH (a:Action) WHERE a.name CONTAINS 'org.entcore.workspace.service.WorkspaceService' 
SET a.name = REPLACE(a.name,'org.entcore.workspace.service.WorkspaceService' ,"org.entcore.workspace.controllers.WorkspaceController");

MERGE (a:Action {displayName:"workspace.manager",name:"org.entcore.workspace.controllers.WorkspaceController|bulkDelete",type:"SECURED_ACTION_RESOURCE"})
WITH a
MATCH (b:Action{name:"org.entcore.workspace.controllers.WorkspaceController|deleteComment"})<-[:AUTHORIZE]-(ro:Role)
WITH a,ro
MERGE (a)<-[:AUTHORIZE]-(ro);

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|copyDocument"})
SET a.displayName="workspace.read";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|copyFolder"})
SET a.displayName="workspace.read" , a.type="SECURED_ACTION_RESOURCE";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|deleteFolder"})
SET a.displayName="workspace.manager" , a.type="SECURED_ACTION_RESOURCE";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|moveFolder"})
SET a.displayName="workspace.manager" , a.type="SECURED_ACTION_RESOURCE";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|moveTrashFolder"})
SET a.displayName="workspace.manager" , a.type="SECURED_ACTION_RESOURCE";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|renameDocument"})
SET a.displayName="workspace.manager";

MATCH (a:Action {name:"org.entcore.workspace.controllers.WorkspaceController|renameFolder"})
SET a.displayName="workspace.manager";

commit