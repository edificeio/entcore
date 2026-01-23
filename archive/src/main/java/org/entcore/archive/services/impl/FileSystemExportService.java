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
import fr.wseduc.webutils.security.RSA;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.archive.Archive;
import org.entcore.archive.controllers.ArchiveController;
import org.entcore.archive.services.ExportService;
import org.entcore.archive.utils.User;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.Zip;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;

import java.io.File;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

import static com.google.common.collect.Lists.newArrayList;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.json.JsonObject.mapFrom;

public class FileSystemExportService implements ExportService {

	private final Vertx vertx;
	private final FileSystem fs;
	private final EventBus eb;
	private final String exportPath;
	private final String handlerActionName;
	private final EmailSender notification;
	private final Storage storage;
	private static final Logger log = LoggerFactory.getLogger(FileSystemExportService.class);
	private AsyncMap<String, Long> userExportInProgress;
	private AsyncMap<String, JsonObject> userExport;
	private final TimelineHelper timeline;
	private final PrivateKey signKey;
	private final boolean forceEncryption;

	private static final long DOWNLOAD_READY = -1l;
	private static final long DOWNLOAD_IN_PROGRESS = -2l;
	private static final long EXPORT_ERROR = -4l;

    private static final String EXPORT_LOCK_PREFIX = "EXPORT_LOCK";
    private static final long EXPORT_LOCK_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES).toMillis();

	public FileSystemExportService(Vertx vertx, FileSystem fs, EventBus eb, String exportPath, String customHandlerActionName,
								   EmailSender notification, Storage storage, TimelineHelper timeline,
								   PrivateKey signKey, boolean forceEncryption) {
		this.vertx = vertx;
		this.fs = fs;
		this.eb = eb;
		this.exportPath = exportPath;
		this.handlerActionName = customHandlerActionName == null ? "export" : customHandlerActionName;
		this.notification = notification;
		this.storage = storage;
		vertx.sharedData().<String, Long>getAsyncMap("userExportInProgress").onSuccess(map -> this.userExportInProgress = map);
    vertx.sharedData().<String, JsonObject>getAsyncMap("userExport").onSuccess(map -> this.userExport = map);
		this.timeline = timeline;
		this.signKey = signKey;
		this.forceEncryption = forceEncryption;
	}

	@Override
	public void export(final UserInfos user, final String locale, JsonArray apps, JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources,
						final HttpServerRequest request, final Handler<Either<String, String>> handler)
	{
		userExportExists(user, new Handler<Boolean>()
		{
			@Override
			public void handle(Boolean event)
			{
				if (Boolean.FALSE.equals(event))
				{
					long now = System.currentTimeMillis();
					final String exportId = now + "_" +user.getUserId();

					MongoDb.getInstance().save(
							Archive.ARCHIVES, new JsonObject().put("file_id", exportId)
									.put("date", MongoDb.now()), res -> {
								if ("ok".equals(res.body().getString("status"))) {
                  final List<Future<Void>> futures = newArrayList(
                    userExportInProgress.put(user.getUserId(), now),

                    userExport.put(user.getUserId(), mapFrom(new UserExport(apps.getList(), exportId)))
                  );
                  Future.all(futures)
                    .onFailure(th -> {
                      log.error("An error occurred while putting an export in the asyncmap userExport", th);
                      handler.handle(new Either.Left<>(th.getMessage()));
                    })
                    .onSuccess(e -> {
                      final String exportDirectory = exportPath + File.separator + exportId;
                      fs.mkdirs(exportDirectory, new Handler<AsyncResult<Void>>()
                      {
                        @Override
                        public void handle(AsyncResult<Void> event)
                        {
                          if (event.succeeded())
                          {
                            final Set<String> g = (user.getGroupsIds() != null) ? new
                              HashSet<>(user.getGroupsIds()) : new HashSet<String>();
                            User.getOldGroups(user.getUserId(), new Handler<JsonArray>()
                            {
                              @Override
                              public void handle(JsonArray objects)
                              {
                                g.addAll(objects.getList());
                                JsonObject j = new JsonObject()
                                  .put("action", handlerActionName)
                                  .put("exportId", exportId)
                                  .put("userId", user.getUserId())
                                  .put("groups", new JsonArray(new ArrayList<>(g)))
                                  .put("path", exportDirectory)
                                  .put("locale", locale)
                                  .put("host", request == null || request.headers() == null ? "" : Renders.getScheme(request) + "://" + request.headers().get("Host"))
                                  .put("apps", apps)
                                  .put("exportDocuments", exportDocuments)
                                  .put("exportSharedResources", exportSharedResources)
                                  .put("resourcesIds", resourcesIds);
                                eb.publish("user.repository", j);
                                handler.handle(new Either.Right<String, String>(exportId));
                              }
                            });
                          }
                          else
                          {
                            log.error("Create export directory error.", event.cause());
                            handler.handle(new Either.Left<String, String>("export.directory.create.error"));
                          }
                        }
                      });
                    });

								} else {
									log.error("Cannot create mongo document in archives");
									handler.handle(new Either.Left<String, String>("export.directory.create.error"));
								}
							});
				}
				else
				{
					handler.handle(new Either.Left<String, String>("export.exists"));
				}
			}
		});
	}

	@Override
	public void userExportExists(UserInfos user, final Handler<Boolean> handler) {
    userExportInProgress.keys()
    .onSuccess(keys -> handler.handle(keys.contains(user.getUserId())))
    .onFailure(th -> {
      log.error("An error occurred while fetching userExportInProgress by id for user " + user.getUserId(), th);
      handler.handle(null);
    });
	}

	@Override
	public void userExportId(UserInfos user, Handler<String> handler) {
		String userId = user.getUserId();
    userExportInProgress.get(userId)
      .compose(progress -> {
        if (progress != null && progress != DOWNLOAD_READY) {
          return userExport.get(user.getUserId());
        }
        return succeededFuture(null);
      })
      .map(UserExport::fromJson)
      .onSuccess(ue -> handler.handle(ue == null ? null : ue.getExportId()))
      .onFailure(th -> {
        log.error("An error occurred while fetching userExport by id for user " + user.getUserId(), th);
        handler.handle(null);
      });
	}


	@Override
	public Future<Boolean> userExportExists(String exportId) {
    return userExportInProgress.get(getUserId(exportId))
      .map(Objects::nonNull);
	}

	@Override
	public void waitingExport(String exportId, final Handler<Boolean> handler) {
		userExportInProgress.get(getUserId(exportId))
      .onSuccess(v -> handler.handle(v != null && v > 0))
      .onFailure(th -> {
        log.error("An error occurred while fetching userExportInProgress by id for " + exportId, th);
        handler.handle(false);
      })
		;
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
	public void onExportDone(final String exportId, String status, final String locale, final String host, final String app) {
		log.debug("Exported method");
		if (exportId == null) {
			log.error("Export receive event without exportId ");
			return;
		}
		final String exportDirectory = exportPath + File.separator + exportId;
		final String userId = getUserId(exportId);
        this.vertx.sharedData().getLockWithTimeout(EXPORT_LOCK_PREFIX + "_" + exportId, EXPORT_LOCK_TIMEOUT)
            .onFailure(th -> log.error("An error occurred while getting a log for export " + exportId + " of user " + userId))
            .onSuccess(lock -> {
                userExport.get(userId)
                        .onFailure(th -> {
                            log.error("Cannot get userExport " + exportId + " of user " + userId, th);
                            lock.release();
                        })
                        .map(UserExport::fromJson)
                        .flatMap(export -> {
                            if (export == null) {
                                log.warn("Received a notification about a finished export (" + exportId + ") but this export could not be found");
                                lock.release();
                                return succeededFuture();
                            }
                            final Map<String, Boolean> states = export.getStateByModule();
                            states.put(app, true);
                            if (export.isFinished()) {
                                log.debug("Export " + exportId + " finished for user " + userId);
                            } else {
                                final String stillProcessingApps = states.entrySet().stream()
                                        .filter(exported -> !exported.getValue())
                                        .map(e -> e.getKey())
                                        .collect(Collectors.joining(","));
                                log.info("Still waiting for apps [" + stillProcessingApps + "] for export " + exportId + " of user " + userId);
                            }
                            if (!"ok".equals(status)) {
                                log.warn("Export from '" + app + "' is '" + status+ "' for export '" + exportId + "' of user '" + userId + "'");
                                export.setProgress(EXPORT_ERROR);
                            }
                            return userExport.put(userId, mapFrom(export)).map(export);
                        })
                        .onFailure(th -> {
                            log.error("Cannot update userExport " + exportId + " of user " + userId, th);
                            lock.release();
                        })
                        .onSuccess(export -> {
                            lock.release();
                            final boolean isFinished = export.isFinished();
                            if (isFinished && export.getProgress() == EXPORT_ERROR) {
                                log.error("Error in export " + exportId);
                                JsonObject errorPayload = new JsonObject()
                                        .put("status", "error")
                                        .put("message", "export.error");
                                eb.publish(getExportBusAddress(exportId), errorPayload);
                                userExportInProgress.remove(userId);
                                storage.deleteRecursive(exportDirectory)
                                        .onComplete(e -> log.debug("Deletion of " + exportDirectory + " is ok ? " + e.succeeded()));
                                fs.deleteRecursive(exportDirectory, true, event -> {
                                    if (event.failed()) {
                                        log.error("Error deleting directory : " + exportDirectory, event.cause());
                                    }
                                });
                                if (notification != null) {
                                    sendExportEmail(exportId, locale, status, host);
                                }
                                return;
                            }
                            if (isFinished) {
                                log.debug("Export " + exportId + " is finished and OK", exportId);
                                // Copy what the different modules produced to this service's file system (because if the modules exported to S3)
                                // then archive cannot access the export files.
                                storage.moveDirectoryToFs(exportDirectory, exportDirectory)
                                        .onFailure(th -> {
                                            JsonObject errorPayload = new JsonObject()
                                                    .put("status", "error")
                                                    .put("message", "export.error");
                                            eb.publish(getExportBusAddress(exportId), errorPayload);
                                            userExportInProgress.remove(userId);
                                            storage.deleteRecursive(exportDirectory)
                                                    .onComplete(e -> log.debug("Deletion of " + exportDirectory + " is ok ? " + e.succeeded()));
                                            fs.deleteRecursive(exportDirectory, true, event -> {
                                                if (event.failed()) {
                                                    log.error("Error deleting directory : " + exportDirectory, event.cause());
                                                }
                                            });
                                            log.error("Error while retrieving exported files", th); // TODO jber purge download;
                                        })
                                        .onSuccess(e  ->
                                                addManifestToExport(exportId, exportDirectory, locale, event -> {
                                                    log.debug("Manifest added for export " + exportId);
                                                    signExport(exportId, exportDirectory, signed -> {
                                                        log.debug("Zipping export " + exportId);
                                                        Zip.getInstance().zipFolder(exportDirectory, exportDirectory + ".zip", true,
                                                                Deflater.NO_COMPRESSION, zipResult -> {
                                                                    if (!"ok".equals(zipResult.body().getString("status")) || signed.failed()) {
                                                                        log.error("Zip export " + exportId + " error : "
                                                                                + (signed.failed() ? "Could not sign the archive" : zipResult.body().getString("message")));
                                                                        zipResult.body().put("message", "zip.export.error");
                                                                        userExport.remove(userId);
                                                                        userExportInProgress.remove(userId);
                                                                        fs.deleteRecursive(exportDirectory, true, event2 -> {
                                                                            if (event2.failed()) {
                                                                                log.error("Error deleting directory : " + exportDirectory,
                                                                                        event2.cause());
                                                                            }
                                                                        });
                                                                        publish(zipResult, exportId, locale, host);
                                                                    } else {
                                                                        log.debug("Storing export zip in file storage");
                                                                        storeZip(zipResult, exportId, exportDirectory, userId, locale, host);
                                                                    }
                                                                });
                                                    });
                                                }));
                            }
                        });
            });
        }

  // TODO check if that should not be conditional when using S3
  private void storeZip(final Message<JsonObject> event, final String exportId, final String exportDirectory, final String userId, final String locale, final String host) {
    log.debug("Starting to upload exported archive " + exportId + " to fs.....");
    storage.writeFsFile(exportId, exportDirectory + ".zip", new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject res) {
        if (!"ok".equals(res.getString("status"))) {
          log.error("Zip storage " + exportId + " error : "
            + res.getString("message"));
          event.body().put("message", "zip.saving.error");
          userExportInProgress.remove(userId);
          publish(event, exportId, locale, host);
        } else {
          log.debug("Exported archive " + exportId + " uploaded");
          userExportInProgress.put(userId, DOWNLOAD_READY);
          publish(event, exportId, locale, host);
        }
        deleteTempZip(exportId);
      }
    });
  }

  public void deleteTempZip(final String exportId1) {
    final String path = exportPath + File.separator + exportId1 + ".zip";
    log.debug("Deleting temp exported archive " + path);
    fs.delete(path, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        if (event.failed()) {
          log.error("Error deleting temp zip export " + exportId1, event.cause());
        } else {
          log.debug("Temp archive " + path + " deleted");
        }
      }
    });
  }

  private void publish(final Message<JsonObject> event, final String exportId, final String locale, final String host) {
    final String address = getExportBusAddress(exportId);
    log.debug("Notifying that export " + exportId + " is done with body " + event.body().encodePrettily());
    eb.request(address, event.body(), new DeliveryOptions().setSendTimeout(5000l),
      (Handler<AsyncResult<Message<JsonObject>>>) res -> {
        final List<Future<Boolean>> futures = newArrayList(
          userExportExists(exportId),
          downloadIsInProgress(exportId)
        );
        Future.all(futures).onSuccess(checks -> {
          if ((!res.succeeded() && checks.<Boolean>resultAt(0)
            && !checks.<Boolean>resultAt(1))
            || (res.succeeded()
            && res.result().body().getBoolean("sendNotifications", false))) {
            if (notification != null) {
              sendExportEmail(exportId, locale,
                event.body().getString("status"), host);
            } else {
              notifyOnTimeline(exportId, locale,
                event.body().getString("status"));
            }
          }
        });
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
		MongoDb.getInstance().delete(Archive.ARCHIVES, new JsonObject().put("file_id", exportId));
		String userId = getUserId(exportId);
        userExport.remove(userId);
		userExportInProgress.remove(userId);
	}

	@Override
	public void clearUserExport(String userId)
	{
		userExportInProgress.remove(userId);
	}

  @Override
  public Future<Map<String, Long>> getUserExportInProgress() {
    return userExportInProgress.entries();
  }

  @Override
  public Future<Void> removeUserExportInProgress(String key) {
    return userExportInProgress.remove(key).mapEmpty();
  }

  @Override
	public Future<Void> setDownloadInProgress(String exportId) {
    return this.userExportExists(exportId).compose(userExport -> {
      if(userExport != null) {
        final String userId = getUserId(exportId);
        return userExportInProgress.put(userId,DOWNLOAD_IN_PROGRESS);
      }
      return succeededFuture();
    });
	}

	@Override
	public Future<Boolean> downloadIsInProgress(String exportId) {
		return userExportInProgress.get(getUserId(exportId))
      .map(v -> v != null && v == DOWNLOAD_IN_PROGRESS);
	}

	private void addManifestToExport(String exportId, String exportDirectory, String locale, Handler<AsyncResult<Void>> handler)
	{
		JsonObject manifest = new JsonObject();
		this.userExport.get(getUserId(exportId))
      .onFailure(th -> {
        log.error("An error occurred while getting userUserport for " + exportId, th);
        handler.handle(failedFuture(th));
      })
      .map(UserExport::fromJson)
      .onSuccess(e -> {
        final Set<String> expectedExport = e.getStateByModule().keySet();
        this.vertx.eventBus().request("portal", new JsonObject().put("action", "getI18n").put("acceptLanguage", locale), json -> {
          vertx.sharedData().<String, String>getAsyncMap("versions")
            .compose(AsyncMap::entries).onSuccess(versionMap -> {
              JsonObject i18n = (JsonObject) (json.result().body());
              versionMap.forEach((k, v) ->
              {
                String[] s = k.split("\\.");
                // Removing of "-" for scrapbook
                String app = (s[s.length - 1]).replaceAll("-", "");

                if (expectedExport.contains(app)) {
                  String i = i18n.getString(app);
                  manifest.put(k, new JsonObject().put("version", v)
                    .put("folder", StringUtils.stripAccents(i == null ? app : i)));
                }
              });

              String path = exportDirectory + File.separator + "Manifest.json";
              fs.writeFile(path, Buffer.buffer(manifest.encodePrettily()), handler);
            }).onFailure(ex -> {
              log.error("Error getting versions map to add export manifest", ex);
              handler.handle(failedFuture(ex));
            });
        });
      });
	}

	private void signExport(String exportId, String exportDirectory, Handler<AsyncResult<Void>> handler)
	{
		if(this.signKey == null)
		{
			if(this.forceEncryption)
			{
				log.error("No signing key for export " + exportId);
				handler.handle(failedFuture("No signing key"));
			}
			else
			{
				handler.handle(succeededFuture());
			}
			return;
		}

		File directory = new File(exportDirectory);
		JsonObject signContents = new JsonObject();

		File[] files = directory.listFiles();

		vertx.executeBlocking(() -> {
		for (File file : files) {
			  String name = file.getName();
				try {
					signContents.put(name, RSA.signFile(exportDirectory + File.separator + name, signKey));
				} catch(Exception e) {
					log.error("Error signing folder " + name + " files for export " + exportId);
					return failedFuture(e);
				}
			}
			return succeededFuture();
		}, false)
		.onSuccess(e -> fs.writeFile(exportDirectory + File.separator + ArchiveController.SIGNATURE_NAME, signContents.toBuffer(), handler))
		.onFailure(th -> handler.handle(failedFuture(th)));
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

	@Override
	public String getExportBusAddress(String exportId)
	{
		return "export." + exportId;
	}



}
