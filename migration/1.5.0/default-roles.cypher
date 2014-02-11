begin transaction

CREATE (c:Application {
  id: '961541b8-603a-4234-bf26-e22d503de8cc',
  name: 'directory_default',
  displayName: 'directory.user',
  grantType: '',
  address: '/userbook/annuaire#/search',
  icon: 'userbook-large',
  target: '',
  scope: ''
}),
c-[r:PROVIDE]->(a:Action:WorkflowAction {
  type: 'SECURED_ACTION_WORKFLOW',
  name:'directory_default|address',
  displayName:'directory_default.address'
});

CREATE (c:Application {
  id: '5efcd525-d1bf-4182-ac58-fd2d504c3ede',
  name: 'my_account_default',
  displayName: 'my.account',
  grantType: '',
  address: '/userbook/mon-compte',
  icon: 'account-large',
  target: '',
  scope: ''
}),
c-[r:PROVIDE]->(a:Action:WorkflowAction {
  type: 'SECURED_ACTION_WORKFLOW',
  name:'my_account_default|address',
  displayName:'my_account_default.address'
});

MATCH (n:Role)
WHERE n.name = 'directory-all-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'c8795607-e447-42b2-9128-78e0391cb759', name:'directory-all-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN ['directory_default|address']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'myaccount-all-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'aa12ac93-2180-491f-b5ec-08f062a9b795', name:'myaccount-all-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN ['my_account_default|address']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'conversation-all-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'327a0130-c74c-4041-a585-4c2acd150580', name:'conversation-all-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN ['org.entcore.conversation.controllers.ConversationController|createDraft',
'org.entcore.conversation.controllers.ConversationController|send',
'org.entcore.conversation.controllers.ConversationController|view']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'wokspace-all-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'cc997238-b84b-483e-ad70-06ca3812ab9b', name:'workspace-all-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN ['org.entcore.workspace.service.WorkspaceService|addDocument',
'org.entcore.workspace.service.WorkspaceService|addFolder',
'org.entcore.workspace.service.WorkspaceService|addRackDocument',
'org.entcore.workspace.service.WorkspaceService|copyRackDocument',
'org.entcore.workspace.service.WorkspaceService|copyRackDocuments',
'org.entcore.workspace.service.WorkspaceService|deleteRackDocument',
'org.entcore.workspace.service.WorkspaceService|getRackDocument',
'org.entcore.workspace.service.WorkspaceService|listDocuments',
'org.entcore.workspace.service.WorkspaceService|listDocumentsByFolder',
'org.entcore.workspace.service.WorkspaceService|listFolders',
'org.entcore.workspace.service.WorkspaceService|listRackDocuments',
'org.entcore.workspace.service.WorkspaceService|listRackTrashDocuments',
'org.entcore.workspace.service.WorkspaceService|moveTrashRack',
'org.entcore.workspace.service.WorkspaceService|rackAvailableUsers',
'org.entcore.workspace.service.WorkspaceService|removeShare',
'org.entcore.workspace.service.WorkspaceService|restoreTrashRack',
'org.entcore.workspace.service.WorkspaceService|shareJson',
'org.entcore.workspace.service.WorkspaceService|shareJsonSubmit',
'org.entcore.workspace.service.WorkspaceService|view']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'blog-student-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'f713da00-a120-4e0e-972a-cd2d2ba35c20', name:'blog-student-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN [
'org.entcore.blog.controllers.BlogController|blog',
'org.entcore.blog.controllers.BlogController|list']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'blog-relative-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'b7f5e7c7-875a-492b-bc40-165d5adff078', name:'blog-relative-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN [
'org.entcore.blog.controllers.BlogController|blog',
'org.entcore.blog.controllers.BlogController|list']
CREATE UNIQUE m-[:AUTHORIZE]->n;

MATCH (n:Role)
WHERE n.name = 'blog-teacher-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'b39565b0-2b80-4c14-bdb7-328d55db11ad', name:'blog-teacher-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN [
'org.entcore.blog.controllers.BlogController|blog',
'org.entcore.blog.controllers.BlogController|create',
'org.entcore.blog.controllers.BlogController|list']
CREATE UNIQUE m-[:AUTHORIZE]->n;

commit
