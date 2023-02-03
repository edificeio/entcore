package org.entcore.admin.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

public class ConfigController extends BaseController {

    @Get("api/config/zimbra")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void readConfigZimbra(HttpServerRequest request) {
        JsonObject services = config.getJsonObject("services", new JsonObject());
        final JsonObject displayZimbra = new JsonObject()
                .put("displayZimbra", services.getBoolean("zimbra", false));
        renderJson(request, displayZimbra);
    }

    @Get("api/config/timetable/import")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void readConfigEdt(HttpServerRequest request) {
        JsonObject services = config.getJsonObject("services", new JsonObject());
        final JsonObject displayEdt = new JsonObject()
                .put("displayEdt", services.getBoolean("edt", false));
        renderJson(request, displayEdt);
    }

    @Get("api/config/subjects")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void readConfigSubjects(HttpServerRequest request) {
        JsonObject services = config.getJsonObject("services", new JsonObject());
        final JsonObject displaySubjects = new JsonObject()
                .put("displaySubjects", services.getBoolean("subjects", false));
        renderJson(request, displaySubjects);
    }

    @Get("api/config/slotprofiles")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void readConfigCalendar(HttpServerRequest request) {
        JsonObject services = config.getJsonObject("services", new JsonObject());
        final JsonObject displayCalendar = new JsonObject()
                .put("displayCalendar", services.getBoolean("calendar", false));
        renderJson(request, displayCalendar);
    }
}
