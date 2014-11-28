begin transaction

// ** Rack rights migration ** //

//Create new roles for the rack app.
MATCH (app:Application {address: "/rack"})-[p:PROVIDE]-(act:Action {type: "SECURED_ACTION_WORKFLOW"})
MERGE act<-[:AUTHORIZE]-(r:Role {name: "Casier ["+act.name+"]"})
SET r.id = id(r)+'-'+timestamp();

//Action matching table :
// org.entcore.workspace.service.WorkspaceService|rackAvailableUsers -> fr.wseduc.rack.controllers.RackController|listUsers
// org.entcore.workspace.service.WorkspaceService|addRackDocument -> fr.wseduc.rack.controllers.RackController|postRack
// org.entcore.workspace.service.WorkspaceService|listRackDocuments -> fr.wseduc.rack.controllers.RackController|listRack
// org.entcore.workspace.service.WorkspaceService|copyRackDocuments -> fr.wseduc.rack.controllers.RackController|rackToWorkspace
// org.entcore.workspace.service.WorkspaceService|copyRackDocument -> fr.wseduc.rack.controllers.RackController|rackToWorkspace
// org.*[rR]ack.* -> fr.wseduc.rack.controllers.RackController|view

//Get existing rack roles in the workspace app + groups associated and match those groups with the new roles from the rack app.
MATCH (g:Group)-[:AUTHORIZED]->(workspaceRole:Role)-[:AUTHORIZE]->(a:Action)
WHERE a.name =~ "org.*[rR]ack.*" AND a.type = "SECURED_ACTION_WORKFLOW"
WITH CASE
    WHEN a.name  = "org.entcore.workspace.service.WorkspaceService|rackAvailableUsers"
        THEN "fr.wseduc.rack.controllers.RackController|listUsers"
    WHEN a.name  = "org.entcore.workspace.service.WorkspaceService|addRackDocument"
        THEN "fr.wseduc.rack.controllers.RackController|postRack"
    WHEN a.name  = "org.entcore.workspace.service.WorkspaceService|listRackDocuments"
        THEN "fr.wseduc.rack.controllers.RackController|listRack"
    WHEN a.name  = "org.entcore.workspace.service.WorkspaceService|copyRackDocument"
        THEN "fr.wseduc.rack.controllers.RackController|rackToWorkspace"
    WHEN a.name  = "org.entcore.workspace.service.WorkspaceService|copyRackDocuments"
        THEN "fr.wseduc.rack.controllers.RackController|rackToWorkspace"
    ELSE
        "fr.wseduc.rack.controllers.RackController|view"
END AS targetAction, g AS authorizedGroups
MATCH (rackRole:Role {name: "Casier ["+targetAction+"]"})
MERGE authorizedGroups-[:AUTHORIZED]->rackRole;

//Associate the view role.
MATCH (g:Group)-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(a:Action) WHERE a.name =~ "org.*[rR]ack.*" and a.type = "SECURED_ACTION_WORKFLOW"
MERGE g-[:AUTHORIZED]->(:Role {name: "Casier [fr.wseduc.rack.controllers.RackController|view]"});

//Delete useless workspace rack actions.
MATCH (app:Application {address: "/rack"})
WITH count(*) AS exists
WHERE exists = 1
MATCH ()-[relations]-(a:Action)
WHERE a.name =~ "org.*[rR]ack.*" AND a.type = "SECURED_ACTION_WORKFLOW"
DELETE relations, a;

commit
