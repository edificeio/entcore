package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.Archive;
import org.entcore.archive.services.ImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultImportService implements ImportService {

    private class UserImport {

        private final int expectedImports;
        private final AtomicInteger counter;
        private final JsonObject results;

        public UserImport(int expectedImports) {
            this.expectedImports = expectedImports;
            this.counter = new AtomicInteger(0);
            this.results = new JsonObject();
        }

        public boolean addAppResult(String app, String resourcesNumber, String duplicatesNumber,
                            String errorsNumber) {
            this.results.put(app, new JsonObject().put("resourcesNumber", resourcesNumber)
                    .put("duplicatesNumber", duplicatesNumber).put("errorsNumber", errorsNumber));
            return this.counter.incrementAndGet() == expectedImports;
        }

        public JsonObject getResults() {
            return results;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);

    private final FileSystem fs;
    private final EventBus eb;
    private final Storage storage;
    private final String importPath;

    private final Map<String, UserImport> userImports;

    public DefaultImportService(Vertx vertx, Storage storage, String importPath) {
        this.storage = storage;
        this.importPath = importPath;
        this.fs = vertx.fileSystem();
        this.eb = vertx.eventBus();
        this.userImports = new HashMap<>();
    }

    @Override
    public void uploadArchive(HttpServerRequest request, UserInfos user, Handler<Either<String, String>> handler) {
        final String importId = System.currentTimeMillis() + "_" + user.getUserId();
        storage.writeUploadToFileSystem(request, (importPath + File.separator + importId), written -> {
            if ("ok".equals(written.getString("status"))) {
                handler.handle(new Either.Right<>(importId));
            } else {
                handler.handle(new Either.Left<>(written.getString("message")));
            }
        });
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

    @Override
    public void deleteArchive(String importId) {
        userImports.remove(importId);
        MongoDb.getInstance().delete(Archive.ARCHIVES, new JsonObject().put("import_id", importId));
        deleteArchiveAndZip(importPath + File.separator + importId);
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
    public void launchImport(String userId, String userName, String importId, String importPath, String locale, JsonObject apps) {
        MongoDb.getInstance().save(Archive.ARCHIVES, new JsonObject().put("import_id", importId)
                        .put("date", MongoDb.now()));
        userImports.put(importId, new UserImport(apps.size()));
        JsonObject j = new JsonObject()
                .put("action", "import")
                .put("importId", importId)
                .put("userId", userId)
                .put("userName", userName)
                .put("locale", locale)
                .put("apps", apps)
                .put("path", importPath);
        eb.publish("user.repository", j);
    }

    @Override
    public void imported(String importId, String app, String resourcesNumber,
                         String duplicatesNumber, String errorsNumber) {
        UserImport userImport = userImports.get(importId);
        if (userImport == null) {
            JsonObject jo = new JsonObject()
                    .put("status", "error");
            eb.send(getImportBusAddress(importId), jo);
            deleteArchive(importId);
        } else {
            final boolean finished = userImport.addAppResult(app, resourcesNumber, duplicatesNumber, errorsNumber);
            if (finished) {
                JsonObject jo = new JsonObject()
                        .put("status", "ok")
                        .put("result", userImport.getResults());
                eb.send(getImportBusAddress(importId), jo);
                deleteArchive(importId);
            }
        }
    }

  @Override
  public String getImportBusAddress(String exportId)
  {
    return "import." + exportId;
  }

}
