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

package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.utils.User;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.Zip;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.zip.Deflater;

public class FileSystemExportService implements ExportService {

	private final FileSystem fs;
	private final EventBus eb;
	private final String exportPath;
	private final Set<String> expectedExports;
	private final EmailSender notification;
	private final Storage storage;
	private static final Logger log = LoggerFactory.getLogger(FileSystemExportService.class);
	private final Map<String, Long> userExportInProgress;
	private final TimelineHelper timeline;

	public FileSystemExportService(FileSystem fs, EventBus eb, String exportPath, Set<String> expectedExports,
			EmailSender notification, Storage storage, Map<String, Long> userExportInProgress, TimelineHelper timeline) {
		this.fs = fs;
		this.eb = eb;
		this.exportPath = exportPath;
		this.expectedExports = expectedExports;
		this.notification = notification;
		this.storage = storage;
		this.userExportInProgress = userExportInProgress;
		this.timeline = timeline;
	}

	@Override
	public void export(final UserInfos user, final String locale, final HttpServerRequest request,
			final Handler<Either<String, String>> handler) {
		userExportExists(user, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.FALSE.equals(event)) {
					long now = System.currentTimeMillis();
					final String exportId = now + "_" +user.getUserId();
					userExportInProgress.put(user.getUserId(), now);
					final String exportDirectory = exportPath + File.separator + exportId;
					fs.mkdir(exportDirectory, true, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.succeeded()) {
								final Set<String> g = (user.getGroupsIds() != null) ? new
									HashSet<>(user.getGroupsIds()) : new HashSet<String>();
								User.getOldGroups(user.getUserId(), new Handler<JsonArray>() {
									@Override
									public void handle(JsonArray objects) {
										g.addAll(objects.toList());
										JsonObject j = new JsonObject()
												.putString("action", "export")
												.putString("exportId", exportId)
												.putString("userId", user.getUserId())
												.putArray("groups", new JsonArray(g.toArray()))
												.putString("path", exportDirectory)
												.putString("locale", locale)
												.putString("host", Renders.getScheme(request) + "://" + request.headers().get("Host"));
										eb.publish("user.repository", j);
										handler.handle(new Either.Right<String, String>(exportId));
									}
								});
							} else {
								log.error("Create export directory error.", event.cause());
								handler.handle(new Either.Left<String, String>("export.directory.create.error"));
							}
						}
					});
				} else {
					handler.handle(new Either.Left<String, String>("export.exists"));
				}
			}
		});
	}

	@Override
	public void userExportExists(UserInfos user, final Handler<Boolean> handler) {
		handler.handle(userExportInProgress.containsKey(user.getUserId()));
	}

	@Override
	public boolean userExportExists(String exportId) {
		return userExportInProgress.containsKey(getUserId(exportId));
	}

	@Override
	public void waitingExport(String exportId, final Handler<Boolean> handler) {
		Long v = userExportInProgress.get(getUserId(exportId));
		handler.handle(v != null && v > 0);
	}

	@Override
	public void exportPath(String exportId, final Handler<Either<String, String>> handler) {
		final String path = exportPath + File.separator + exportId + ".zip";
		fs.exists(path, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> event) {
				if (event.succeeded()) {
					if (Boolean.TRUE.equals(event.result())) {
						handler.handle(new Either.Right<String, String>(path));
					} else {
						handler.handle(new Either.Right<String, String>(null));
					}
				} else {
					log.error("Export exists error.", event.cause());
					handler.handle(new Either.Left<String, String>("get.export.path.error"));
				}
			}
		});
	}

	@Override
	public void exported(final String exportId, String status, final String locale, final String host) {
		log.debug("Exported method");
		if (exportId == null) {
			log.error("Export receive event without exportId ");
			return;
		}
		final String exportDirectory = exportPath + File.separator + exportId;
		if (!"ok".equals(status)) {
			log.error("Error in export " + exportId);
			JsonObject j = new JsonObject()
					.putString("status", "error")
					.putString("message", "export.error");
			eb.publish("export." + exportId, j);
			fs.delete(exportDirectory, true, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.failed()) {
						log.error("Error deleting directory : " + exportDirectory, event.cause());
					}
				}
			});
			userExportInProgress.remove(getUserId(exportId));
			if (notification != null) {
				sendExportEmail(exportId, locale, status, host);
			}
			return;
		}

		log.debug("Read export directory");
		fs.readDir(exportDirectory, new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(AsyncResult<String[]> event) {
				if (event.succeeded()) {
					if (log.isDebugEnabled()) {
						log.debug("read export directory : ok - Result length : "
								+ event.result().length + ", expected " + expectedExports.size());
					}
					if (event.result().length == expectedExports.size()) {
						Zip.getInstance().zipFolder(exportDirectory, exportDirectory + ".zip", true,
								Deflater.NO_COMPRESSION, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(final Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									log.error("Zip export " + exportId + " error : " +
											event.body().getString("message"));
									event.body().putString("message", "zip.export.error");
									userExportInProgress.remove(getUserId(exportId));
									fs.delete(exportDirectory, true, new Handler<AsyncResult<Void>>() {
										@Override
										public void handle(AsyncResult<Void> event) {
										if (event.failed()) {
											log.error("Error deleting directory : " + exportDirectory, event.cause());
										}
										}
									});
									publish(event);
								} else {
									storeZip(event);
								}
							}

							private void storeZip(final Message<JsonObject> event) {
								storage.writeFsFile(exportId, exportDirectory + ".zip", new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject res) {
										if (!"ok".equals(res.getString("status"))) {
											log.error("Zip storage " + exportId + " error : "
													+ res.getString("message"));
											event.body().putString("message", "zip.saving.error");
											userExportInProgress.remove(getUserId(exportId));
											publish(event);
										} else {
											userExportInProgress.put(getUserId(exportId), -1l);
											MongoDb.getInstance().save(Archive.ARCHIVES, new JsonObject()
													.putString("file_id", exportId)
													.putObject("date", MongoDb.now()), new Handler<Message<JsonObject>>() {
												@Override
												public void handle(Message<JsonObject> res) {
													publish(event);
												}
											});
										}
										deleteTempZip(exportId);
									}
								});
							}

							public void deleteTempZip(final String exportId) {
								final String path = exportPath + File.separator + exportId + ".zip";
								fs.delete(path, new Handler<AsyncResult<Void>>() {
									@Override
									public void handle(AsyncResult<Void> event) {
										if (event.failed()) {
											log.error("Error deleting temp zip export " + exportId, event.cause());
										}
									}
								});
							}

							private void publish(final Message<JsonObject> event) {
								final String address = "export." + exportId;
								eb.sendWithTimeout(address, event.body(), 5000l,
										new Handler<AsyncResult<Message<JsonObject>>>() {

									@Override
									public void handle(AsyncResult<Message<JsonObject>> res) {
										if (!res.succeeded() && userExportExists(exportId) && !downloadIsInProgress(exportId)) {
											if (notification != null) {
												sendExportEmail(exportId, locale, event.body().getString("status"), host);
											} else {
												notifyOnTimeline(exportId, locale, event.body().getString("status"));
											}
										}
									}
								});

							}
						});
					}
				} else {
					log.error("Error listing export directory " + exportId, event.cause());
				}
			}
		});
	}

	@Override
	public void deleteExport(final String exportId) {
		storage.removeFile(exportId, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (!"ok".equals(event.getString("status"))) {
					log.error("Error deleting export " + exportId  + ". - " + event.getString("message"));
				}
			}
		});
		MongoDb.getInstance().delete(Archive.ARCHIVES, new JsonObject().putString("file_id", exportId));
		String userId = getUserId(exportId);
		userExportInProgress.remove(userId);
	}

	@Override
	public void setDownloadInProgress(String exportId) {
		String userId = getUserId(exportId);
		if (userExportInProgress.containsKey(userId)) {
			userExportInProgress.put(userId, -2l);
		}
	}

	@Override
	public boolean downloadIsInProgress(String exportId) {
		Long v = userExportInProgress.get(getUserId(exportId));
		return v != null && v == -2l;
	}

	private String getUserId(String exportId) {
		return exportId.substring(exportId.indexOf('_') + 1);
	}


	private void notifyOnTimeline(String exportId, String locale, String status) {
		final String userId = getUserId(exportId);
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		final JsonObject params = new JsonObject()
				.putString("resourceUri", "/archive/export/" + exportId)
				.putString("resourceName", exportId + ".zip");

		timeline.notifyTimeline(new JsonHttpServerRequest(new JsonObject().putObject("headers", new JsonObject().putString("Accept-Language", locale))),
				"archive.archives" +  "_" + status, null, recipients, params);
	}

	private void sendExportEmail(final String exportId, final String locale, final String status, final String host) {
		final String userId = getUserId(exportId);
		String query = "MATCH (u:User {id : {userId}}) RETURN u.email as email ";
		JsonObject params = new JsonObject().putString("userId", userId);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					JsonObject e = res.get(0);
					String email = e.getString("email");
					if (email != null && !email.trim().isEmpty()) {
						HttpServerRequest r = new JsonHttpServerRequest(new JsonObject()
								.putObject("headers", new JsonObject().putString("Accept-Language", locale)));
						String subject, template;
						JsonObject p = new JsonObject();
						if ("ok".equals(status)) {
							subject = "email.export.ok";
							template = "email/export.ok.html";
							p.putString("download", host + "/archive/export/" + exportId);
							if (log.isDebugEnabled()) {
								log.debug(host + "/archive/export/" + exportId);
							}
						} else {
							subject = "email.export.ko";
							template = "email/export.ko.html";
						}
						notification.sendEmail(r, email, null, null, subject, template, p, true,
								new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (event == null || !"ok".equals(event.body().getString("status"))) {
									log.error("Error sending export email for user " + userId);
								}
							}
						});
					} else {
						log.info("User " + userId + " hasn't email.");
					}
				} else if (res != null) {
					log.warn("User " + userId + " not found.");
				} else {
					log.error("Error finding user " + userId +
							" email : " + event.body().getString("message"));
				}
			}
		});
	}

}
