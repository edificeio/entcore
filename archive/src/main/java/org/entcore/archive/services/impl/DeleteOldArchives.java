/*
 * Copyright © "Open Digital Education", 2015
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

 */

package org.entcore.archive.services.impl;


import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ImportService;
import org.entcore.common.storage.Storage;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DeleteOldArchives implements Handler<Long> {

    private final MongoDb mongo = MongoDb.getInstance();
    private final Vertx vertx;
    private final Storage storage;
    private final int delay;
    private ImportService importService;
    private String exportPath;
    private final String importPath;
    private final int maxFileAge;
    private final boolean enablePurgeByFileAge;

    private static final Logger log = LoggerFactory.getLogger(DeleteOldArchives.class);


    public DeleteOldArchives(Vertx vertx, Storage storage, int delay, String exportPath, ImportService importService, final String importPath, final boolean enablePurgeByFileAge, final int maxFileAge) {
        this.storage = storage;
        this.vertx = vertx;
        this.delay = delay;
        this.importService = importService;
        this.exportPath = exportPath;
        this.importPath = importPath;
        this.maxFileAge = maxFileAge;
        this.enablePurgeByFileAge = enablePurgeByFileAge;
    }

    @Override
    public void handle(Long event) {
        log.info("[cron][purge] Starting...");
        this.purgeArchive().onComplete(e -> {
            if (this.enablePurgeByFileAge) {
                CompositeFuture.all(this.purgeFileByAge(this.importPath), this.purgeFileByAge(this.exportPath)).onComplete(onFinish -> {
                    log.info("[cron][purge] Finished");
                });
            } else {
                log.info("[cron][purge] Purge by file age is disabled");
            }
        });
    }

    public Future<Void> purgeArchive() {
        final Promise promise = Promise.promise();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR, -delay);
        final JsonObject query = new JsonObject()
                .put("date", new JsonObject()
                        .put("$lt", new JsonObject()
                                .put("$date", c.getTime().getTime())));
        mongo.find(Archive.ARCHIVES, query, event -> {
            JsonArray res = event.body().getJsonArray("results");
            if ("ok".equals(event.body().getString("status"))) {
                if (res != null && res.size() > 0) {
                    JsonArray ids = new JsonArray();
                    for (Object object : res) {
                        if (!(object instanceof JsonObject)) continue;
                        JsonObject jo = (JsonObject) object;
                        if (jo.containsKey("file_id")) {
                            final String exportId = jo.getString("file_id");
                            ids.add(exportId);
                            if (!StringUtils.isEmpty(exportId) && !StringUtils.isEmpty(exportPath) &&
                                    exportId.matches("[0-9]+_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                                final String path = exportPath + File.separator + exportId;
                                vertx.fileSystem().deleteRecursive(path, true, delEvent -> {
                                    if (delEvent.failed()) {
                                        log.error("Error deleting directory : " + path, delEvent.cause());
                                    }
                                });
                                final String zipPath = path + ".zip";
                                vertx.fileSystem().delete(zipPath, delEvent -> {
                                    if (delEvent.failed()) {
                                        log.error("Error deleting temp zip export " + zipPath, delEvent.cause());
                                    }

                                });
                            }

                        } else if (jo.containsKey("import_id")) {
                            importService.deleteArchive(jo.getString("import_id"));
                        }
                    }
                    log.info("[cron][purge] Removing files. numberOfFiles=" + ids.size());
                    storage.removeFiles(ids, removeEvent -> {
                        mongo.delete(Archive.ARCHIVES, query);
                        promise.complete();
                    });
                } else {
                    log.info("[cron][purge] Nothing to delete.");
                    promise.complete();
                }
            } else {
                final String error = event.body().getString("message");
                log.info("[cron][purge] Failed to fetch: " + error);
                promise.fail(error);
            }
        });
        return promise.future();
    }

    private Future<Void> purgeFileByAge(final String path) {
        log.info("[cron][purge] Starting deleting file from path=" + path + " having age (hours) >" + this.delay);
        final Date minDate = Date.from(Instant.now().minus(this.maxFileAge, ChronoUnit.HOURS));
        return this.storage.deleteByFilter(path, (info) -> {
            final boolean shouldDelete = info.props.lastModifiedTime() < minDate.getTime();
            if (shouldDelete) {
                log.info("[cron][purge] deleting file=" + info.path);
            }
            return shouldDelete;
        }).onComplete(e -> {
            if (e.succeeded()) {
                final long numberDelete = e.result().stream().filter(file -> file.deleted).count();
                final long numberChecked = e.result().size();
                log.info("[cron][purge] Finished purge on path=" + path + " numberOfFileDeleted=" + numberDelete + " numberOfFileChecked=" + numberChecked);
            } else {
                log.error("[cron][purge] Failed with error on path=" + path, e.cause());
            }
        }).mapEmpty();
    }

}
