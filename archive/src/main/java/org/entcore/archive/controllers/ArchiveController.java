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
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.MessageConsumer;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.services.impl.FileSystemExportService;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.user.UserUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;


import java.security.PrivateKey;
import java.util.*;


public class ArchiveController extends BaseController {

	public static final String SIGNATURE_NAME = "archive.signature";

	private ExportService exportService;
	private EventStore eventStore;
	private Storage storage;
	private Map<String, Long> archiveInProgress;
	private PrivateKey signKey;
	private boolean forceEncryption;

	private enum ArchiveEvent { ACCESS }

	public ArchiveController(Storage storage, Map<String, Long> archiveInProgress, PrivateKey signKey, boolean forceEncryption) {
		this.storage = storage;
		this.archiveInProgress = archiveInProgress;
		this.signKey = signKey;
		this.forceEncryption = forceEncryption;
	}

	@Override
	public void init(Vertx vertx, final JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions)
	{
		super.init(vertx, config, rm, securedActions);

		String exportPath = config.getString("export-path", System.getProperty("java.io.tmpdir"));

		EmailFactory emailFactory = new EmailFactory(vertx, config);
		EmailSender notification = config.getBoolean("send.export.email", false) ?
				emailFactory.getSender() : null;

		exportService = new FileSystemExportService(vertx, vertx.fileSystem(),
				eb, exportPath, null, notification, storage, archiveInProgress, new TimelineHelper(vertx, eb, config),
				signKey, forceEncryption);
		eventStore = EventStoreFactory.getFactory().getEventStore(Archive.class.getSimpleName());

		Long periodicUserClear = config.getLong("periodicUserClear");

		if (periodicUserClear != null)
		{
			vertx.setPeriodic(periodicUserClear, new Handler<Long>()
			{
				@Override
				public void handle(Long event)
				{
					final long limit = System.currentTimeMillis() - config.getLong("userClearDelay", 3600000l);
					Set<Map.Entry<String, Long>> entries = new HashSet<>(archiveInProgress.entrySet());

					for (Map.Entry<String, Long> e : entries)
					{
						if (e.getValue() == null || e.getValue() < limit)
						{
							archiveInProgress.remove(e.getKey());
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
		UserUtils.getUserInfos(eb, request, user -> {
			if(user != null) {
				initExport(request, user.getLogin(), user.getUserId());
			}
			else {
				unauthorized(request);
			}
		});
	}

	@Post("/export/user")
	public void exportForUser(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, body -> {
			final String login = body.getString("login");
			final String userId = body.getString("id");
			if (StringUtils.isEmpty(login) || StringUtils.isEmpty(userId)) {
				renderError(request);
				return;
			}
			initExport(request, login, userId);
		});
	}

	private void initExport(final HttpServerRequest request, final String login, final String userId) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				log.info("Début d'export par l'utilisateur " + login);
				eb.send("entcore.export",
						new JsonObject()
								.put("action", "start")
								.put("userId", userId)
								.put("locale", I18n.acceptLanguage(request))
								.put("apps", event.toJsonObject().getJsonArray("apps"))
								.put("exportDocuments", event.toJsonObject().getBoolean("exportDocuments", true))
								.put("request", new JsonObject().put("headers", new JsonObject().put("Host", request.getHeader("Host")))),
						new Handler<AsyncResult<Message<JsonObject>>>() {
							@Override
							public void handle(AsyncResult<Message<JsonObject>> res) {
								if(res.succeeded() == true) {
									JsonObject msg = res.result().body();
									if(msg.getString("status").equals("ok")) {
										log.info("Fin d'export pour l'utilisateur " + login + " exportId: " + msg.getString("exportId"));
										renderJson(request, new JsonObject().put("message", "export.in.progress").put("exportId", msg.getString("exportId")));
									} else {
										log.info("Echec de l'export pour l'utilisateur " + login + " exportId: " + msg.getString("exportId"));
										badRequest(request, msg.getString("message"));
									}
								} else {
									log.info("Echec de l'export pour l'utilisateur " + login);
									badRequest(request, res.cause().getMessage());
								}
							}
						});
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
					final String address = exportService.getExportBusAddress(exportId);
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
	public void downloadExport(final HttpServerRequest request)
	{
		final String exportId = request.params().get("exportId");
		storage.sendFile(exportId, exportId + ".zip", request, false, null, new Handler<AsyncResult<Void>>()
		{
			@Override
			public void handle(AsyncResult<Void> event)
			{
				if (event.succeeded() && request.response().getStatusCode() == 200)
				{
					exportService.deleteExport(exportId);
				}
				else if (!request.response().ended())
				{
					notFound(request);
				}
			}
		});
	}

	@Delete("/export/clear/user/:userId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void clearUserExport(final HttpServerRequest request)
	{
		exportService.clearUserExport(request.params().get("userId"));
		Renders.ok(request);
	}

	@BusAddress("entcore.export")
	public void export(Message<JsonObject> message)
	{
		String action = message.body().getString("action", "");
		switch (action)
		{
			case "start":
				JsonObject body = message.body();

				String userId = body.getString("userId");
				String locale = body.getString("locale");
				JsonArray apps = body.getJsonArray("apps");
				JsonArray resourcesIds = body.getJsonArray("resourcesIds");
				Boolean synchroniseReply = body.getBoolean("synchroniseReply", false);
				Boolean force = body.getBoolean("force", false);
				HttpServerRequest request = new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject()));

				if(userId == null || apps == null || locale == null)
				{
					message.reply(new JsonObject().put("status", "error").put("message", "Missing arguments userId or apps or locale"));
					break;
				}

				UserUtils.getUserInfos(eb, userId, user ->
				{
					if(Boolean.TRUE.equals(force)){
						archiveInProgress.remove(userId);
					}
					exportService.export(user, locale, apps, resourcesIds, request,
						new Handler<Either<String, String>>()
					{
						@Override
						public void handle(Either<String, String> event)
						{
							if (event.isRight() == true)
							{
								String exportId = event.right().getValue();

								if(Boolean.TRUE.equals(synchroniseReply) == false)
								{
									message.reply(
										new JsonObject()
											.put("status", "ok")
											.put("exportId", exportId)
											.put("exportPath", exportId + ".zip")
									);
								}
								else
								{
									final String address = exportService.getExportBusAddress(exportId);

									final MessageConsumer<JsonObject> consumer = eb.consumer(address);
									consumer.handler(new Handler<Message<JsonObject>>()
									{
										@Override
										public void handle(Message<JsonObject> event)
										{
											event.reply(new JsonObject().put("status", "ok").put("sendNotifications", false));
											consumer.unregister();

											message.reply(
												new JsonObject()
													.put("status", "ok")
													.put("exportId", exportId)
													.put("exportPath", exportId + ".zip")
											);
										}
									});
								}
							}
							else
							{
								message.reply(new JsonObject().put("status", "error").put("message", event.left().getValue()));
							}
						}
					});
				});
				break;
			case "delete":
				String exportId = message.body().getString("exportId");

				if(exportId == null)
					message.reply(new JsonObject().put("status", "error").put("message", "Missing argument userId"));
				else
				{
					exportService.deleteExport(exportId);
					message.reply(new JsonObject().put("status", "ok"));
				}
				break;
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

	@Get("/export")
	public void unitaryExport(final HttpServerRequest request)
	{
		final String application = request.params().get("application");
		final String resourceId = request.params().get("resourceId");

		if (application == null || resourceId == null)
				badRequest(request);
		else
		{
			UserUtils.getUserInfos(eb, request, user ->
			{
				if(user != null)
				{
					eb.send("entcore.export",
						new JsonObject()
							.put("action", "start")
							.put("userId", user.getUserId())
							.put("locale", I18n.acceptLanguage(request))
							.put("apps", new JsonArray().add(application))
							.put("resourcesIds", new JsonArray().add(resourceId)),
						new Handler<AsyncResult<Message<JsonObject>>>()
					{
						@Override
						public void handle(AsyncResult<Message<JsonObject>> res)
						{
							if(res.succeeded() == true)
							{
								JsonObject msg = res.result().body();
								if(msg.getString("status").equals("ok"))
									renderJson(request, new JsonObject().put("message", "export.in.progress").put("exportId", msg.getString("exportId")));
								else
									badRequest(request, msg.getString("message"));
							}
							else
								badRequest(request, res.cause().getMessage());
						}
					});
				}
				else
					unauthorized(request);
			});
		}
	}

}
