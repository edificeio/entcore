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

package org.entcore.infra;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.utils.MapFactory;
import org.entcore.infra.controllers.*;
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

			serverMap.put("sameSiteValue", config.getString("sameSiteValue", "Strict"));
			serverMap.put("hidePersonalData", config.getBoolean("hidePersonalData", false));

			//JWT need signKey
			SecurityHandler.setVertx(vertx);
			//encoding
			final JsonArray encondings = config.getJsonArray("encoding-available", new JsonArray());
			final JsonArray safeEncondigs = new JsonArray();
			for(final Object o : encondings){
				safeEncondigs.add(o.toString());
			}
			serverMap.put("encoding-available", safeEncondigs.encode());
			//
			CookieHelper.getInstance().init((String) vertx
					.sharedData().getLocalMap("server").get("signKey"),
					(String) vertx.sharedData().getLocalMap("server").get("sameSiteValue"),
					log);

			JsonObject swift = config.getJsonObject("swift");
			if (swift != null) {
				serverMap.put("swift", swift.encode());
			}
			JsonObject emailConfig = config.getJsonObject("emailConfig");
			if (emailConfig != null) {
				serverMap.put("emailConfig", emailConfig.encode());
				if(emailConfig.containsKey("postgresql")){
					addController(new MailController(vertx, emailConfig));
				}
			}
			JsonObject dataValidationConfig = config.getJsonObject("emailValidationConfig");
			if (dataValidationConfig != null) {
				serverMap.put("emailValidationConfig", dataValidationConfig.encode());
			}
			JsonObject mfaConfig = config.getJsonObject("mfaConfig");
			if (mfaConfig != null) {
				serverMap.put("mfaConfig", mfaConfig.encode());
			}
			final JsonObject webviewConfig = config.getJsonObject("webviewConfig");
			if (webviewConfig != null) {
				serverMap.put("webviewConfig", webviewConfig.encode());
			}
			JsonObject filesystem = config.getJsonObject("file-system");
			if (filesystem != null) {
				serverMap.put("file-system", filesystem.encode());
			}
			JsonObject neo4jConfig = config.getJsonObject("neo4jConfig");
			if (neo4jConfig != null) {
				serverMap.put("neo4jConfig", neo4jConfig.encode());
			}
			JsonObject mongoConfig = config.getJsonObject("mongoConfig");
			if (mongoConfig != null) {
				serverMap.put("mongoConfig", mongoConfig.encode());
			}
			JsonObject postgresConfig = config.getJsonObject("postgresConfig");
			if (postgresConfig != null) {
				serverMap.put("postgresConfig", postgresConfig.encode());
			}
			JsonObject explorerConfig = config.getJsonObject("explorerConfig");
			if (explorerConfig != null) {
				serverMap.put("explorerConfig", explorerConfig.encode());
			}
			JsonObject redisConfig = config.getJsonObject("redisConfig");
			if (redisConfig != null) {
				serverMap.put("redisConfig", redisConfig.encode());
			}
			JsonObject oauthCache = config.getJsonObject("oauthCache");
			if (oauthCache != null) {
				serverMap.put("oauthCache", oauthCache.encode());
			}
			serverMap.put("cache-enabled", config.getBoolean("cache-enabled", false));
			final String csp = config.getString("content-security-policy");
			if (isNotEmpty(csp)) {
				serverMap.put("contentSecurityPolicy", csp);
			}
			JsonObject nodePdfGenerator = config.getJsonObject("node-pdf-generator");
			if (nodePdfGenerator != null) {
				serverMap.put("node-pdf-generator", nodePdfGenerator.encode());
			}
			final String staticHost = config.getString("static-host");
			if(staticHost != null)
			{
				serverMap.put("static-host", staticHost);
			}
			JsonObject eventStoreConfig = config.getJsonObject("event-store");
			if (eventStoreConfig != null) {
				serverMap.put("event-store", eventStoreConfig.encode());
			}
			serverMap.put("gridfsAddress", config.getString("gridfs-address", "wse.gridfs.persistor"));
			final JsonObject metricsOptions = config.getJsonObject("metricsOptions");
			if(metricsOptions != null) {
				serverMap.put("metricsOptions", metricsOptions.encode());
			}
			//initModulesHelpers(node);

			/* sharedConf sub-object */
			JsonObject sharedConf = config.getJsonObject("sharedConf", new JsonObject());
			for(String field : sharedConf.fieldNames()){
				serverMap.put(field, sharedConf.getValue(field));
			}

			vertx.sharedData().getLocalMap("skins").putAll(config.getJsonObject("skins", new JsonObject()).getMap());

			log.info("config skin-levels = " + config.getJsonObject("skin-levels", new JsonObject()));

			vertx.sharedData().getLocalMap("skin-levels").putAll(config.getJsonObject("skin-levels", new JsonObject()).getMap());

			log.info("localMap skin-levels = " + vertx.sharedData().getLocalMap("skin-levels"));

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
		EventStoreService eventStoreService = new MongoDbEventStore(vertx);
		EventStoreController eventStoreController = new EventStoreController(eventConfig);
		eventStoreController.setEventStoreService(eventStoreService);
		addController(eventStoreController);
		addController(new MonitoringController());
		addController(new EmbedController());
		if (config.getJsonObject("node-pdf-generator") != null) {
			try {
				PdfController pdfController = new PdfController();
				pdfController.setPdfGenerator(new PdfFactory(vertx));
				addController(pdfController);
			} catch (Exception e) {
				log.error("Error loading pdf controller.", e);
			}
		}
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
