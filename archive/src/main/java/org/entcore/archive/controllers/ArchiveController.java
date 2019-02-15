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

package org.entcore.archive.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.impl.FileSystemExportService;
import org.entcore.archive.services.impl.UserExport;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import org.entcore.common.utils.MapFactory;
import org.vertx.java.core.http.RouteMatcher;

import java.util.*;

public class ArchiveController extends BaseController {

	private ExportService exportService;
	private EventStore eventStore;
	private Storage storage;
	private enum ArchiveEvent { ACCESS }

	@Override
	public void init(Vertx vertx, final JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		String exportPath = config.getString("export-path", System.getProperty("java.io.tmpdir"));
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		Boolean cluster = (Boolean) server.get("cluster");
		final Map<String, UserExport> userExport = MapFactory.getSyncClusterMap(Archive.ARCHIVES, vertx);
		EmailFactory emailFactory = new EmailFactory(vertx, config);
		EmailSender notification = config.getBoolean("send.export.email", false) ?
				emailFactory.getSender() : null;
		storage = new StorageFactory(vertx, config).getStorage();
		exportService = new FileSystemExportService(vertx, vertx.fileSystem(),
				eb, exportPath, notification, storage, userExport, new TimelineHelper(vertx, eb, config));
		eventStore = EventStoreFactory.getFactory().getEventStore(Archive.class.getSimpleName());
		Long periodicUserClear = config.getLong("periodicUserClear");
		if (periodicUserClear != null) {
			vertx.setPeriodic(periodicUserClear, new Handler<Long>() {
				@Override
				public void handle(Long event) {
					final long limit = System.currentTimeMillis() - config.getLong("userClearDelay", 3600000l);
					Set<Map.Entry<String, UserExport>> entries = new HashSet<>(userExport.entrySet());
					for (Map.Entry<String, UserExport> e : entries) {
						if (e.getValue() == null || e.getValue().getProgress() < limit) {
							userExport.remove(e.getKey());
						}
					}
				}
			});
		}
	}

	@Get("")
	@SecuredAction("archive.view")
	public void view(HttpServerRequest request) {
		renderView(request);
		eventStore.createAndStoreEvent(ArchiveEvent.ACCESS.name(), request);
	}

	@Post("/export")
	@SecuredAction("archive.export")
	public void export(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.bodyHandler(new Handler<Buffer>() {
						public void handle(Buffer event) {
							JsonArray apps = event.toJsonObject().getJsonArray("apps");
							exportService.export(user, I18n.acceptLanguage(request), apps, request,
									new Handler<Either<String, String>>() {
										@Override
										public void handle(Either<String, String> event) {
											if (event.isRight()) {
												renderJson(request,
														new JsonObject().put("message", "export.in.progress")
																.put("exportId", event.right().getValue()));
											} else {
												badRequest(request, event.left().getValue());
											}
										}
									});
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/export/verify/:exportId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void verifyExport(final HttpServerRequest request) {
		final String exportId = request.params().get("exportId");
		exportService.waitingExport(exportId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					log.debug("waiting export true");
					final String address = "export." + exportId;
					final MessageConsumer<JsonObject> consumer = eb.consumer(address);
					final Handler<Message<JsonObject>> downloadHandler = new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							String path = event.body().getString("destZip");
							if ("ok".equals(event.body().getString("status")) && path != null) {
								log.debug("Download export " + exportId);
								event.reply(new JsonObject().put("status", "ok"));
								verifyExport(request, exportId);
							} else {
								event.reply(new JsonObject().put("status", "error"));
								renderError(request, event.body());
							}
							consumer.unregister();
						}
					};
					request.response().closeHandler(new Handler<Void>() {
						@Override
						public void handle(Void event) {
							consumer.unregister();
							if (log.isDebugEnabled()) {
								log.debug("Unregister handler : " + address);
							}
						}
					});
					consumer.handler(downloadHandler);
				} else {
					log.debug("waiting export false");
					verifyExport(request, exportId);
				}
			}
		});
	}

	private void verifyExport(final HttpServerRequest request, final String exportId) {
		exportService.setDownloadInProgress(exportId);
		storage.readFile(exportId, new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				if (event != null && request.response().getStatusCode() == 200) {
					renderJson(request, new JsonObject().put("status", "ok"));
				} else if (!request.response().ended()) {
					notFound(request);
				}
			}
		});
	}

	@Get("/export/:exportId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void downloadExport(final HttpServerRequest request) {
		final String exportId = request.params().get("exportId");
		storage.sendFile(exportId, exportId + ".zip", request, false, null, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded() && request.response().getStatusCode() == 200) {
					exportService.deleteExport(exportId);
				} else if (!request.response().ended()) {
					notFound(request);
				}
			}
		});
	}

	@BusAddress("entcore.export")
	public void export(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "exported" :
				exportService.exported(
						message.body().getString("exportId"),
						message.body().getString("status"),
						message.body().getString("locale", "fr"),
						message.body().getString("host", config.getString("host", ""))
				);
				break;
			default: log.error("Archive : invalid action " + action);
		}
	}

}
