package org.entcore.admin.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

public class ZimbraController extends BaseController {

    @Get("api/zimbra/config")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    public void readConfig(HttpServerRequest request) {
        final JsonObject displayZimbra = new JsonObject()
                .put("displayZimbra", config.getBoolean("displayZimbra", false));
        renderJson(request, displayZimbra);
    }

}
