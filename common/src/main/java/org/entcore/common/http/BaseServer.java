/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.common.http;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.AccessLogger;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.AccessLoggerFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.validation.JsonSchemaValidator;

import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.controller.ConfController;
import org.entcore.common.controller.RightsController;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.*;
import org.entcore.common.http.response.SecurityHookRender;
import org.entcore.common.http.response.OverrideThemeHookRender;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.search.SearchingEvents;
import org.entcore.common.search.SearchingHandler;
import org.entcore.common.sql.DB;
import org.entcore.common.sql.Sql;
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

	private ResourcesProvider resourceProvider = null;
	private RepositoryHandler repositoryHandler;
	private SearchingHandler searchingHandler;
	private String schema;
	private String contentSecurityPolicy;
	private AccessLogger accessLogger;

	@Override
	public void start() throws Exception {
		if (resourceProvider == null) {
			setResourceProvider(new ResourceProviderFilter());
		}
		super.start();

		accessLogger = new EntAccessLogger(getEventBus(vertx));
		initFilters();

		String node = (String) vertx.sharedData().getLocalMap("server").get("node");

		contentSecurityPolicy = (String) vertx.sharedData().getLocalMap("server").get("contentSecurityPolicy");

		repositoryHandler = new RepositoryHandler(getEventBus(vertx));
		searchingHandler = new SearchingHandler(getEventBus(vertx));

		Config.getInstance().setConfig(config);

		if (node != null) {
			initModulesHelpers(node);
		}

		EventStoreFactory eventStoreFactory = EventStoreFactory.getFactory();
		eventStoreFactory.setVertx(vertx);

		if (config.getBoolean("csrf-token", false)) {
			addFilter(new CsrfFilter(getEventBus(vertx), securedUriBinding));
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
	}

	protected void initFilters() {
		clearFilters();
		addFilter(new AccessLoggerFilter(accessLogger));
		addFilter(new UserAuthFilter(new AppOAuthResourceProvider(
				getEventBus(vertx), getPathPrefix(config)), new BasicFilter()));
	}

	@Override
	protected Server addController(BaseController controller) {
		controller.setAccessLogger(accessLogger);
		super.addController(controller);
		if (config.getString("override-theme") != null) {
			controller.addHookRenderProcess(new OverrideThemeHookRender(getEventBus(vertx), config.getString("override-theme")));
		}
		if (config.getBoolean("csrf-token", true) || contentSecurityPolicy != null) {
			controller.addHookRenderProcess(new SecurityHookRender(getEventBus(vertx),
					config.getBoolean("csrf-token", true), contentSecurityPolicy));
		}
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
				Neo4j.getInstance().init(vertx, config.getJsonObject("neo4jConfig"));
			} else {
				String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
				Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
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
											final String domain = reverseSkins.get(theme.substring(theme.lastIndexOf(File.separator) + 1));
											if (domain == null) {
												log.warn("Missing domain for theme : " + theme);
												continue;
											}
											final String i18nDirectory = theme + File.separator + "i18n" + File.separator + className;
											readI18n(domain, i18nDirectory, null);
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
