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

package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.utils.User;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.Zip;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.io.File;
import java.util.*;
import java.util.zip.Deflater;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class FileSystemExportService implements ExportService {

	private final Vertx vertx;
	private final FileSystem fs;
	private final EventBus eb;
	private final String exportPath;
	private final EmailSender notification;
	private final Storage storage;
	private static final Logger log = LoggerFactory.getLogger(FileSystemExportService.class);
	private final Map<String, Long> userExportInProgress;
	private final Map<String, UserExport> userExport;
	private final TimelineHelper timeline;

	private static final long DOWNLOAD_READY = -1l;
	private static final long DOWNLOAD_IN_PROGRESS = -2l;
	private static final long EXPORT_ERROR = -4l;

	public FileSystemExportService(Vertx vertx, FileSystem fs, EventBus eb, String exportPath,
			EmailSender notification, Storage storage,
			Map<String, Long> userExportInProgress, TimelineHelper timeline) {
		this.vertx = vertx;
		this.fs = fs;
		this.eb = eb;
		this.exportPath = exportPath;
		this.notification = notification;
		this.storage = storage;
		this.userExportInProgress = userExportInProgress;
		this.userExport = new HashMap<>();
		this.timeline = timeline;
	}

	@Override
	public void export(final UserInfos user, final String locale, JsonArray apps, final HttpServerRequest request,
					   final Handler<Either<String, String>> handler) {
		userExportExists(user, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.FALSE.equals(event)) {
					long now = System.currentTimeMillis();
					final String exportId = now + "_" +user.getUserId();
					userExportInProgress.put(user.getUserId(), now);
					userExport.put(user.getUserId(), new UserExport(new HashSet<>(apps.getList()), exportId));
					final String exportDirectory = exportPath + File.separator + exportId;
					fs.mkdirs(exportDirectory, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.succeeded()) {
								final Set<String> g = (user.getGroupsIds() != null) ? new
									HashSet<>(user.getGroupsIds()) : new HashSet<String>();
								User.getOldGroups(user.getUserId(), new Handler<JsonArray>() {
									@Override
									public void handle(JsonArray objects) {
										g.addAll(objects.getList());
										JsonObject j = new JsonObject()
												.put("action", "export")
												.put("exportId", exportId)
												.put("userId", user.getUserId())
												.put("groups", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(g)))
												.put("path", exportDirectory)
												.put("locale", locale)
												.put("host", Renders.getScheme(request) + "://" + request.headers().get("Host"))
												.put("apps", apps);
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
	public void userExportId(UserInfos user, Handler<String> handler) {
		String userId = user.getUserId();
		if (userExportInProgress.containsKey(userId)
				&& userExportInProgress.get(userId) != DOWNLOAD_READY) {
			UserExport ue = userExport.get(user.getUserId());
			handler.handle(ue != null ? ue.getExportId() : null);
		} else {
			handler.handle(null);
		}
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
		final String userId = getUserId(exportId);
		if (!userExportInProgress.containsKey(userId)) {
			return;
		}
		final UserExport export = userExport.get(userId);
		final int counter = export.incrementAndGetCounter();
		final boolean isFinished = counter == export.getExpectedExport().size();
		if (!"ok".equals(status)) {
			export.setProgress(EXPORT_ERROR);
		}
		if (isFinished && export.getProgress().longValue() == EXPORT_ERROR) {
			log.error("Error in export " + exportId);
			JsonObject j = new JsonObject()
					.put("status", "error")
					.put("message", "export.error");
			eb.publish("export." + exportId, j);
			userExportInProgress.remove(userId);
			fs.deleteRecursive(exportDirectory, true, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.failed()) {
						log.error("Error deleting directory : " + exportDirectory, event.cause());
					}
				}
			});
			if (notification != null) {
				sendExportEmail(exportId, locale, status, host);
			}
			return;
		}
		if (isFinished) {
			addManifestToExport(exportId, exportDirectory, locale, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					Zip.getInstance().zipFolder(exportDirectory, exportDirectory + ".zip", true,
							Deflater.NO_COMPRESSION, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(final Message<JsonObject> event) {
									if (!"ok".equals(event.body().getString("status"))) {
										log.error("Zip export " + exportId + " error : "
												+ event.body().getString("message"));
										event.body().put("message", "zip.export.error");
										userExportInProgress.remove(userId);
										fs.deleteRecursive(exportDirectory, true, new Handler<AsyncResult<Void>>() {
											@Override
											public void handle(AsyncResult<Void> event) {
												if (event.failed()) {
													log.error("Error deleting directory : " + exportDirectory,
															event.cause());
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
												event.body().put("message", "zip.saving.error");
												userExportInProgress.remove(userId);
												publish(event);
											} else {
												userExportInProgress.put(userId,DOWNLOAD_READY);
												MongoDb.getInstance().save(
														Archive.ARCHIVES, new JsonObject().put("file_id", exportId)
																.put("date", MongoDb.now()),
														new Handler<Message<JsonObject>>() {
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
									eb.send(address, event.body(), new DeliveryOptions().setSendTimeout(5000l),
											new Handler<AsyncResult<Message<JsonObject>>>() {
												@Override
												public void handle(AsyncResult<Message<JsonObject>> res) {
													if ((!res.succeeded() && userExportExists(exportId)
															&& !downloadIsInProgress(exportId))
															|| (res.succeeded()
															&& res.result().body().getBoolean("sendNotifications", false)
															.booleanValue())) {
														if (notification != null) {
															sendExportEmail(exportId, locale,
																	event.body().getString("status"), host);
														} else {
															notifyOnTimeline(exportId, locale,
																	event.body().getString("status"));
														}
													}
												}
											});
								}
							});
				}
			});
		}
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
		MongoDb.getInstance().delete(Archive.ARCHIVES, new JsonObject().put("file_id", exportId));
		String userId = getUserId(exportId);
		userExportInProgress.remove(userId);
	}

	@Override
	public void setDownloadInProgress(String exportId) {
		String userId = getUserId(exportId);
		if (userExportInProgress.containsKey(userId)) {
			userExportInProgress.put(userId,DOWNLOAD_IN_PROGRESS);
		}
	}

	@Override
	public boolean downloadIsInProgress(String exportId) {
		Long v = userExportInProgress.get(getUserId(exportId));
		return v != null && v == DOWNLOAD_IN_PROGRESS;
	}

	private void addManifestToExport(String exportId, String exportDirectory, String locale, Handler<AsyncResult<Void>> handler) {
		LocalMap<String, String> versionMap = vertx.sharedData().getLocalMap("versions");
		JsonObject manifest = new JsonObject();
		Set<String> expectedExport = this.userExport.get(getUserId(exportId)).getExpectedExport();
		this.vertx.eventBus().send("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), json -> {
			JsonObject i18n = (JsonObject)(json.result().body());
			versionMap.forEach((k, v) -> {
				String[] s = k.split("\\.");
				// Removing of "-" for scrapbook
				String app = (s[s.length - 1]).replaceAll("-", "");
				if (expectedExport.contains(app)) {
					String i = i18n.getString(app);
					manifest.put(k, new JsonObject().put("version",v)
							.put("folder", StringUtils.stripAccents(i == null ? app : i)));
				}
			});
			String path = exportDirectory + File.separator + "Manifest.json";
			fs.writeFile(path, Buffer.factory.buffer(manifest.encodePrettily()), handler);
		});
	}

	private String getUserId(String exportId) {
		return exportId.substring(exportId.indexOf('_') + 1);
	}

	private void notifyOnTimeline(String exportId, String locale, String status) {
		final String userId = getUserId(exportId);
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		final JsonObject params = new JsonObject()
				.put("resourceUri", "/archive/export/" + exportId)
				.put("resourceName", exportId + ".zip");

		timeline.notifyTimeline(new JsonHttpServerRequest(new JsonObject().put("headers", new JsonObject().put("Accept-Language", locale))),
				"archive.archives" +  "_" + status, null, recipients, params);
	}

	private void sendExportEmail(final String exportId, final String locale, final String status, final String host) {
		final String userId = getUserId(exportId);
		String query = "MATCH (u:User {id : {userId}}) RETURN u.email as email ";
		JsonObject params = new JsonObject().put("userId", userId);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					JsonObject e = res.getJsonObject(0);
					String email = e.getString("email");
					if (email != null && !email.trim().isEmpty()) {
						HttpServerRequest r = new JsonHttpServerRequest(new JsonObject()
								.put("headers", new JsonObject().put("Accept-Language", locale)));
						String subject, template;
						JsonObject p = new JsonObject();
						if ("ok".equals(status)) {
							subject = "email.export.ok";
							template = "email/export.ok.html";
							p.put("download", host + "/archive/export/" + exportId);
							if (log.isDebugEnabled()) {
								log.debug(host + "/archive/export/" + exportId);
							}
						} else {
							subject = "email.export.ko";
							template = "email/export.ko.html";
						}
						notification.sendEmail(r, email, null, null, subject, template, p, true,
								handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (event == null || !"ok".equals(event.body().getString("status"))) {
									log.error("Error sending export email for user " + userId);
								}
							}
						}));
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
