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
import fr.wseduc.webutils.collections.SharedDataHelper;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.utils.MapFactory;
import org.entcore.infra.controllers.*;
import org.entcore.infra.cron.HardBounceTask;
import org.entcore.infra.cron.MonitoringEventsChecker;
import org.entcore.infra.metrics.MicrometerInfraMetricsRecorder;
import org.entcore.infra.services.EventStoreService;
import org.entcore.infra.services.impl.ClamAvService;
import org.entcore.infra.services.impl.ExecCommandWorker;
import org.entcore.infra.services.impl.MongoDbEventStore;

import java.text.ParseException;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Starter extends BaseServer {

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		final Promise<Void> initInfraPromise = Promise.promise();
		super.start(initInfraPromise);
		initInfraPromise.future().compose(init -> initInfra()).onComplete(startPromise);
	}

	public Future<Void> initInfra() {
		Promise<Void> returnPromise = Promise.promise();
		try {
			vertx.sharedData().getLocalAsyncMap("server").onSuccess(asyncServerMap -> {
				asyncServerMap.get("emailConfig")
						.map(config -> (String) config)
						.compose(emailConfigStr -> {
							if (isNotEmpty(emailConfigStr)) {
								JsonObject emailConfig = new JsonObject(emailConfigStr);
								if(emailConfig.containsKey("postgresql")) {
									addController(new MailController(vertx, emailConfig));
								}
							}
							return Future.succeededFuture();
						});
			}).onFailure(th -> log.error("Error getting server map", th));

			final MessageConsumer<JsonObject> messageConsumer = vertx.eventBus().consumer("app-registry.loaded");
			messageConsumer.handler(message -> {
				loadInvalidEmails(); // TODO change map loadding if needed
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
		final JsonObject checkMonitoringEvents = config.getJsonObject("check-monitoring-events");
		if (checkMonitoringEvents != null) {
			MonitoringEventsChecker monitoringEventsChecker = new MonitoringEventsChecker(
				vertx.eventBus(),
				checkMonitoringEvents.getInteger("session-min-delay", 1000),
				checkMonitoringEvents.getInteger("session-threshold", 1000),
				checkMonitoringEvents.getLong("session-window-duration", 300000L)
			);
			vertx.setPeriodic(checkMonitoringEvents.getLong("period", 300000L), monitoringEventsChecker);
		}
		if(config.getJsonObject("metricsOptions") == null) {
			SharedDataHelper.getInstance().<String, String>getLocal("server", "metricsOptions").onSuccess(metricsOptions -> {
				if(isNotEmpty(metricsOptions) && new MetricsOptions(new JsonObject(metricsOptions)).isEnabled()){
					new MicrometerInfraMetricsRecorder(vertx);
				}
			}).onFailure(ex -> log.error("Error getting metrics options", ex));
		} else if (new MetricsOptions(config.getJsonObject("metricsOptions")).isEnabled()) {
			new MicrometerInfraMetricsRecorder(vertx);
		}
		returnPromise.complete();
		return returnPromise.future();
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
				EmailFactory emailFactory = EmailFactory.getInstance();
				HardBounceTask hardBounceTask = new HardBounceTask(emailFactory.getSender(), config.getInteger("hard-bounces-day", -1),
						new TimelineHelper(vertx, getEventBus(vertx), config), invalidEmails);
				// Enable hard bounce task to be triggered via API
				addController(new TaskController(hardBounceTask));
				// Schedule hard bounce task from cron expression
				try {
					new CronTrigger(vertx, config.getString("hard-bounces-cron", "0 0 7 * * ? *"))
							.schedule(hardBounceTask);
				} catch (ParseException e) {
					log.error(e.getMessage(), e);
					vertx.close();
				}
			}
		});
	}

}
