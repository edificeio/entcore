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
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.utils.MapFactory;
import org.entcore.infra.controllers.AntiVirusController;
import org.entcore.infra.controllers.EmbedController;
import org.entcore.infra.controllers.EventStoreController;
import org.entcore.infra.controllers.MonitoringController;
import org.entcore.infra.cron.HardBounceTask;
import org.entcore.infra.services.EventStoreService;
import org.entcore.infra.services.impl.ClamAvService;
import org.entcore.infra.services.impl.ExecCommandWorker;
import org.entcore.infra.services.impl.MongoDbEventStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Starter extends BaseServer {

	@Override
	public void start() {
		try {
			super.start();
			final LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");

			serverMap.put("signKey", config.getString("key", "zbxgKWuzfxaYzbXcHnK3WnWK" + Math.random()));
			CookieHelper.getInstance().init((String) vertx
					.sharedData().getLocalMap("server").get("signKey"), log);

			JsonObject swift = config.getJsonObject("swift");
			if (swift != null) {
				serverMap.put("swift", swift.encode());
			}
			JsonObject emailConfig = config.getJsonObject("emailConfig");
			if (emailConfig != null) {
				serverMap.put("emailConfig", emailConfig.encode());
			}
			JsonObject filesystem = config.getJsonObject("file-system");
			if (filesystem != null) {
				serverMap.put("file-system", filesystem.encode());
			}
			JsonObject neo4jConfig = config.getJsonObject("neo4jConfig");
			if (neo4jConfig != null) {
				serverMap.put("neo4jConfig", neo4jConfig.encode());
			}
			final String csp = config.getString("content-security-policy");
			if (isNotEmpty(csp)) {
				serverMap.put("contentSecurityPolicy", csp);
			}
			serverMap.put("gridfsAddress", config.getString("gridfs-address", "wse.gridfs.persistor"));
			//initModulesHelpers(node);

			/* sharedConf sub-object */
			JsonObject sharedConf = config.getJsonObject("sharedConf", new JsonObject());
			for(String field : sharedConf.fieldNames()){
				serverMap.put(field, sharedConf.getValue(field));
			}

			vertx.sharedData().getLocalMap("skins").putAll(config.getJsonObject("skins", new JsonObject()).getMap());

			final MessageConsumer<JsonObject> messageConsumer = vertx.eventBus().localConsumer("app-registry.loaded");
			messageConsumer.handler(message -> {
//				JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
//				validator.setEventBus(getEventBus(vertx));
//				validator.setAddress(node + "json.schema.validator");
//				validator.loadJsonSchema(getPathPrefix(config), vertx);
				registerGlobalWidgets(config.getString("widgets-path", config.getString("assets-path", ".") + "/assets/widgets"));
				loadInvalidEmails();
				messageConsumer.unregister();
			});
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}
		JsonObject eventConfig = config.getJsonObject("eventConfig", new JsonObject());
		EventStoreService eventStoreService = new MongoDbEventStore();
		EventStoreController eventStoreController = new EventStoreController(eventConfig);
		eventStoreController.setEventStoreService(eventStoreService);
		addController(eventStoreController);
		addController(new MonitoringController());
		addController(new EmbedController());
		if (config.getBoolean("antivirus", false)) {
			ClamAvService antivirusService = new ClamAvService();
			antivirusService.setVertx(vertx);
			antivirusService.setTimeline(new TimelineHelper(vertx, getEventBus(vertx), config));
			antivirusService.setRender(new Renders(vertx, config));
			antivirusService.init();
			AntiVirusController antiVirusController = new AntiVirusController();
			antiVirusController.setAntivirusService(antivirusService);
			addController(antiVirusController);
			vertx.deployVerticle(ExecCommandWorker.class.getName(), new DeploymentOptions().setWorker(true));
		}
	}

	private void loadInvalidEmails() {
		MapFactory.getClusterMap("invalidEmails", vertx, new Handler<AsyncMap<Object, Object>>() {
			@Override
			public void handle(final AsyncMap<Object, Object> invalidEmails) {
				if (invalidEmails != null) {
					invalidEmails.size(new Handler<AsyncResult<Integer>>() {
						@Override
						public void handle(AsyncResult<Integer> event) {
							if (event.succeeded() && event.result() < 1) {
								MongoDb.getInstance().findOne(HardBounceTask.PLATEFORM_COLLECTION, new JsonObject()
										.put("type", HardBounceTask.PLATFORM_ITEM_TYPE), new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										JsonObject res = event.body().getJsonObject("result");
										if ("ok".equals(event.body().getString("status")) && res != null && res.getJsonArray("invalid-emails") != null) {
											for (Object o : res.getJsonArray("invalid-emails")) {
												invalidEmails.put(o, "", new Handler<AsyncResult<Void>>() {
													@Override
													public void handle(AsyncResult<Void> event) {
														if (event.failed()) {
															log.error("Error adding invalid email.", event.cause());
														}
													}
												});
											}
										} else {
											log.error(event.body().getString("message"));
										}
									}
								});
							}
						}
					});
				}
				EmailFactory emailFactory = new EmailFactory(vertx, config);
				try {
					new CronTrigger(vertx, config.getString("hard-bounces-cron", "0 0 7 * * ? *"))
							.schedule(new HardBounceTask(emailFactory.getSender(), config.getInteger("hard-bounces-day", -1),
									new TimelineHelper(vertx, getEventBus(vertx), config), invalidEmails));
				} catch (ParseException e) {
					log.error(e.getMessage(), e);
					vertx.close();
				}
			}
		});
	}

	private void registerWidget(final String widgetPath){
		final String widgetName = new File(widgetPath).getName();
		JsonObject widget = new JsonObject()
				.put("name", widgetName)
				.put("js", "/assets/widgets/"+widgetName+"/"+widgetName+".js")
				.put("path", "/assets/widgets/"+widgetName+"/"+widgetName+".html");

		if(vertx.fileSystem().existsBlocking(widgetPath + "/i18n")){
			widget.put("i18n", "/assets/widgets/"+widgetName+"/i18n");
		}

		JsonObject message = new JsonObject()
				.put("widget", widget);
		vertx.eventBus().send("wse.app.registry.widgets", message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status"))){
					log.error("Error while registering widget "+widgetName+". "+event.body().getJsonArray("errors"));
					return;
				}
				log.info("Successfully registered widget "+widgetName);
			}
		}));
	}

	private void registerGlobalWidgets(String widgetsPath) {
		vertx.fileSystem().readDir(widgetsPath, new Handler<AsyncResult<List<String>>>() {
			public void handle(AsyncResult<List<String>> asyn) {
				if(asyn.failed()){
					log.error("Error while registering global widgets.", asyn.cause());
					return;
				}
				final List<String> paths = asyn.result();
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

}
