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

package org.entcore.infra;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.validation.JsonSchemaValidator;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.utils.StringUtils;
import org.entcore.infra.controllers.EmbedController;
import org.entcore.infra.controllers.EventStoreController;
import org.entcore.infra.controllers.MonitoringController;
import org.entcore.infra.cron.HardBounceTask;
import org.entcore.infra.services.EventStoreService;
import org.entcore.infra.services.impl.MongoDbEventStore;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Starter extends BaseServer {

	private String node;
	private boolean cluster;
	public final static Map<String,String> MAP_APP_VERSION = new HashMap<>();

	@Override
	public void start() {
		try {
			if (container.config() == null || container.config().size() == 0) {
				config = getConfig("", "mod.json");
			}
			super.start();
			final ConcurrentSharedMap<Object, Object> serverMap = vertx.sharedData().getMap("server");
			serverMap.put("signKey", config.getString("key", "zbxgKWuzfxaYzbXcHnK3WnWK" + Math.random()));
			CookieHelper.getInstance().init((String) vertx
					.sharedData().getMap("server").get("signKey"), log);
			cluster = config.getBoolean("cluster", false);
			serverMap.put("cluster", cluster);
			node = config.getString("node", "");
			serverMap.put("node", node);
			JsonObject swift = config.getObject("swift");
			if (swift != null) {
				serverMap.put("swift", swift.encode());
			}
			JsonObject emailConfig = config.getObject("emailConfig");
			if (emailConfig != null) {
				serverMap.put("emailConfig", emailConfig.encode());
			}
			JsonObject filesystem = config.getObject("file-system");
			if (filesystem != null) {
				serverMap.put("file-system", filesystem.encode());
			}
			JsonObject neo4jConfig = config.getObject("neo4jConfig");
			if (neo4jConfig != null) {
				serverMap.put("neo4jConfig", neo4jConfig.encode());
			}
			final String csp = config.getString("content-security-policy");
			if (isNotEmpty(csp)) {
				serverMap.put("contentSecurityPolicy", csp);
			}
			serverMap.put("gridfsAddress", config.getString("gridfs-address", "wse.gridfs.persistor"));
			initModulesHelpers(node);

			/* sharedConf sub-object */
			JsonObject sharedConf = config.getObject("sharedConf", new JsonObject());
			for(String field : sharedConf.getFieldNames()){
				serverMap.put(field, sharedConf.getValue(field));
			}

			vertx.sharedData().getMap("skins").putAll(config.getObject("skins", new JsonObject()).toMap());

			deployPreRequiredModules(config.getArray("pre-required-modules"), new VoidHandler() {
				@Override
				protected void handle() {
					JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
					validator.setEventBus(getEventBus(vertx));
					validator.setAddress(node + "json.schema.validator");
					validator.loadJsonSchema(getPathPrefix(config), vertx);

					deployModule(config.getObject("app-registry"), false, false, new Handler<AsyncResult<String>>() {
						@Override
						public void handle(AsyncResult<String> event) {
							if (event.succeeded()) {
								deployModules(config.getArray("external-modules", new JsonArray()), false);
								deployModules(config.getArray("one-modules", new JsonArray()), true);
								registerGlobalWidgets(config.getString("widgets-path", "../../assets/widgets"));
								loadInvalidEmails();
							}
						}
					});
				}
			});
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}
		EventStoreService eventStoreService = new MongoDbEventStore();
		EventStoreController eventStoreController = new EventStoreController();
		eventStoreController.setEventStoreService(eventStoreService);
		addController(eventStoreController);
		addController(new MonitoringController());
		addController(new EmbedController());
	}

	private void loadInvalidEmails() {
		final Map<Object, Object> invalidEmails;
		if (cluster) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			invalidEmails = cm.getSyncMap("invalidEmails");
		} else {
			invalidEmails = vertx.sharedData().getMap("invalidEmails");
		}
		if (invalidEmails != null && invalidEmails.isEmpty()) {
			MongoDb.getInstance().findOne(HardBounceTask.PLATEFORM_COLLECTION, new JsonObject()
					.putString("type", HardBounceTask.PLATFORM_ITEM_TYPE), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject res = event.body().getObject("result");
					if ("ok".equals(event.body().getString("status")) && res != null && res.getArray("invalid-emails") != null) {
						for (Object o : res.getArray("invalid-emails")) {
							invalidEmails.put(o, "");
						}
					} else {
						log.error(event.body().getString("message"));
					}
				}
			});
		}
		EmailFactory emailFactory = new EmailFactory(vertx, container);
		try {
			new CronTrigger(vertx, config.getString("hard-bounces-cron", "0 0 7 * * ? *"))
					.schedule(new HardBounceTask(emailFactory.getSender(), config.getInteger("hard-bounces-day", -1),
							new TimelineHelper(vertx, getEventBus(vertx), container), invalidEmails));
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			vertx.stop();
		}
	}

	private void deployPreRequiredModules(final JsonArray array, final VoidHandler handler) {
		if (array == null || array.size() == 0) {
			handler.handle(null);
			return;
		}
		final Handler [] handlers = new Handler[array.size() + 1];
		handlers[handlers.length - 1] = new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()) {
					handler.handle(null);
				} else {
					log.error("Error deploying pre-required module.", event.cause());
					vertx.stop();
				}
			}
		};
		for (int i = array.size() - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new Handler<AsyncResult<String>>() {

				@Override
				public void handle(AsyncResult<String> event) {
					if (event.succeeded()) {
						deployModule(array.<JsonObject>get(j), false, cluster, handlers[j + 1]);
					} else {
						log.error("Error deploying pre-required module.", event.cause());
						vertx.stop();
					}
				}
			};

		}
		handlers[0].handle(new AsyncResult<String>() {
			@Override
			public String result() {
				return null;
			}

			@Override
			public Throwable cause() {
				return null;
			}

			@Override
			public boolean succeeded() {
				return true;
			}

			@Override
			public boolean failed() {
				return false;
			}
		});
	}

	private void deployModule(JsonObject module, boolean internal, boolean overideBusAddress,
			Handler<AsyncResult<String>> handler) {
		if (module.getString("name") == null) {
			return;
		}
		JsonObject conf = new JsonObject();
		if (internal) {
			try {
				conf = getConfig("../" + module.getString("name") + "/", "mod.json");
			} catch (Exception e) {
				log.error("Invalid configuration for module " + module.getString("name"), e);
				return;
			}
		}
		conf = conf.mergeIn(module.getObject("config", new JsonObject()));
		String address = conf.getString("address");
		if (overideBusAddress && !node.isEmpty() && address != null) {
			conf.putString("address", node + address);
		}
		container.deployModule(module.getString("name"),
				conf, module.getInteger("instances", 1), handler);
	}

	private void addAppVersion(final JsonObject module) {
		final List<String> lNameVersion = StringUtils.split(module.getString("name", ""), "~");
		if (lNameVersion != null && lNameVersion.size() == 3) {
			MAP_APP_VERSION.put(lNameVersion.get(0) + "." + lNameVersion.get(1), lNameVersion.get(2));
		}
	}

	private void deployModules(JsonArray modules, boolean internal) {
		for (Object o : modules) {
			JsonObject module = (JsonObject) o;
			if (module.getString("name") == null) {
				continue;
			}
			JsonObject conf = new JsonObject();
			if (internal) {
				try {
					conf = getConfig("../" + module.getString("name") + "/", "mod.json");
				} catch (Exception e) {
					log.error("Invalid configuration for module " + module.getString("name"), e);
					continue;
				}
			}
			conf = conf.mergeIn(module.getObject("config", new JsonObject()));
			addAppVersion(module);
			container.deployModule(module.getString("name"),
					conf, module.getInteger("instances", 1));
		}
	}

	private void registerWidget(final String widgetPath){
		final String widgetName = new File(widgetPath).getName();
		JsonObject widget = new JsonObject()
				.putString("name", widgetName)
				.putString("js", "/assets/widgets/"+widgetName+"/"+widgetName+".js")
				.putString("path", "/assets/widgets/"+widgetName+"/"+widgetName+".html");

		if(vertx.fileSystem().existsSync(widgetPath+"/i18n")){
			widget.putString("i18n", "/assets/widgets/"+widgetName+"/i18n");
		}

		JsonObject message = new JsonObject()
				.putObject("widget", widget);
		vertx.eventBus().send("wse.app.registry.widgets", message, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status"))){
					log.error("Error while registering widget "+widgetName+". "+event.body().getArray("errors"));
					return;
				}
				log.info("Successfully registered widget "+widgetName);
			}
		});
	}

	private void registerGlobalWidgets(String widgetsPath) {
		vertx.fileSystem().readDir(widgetsPath, new Handler<AsyncResult<String[]>>() {
			public void handle(AsyncResult<String[]> asyn) {
				if(asyn.failed()){
					log.error("Error while registering global widgets.", asyn.cause());
					return;
				}
				String[] paths = asyn.result();
				for(final String path: paths){
					vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
						public void handle(AsyncResult<FileProps> asyn) {
							if(asyn.failed()){
								log.error("Error while registering global widget " + path, asyn.cause());
								return;
							}
							if(asyn.result().isDirectory()){
								registerWidget(path);
							}
						}
					});
				}
			}
		});
	}

	protected JsonObject getConfig(String path, String fileName) throws Exception {
		Buffer b = vertx.fileSystem().readFileSync(path + fileName);
		if (b == null) {
			log.error("Configuration file "+ fileName +"not found");
			throw new Exception("Configuration file "+ fileName +" not found");
		}
		else {
			return new JsonObject(b.toString());
		}
	}

}
