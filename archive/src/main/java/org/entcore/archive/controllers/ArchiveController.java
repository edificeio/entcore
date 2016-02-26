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
import org.entcore.archive.Archive;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.impl.FileSystemExportService;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;
import org.vertx.java.platform.Container;

import java.util.*;

public class ArchiveController extends BaseController {

	private ExportService exportService;
	private EventStore eventStore;
	private Storage storage;
	private enum ArchiveEvent { ACCESS }

	@Override
	public void init(Vertx vertx, final Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		String exportPath = container.config()
				.getString("export-path", System.getProperty("java.io.tmpdir"));
		Set<String> expectedExports = new HashSet<>();
		final JsonArray e = container.config().getArray("expected-exports");
		for (Object o : e) {
			if (o instanceof String) {
				expectedExports.add((String) o);
			}
		}
		ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		Boolean cluster = (Boolean) server.get("cluster");
		final Map<String, Long> userExport;
		if (Boolean.TRUE.equals(cluster)) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			userExport = cm.getSyncMap(Archive.ARCHIVES);
		} else {
			userExport = new HashMap<>();
		}
		EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
		EmailSender notification = container.config().getBoolean("send.export.email", false) ?
				emailFactory.getSender() : null;
		storage = new StorageFactory(vertx, container.config()).getStorage();
		exportService = new FileSystemExportService(vertx.fileSystem(),
				eb, exportPath, expectedExports, notification, storage, userExport, new TimelineHelper(vertx, eb, container));
		eventStore = EventStoreFactory.getFactory().getEventStore(Archive.class.getSimpleName());
		Long periodicUserClear = container.config().getLong("periodicUserClear");
		if (periodicUserClear != null) {
			vertx.setPeriodic(periodicUserClear, new Handler<Long>() {
				@Override
				public void handle(Long event) {
					final long limit = System.currentTimeMillis() - container.config().getLong("userClearDelay", 3600000l);
					Set<Map.Entry<String, Long>> entries = new HashSet<>(userExport.entrySet());
					for (Map.Entry<String, Long> e: entries) {
						if (e.getValue() == null || e.getValue() < limit) {
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
					exportService.export(user, I18n.acceptLanguage(request), request, new Handler<Either<String, String>>() {
						@Override
						public void handle(Either<String, String> event) {
							if (event.isRight()) {
								renderJson(request, new JsonObject()
										.putString("message", "export.in.progress")
										.putString("exportId", event.right().getValue())
								);
							} else {
								badRequest(request, event.left().getValue());
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/export/:exportId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void downloadExport(final HttpServerRequest request) {
		final String exportId = request.params().get("exportId");
		exportService.waitingExport(exportId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					log.debug("waiting export true");
					final String address = "export." + exportId;
					final Handler<Message<JsonObject>> downloadHandler = new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							String path = event.body().getString("destZip");
							if ("ok".equals(event.body().getString("status")) && path != null) {
								log.debug("Download export " + exportId);
								event.reply(new JsonObject().putString("status", "ok"));
								downloadExport(request, exportId);
							} else {
								event.reply(new JsonObject().putString("status", "error"));
								renderError(request, event.body());
							}
							eb.unregisterHandler(address, this);
						}
					};
					request.response().closeHandler(new Handler<Void>() {
						@Override
						public void handle(Void event) {
							eb.unregisterHandler(address, downloadHandler);
							if (log.isDebugEnabled()) {
								log.debug("Unregister handler : " + address);
							}
						}
					});
					eb.registerHandler(address, downloadHandler);
				} else {
					log.debug("waiting export false");
					downloadExport(request, exportId);
				}
			}
		});
	}

	private void downloadExport(final HttpServerRequest request, final String exportId) {
		exportService.setDownloadInProgress(exportId);
		storage.sendFile(exportId, exportId + ".zip", request, false, null, new AsyncResultHandler<Void>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded() && request.response().getStatusCode() == 200) {
					exportService.deleteExport(exportId);
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
						message.body().getString("host", container.config().getString("host", ""))
				);
				break;
			default: log.error("Archive : invalid action " + action);
		}
	}


}
