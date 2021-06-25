package org.entcore.archive.services.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.entcore.archive.services.RepriseService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserDataSync;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultRepriseService implements RepriseService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRepriseService.class);

    private final EventBus eb;
    private final Storage storage;
    private final WebClient client;
    private final JsonObject reprise;

    public DefaultRepriseService(Vertx vertx, Storage storage, JsonObject reprise) {
        this.eb = vertx.eventBus();
        this.storage = storage;
        this.client = WebClient.create(vertx);
        this.reprise = reprise;
    }

    @Override
    public void launchExportForUsersFromOldPlatform() {
        final Promise<JsonArray> promise = Promise.promise();
        final JsonObject matcher = new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ACTIVATED);
        final JsonObject keys = new JsonObject().put("login", 1).put("_old_id", 1);
        final JsonObject sort = new JsonObject().put("_exportAttemps", 1);
        final Integer limit = this.reprise.getInteger("limit", 1);
        final JsonObject action = new JsonObject()
                .put("action", "find-users-old-platform")
                .put("matcher", matcher)
                .put("keys", keys)
                .put("sort", sort)
                .put("limit", limit);
        eb.request("entcore.feeder", action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray users = body.getJsonArray("results");
                promise.complete(users);
            } else {
                log.error("[Archive] Error retrieving activated old platform users: " + body.getString("message"));
                promise.fail(body.getString("message"));
            }
        }));
        promise.future().compose(users -> {
            List<Future> list = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) {
                final Promise<Void> incrementTryPromise = Promise.promise();
                list.add(incrementTryPromise.future());
                final JsonObject user = users.getJsonObject(i);
                final JsonObject criteria = new JsonObject().put(UserDataSync.OLD_ID_FIELD, user.getString("_old_id"));
                final JsonObject update = new JsonObject().put("$inc", new JsonObject().put("_exportAttemps", 1));
                JsonObject action2 = new JsonObject()
                        .put("action", "update-users-old-platform")
                        .put("criteria", criteria)
                        .put("update", update);
                eb.request("entcore.feeder", action2, handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        incrementTryPromise.complete();
                    } else {
                        incrementTryPromise.fail(body.getString("message"));
                    }
                }));
            }
            final Promise<JsonArray> compositePromise = Promise.promise();
            CompositeFuture.join(list).onComplete(handler -> {
                if (handler.succeeded()) {
                    compositePromise.complete(users);
                } else {
                    compositePromise.fail(handler.cause());
                }
            });
            return compositePromise.future();
        }).onComplete(asyncUsers -> {
            if (asyncUsers.succeeded()) {
                JsonArray users = asyncUsers.result();
                for (int i = 0; i < users.size(); i++) {
                    final JsonObject user = users.getJsonObject(i);
                    final String userId = user.getString("_old_id");
                    final String login = user.getString("login");
                    Future<String> export = this.launchExportForUser(userId, login);
                    export.onComplete(asyncResult -> {
                        final JsonObject criteria = new JsonObject().put(UserDataSync.OLD_ID_FIELD, user.getString("_old_id"));
                        final JsonObject set = new JsonObject();
                        if (asyncResult.failed()) {
                            set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ERROR_EXPORT);
                        } else {
                            final String exportId = asyncResult.result();
                            set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.EXPORTED).put("_exportId", exportId);
                        }
                        final JsonObject update = new JsonObject().put("$set", set);
                        JsonObject action3 = new JsonObject()
                                .put("action", "update-users-old-platform")
                                .put("criteria", criteria)
                                .put("update", update);
                        eb.request("entcore.feeder", action3, handlerToAsyncHandler(message -> {
                            JsonObject body = message.body();
                            if ("ok".equals(body.getString("status"))) {
                                log.info("[Reprise] Export for " + login + " (" + userId + ") succeeded");
                            } else {
                                log.error("[Reprise] Export for " + login + " (" + userId + ") failed: " + body.getString("message"));
                            }
                        }));
                    });
                }
            } else {
                log.error("[Reprise] Error on export: " + asyncUsers.cause().toString());
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

}
