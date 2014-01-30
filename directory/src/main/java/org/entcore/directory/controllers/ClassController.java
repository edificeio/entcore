package org.entcore.directory.controllers;

import edu.one.core.infra.Controller;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.impl.DefaultClassService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

import static edu.one.core.infra.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ClassController extends Controller {

	private final ClassService classService;

	public ClassController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		this.classService = new DefaultClassService(new Neo(eb,log));
	}

	@SecuredAction(value = "class.update", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String classId = request.params().get("classId");
				classService.update(classId, body, defaultResponseHandler(request));
			}
		});
	}

}
