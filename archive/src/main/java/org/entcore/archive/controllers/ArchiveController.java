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
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.impl.FileSystemExportService;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArchiveController extends BaseController {

	private ExportService exportService;

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm,
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
		NotificationHelper notification = new NotificationHelper(vertx, eb, container);
		exportService = new FileSystemExportService(vertx.fileSystem(),
				eb, exportPath, expectedExports, notification);
	}

	@Get("")
	@SecuredAction("archive.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@Post("/export")
	@SecuredAction("archive.export")
	public void export(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					exportService.export(user, I18n.acceptLanguage(request), new Handler<Either<String, String>>() {
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
		exportService.exportPath(exportId, new Handler<Either<String, String>>() {
			@Override
			public void handle(Either<String, String> e) {
				if (e.isRight() && e.right().getValue() != null) {
					request.response().sendFile(e.right().getValue(), deleteHandler(exportId));
				} else if (e.isRight()) {
					waitingExport(request, exportId);
				} else {
					DefaultResponseHandler.leftToResponse(request, e.left());
				}
			}
		});
	}

	private void waitingExport(final HttpServerRequest request, final String exportId) {
		exportService.waitingExport(exportId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					final String address = "export." + exportId;
					eb.registerHandler(address, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							String path = event.body().getString("destZip");
							if ("ok".equals(event.body().getString("status")) && path != null) {
								request.response().sendFile(path, deleteHandler(exportId));
							} else {
								renderError(request, event.body());
							}
							eb.unregisterHandler(address, this);
						}
					});
				} else {
					notFound(request, "exportId.not.found");
				}
			}
		});
	}

	private Handler<AsyncResult<Void>> deleteHandler(final String exportId) {
		return new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					exportService.deleteExport(exportId);
				}
			}
		};
	}

	@BusAddress("entcore.export")
	public void export(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "exported" :
				exportService.exported(
						message.body().getString("exportId"),
						message.body().getString("status"),
						message.body().getString("locale", "fr")
						);
				break;
		}
	}


}
