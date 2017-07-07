package org.entcore.directory.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.http.HttpServerRequest;

public class CalendarController extends BaseController {

    @Get("/calendar")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    public void timetable(HttpServerRequest request) {
        renderView(request);
    }
}
