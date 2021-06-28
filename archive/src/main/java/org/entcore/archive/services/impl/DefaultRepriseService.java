package org.entcore.archive.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.entcore.archive.services.RepriseService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserDataSync;
import org.entcore.common.user.UserInfos;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import fr.wseduc.bus.BusAddress;
import org.entcore.archive.services.ImportService;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultRepriseService implements RepriseService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRepriseService.class);
    private static final String FEEDER_BUS_ADDRESS = "entcore.feeder";

    private final EventBus eb;
    private final Storage storage;
    private final WebClient client;
    private final JsonObject reprise;
    private final JsonObject archiveConfig;
    private final ImportService importService;

    public DefaultRepriseService(Vertx vertx, Storage storage, JsonObject reprise, JsonObject archiveConfig, ImportService importService) {
        this.eb = vertx.eventBus();
        this.storage = storage;
        this.client = WebClient.create(vertx);
        this.reprise = reprise;
        this.archiveConfig = archiveConfig;
        this.importService = importService;
    }

    @Override
    public void launchExportForUsersFromOldPlatform(final boolean relativePersonnelFirst) {
        final Promise<JsonArray> promise = Promise.promise();
        final JsonObject matcher = new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ACTIVATED);
        if (relativePersonnelFirst) {
            matcher.put(UserDataSync.PROFILE_FIELD, new JsonObject().put("$in", Arrays.asList(new String[]{UserDataSync.TEACHER_PROFILE, UserDataSync.RELATIVE_PROFILE})));
        }
        final JsonObject keys = new JsonObject().put("login", 1).put("_old_id", 1);
        final JsonObject sort = new JsonObject().put(UserDataSync.EXPORT_ATTEMPTS_FIELD, 1).put("modified", 1);
        final Integer limit = this.reprise.getInteger("limit", 1);
        final JsonObject action = new JsonObject()
                .put("action", "find-users-old-platform")
                .put("matcher", matcher)
                .put("keys", keys)
                .put("sort", sort)
                .put("limit", limit);
        eb.request(FEEDER_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray users = body.getJsonArray("results");
                if (users.isEmpty() && relativePersonnelFirst) {
                    // There are no more teachers and relatives to export, we relaunch it for other profiles
                    launchExportForUsersFromOldPlatform(false);
                } else {
                    promise.complete(users);
                }
            } else {
                promise.fail(body.getString("message"));
            }
        }));
        promise.future().compose(users -> {
            final Promise<JsonArray> incrementTryPromise = Promise.promise();
            if (users.isEmpty()) {
                incrementTryPromise.complete(users);
            } else {
                final List<String> userIds = users.stream().map(user -> ((JsonObject)user).getString("_old_id")).collect(Collectors.toList());
                final JsonObject criteria = new JsonObject().put(UserDataSync.OLD_ID_FIELD, new JsonObject().put("$in", userIds));
                final JsonObject update = new JsonObject().put("$inc", new JsonObject().put(UserDataSync.EXPORT_ATTEMPTS_FIELD, 1));
                JsonObject action2 = new JsonObject()
                        .put("action", "update-users-old-platform")
                        .put("criteria", criteria)
                        .put("update", update);
                eb.request(FEEDER_BUS_ADDRESS, action2, handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        incrementTryPromise.complete(users);
                    } else {
                        incrementTryPromise.fail(body.getString("message"));
                    }
                }));
            }
            return incrementTryPromise.future();
        }).onComplete(asyncUsers -> {
            if (asyncUsers.succeeded()) {
                JsonArray users = asyncUsers.result();
                for (int i = 0; i < users.size(); i++) {
                    final JsonObject user = users.getJsonObject(i);
                    final String userId = user.getString("_old_id");
                    final String login = user.getString("login");
                    log.info("[Reprise] Export for " + login + " (" + userId + ") has started");
                    Future<String> export = this.launchExportForUser(userId, login);
                    export.onComplete(asyncResult -> {
                        final JsonObject criteria = new JsonObject().put(UserDataSync.OLD_ID_FIELD, user.getString("_old_id"));
                        final JsonObject set = new JsonObject();
                        if (asyncResult.failed()) {
                            set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ERROR_EXPORT);
                            log.error("[Reprise] Export for " + login + " (" + userId + ") failed: " + asyncResult.cause().toString());
                        } else {
                            final String exportId = asyncResult.result();
                            set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.EXPORTED).put(UserDataSync.EXPORT_ID_FIELD, exportId);
                            log.info("[Reprise] Export for " + login + " (" + userId + ") succeeded");
                        }
                        final JsonObject update = new JsonObject().put("$set", set);
                        JsonObject action3 = new JsonObject()
                                .put("action", "update-users-old-platform")
                                .put("criteria", criteria)
                                .put("update", update);
                        eb.request(FEEDER_BUS_ADDRESS, action3, handlerToAsyncHandler(message -> {
                            JsonObject body = message.body();
                            if (!"ok".equals(body.getString("status"))) {
                                log.error("[Reprise] Error updating " + login + " (" + userId + ") export status on \"oldplatformusers\":" + body.getString("message"));
                                eventStore.createAndStoreEvent(RepriseEvent.EXPORT_ERROR.name(),
								    (UserInfos) null, new JsonObject().put("user-old-id", userId).put("user-login", login));
                            } else {
                                log.info("[Reprise] " + login + " (" + userId + ") export status has been updated on \"oldplatformusers\"");
                                eventStore.createAndStoreEvent(RepriseEvent.EXPORT_OK.name(),
								    (UserInfos) null, new JsonObject().put("user-old-id", userId).put("user-login", login));
                            }
                        }));
                    });
                }
            } else {
                log.error("[Reprise] Error on export task: " + asyncUsers.cause().toString());
            }
        });
    }

    private Future<String> launchExportForUser(final String userId, final String login) {
        final Promise<String> promise = Promise.promise();
        final String reprisePlatformURL = this.reprise.getString("platform-url");
        final String basicAuthCredential = this.reprise.getString("basic-auth-credential");
        HttpRequest<Buffer> httpRequest1 = client.postAbs(reprisePlatformURL + "/archive/export/user");
        httpRequest1.putHeader("Authorization", "Basic " + basicAuthCredential);
        final JsonObject body = new JsonObject()
                .put("login", login)
                .put("userId", userId)
                .put("exportDocuments", this.reprise.getBoolean("export-documents", true))
                .put("exportSharedResources", this.reprise.getBoolean("export-shared-resources", true));
        final Promise<String> initExportPromise = Promise.promise();
        httpRequest1.sendJsonObject(body, asyncResult -> {
            if (asyncResult.failed()) {
                initExportPromise.fail(asyncResult.cause());
            } else {
                HttpResponse<Buffer> httpResponse = asyncResult.result();
                final String exportId = httpResponse.bodyAsJsonObject().getString("exportId");
                initExportPromise.complete(exportId);
            }
        });
        initExportPromise.future().compose(exportId -> {
            HttpRequest<Buffer> httpRequest2 = client.getAbs(reprisePlatformURL + "/archive/export/verify/" + exportId);
            httpRequest2.putHeader("Authorization", "Basic " + basicAuthCredential);
            final Promise<String> verifyExportPromise = Promise.promise();
            httpRequest2.send(asyncResult -> {
                if (asyncResult.failed()) {
                    verifyExportPromise.fail(asyncResult.cause());
                } else {
                    verifyExportPromise.complete(exportId);
                }
            });
            return verifyExportPromise.future();
        }).compose(exportId -> {
            HttpRequest<Buffer> httpRequest3 = client.getAbs(reprisePlatformURL + "/archive/export/" + exportId);
            httpRequest3.putHeader("Authorization", "Basic " + basicAuthCredential);
            final Promise<String> downloadExportPromise = Promise.promise();
            httpRequest3.send(asyncResult -> {
                if (asyncResult.failed()) {
                    downloadExportPromise.fail(asyncResult.cause());
                } else {
                    final Buffer archive = asyncResult.result().body();
                    final String filename = exportId + ".zip";
                    final String path = this.reprise.getString("path") + File.separator + filename;
                    storage.writeBuffer(path, exportId, archive, "application/zip", filename, result -> {
                        if ("ok".equals(result.getString("status"))) {
                            downloadExportPromise.complete(exportId);
                        } else {
                            downloadExportPromise.fail(result.getString("message"));
                        }
                    });
                }
            });
            return downloadExportPromise.future();
        }).onComplete(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
            } else {
                promise.complete(result.result());
            }
        });
        return promise.future();
    }

    @Override
    public void launchImportForUsersFromOldPlatform() {
        final JsonObject matcher = new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.EXPORTED);
        final JsonObject keys = new JsonObject().put("login", 1).put(UserDataSync.NEW_ID_FIELD, 1).put(UserDataSync.EXPORT_ID_FIELD, 1);
        final JsonObject sort = new JsonObject().put(UserDataSync.IMPORT_ATTEMPTS_FIELD, 1);
        final Integer limit = this.reprise.getInteger("limit", 1);
        final JsonObject action = new JsonObject()
                .put("action", "find-users-old-platform")
                .put("matcher", matcher)
                .put("keys", keys)
                .put("sort", sort)
                .put("limit", limit);

        final Promise<JsonArray> promise = Promise.promise();
        eb.request(FEEDER_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                promise.complete(body.getJsonArray("results"));
            } else {
                promise.fail(body.getString("message"));
            }
        }));
        promise.future().compose(users -> {
            final Promise<JsonArray> incrementTryPromise = Promise.promise();
            if (users.isEmpty()) {
                incrementTryPromise.complete(users);
            } else {
                final List<String> userIds = users.stream().map(user -> ((JsonObject)user).getString(UserDataSync.NEW_ID_FIELD)).collect(Collectors.toList());
                final JsonObject criteria = new JsonObject().put(UserDataSync.NEW_ID_FIELD, new JsonObject().put("$in", userIds));
                final JsonObject update = new JsonObject().put("$inc", new JsonObject().put(UserDataSync.IMPORT_ATTEMPTS_FIELD, 1));
                JsonObject action2 = new JsonObject()
                        .put("action", "update-users-old-platform")
                        .put("criteria", criteria)
                        .put("update", update);
                eb.request(FEEDER_BUS_ADDRESS, action2, handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        incrementTryPromise.complete(users);
                    } else {
                        incrementTryPromise.fail(body.getString("message"));
                    }
                }));
            }
            return incrementTryPromise.future();
        }).onComplete(asyncUsers -> {
            if (asyncUsers.succeeded()) {
                JsonArray users = asyncUsers.result();
                for (int i = 0; i < users.size(); i++) {
                    final JsonObject user = users.getJsonObject(i);
                    final String userId = user.getString(UserDataSync.NEW_ID_FIELD);
                    final String login = user.getString("login");
                    final String exportId = user.getString(UserDataSync.EXPORT_ID_FIELD);
                    importService.importFromFile(exportId, userId, login, login, reprise.getString("locale", "fr"), archiveConfig.getString("host"), archiveConfig);

                    final String address = importService.getImportBusAddress(exportId);
                    final MessageConsumer<JsonObject> consumer = eb.consumer(address);

                    final Handler<Message<JsonObject>> importHandler = event ->
                    {
                        JsonObject update = new JsonObject();
                        JsonObject action2 = new JsonObject()
                            .put("action", "update-users-old-platform")
                            .put("criteria", new JsonObject()
                                .put(UserDataSync.NEW_ID_FIELD, userId)
                            )
                            .put("update", new JsonObject()
                                .put("$set", update)
                            );

                        if ("ok".equals(event.body().getString("status"))) {
                            event.reply(new JsonObject().put("status", "ok"));
                            update.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.IMPORTED);
                        } else {
                            event.reply(new JsonObject().put("status", "error"));
                            update.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ERROR_IMPORT);
                        }
                        eb.request("entcore.feeder", action2, handlerToAsyncHandler(message2 -> {
                            JsonObject body = message2.body();
                            if (!"ok".equals(body.getString("status"))) {
                                log.error("[Reprise] Error updating " + login + " (" + userId + ") export status on \"oldplatformusers\":" + body.getString("message"));
                                eventStore.createAndStoreEvent(RepriseEvent.IMPORT_ERROR.name(),
								    (UserInfos) null, new JsonObject().put("user-new-id", userId).put("user-login", login));
                            } else {
                                log.info("[Reprise] " + login + " (" + userId + ") export status has been updated on \"oldplatformusers\"");

                                int nbResources = 0;
                                for(Map.Entry<String, Object> e : event.body().getJsonObject("result").getMap().entrySet())
                                    nbResources += Integer.parseInt(((JsonObject)e.getValue()).getString("resourcesNumber", "0"));

                                eventStore.createAndStoreEvent(RepriseEvent.IMPORT_OK.name(),
								    (UserInfos) null, new JsonObject().put("user-new-id", userId).put("user-login", login).put("nb-resources", nbResources));
                            }
                        }));
                        consumer.unregister();
                    };
                    consumer.handler(importHandler);
                }
            } else {
                log.error("[Reprise] Error on import task: " + asyncUsers.cause().toString());
            }
        });
    }

    @Override
    public void imported(String importId, String app, JsonObject rapport)
    {
        this.importService.imported(importId, app, rapport);
    }

}
