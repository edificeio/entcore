package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.RepriseService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserDataSync;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.time.OffsetTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultRepriseService implements RepriseService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRepriseService.class);
    private static final String FEEDER_BUS_ADDRESS = "entcore.feeder";

    private final EventBus eb;
    private final WebClient client;
    private final JsonObject reprise;
    private final JsonObject archiveConfig;
    private final ImportService importService;
    private OffsetTime limitHour;
    private final AtomicInteger numberOfExports;
    private final AtomicBoolean keepExporting;
    private final AtomicInteger numberOfImports;
    private final AtomicBoolean keepImporting;
    private final Integer limit;
    private final Vertx vertx;

    public DefaultRepriseService(Vertx vertx, Storage storage, JsonObject reprise, JsonObject archiveConfig, ImportService importService) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();
        this.client = WebClient.create(vertx);
        this.reprise = reprise;
        this.archiveConfig = archiveConfig;
        this.importService = importService;
        final String limitHour = reprise.getString("limit-hour");
        if (!StringUtils.isEmpty(limitHour)) {
            try {
                this.limitHour = OffsetTime.parse(limitHour);
            } catch (DateTimeParseException dtpe) {
                log.error("[Reprise] Error parsing limit-hour: " + dtpe.toString());
            }
        }
        numberOfExports = new AtomicInteger(0);
        keepExporting = new AtomicBoolean(true);
        numberOfImports = new AtomicInteger(0);
        keepImporting = new AtomicBoolean(true);
        limit = this.reprise.getInteger("limit", 1);
    }

    private boolean hasLimitHourPassed() {
        return limitHour != null && OffsetTime.now().isAfter(limitHour);
    }

    @Override
    public void launchExportForUsersFromOldPlatform(final boolean teacherPersonnelFirst) {
        if (!keepExporting.get()) {
            return;
        }
        numberOfExports.incrementAndGet();
        final Promise<JsonObject> promise = Promise.promise();
        final JsonObject matcher = new JsonObject()
                .put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ACTIVATED)
                .put(UserDataSync.IS_EXPORTING_FIELD, new JsonObject().put("$ne", true));
        if (teacherPersonnelFirst) {
            matcher.put(UserDataSync.PROFILE_FIELD, new JsonObject().put("$in", Arrays.asList(new String[]{UserDataSync.TEACHER_PROFILE, UserDataSync.PERSONNEL_PROFILE})));
        }
        final JsonObject keys = new JsonObject()
                .put("login", 1)
                .put("_old_id", 1);
        final JsonObject update = new JsonObject()
                .put("$inc", new JsonObject().put(UserDataSync.EXPORT_ATTEMPTS_FIELD, 1))
                .put("$set", new JsonObject()
                        .put("modified", MongoDb.now())
                        .put(UserDataSync.IS_EXPORTING_FIELD, true));
        final JsonObject sort = new JsonObject()
                .put(UserDataSync.EXPORT_ATTEMPTS_FIELD, 1)
                .put("modified", 1);
        final JsonObject action = new JsonObject()
                .put("action", "find-users-old-platform")
                .put("matcher", matcher)
                .put("keys", keys)
                .put("sort", sort)
                .put("update", update);
        eb.request(FEEDER_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonObject user = body.getJsonObject("result");
                if (user == null) {
                    if(teacherPersonnelFirst) {
                        // There are no more teachers and relatives to export, we relaunch it for other profiles
                        if (numberOfExports.decrementAndGet() < limit.intValue() && !hasLimitHourPassed()) {
                            launchExportForUsersFromOldPlatform(false);
                        }
                    } else {
                        keepExporting.set(false);
                        log.info("[Reprise] Export task has terminated successfully");
                    }
                } else {
                    promise.complete(user);
                }
            } else {
                promise.fail(body.getString("message"));
            }
        }));
        promise.future().onComplete(asyncUser -> {
            if (asyncUser.succeeded()) {
                JsonObject user = asyncUser.result();
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
                    final JsonObject unset = new JsonObject().put(UserDataSync.IS_EXPORTING_FIELD, "");
                    final JsonObject update2 = new JsonObject().put("$set", set).put("$unset", unset);
                    JsonObject action3 = new JsonObject()
                            .put("action", "update-users-old-platform")
                            .put("criteria", criteria)
                            .put("update", update2);
                    eb.request(FEEDER_BUS_ADDRESS, action3, handlerToAsyncHandler(message -> {
                        JsonObject body = message.body();
                        if (!"ok".equals(body.getString("status"))) {
                            final String errorMessage = body.getString("message");
                            log.error("[Reprise] Error updating " + login + " (" + userId + ") export status on \"oldplatformusers\":" + errorMessage);
                            eventStore.createAndStoreEvent(RepriseEvent.EXPORT_ERROR.name(),
                                    (UserInfos) null, new JsonObject().put("user-old-id", userId).put("user-login", login));
                        } else {
                            log.info("[Reprise] " + login + " (" + userId + ") export status has been updated on \"oldplatformusers\"");
                            eventStore.createAndStoreEvent(RepriseEvent.EXPORT_OK.name(),
                                    (UserInfos) null, new JsonObject().put("user-old-id", userId).put("user-login", login));
                        }
                        if (numberOfExports.decrementAndGet() < limit.intValue() && !hasLimitHourPassed()) {
                            launchExportForUsersFromOldPlatform(teacherPersonnelFirst);
                        }
                    }));
                });
            } else {
                log.error("[Reprise] Error on export task: " + asyncUser.cause().toString());
            }
            if (numberOfExports.get() < limit.intValue() && !hasLimitHourPassed()) {
                launchExportForUsersFromOldPlatform(teacherPersonnelFirst);
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
                if (httpResponse.statusCode() == 200) {
                    final String exportId = httpResponse.bodyAsJsonObject().getString("exportId");
                    initExportPromise.complete(exportId);
                }
                else {
                    initExportPromise.fail(httpResponse.statusMessage());
                }
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
                    HttpResponse<Buffer> httpResponse = asyncResult.result();
                    if (httpResponse.statusCode() == 200) {
                        verifyExportPromise.complete(exportId);
                    } else {
                        verifyExportPromise.fail(httpResponse.statusMessage());
                    }
                }
            });
            return verifyExportPromise.future();
        }).compose(exportId -> {
            final Promise<String> downloadExportPromise = Promise.promise();
            final String path = this.reprise.getString("path") + File.separator + exportId;
            final AsyncFile exportFile = vertx.fileSystem().openBlocking(path, new OpenOptions());
            exportFile.exceptionHandler(except -> {
                downloadExportPromise.fail(except);
            });

            final HttpRequest<Void> httpRequest3 = client.getAbs(reprisePlatformURL + "/archive/export/" + exportId)
                    .as(BodyCodec.pipe(exportFile));
            httpRequest3.putHeader("Authorization", "Basic " + basicAuthCredential);
            httpRequest3.send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Void> resp = ar.result();
                    if (resp.statusCode() == 200) {
                        log.info("export get succeeded : " + exportId);
                        downloadExportPromise.complete(exportId);
                    } else {
                        downloadExportPromise.fail(resp.statusMessage());
                    }
                } else {
                    downloadExportPromise.fail(ar.cause());
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
    public void launchImportForUsersFromOldPlatform(final boolean teacherPersonnelFirst) {
        if (!keepImporting.get()) {
            return;
        }
        numberOfImports.incrementAndGet();
        final JsonObject matcher = new JsonObject()
                .put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.EXPORTED)
                .put(UserDataSync.IS_IMPORTING_FIELD, new JsonObject().put("$ne", true));
        if (teacherPersonnelFirst) {
            matcher.put(UserDataSync.PROFILE_FIELD, new JsonObject().put("$in", Arrays.asList(new String[]{UserDataSync.TEACHER_PROFILE, UserDataSync.PERSONNEL_PROFILE})));
        }
        final JsonObject keys = new JsonObject()
                .put("login", 1)
                .put(UserDataSync.NEW_ID_FIELD, 1)
                .put(UserDataSync.EXPORT_ID_FIELD, 1);
        final JsonObject update = new JsonObject()
                .put("$inc", new JsonObject().put(UserDataSync.IMPORT_ATTEMPTS_FIELD, 1))
                .put("$set", new JsonObject()
                        .put("modified", MongoDb.now())
                        .put(UserDataSync.IS_IMPORTING_FIELD, true));
        final JsonObject sort = new JsonObject()
                .put(UserDataSync.IMPORT_ATTEMPTS_FIELD, 1)
                .put("modified", 1);
        final JsonObject action = new JsonObject()
                .put("action", "find-users-old-platform")
                .put("matcher", matcher)
                .put("keys", keys)
                .put("sort", sort)
                .put("update", update);

        final Promise<JsonObject> promise = Promise.promise();
        eb.request(FEEDER_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonObject user = body.getJsonObject("result");
                if (user == null) {
                    if(teacherPersonnelFirst) {
                        // There are no more teachers and relatives to import, we relaunch it for other profiles
                        if (numberOfImports.decrementAndGet() < limit.intValue() && !hasLimitHourPassed()) {
                            launchImportForUsersFromOldPlatform(false);
                        }
                    } else {
                        keepImporting.set(false);
                        log.info("[Reprise] Import task has terminated successfully");
                    }
                } else {
                    promise.complete(user);
                }
            } else {
                promise.fail(body.getString("message"));
            }
        }));
        promise.future().onComplete(asyncUser -> {
            if (asyncUser.succeeded()) {
                JsonObject user = asyncUser.result();
                final String userId = user.getString(UserDataSync.NEW_ID_FIELD);
                final String login = user.getString("login");
                final String exportId = user.getString(UserDataSync.EXPORT_ID_FIELD);
                log.info("[Reprise] Import for " + login + " (" + userId + ") has started");

                final String address = importService.getImportBusAddress(exportId);
                final MessageConsumer<JsonObject> consumer = eb.consumer(address);

                final Handler<Message<JsonObject>> importHandler = event ->
                {
                    JsonObject set = new JsonObject(), unset = new JsonObject();
                    JsonObject action2 = new JsonObject()
                            .put("action", "update-users-old-platform")
                            .put("criteria", new JsonObject()
                                    .put(UserDataSync.NEW_ID_FIELD, userId)
                            )
                            .put("update", new JsonObject()
                                    .put("$set", set)
                                    .put("$unset", unset)
                            );
                    unset.put(UserDataSync.IS_IMPORTING_FIELD, "");
                    if ("ok".equals(event.body().getString("status"))) {
                        event.reply(new JsonObject().put("status", "ok"));
                        set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.IMPORTED);
                        log.info("[Reprise] Import for " + login + " (" + userId + ") succeeded");
                    } else {
                        event.reply(new JsonObject().put("status", "error"));
                        set.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ERROR_IMPORT);
                        log.error("[Reprise] Export for " + login + " (" + userId + ") failed");
                    }
                    eb.request("entcore.feeder", action2, handlerToAsyncHandler(message2 -> {
                        JsonObject body = message2.body();
                        if (!"ok".equals(body.getString("status"))) {
                            final String errorMessage = body.getString("message");
                            log.error("[Reprise] Error updating " + login + " (" + userId + ") import status on \"oldplatformusers\":" + errorMessage);
                            eventStore.createAndStoreEvent(RepriseEvent.IMPORT_ERROR.name(),
                                    (UserInfos) null, new JsonObject().put("user-new-id", userId).put("user-login", login));
                        } else {
                            log.info("[Reprise] " + login + " (" + userId + ") import status has been updated on \"oldplatformusers\"");

                            int nbResources = 0;
                            for(Map.Entry<String, Object> e : event.body().getJsonObject("result").getMap().entrySet())
                                nbResources += Integer.parseInt(((JsonObject)e.getValue()).getString("resourcesNumber", "0"));

                            eventStore.createAndStoreEvent(RepriseEvent.IMPORT_OK.name(),
                                    (UserInfos) null, new JsonObject().put("user-new-id", userId).put("user-login", login).put("nb-resources", nbResources));
                        }
                        if (numberOfImports.decrementAndGet() < limit.intValue() && !hasLimitHourPassed()) {
                            launchImportForUsersFromOldPlatform(teacherPersonnelFirst);
                        }
                    }));
                    consumer.unregister();
                };
                consumer.handler(importHandler);
                importService.importFromFile(exportId, userId, login, login, reprise.getString("locale", "fr"), archiveConfig.getString("host"), archiveConfig);
            } else {
                log.error("[Reprise] Error on import task: " + asyncUser.cause().toString());
            }
            if (numberOfImports.get() < limit.intValue() && !hasLimitHourPassed()) {
                launchImportForUsersFromOldPlatform(teacherPersonnelFirst);
            }
        });
    }

    @Override
    public void imported(String importId, String app, JsonObject rapport)
    {
        this.importService.imported(importId, app, rapport);
    }

}
