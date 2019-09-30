package org.entcore.archive.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.services.ImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DefaultImportService implements ImportService {

    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);


    private final FileSystem fs;
    private final EventBus eb;
    private final Storage storage;
    private final String importPath;

    public DefaultImportService(Vertx vertx, Storage storage, String importPath) {
        this.storage = storage;
        this.importPath = importPath + File.separator + "import";
        this.fs = vertx.fileSystem();
        this.eb = vertx.eventBus();
    }

    @Override
    public void uploadArchive(HttpServerRequest request, Handler<Either<String, String>> handler) {
        storage.writeUploadToFileSystem(request, importPath, written -> {
            if ("ok".equals(written.getString("status"))) {
                String id = written.getString("_id");
                handler.handle(new Either.Right<>(id));
            } else {
                handler.handle(new Either.Left<>(written.getString("message")));
            }
        });
    }

    @Override
    public void deleteArchive(String importId) {
        deleteArchiveAndZip(importPath + File.separator + importId);
    }

    @Override
    public void analyzeArchive(UserInfos user, String importId, String locale, JsonObject config,
                               Handler<Either<String, JsonObject>> handler) {
        final String filePath = importPath + File.separator + importId;
        final String unzippedPath = filePath + "_unzip";
        fs.mkdirs(unzippedPath, done -> {
            try {
                FileUtils.unzip(filePath, unzippedPath);
            } catch (IOException ioe){
                deleteAndHandleError(filePath, ioe.getMessage(), handler);
                return;
            }
            fs.readDir(unzippedPath, results -> {
               if (results.succeeded()) {
                   if (results.result().size() == 1) {
                       fs.readDir(results.result().get(0), files -> {
                           if (files.succeeded()) {
                               if (files.result().size() > 0 &&
                                   files.result().stream().anyMatch(file -> file.endsWith("Manifest.json"))) {
                                   parseFolders(user, importId, filePath, locale, config, files.result(), handler);
                               } else {
                                   deleteAndHandleError(filePath, "Archive file not recognized - Missing 'Manifest.json'", handler);
                               }
                           } else {
                               deleteAndHandleError(filePath, files.cause().getMessage(), handler);
                           }
                       });
                   } else {
                       deleteAndHandleError(filePath,"Archive file not recognized", handler);
                   }
               } else {
                   deleteAndHandleError(filePath, results.cause().getMessage(), handler);
               }
            });
        });
    }

    private void deleteAndHandleError(String filePath, String error, Handler<Either<String, JsonObject>> handler) {
        deleteArchiveAndZip(filePath);
        handler.handle(new Either.Left<>(error));
    }

    private void deleteArchiveAndZip(String filePath) {
        fs.deleteRecursive(filePath, true, deleted -> {
            if (deleted.failed()) {
                log.error("[Archive] - Import could not be deleted - " + deleted.cause().getMessage());
            }
        });
        fs.deleteRecursive(filePath + "_unzip", true, deleted -> {
            if (deleted.failed()) {
                log.error("[Archive] - Import could not be deleted - " + deleted.cause().getMessage());
            }
        });
    }

    private void parseFolders(UserInfos user, String importId, String path, String locale, JsonObject config,
                              List<String> folders, Handler<Either<String, JsonObject>> handler) {
        String manifestPath = folders.stream().filter(f -> f.endsWith("Manifest.json")).findFirst().get();
        fs.readFile(manifestPath, res -> {
           if (res.failed()) {
               deleteAndHandleError(path, "Archive file not recognized - Missing 'Manifest.json'", handler);
           } else {
               JsonObject reply = new JsonObject().put("importId", importId).put("path",FileUtils.getParentPath(manifestPath));
               JsonObject foundApps = new JsonObject();
               JsonObject apps = res.result().toJsonObject();
               eb.send("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), map -> {
                   if (map.succeeded()) {

                       JsonObject i18n = (JsonObject)(map.result().body());

                       apps.fieldNames().forEach(key -> {

                           String appName = key.substring(key.lastIndexOf(".") + 1);

                           String workflow = config.getJsonObject("publicConf").getJsonObject("apps").getString(appName);
                           if (user.getAuthorizedActions().stream().noneMatch(action -> action.getName().equals(workflow))) {
                               return;
                           }

                           Object o = apps.getValue(key);

                           if (o instanceof JsonObject) {
                               // case where Manifest contains folder name
                               JsonObject jo = (JsonObject)o;
                               String folderName = jo.getString("folder");
                               if (folders.stream().anyMatch(f -> f.endsWith(folderName))) {
                                   foundApps.put(appName, folderName);
                               }
                           } else {
                               // case where Manifest doesn't contain folder name
                               String i = i18n.getString(appName);
                               String translated = StringUtils.stripAccents(i == null ? appName : i);
                               if (folders.stream().anyMatch(f -> f.endsWith(translated))) {
                                   foundApps.put(appName, translated);
                               }
                           }

                       });
                       reply.put("apps", foundApps);
                       handler.handle(new Either.Right<>(reply));
                   } else {
                       deleteAndHandleError(path, "[Archive] - Could not recognize folders. ", handler);
                   }
               });
           }
        });
    }

    @Override
    public void launchImport(String userId, String importId, String importPath, JsonObject apps) {
        JsonObject j = new JsonObject()
                .put("action", "import")
                .put("importId", importId)
                .put("userId", userId)
                .put("apps", apps)
                .put("path", importPath);
        eb.publish("user.repository", j);
    }


}
