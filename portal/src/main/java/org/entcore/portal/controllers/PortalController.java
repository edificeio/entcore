/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.portal.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.StaticResource;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.portal.Portal;
import org.entcore.portal.utils.ThemeUtils;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.user.SessionAttributes.*;

public class PortalController extends BaseController {

	private LocalMap<String, String> staticRessources;
	private Map<String, String> fixResources = new HashMap<>();
	private boolean dev;
	private Map<String, List<String>> themes;
	private Map<String, JsonArray> themesDetails;
	private Map<String, String> hostSkin;
	private String assetsPath;
	private EventStore eventStore;
	private enum PortalEvent { ACCESS_ADAPTER, ACCESS }
	private String defaultSkin;
	private JsonObject defaultTracker;

	@Override
	public void init(final Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		this.staticRessources = vertx.sharedData().getLocalMap("staticRessources");
		dev = "dev".equals(config.getString("mode"));
		assetsPath = config.getString("assets-path", ".");
		JsonObject skins = new JsonObject(vertx.sharedData().<String, Object>getLocalMap("skins"));
		defaultSkin = config.getString("skin", "raw");
		themes = new HashMap<>();
		themesDetails = new HashMap<>();
		this.hostSkin = new HashMap<>();
		for (final String domain: skins.fieldNames()) {
			final String skin = skins.getString(domain);
			this.hostSkin.put(domain, skin);
			ThemeUtils.availableThemes(vertx, assetsPath + "/assets/themes/" + skin + "/skins", false, new Handler<List<String>>() {
				@Override
				public void handle(List<String> event) {
					themes.put(skin, event);
					JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
					for (final String s : event) {
						String path = assetsPath + "/assets/themes/" + skin + "/skins/" + s + "/";
						final JsonObject j = new JsonObject()
								.put("_id", s)
								.put("path", path.substring(assetsPath.length()));
						if ("default".equals(s)) {
							vertx.fileSystem().readFile(path + "/details.json", new Handler<AsyncResult<Buffer>>() {
								@Override
								public void handle(AsyncResult<Buffer> event) {
									if (event.succeeded()) {
										JsonObject d = new JsonObject(event.result().toString());
										j.put("displayName", d.getString("displayName"));
									} else {
										j.put("displayName", s);
									}
								}
							});
						} else {
							j.put("displayName", s);
						}
						a.add(j);
					}
					themesDetails.put(skin, a);
				}
			});
		}
		defaultTracker = config.getJsonObject( "tracker", new JsonObject().put("type", "none") );
		eventStore = EventStoreFactory.getFactory().getEventStore(Portal.class.getSimpleName());
		vertx.sharedData().getLocalMap("server").put("assetPath", assetsPath);
	}

	@Get("/welcome")
	@SecuredAction(value = "portal.auth",type = ActionType.AUTHENTICATED)
	public void welcome(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/")
	@SecuredAction(value = "portal.auth",type = ActionType.AUTHENTICATED)
	public void portal(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					redirectPermanent(request, config.getString("root-page", "/welcome"));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/applications-list")
	@SecuredAction(value = "portal.auth",type = ActionType.AUTHENTICATED)
	public void applicationsList(final HttpServerRequest request) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				JsonArray myAppsForceAsApplication = config.getJsonArray("my-apps-force-as-application", new JsonArray());
				JsonArray myAppsForceAsConnector = config.getJsonArray("my-apps-force-as-connector", new JsonArray());
				JsonArray apps = session.getJsonArray("apps", new fr.wseduc.webutils.collections.JsonArray());
				for (Object o : apps) {
					if (!(o instanceof JsonObject)) continue;
					JsonObject j = (JsonObject) o;
					String d = j.getString("displayName");
					if (d == null || d.trim().isEmpty()) {
						d = j.getString("name");
					}
					if (d != null) {
						j.put("displayName", d);
					}
					myAppsForceExternal(j, myAppsForceAsApplication, false);
					myAppsForceExternal(j, myAppsForceAsConnector, true);
				}
				JsonObject json = new JsonObject()
						.put("apps", apps);
				renderJson(request, json);
			}
		});
	}

	private void myAppsForceExternal(JsonObject app, JsonArray rules, boolean forceIsExternalTo) {
		for (int i = 0; i < rules.size(); i++) {
			JsonObject jo = rules.getJsonObject(i);
			String field = jo.getString("field");
			Boolean fullMatch = jo.getBoolean("fullMatch", false);
			Boolean caseSensitive = jo.getBoolean("caseSensitive", true);
			String pattern = jo.getString("pattern");
			if (field == null || pattern == null) continue;
			if (fullMatch.booleanValue()) {
				pattern = "^" + pattern + "$";
			}
			Pattern p;
			if (caseSensitive.booleanValue()) {
				p = Pattern.compile(pattern);
			} else {
				p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			}
			String fieldValue = app.getString(field);
			if (fieldValue == null) continue;
			Matcher m = p.matcher(fieldValue);
			if (m.find()) {
				app.put("isExternal", forceIsExternalTo);
				return;
			}
		}
	}

	@Get("/adapter")
	@SecuredAction(value = "portal.auth",type = ActionType.AUTHENTICATED)
	public void adapter(final HttpServerRequest request) {
		String eliotPrefix = request.params().get("eliot");
		eliotPrefix = eliotPrefix == null ? "" : eliotPrefix;

		renderView(request, new JsonObject().put("eliotPrefix", eliotPrefix));
		eventStore.createAndStoreEvent(PortalEvent.ACCESS_ADAPTER.name(),
				request, new JsonObject().put("adapter", request.uri()));
	}

	@Get(value = "/assets/.+", regex = true)
	public void assets(final HttpServerRequest request) {
		String path = assetsPath + request.path();
		if (dev) {
			request.response().sendFile(path, ar -> {
				if (ar.failed() && !request.response().ended()) {
					notFound(request);
				}
			});
		} else {
			sendWithLastModified(request, path, true);
		}
	}

	@Get(value = "/current/assets/.+", regex = true)
	public void currentAssets(final HttpServerRequest request) {
		final String path = assetsPath + getThemePrefix(request) + request.path().substring(15);
		if (dev) {
			request.response().sendFile(path, ar -> {
				if (ar.failed() && !request.response().ended()) {
					notFound(request);
				}
			});
		} else {
			sendWithLastModified(request, path, true);
		}
	}

	private String getSkinFromConditions(HttpServerRequest request) {
		if (request == null) {
			return defaultSkin;
		}
		final String overrideTheme = CookieHelper.get("theme", request);
		if (isNotEmpty(overrideTheme)) {
			return overrideTheme;
		}
		final String theme = I18n.getTheme(request);
		if (isNotEmpty(theme)) {
			return theme;
		}
		String skin = hostSkin.get(getHost(request));
		return (skin != null && !skin.trim().isEmpty()) ? skin : defaultSkin;
	}

	private String getThemePrefix(HttpServerRequest request) {
		return "/assets/themes/" + getSkinFromConditions(request);
	}

	private void sendWithLastModified(final HttpServerRequest request, final String path, final boolean decodeIfNeeded) {
		if (staticRessources.containsKey(request.uri())) {
			final String safePath = fixResources.getOrDefault(request.uri(), path);
			final String modifiedDate = staticRessources.get(request.uri());
			StaticResource.serveRessource(request,safePath,modifiedDate, dev);
		} else {
			vertx.fileSystem().props(path, af -> {
				if (af.succeeded()) {
					final String lastModified = StaticResource.formatDate(af.result().lastModifiedTime());
					staticRessources.put(request.uri(), lastModified);
					StaticResource.serveRessource(request,path,lastModified, dev);
				} else {
					if(decodeIfNeeded && af.cause() instanceof FileSystemException && af.cause().getCause() != null && af.cause().getCause() instanceof NoSuchFileException){
						try {
							final String decoded = URLDecoder.decode(path, "UTF-8");
							fixResources.put(request.uri(), decoded);
							sendWithLastModified(request, decoded, false);
							return;
						} catch (UnsupportedEncodingException e) {}
					}
					request.response().sendFile(path, ar -> {
						if (ar.failed() && !request.response().ended()) {
							notFound(request);
						}
					});
				}
			});
		}
	}

	@Get("/theme")
	@SecuredAction(value = "portal", type = ActionType.AUTHENTICATED)
	public void getTheme(final HttpServerRequest request) {
		final String theme_attr = THEME_ATTRIBUTE + getHost(request);
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					Object t = user.getAttribute(theme_attr);
					if (t != null) {
						renderJson(request, new JsonObject(t.toString()));
						return;
					}
					JsonObject urls = config.getJsonObject("urls", new JsonObject());
					final JsonObject theme = new JsonObject()
							.put("template", "/public/template/portal.html")
							.put("logoutCallback", getLogoutCallback(request, urls));
					String query =
							"MATCH (n:User)-[:USERBOOK]->u " +
							"WHERE n.id = {id} " +
							"RETURN u.theme" + getSkinFromConditions(request).replaceAll("\\W+", "") + " as theme";
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								JsonArray result = event.body().getJsonArray("result");
								String userTheme = (result != null && result.size() == 1) ?
										result.getJsonObject(0).getString("theme") : null;
								List<String> t = themes.get(getSkinFromConditions(request));
								if (userTheme != null && t != null && t.contains(userTheme)) {
									theme.put("skin", getThemePrefix(request) + "/skins/" + userTheme + "/");
									theme.put("themeName", getSkinFromConditions(request));
									theme.put("skinName", userTheme);
								} else {
									theme.put("skin", getThemePrefix(request) + "/skins/default/");
									theme.put("themeName", getSkinFromConditions(request));
									theme.put("skinName", "default");
								}
							} else {
								theme.put("skin", getThemePrefix(request) + "/skins/default/");
								theme.put("themeName", getSkinFromConditions(request));
								theme.put("skinName", "default");
							}
							renderJson(request, theme);
							UserUtils.addSessionAttribute(eb, user.getUserId(), theme_attr, theme.encode(), null);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private String getLogoutCallback(HttpServerRequest request, JsonObject urls) {
		final String logoutCallback = CookieHelper.get("logoutCallback", request);
		return isNotEmpty(logoutCallback) ? logoutCallback : urls.getString("logoutCallback", "");
	}

	@Get("/skin")
	public void getSkin(final HttpServerRequest request) {
		renderJson(request, new JsonObject().put("skin", getSkinFromConditions(request)));
	}

	@Get("/skins")
	public void getSkins(final  HttpServerRequest request) {
		renderJson(request, new JsonObject().put("skins", new fr.wseduc.webutils.collections.JsonArray()), 200);
	}

	@Put("skin")
	public void putSkin(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject jo) {
				CookieHelper.set("customSkin", jo.getString("skin"), request);
				renderJson(request, new JsonObject(), 200);
			}
		});
	}

	@Get("/locale")
	public void locale(HttpServerRequest request) {
		String lang = I18n.acceptLanguage(request);
		if (lang == null) {
			lang = "fr";
		}
		String[] langs = lang.split(",");
		renderJson(request, new JsonObject().put("locale",
				Locale.forLanguageTag(langs[0].split("-")[0]).toString()));
	}

	@Get("/admin-urls")
	@SecuredAction(value = "config", type = ActionType.AUTHENTICATED)
	public void adminURLS(HttpServerRequest request){
		renderJson(request, config.getJsonArray("admin-urls", new fr.wseduc.webutils.collections.JsonArray()));
	}

	@Get("/resources-applications")
	public void resourcesApplications(HttpServerRequest request){
		renderJson(request, config.getJsonArray("resources-applications", new fr.wseduc.webutils.collections.JsonArray()));
	}

	@Get("/quickstart")
	@SecuredAction("portal.quickstart")
	public void quickstart(HttpServerRequest request){
		renderJson(request, new fr.wseduc.webutils.collections.JsonArray());
	}

	@Get("/themes")
	@SecuredAction(value = "config", type = ActionType.AUTHENTICATED)
	public void themes(HttpServerRequest request){
		JsonArray themes = themesDetails.get(getSkinFromConditions(request));
		if (themes == null) {
			themes = new fr.wseduc.webutils.collections.JsonArray();
		}
		renderJson(request, themes);
	}

	@Get("/conf/smartBanner")
	@SecuredAction(value = "config", type = ActionType.AUTHENTICATED)
	public void getSmartBannerConf(final HttpServerRequest request){
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				JsonObject json = config.getJsonObject("smartBanner", new JsonObject());
				boolean canAccessApp = user.getHasApp() != null && user.getHasApp();
				boolean isExcluded = json.getJsonArray("excludeUserTypes", new JsonArray()).contains(user.getType());
				if (canAccessApp && !isExcluded) {
					renderJson(request, json);
				} else {
					forbidden(request);
				}
			} else {
				unauthorized(request);
			}
		});
	}

	/**
	 * Get the configured tracking system.
	 * @param request request
	 */
	@Get("/analyticsConf")
	public void tracker(HttpServerRequest request) {
		if( "matomo".equals(defaultTracker.getString("type")) ) {
			UserUtils.getUserInfos(eb, request, user -> {
				JsonObject personalizedTracker = defaultTracker.copy();
				if (user != null) {
					JsonObject matomoConfig = personalizedTracker.getJsonObject("matomo");
					matomoConfig.put("UserId", user.getUserId());
					matomoConfig.put("Profile", user.getType());
					if( user.getStructures().size() > 0 )
						matomoConfig.put("School", user.getStructures().get(0));
					matomoConfig.put("Project", request.host());
				}
				renderJson(request, personalizedTracker);
			});
		} else {
			// Default tracker
			renderJson(request, defaultTracker);
		}
	}

	@BusAddress("portal")
	public void export(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "getI18n" :
				String label = message.body().getString("label");
				String acceptLanguage = message.body().getString("acceptLanguage");

				JsonObject i18n = I18n.getInstance().load(acceptLanguage, I18n.DEFAULT_DOMAIN);

				if(label == null)
					message.reply(i18n);
				else
					message.reply(new JsonObject().put("label", i18n.getString(label)));
				break;

			case "getTheme" : 
				HttpServerRequest fakeRequest = new JsonHttpServerRequest( new JsonObject(), message.headers() );
				String theme = this.getSkinFromConditions(fakeRequest);
				message.reply( new JsonObject().put("theme", theme) );
				break;

			default: log.error("Archive : invalid action " + action);
		}
	}

	/**
	 * Get the configured scriptPath for cantoo script.
	 * @param request nothing to do
	 * @return { "scriptPath": "https://cantoo.com/script.js" }
	 * 
	 * @security optionalFeature.cantoo
	 * @workflow optionalFeatureCantoo
	 */
	@Get("optionalFeature/cantoo")
	@SecuredAction("optionalFeature.cantoo")
	public void optionalFeatureCantoo(HttpServerRequest request) {

		// get scriptPath from config
		String scriptPath = config.getString("optionalFeature-cantoo-scriptPath", "");

		if(!scriptPath.isEmpty()) {
			
			JsonObject result = new JsonObject();
			result.put("scriptPath", scriptPath);

			request.response().putHeader("content-type", "application/json");
			request.response().putHeader("Cache-Control", "no-cache, must-revalidate");
			request.response().putHeader("Expires", "-1");
			
			//return scriptPath of script
			request.response().end(result.encode());
			
		} else {
			unauthorized(request);
		}
		
	}

	//TODO IMPLIMENTATION OF THE ZIMBRA METHODE FOR REDIRECT TO THERE EMAIL SYSTEM USING THE writeTOEmailProvider AND Adding writeTOEmailProviderZimbra WORFLOW
	/**
	 * REDIRECTION TO WORDLINE Mail application
	 * 
	 * @param request id, login (for user), type of profile(user, group)
	 * @return url to redirect
	 * @workflow optionalFeatureWriteToEmailProviderWordline
	 */
	@Get("optionalFeature/writeToEmailProvider/wordline")
	@SecuredAction("optionalFeature.writeToEmailProviderWordline")
	public void optionalFeatureWriteToEmailProviderWordline(final HttpServerRequest request) {
		optionalFeatureWriteToEmailProvider(request, "wordline");
	}

	private void optionalFeatureWriteToEmailProvider(final HttpServerRequest request, String providerId) {
		
		final String id = request.params().get("id");
		final String login = request.params().get("login");
		final String type = request.params().get("type");

		if(type == null || type.isEmpty() || (id == null && login == null)) {
			badRequest(request);
			return;
		}
		
		JsonArray emailProvider = config.getJsonArray("emailProvider",  new JsonArray());

		if(emailProvider != null && !emailProvider.isEmpty()) {
			JsonObject provider = (JsonObject) emailProvider.stream().filter(o -> ((JsonObject) o).getString("id").equals(providerId)).findFirst().orElse(null);
			if(provider != null && !provider.getString("url", "").isEmpty()) {
				UserUtils.getUserInfos(eb, request, user -> {
					if (user != null) {

						JsonObject result = new JsonObject();
						// get url from config
						String url = provider.getString("url", "");

						result.put("url", url + "?id=" + id + "&login=" + login + "&type=" + type);

						request.response().putHeader("content-type", "application/json");
						request.response().putHeader("Cache-Control", "no-cache, must-revalidate");
						request.response().putHeader("Expires", "-1");
						//return url to redirect
						request.response().end(result.encode());
					} else {
						unauthorized(request);
					}
				});
			} else {
				badRequest(request);
			}
		} else {
			badRequest(request);
		}
	}
	
	@Get("zendeskGuide/config")
	@SecuredAction(value = "config", type = ActionType.AUTHENTICATED)
	public void zendeskGuideConfig(HttpServerRequest request) {

		final JsonObject zendeskConfig = config.getJsonObject("zendeskGuide", new JsonObject());
		String module = request.params().get("module");
		if(zendeskConfig.isEmpty() || !zendeskConfig.containsKey("key")) {
			renderJson(request, new JsonObject());
			return;
		}


		// If module is not provided, try to get it from the referer
		if(module == null && request.headers().get("Referer") != null) {
			final String urlPath = request.headers().get("Referer");
			final String[] pathSegments = urlPath.split("/");
			if(pathSegments.length > 3 && !pathSegments[3].isEmpty()) {
				module = pathSegments[3];
				if(module.contains("?")) {
					final String[] moduleSegments = module.split("\\?");
					module = moduleSegments[0];
				}
			} else {
				module = "portal";
			}
		}

		if(module != null && zendeskConfig.containsKey("modules") && zendeskConfig.getJsonObject("modules").containsKey(module)) {
			zendeskConfig.put("module", zendeskConfig.getJsonObject("modules").getJsonObject(module));
		} else {
			zendeskConfig.put("module", new JsonObject());
		}

		renderJson(request, zendeskConfig);
	}

	//TODO: remove this method when the old help is removed from all projects
	@Get("/oldHelp")
	@SecuredAction("portal.oldHelpEnable")
	public void oldHelpEnable(HttpServerRequest request) {
		renderJson(request, new JsonObject().put("enable", true));
	}

}
