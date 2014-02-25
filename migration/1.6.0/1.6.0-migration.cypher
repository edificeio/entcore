begin transaction
match (a:Action) where a.name='org.entcore.admin.controllers.AdminController|admin'
set a.name='org.entcore.portal.service.PortalService|admin';
commit

