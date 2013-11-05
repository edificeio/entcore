package edu.one.core.portal.service;

import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.Controller;
import edu.one.core.infra.http.StaticResource;
import edu.one.core.common.user.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.portal.mustache.AssetResourceTemplateFunction;
import edu.one.core.portal.utils.ThemeUtils;
import edu.one.core.security.ActionType;
import edu.one.core.security.SecuredAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class PortalService extends Controller {

	private final ConcurrentMap<String, String> staticRessources;
	private final boolean dev;
	private final Neo neo;
	private List<String> themes;
	private final String themesPrefix;

	public PortalService(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		try {
			putTemplateFunction("asset", new AssetResourceTemplateFunction(
					container.config().getString("skin"),
					container.config().getBoolean("ssl", false)));
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}
		this.staticRessources = vertx.sharedData().getMap("staticRessources");
		dev = "dev".equals(container.config().getString("mode"));
		this.neo = new Neo(eb, log);
		// TODO configurable external assets with multiple skins
		themesPrefix = "/assets/themes/" + container.config().getString("skin");
		ThemeUtils.availableThemes(vertx, "." + themesPrefix, false, new Handler<List<String>>() {
			@Override
			public void handle(List<String> event) {
				themes = event;
			}
		});
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

	public void assets(final HttpServerRequest request) {
		if (dev) {
			request.response().sendFile("." + request.path());
		} else {
			if (staticRessources.containsKey(request.uri())) {
				StaticResource.serveRessource(request,
						"." + request.path(),
						staticRessources.get(request.uri()));
			} else {
				vertx.fileSystem().props("." + request.path(),
						new Handler<AsyncResult<FileProps>>(){
					@Override
					public void handle(AsyncResult<FileProps> af) {
						if (af.succeeded()) {
							String lastModified = StaticResource.formatDate(af.result().lastModifiedTime());
							staticRessources.put(request.uri(), lastModified);
							StaticResource.serveRessource(request,
									"." + request.path(),
									lastModified);
						} else {
							request.response().sendFile("." + request.path());
						}
					}
				});
			}
		}
	}

	@SecuredAction(value = "portal", type = ActionType.AUTHENTICATED)
	public void getTheme(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					JsonObject urls = container.config().getObject("urls", new JsonObject());
					final JsonObject theme = new JsonObject()
							.putString("template", "/public/template/portal.html") // TODO configurable ?
							.putString("logoutCallback", urls.getString("logoutCallback", ""));
					String query =
							"START n=node:node_auto_index(id={id}) " +
							"MATCH n-[:USERBOOK]->u " +
							"RETURN u.theme? as theme";
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					neo.send(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								String userTheme = event.body().getObject("result", new JsonObject())
										.getObject("0", new JsonObject()).getString("theme");
								if (userTheme != null && themes.contains(userTheme)) {
									theme.putString("skin", themesPrefix + "/" + userTheme + "/");
								} else {
									theme.putString("skin", themesPrefix + "/default/");
								}
							} else {
								theme.putString("skin", themesPrefix + "/default/");
							}
							renderJson(request, theme);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

}
