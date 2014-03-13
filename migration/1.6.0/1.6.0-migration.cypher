begin transaction
match (a:Action) where a.name='org.entcore.admin.controllers.AdminController|admin'
	set a.name='org.entcore.portal.service.PortalService|admin';
match (a:Action) where a.name = 'org.entcore.portal.service.PortalService|getTheme'
	set a.type='SECURED_ACTION_AUTHENTICATED', a:AuthenticatedAction
	remove a:ResourceAction;
match (a:Action) where a.name = 'org.entcore.portal.service.PortalService|apps'
	set a.type='SECURED_ACTION_AUTHENTICATED', a:AuthenticatedAction
	remove a:ResourceAction;
match (a:Action) where a.name = 'org.entcore.portal.service.PortalService|adapter'
	set a.type='SECURED_ACTION_AUTHENTICATED', a:AuthenticatedAction
	remove a:ResourceAction;
match (a:Action) where a.name = 'org.entcore.portal.service.PortalService|portal'
	set a.type='SECURED_ACTION_AUTHENTICATED', a:AuthenticatedAction
	remove a:ResourceAction;

MATCH (u:SuperAdmin)
DELETE u;
CREATE (p:Profile { id: 'ff61b864-039c-484b-93a3-fea53d3d6441', externalId: 'PROFILE_STUDENT', checksum: '69609a7ae81a5cd64868eb10024366fd01b48e43', name:'Student'});
CREATE (p:Profile { id: '5c7990da-30eb-492c-b03c-ef159461cc6a', externalId: 'PROFILE_RELATIVE', checksum: '3544dccaa5ea0da52eb1f237ce2fd225e8cf55f6', name:'Relative'});
CREATE (p:Profile { id: '2b873777-a7df-48f3-bfb1-365c8b117b9f', externalId: 'PROFILE_PERSONNEL', checksum: 'e25408caf927e8e088086c69c8bb0a947d5e45e8', name:'Personnel'});
CREATE (p:Profile { id: 'c3494ee9-659a-4c62-964b-330cf70c6bb0', externalId: 'PROFILE_TEACHER', checksum: 'abd1e8459ca81a81099740ea74e847dbc52b928a', name:'Teacher'});
MATCH (c:Class)-[r:APPARTIENT]->(s:School)
CREATE UNIQUE c-[:BELONGS]->s
DELETE r;
MATCH (s:Student)-[r:EN_RELATION_AVEC]->(u:Relative)
CREATE UNIQUE s-[:RELATED]->u
DELETE r;
MATCH (c:Class)<-[r:APPARTIENT]-(u:User)
DELETE r;
MATCH (s:School)<-[r:APPARTIENT]-(u:User)
CREATE UNIQUE u-[:ADMINISTRATIVE_ATTACHMENT]->s
DELETE r;
MATCH (pg:ProfileGroup)<-[r:APPARTIENT]-(u:User)
CREATE UNIQUE u-[:IN]->pg
DELETE r;
MATCH (pg:SchoolPrincipalGroup), (p:Profile { name : 'Personnel' })
CREATE UNIQUE pg-[:HAS_PROFILE]->p;
MATCH (pg:SchoolTeacherGroup), (p:Profile { name : 'Teacher' })
CREATE UNIQUE pg-[:HAS_PROFILE]->p;
MATCH (pg:SchoolStudentGroup), (p:Profile { name : 'Student' })
CREATE UNIQUE pg-[:HAS_PROFILE]->p;
MATCH (pg:SchoolRelativeGroup), (p:Profile { name : 'Relative' })
CREATE UNIQUE pg-[:HAS_PROFILE]->p;
MATCH (s:School)
SET s:Structure
REMOVE s:School;
MATCH (pg:SchoolPrincipalGroup)
REMOVE pg:SchoolPrincipalGroup:SchoolProfileGroup;
MATCH (pg:SchoolTeacherGroup)
REMOVE pg:SchoolTeacherGroup:SchoolProfileGroup;
MATCH (pg:SchoolStudentGroup)
REMOVE pg:SchoolStudentGroup:SchoolProfileGroup;
MATCH (pg:SchoolRelativeGroup)
REMOVE pg:SchoolRelativeGroup:SchoolProfileGroup;
MATCH (pg:ClassRelativeGroup)
REMOVE pg:ClassRelativeGroup:ClassProfileGroup;
MATCH (pg:ClassTeacherGroup)
REMOVE pg:ClassTeacherGroup:ClassProfileGroup;
MATCH (pg:ClassStudentGroup)
REMOVE pg:ClassStudentGroup:ClassProfileGroup;
MATCH (s:Student)
REMOVE s:Student;
MATCH (r:Relative)
REMOVE r:Relative;
MATCH (t:Teacher)
REMOVE t:Teacher;
commit
