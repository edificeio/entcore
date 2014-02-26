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
commit

