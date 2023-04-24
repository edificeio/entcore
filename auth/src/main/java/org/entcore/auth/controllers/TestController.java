package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import org.entcore.auth.users.NewDeviceWarningTask;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;
import java.util.Map;

public class TestController extends BaseController {
    private NewDeviceWarningTask NDWTask;

    public TestController(NewDeviceWarningTask NDWTask){
        this.NDWTask = NDWTask;
    }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
    }

    @Get("/test/newdevicemail")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
    public void newDevice(final HttpServerRequest request) {
        NDWTask.sendFakeEmail(request, emailOrError -> {
            request.response().putHeader("content-type", "text/html; charset=utf-8");
            request.response().setStatusCode(200);
            request.response().end(emailOrError);
        });
    }

}
