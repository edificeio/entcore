package edu.one.core.portal.service;

import edu.one.core.infra.Controller;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.portal.mustache.AssetResourceTemplateFunction;
import edu.one.core.security.ActionType;
import edu.one.core.security.SecuredAction;

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class PortalService extends Controller {

	public PortalService(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		try {
			putTemplateFunction("asset", new AssetResourceTemplateFunction(container.config().getString("skin")));
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}
	}

	@SecuredAction(value = "portal.auth",type = ActionType.RESOURCE)
	public void portal(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					JsonObject jo = new JsonObject()
						.putString("userFirstname", user.getFirstName())
						.putString("userClass", user.getClassId());
					JsonObject urls = container.config().getObject("urls");
					renderView(request, jo.mergeIn(urls), "portal.html", null);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	public void themeDocumentation(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction(value = "portal.auth",type = ActionType.RESOURCE)
	public void apps(final HttpServerRequest request) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				JsonObject json = new JsonObject()
				.putArray("apps", session.getArray("apps", new JsonArray()));
				renderView(request, json);
			}
		});

	}

	public void assets(HttpServerRequest request) {
		request.response().sendFile("." + request.path());
	}
}
