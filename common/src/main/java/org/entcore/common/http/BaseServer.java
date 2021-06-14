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

package org.entcore.common.http;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.AccessLogger;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.AccessLoggerFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.validation.JsonSchemaValidator;

import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.cache.CacheFilter;
import org.entcore.common.cache.CacheService;
import org.entcore.common.cache.RedisCacheService;
import org.entcore.common.controller.ConfController;
import org.entcore.common.controller.RightsController;
import org.entcore.common.elasticsearch.ElasticSearch;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.*;
import org.entcore.common.http.response.SecurityHookRender;
import org.entcore.common.http.response.OverrideThemeHookRender;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.redis.Redis;
import org.entcore.common.search.SearchingEvents;
import org.entcore.common.search.SearchingHandler;
import org.entcore.common.sql.DB;
import org.entcore.common.sql.Sql;
import org.entcore.common.trace.TraceFilter;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.Zip;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.util.*;

public abstract class BaseServer extends Server {
	private static String moduleName;
	private ResourcesProvider resourceProvider = null;
	private RepositoryHandler repositoryHandler;
	private SearchingHandler searchingHandler;
	private String schema;
	private String contentSecurityPolicy;
	private AccessLogger accessLogger;

	public static String getModuleName() {
		return moduleName;
	}

	@Override
	public void start() throws Exception {
		moduleName = getClass().getSimpleName();
		if (resourceProvider == null) {
			setResourceProvider(new ResourceProviderFilter());
		}
		super.start();

		accessLogger = new EntAccessLogger(getEventBus(vertx));

		EventStoreFactory eventStoreFactory = EventStoreFactory.getFactory();
		eventStoreFactory.setVertx(vertx);

		initFilters();

		String node = (String) vertx.sharedData().getLocalMap("server").get("node");

		contentSecurityPolicy = (String) vertx.sharedData().getLocalMap("server").get("contentSecurityPolicy");

		repositoryHandler = new RepositoryHandler(getEventBus(vertx));
		searchingHandler = new SearchingHandler(getEventBus(vertx));

		Config.getInstance().setConfig(config);

		if (node != null) {
			initModulesHelpers(node);
		}

		if (config.getBoolean("csrf-token", false)) {
			addFilter(new CsrfFilter(getEventBus(vertx), securedUriBinding));
		}

		final LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		final Boolean cacheEnabled = (Boolean) server.getOrDefault("cache-filter", false);
		if(Boolean.TRUE.equals(cacheEnabled)){
			final CacheService cacheService = CacheService.create(vertx);
			addFilter(new CacheFilter(getEventBus(vertx),securedUriBinding, cacheService));
		}

		if (config.getString("integration-mode","BUS").equals("HTTP")) {
			addFilter(new HttpActionFilter(securedUriBinding, config, vertx, resourceProvider));
		} else {
			addFilter(new ActionFilter(securedUriBinding, vertx, resourceProvider));
		}
		vertx.eventBus().localConsumer("user.repository", repositoryHandler);
		vertx.eventBus().localConsumer("search.searching", this.searchingHandler);

		loadI18nAssetsFiles();

		addController(new RightsController());
		addController(new ConfController());
		SecurityHandler.setVertx(vertx);

		final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
		Renders.getAllowedHosts().addAll(skins.keySet());
	}

	protected void initFilters() {
		//prepare cache if needed
		final LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		final Optional<Object> oauthCache = Optional.ofNullable(server.get("oauthCache"));
		final Optional<JsonObject> oauthConfigJson = oauthCache.map(e-> new JsonObject((String)e));
		final Optional<Integer> oauthTtl = oauthConfigJson.map( e -> e.getInteger("ttlSeconds"));
		//
		clearFilters();
		addFilter(new AccessLoggerFilter(accessLogger));
		addFilter(new WebviewFilter(vertx, getEventBus(vertx)));
		addFilter(new UserAuthFilter(new AppOAuthResourceProvider(
				getEventBus(vertx), getPathPrefix(config), ()->{
					try{
						if(!oauthConfigJson.get().getBoolean("enabled", false)) return Optional.empty();
						return Optional.ofNullable(CacheService.create(vertx));
					}catch(Exception e){
						return Optional.empty();
					}
		}, oauthTtl), new BasicFilter()));

		addFilter(new TermsRevalidationFilter(getEventBus(vertx)));

		addFilter(new TraceFilter(getEventBus(vertx), securedUriBinding));
	}

	@Override
	protected Server addController(BaseController controller) {
		controller.setAccessLogger(accessLogger);
		super.addController(controller);
		if (config.getJsonObject("override-theme") != null) {
			controller.addHookRenderProcess(new OverrideThemeHookRender(getEventBus(vertx), config.getJsonObject("override-theme")));
		}
		controller.addHookRenderProcess(new SecurityHookRender(getEventBus(vertx),
				true, contentSecurityPolicy));
		return this;
	}

	@Override
	protected void i18nMessages(HttpServerRequest request) {
		String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		if (sessionId != null && !sessionId.trim().isEmpty()) {
			final HttpServerRequest r = new SecureHttpServerRequest(request);
			UserUtils.getSession(vertx.eventBus(), r, new Handler<JsonObject>() {

				@Override
				public void handle(JsonObject session) {
					BaseServer.super.i18nMessages(r);
				}
			});
		} else {
			super.i18nMessages(request);
		}
	}

	protected void initModulesHelpers(String node) {
		if (config.getBoolean("neo4j", true)) {
			if (config.getJsonObject("neo4jConfig") != null) {
				final JsonObject neo4jConfigJson = config.getJsonObject("neo4jConfig").copy();
				final JsonObject neo4jConfigOverride = config.getJsonObject("neo4jConfigOverride", new JsonObject());
				Neo4j.getInstance().init(vertx, neo4jConfigJson.mergeIn(neo4jConfigOverride));
			} else {
				final String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
				final JsonObject neo4jConfigJson = new JsonObject(neo4jConfig);
				final JsonObject neo4jConfigOverride = config.getJsonObject("neo4jConfigOverride", new JsonObject());
				Neo4j.getInstance().init(vertx, neo4jConfigJson.mergeIn(neo4jConfigOverride));
			}
			Neo4jUtils.loadScripts(this.getClass().getSimpleName(), vertx,
					FileResolver.absolutePath(config.getString("neo4j-init-scripts", "neo4j")));
		}
		if (config.getBoolean("mongodb", true)) {
			MongoDb.getInstance().init(getEventBus(vertx), node +
					config.getString("mongo-address", "wse.mongodb.persistor"));
		}
		if (config.getBoolean("zip", true)) {
			Zip.getInstance().init(getEventBus(vertx), node +
					config.getString("zip-address", "entcore.zipper"));
		}
		if (config.getBoolean("sql", false)) {
			Sql.getInstance().init(getEventBus(vertx), node +
					config.getString("sql-address", "sql.persistor"));
			schema = config.getString("db-schema", getPathPrefix(config).replaceAll("/", ""));
			DB.loadScripts(schema, vertx, FileResolver.absolutePath(config.getString("init-scripts", "sql")));
		}
		if (config.getBoolean("elasticsearch", false)) {
			if (config.getJsonObject("elasticsearchConfig") != null) {
				ElasticSearch.getInstance().init(vertx, config.getJsonObject("elasticsearchConfig"));
			} else {
				String elasticsearchConfig = (String) vertx.sharedData().getLocalMap("server").get("elasticsearchConfig");
				ElasticSearch.getInstance().init(vertx, new JsonObject(elasticsearchConfig));
			}
		}
		if (config.getBoolean("redis", true)) {
			if (config.getJsonObject("redisConfig") != null) {
				Redis.getInstance().init(vertx, config.getJsonObject("redisConfig"));
			}else{
				final String redisConf = (String) vertx.sharedData().getLocalMap("server").get("redisConfig");
				if(redisConf!=null){
					Redis.getInstance().init(vertx, new JsonObject(redisConf));
				}
			}
		}
		//

		JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
		validator.setEventBus(getEventBus(vertx));
		validator.setAddress(node + "json.schema.validator");
		validator.loadJsonSchema(getPathPrefix(config), vertx);
	}

	private void loadI18nAssetsFiles() {
		final String assetsDirectory = config.getString("assets-path", "../..") + File.separator + "assets";
		final String className = this.getClass().getSimpleName();
		readI18n(I18n.DEFAULT_DOMAIN, assetsDirectory + File.separator + "i18n" + File.separator + className,
				new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String themesDirectory = assetsDirectory + File.separator + "themes";
				final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
				final Map<String, String> reverseSkins = new HashMap<>();
				for (Map.Entry<String, String> e: skins.entrySet()) {
					reverseSkins.put(e.getValue(), e.getKey());
				}
				vertx.fileSystem().exists(themesDirectory, new Handler<AsyncResult<Boolean>>() {
					@Override
					public void handle(AsyncResult<Boolean> event) {
						if (event.succeeded() && event.result()) {
							vertx.fileSystem().readDir(themesDirectory, new Handler<AsyncResult<List<String>>>() {
								@Override
								public void handle(AsyncResult<List<String>> themes) {
									if (themes.succeeded()) {
										for (String theme : themes.result()) {
											final String themeName = theme.substring(theme.lastIndexOf(File.separator) + 1);
											final String domain = reverseSkins.get(themeName);
											if (domain == null) {
												log.warn("Missing domain for theme : " + theme);
												continue;
											}
											final String i18nDirectory = theme + File.separator + "i18n" + File.separator + className;
											readI18n(domain, i18nDirectory, null, themeName);
										}
									} else {
										log.error("Error listing themes directory.", themes.cause());
									}
								}
							});
						} else {
							log.error("Missing themes directory.", event.cause());
						}
					}
				});
			}
		});

	}

	private void readI18n(final String domain, final String i18nDirectory, final Handler<Void> handler) {
		readI18n(domain, i18nDirectory, handler, null);
	}

	private void readI18n(final String domain, final String i18nDirectory, final Handler<Void> handler, final String themeName) {
		vertx.fileSystem().exists(i18nDirectory, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> ar) {
				if (ar.succeeded() && ar.result()) {
					vertx.fileSystem().readDir(i18nDirectory, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> asyncResult) {
							if (asyncResult.succeeded()) {
								List<String> files = asyncResult.result();
								final I18n i18n = I18n.getInstance();
								for (final String s : files) {
									final Locale locale = Locale.forLanguageTag(
											s.substring(s.lastIndexOf(File.separatorChar) + 1, s.lastIndexOf('.')));
									vertx.fileSystem().readFile(s, new Handler<AsyncResult<Buffer>>() {
										@Override
										public void handle(AsyncResult<Buffer> ar) {
											if (ar.succeeded()) {
												try {
													i18n.add(domain, locale, new JsonObject(ar.result().toString()));
													if (themeName != null) {
														i18n.add(themeName, locale, new JsonObject(ar.result().toString()), true);
													}
													if (handler != null) {
														handler.handle(null);
													}
												} catch (Exception e) {
													log.error("Error loading i18n asset file : " + s, e);
												}
											} else {
												log.error("Error loading i18n asset file : " + s, ar.cause());
											}
										}
									});
								}
							} else {
								log.error("Error loading assets i18n.", asyncResult.cause());
							}
						}
					});
				} else if (ar.failed()) {
					log.error("Error loading assets i18n.", ar.cause());
				}
			}
		});
	}

	protected BaseServer setResourceProvider(ResourcesProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
		return this;
	}

	protected BaseServer setRepositoryEvents(RepositoryEvents repositoryEvents) {
		repositoryHandler.setRepositoryEvents(repositoryEvents);
		return this;
	}

	protected BaseServer setSearchingEvents(final SearchingEvents searchingEvents) {
		searchingHandler.setSearchingEvents(searchingEvents);
		final LocalMap<String, String> set = vertx.sharedData().getLocalMap(SearchingHandler.class.getName());
		set.putIfAbsent(searchingEvents.getClass().getSimpleName(), "");
		return this;
	}

	protected BaseServer setDefaultResourceFilter(ResourcesProvider resourceProvider) {
		if (this.resourceProvider instanceof ResourceProviderFilter) {
			((ResourceProviderFilter) this.resourceProvider).setDefault(resourceProvider);
		}
		return this;
	}

	public String getSchema() {
		return schema;
	}

}
