package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.Archive;
import io.vertx.core.buffer.Buffer;
import org.entcore.archive.services.ImportService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

        public boolean addAppResult(String app, JsonObject importRapport)
        {
            this.results.put(app, importRapport);
            return this.counter.incrementAndGet() == expectedImports;
        }

        public JsonObject getResults() {
            return results;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);

    private final Vertx vertx;
    private final FileSystem fs;
    private final EventBus eb;
    private final Storage storage;
    private final String importPath;
    private final String handlerActionName;

    private final Neo4j neo = Neo4j.getInstance();

    private final Map<String, UserImport> userImports;

    public DefaultImportService(Vertx vertx, Storage storage, String importPath, String customHandlerActionName) {
        this.vertx = vertx;
        this.storage = storage;
        this.importPath = importPath;
        this.fs = vertx.fileSystem();
        this.eb = vertx.eventBus();
        this.userImports = new HashMap<>();
        this.handlerActionName = customHandlerActionName == null ? "import" : customHandlerActionName;
    }

    @Override
    public boolean isUserAlreadyImporting(String userId) {
        return userImports.keySet().stream().anyMatch(id -> id.endsWith(userId));
    }

    @Override
    public void uploadArchive(HttpServerRequest request, UserInfos user, Handler<Either<String, String>> handler) {
        final String importId = System.currentTimeMillis() + "_" + user.getUserId();
        final String filePath = importPath + File.separator + importId;
        storage.writeUploadToFileSystem(request, filePath, written -> {
            if ("ok".equals(written.getString("status"))) {
                MongoDb.getInstance().save(Archive.ARCHIVES, new JsonObject().put("import_id", importId)
                        .put("date", MongoDb.now()));
                handler.handle(new Either.Right<>(importId));
            } else {
                deleteArchive(importId);
                handler.handle(new Either.Left<>(written.getString("message")));
            }
        });
    }

    @Override
    public void copyArchive(String archiveId, Handler<Either<String, String>> handler)
    {
      storage.copyFileId(archiveId, importPath + File.separator + archiveId, new Handler<JsonObject>()
      {
        @Override
        public void handle(JsonObject obj)
        {
          if(obj.getString("status").equals("ok") == true)
            handler.handle(new Either.Right<>(archiveId));
          else {
              deleteArchive(archiveId);
              handler.handle(new Either.Left<>(obj.getString("message")));
          }
        }
      });
    }

    @Override
    public void analyzeArchive(UserInfos user, String importId, String locale, JsonObject config,
                               Handler<Either<String, JsonObject>> handler) {
        final String filePath = importPath + File.separator + importId;
        final String unzippedPath = filePath + "_unzip";
        fs.mkdirs(unzippedPath, done -> {
            FileUtils.unzip(filePath, unzippedPath, new Handler<Either<String, Void>>()
            {
                @Override
                public void handle(Either<String, Void> res)
                {
                    if(res.isRight() == true)
                    {
                        fs.readDir(unzippedPath, results -> {
                            if (results.succeeded()) {
                                if (results.result().size() == 1) {
                                    fs.readDir(results.result().get(0), files -> {
                                        if (files.succeeded()) {
                                            if (files.result().size() > 0 &&
                                                    files.result().stream().anyMatch(file -> file.endsWith("Manifest.json"))) {
                                                parseFolders(user, importId, filePath, locale, config, files.result(), handler);
                                            } else {
                                                deleteAndHandleError(importId, "Archive file not recognized - Missing 'Manifest.json'", handler);
                                            }
                                        } else {
                                            deleteAndHandleError(importId, files.cause().getMessage(), handler);
                                        }
                                    });
                                } else {
                                    deleteAndHandleError(importId,"Archive file not recognized", handler);
                                }
                            } else {
                                deleteAndHandleError(importId, results.cause().getMessage(), handler);
                            }
                        });
                    }
                    else
                    {
                        deleteAndHandleError(importId, res.left().getValue(), handler);
                    }
                }
            });
        });
    }

    private void deleteAndHandleError(String importId, String error, Handler<Either<String, JsonObject>> handler) {
        deleteArchive(importId);
        handler.handle(new Either.Left<>(error));
    }

    @Override
    public void deleteArchive(String importId) {
        if (StringUtils.isEmpty(importId) ||
                !importId.matches("[0-9]+_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            log.error("[Archive] - Import could not be deleted, wrong importId: " + importId);
            return;
        }
        userImports.remove(importId);
        MongoDb.getInstance().delete(Archive.ARCHIVES, new JsonObject().put("import_id", importId));

        if (!StringUtils.isEmpty(importPath)) {

            String filePath = importPath + File.separator + importId;
            log.debug("[Archive] - Deleting import located at " + filePath);

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
    }

    private void parseFolders(UserInfos user, String importId, String path, String locale, JsonObject config,
                              List<String> folders, Handler<Either<String, JsonObject>> handler) {
        String manifestPath = folders.stream().filter(f -> f.endsWith("Manifest.json")).findFirst().get();
        fs.readFile(manifestPath, res -> {
           if (res.failed()) {
               deleteAndHandleError(importId, "Archive file not recognized - Missing 'Manifest.json'", handler);
           } else {
               JsonObject reply = new JsonObject().put("importId", importId).put("path",FileUtils.getParentPath(manifestPath));
               JsonObject foundApps = new JsonObject();
               JsonObject apps = res.result().toJsonObject();
               eb.send("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), map -> {
                   if (map.succeeded()) {

                       JsonObject i18n = (JsonObject)(map.result().body());

                       apps.fieldNames().forEach(key -> {

                           String appName = key.substring(key.lastIndexOf(".") + 1).replace("-", "");

                           String workflow = config.getJsonObject("publicConf").getJsonObject("apps").getString(appName);
                           if (user.getAuthorizedActions().stream().noneMatch(action -> action.getName().equals(workflow))) {
                               return;
                           }

                           Object o = apps.getValue(key);

                           if (o instanceof JsonObject) {
                               // case where Manifest contains folder name

                               // TO DO: Allow timelinegenerator import later by deleting this bloc
                               if ("timelinegenerator".equals(appName) &&
                                       StringUtils.versionComparator.compare(((JsonObject)o).getString("version"), "1.7") < 0) {
                                   return;
                               }
                               //

                               // TO DO: Allow pad import later by deleting this bloc
                               if ("collaborativeeditor".equals(appName) &&
                                       StringUtils.versionComparator.compare(((JsonObject)o).getString("version"), "1.7") <= 0) {
                                   return;
                               }
                               //

                               JsonObject jo = (JsonObject)o;
                               String folderName = jo.getString("folder");
                               if (folders.stream().anyMatch(f -> f.endsWith(folderName))) {
                                   foundApps.put(appName, folderName);
                               }
                           } else {
                               // case where Manifest doesn't contain folder name

                               // TO DO: Allow timelinegenerator import later by deleting this bloc
                               if ("timelinegenerator".equals(appName) &&
                                       StringUtils.versionComparator.compare((String)o, "1.7") < 0) {
                                   return;
                               }
                               //

                               // TO DO: Allow pad import later by deleting this bloc
                               if ("collaborativeeditor".equals(appName) &&
                                       StringUtils.versionComparator.compare((String)o, "1.7") <= 0) {
                                   return;
                               }
                               //

                               String i = i18n.getString(appName);
                               String translated = StringUtils.stripAccents(i == null ? appName : i);
                               if (folders.stream().anyMatch(f -> f.endsWith(translated))) {
                                   foundApps.put(appName, translated);
                               }
                           }

                       });

                       final JsonObject foundAppsWithSize = new JsonObject();
                       final List<Future> getFoldersSize = new ArrayList<>();

                       foundApps.fieldNames().forEach(app -> {

                           String folder = foundApps.getString(app);
                           String folderPath;
                           if ("workspace".equals(app) || "rack".equals(app)) {
                               folderPath = folders.stream().filter(f -> f.endsWith(folder)).findFirst().get();
                           } else {
                               folderPath = folders.stream().filter(f -> f.endsWith(folder)).findFirst().get()
                                       + File.separator + "Documents";
                           }
                           Future<Long> size = Future.future();
                           fs.exists(folderPath, exist -> {
                               if (exist.result()) {
                                   Future<Long> promise = recursiveSize(folderPath);
                                   promise.setHandler(result -> {
                                       foundAppsWithSize.put(app, new JsonObject()
                                               .put("folder", folder).put("size", result.result()));
                                       size.complete(result.result());
                                   });
                               } else {
                                   foundAppsWithSize.put(app, new JsonObject()
                                           .put("folder", folder).put("size", 0l));
                                   size.complete(0l);
                               }
                           });
                           getFoldersSize.add(size);

                       });
                       CompositeFuture.join(getFoldersSize).setHandler(completed -> {
                           reply.put("apps", foundAppsWithSize);
                           getQuota(user, reply, replyWithQuota -> {
                               handler.handle(new Either.Right<>(replyWithQuota));
                           });
                       });

                   } else {
                       deleteAndHandleError(importId, "[Archive] - Could not recognize folders. ", handler);
                   }
               });
           }
        });
    }

    private void getQuota(UserInfos user, JsonObject reply, Handler<JsonObject> handler) {
        neo.execute("MATCH (u:User {id: {userId}})-[:USERBOOK]->(ub:UserBook) RETURN (ub.quota - ub.storage) AS quota",
                new JsonObject().put("userId", user.getUserId()), result -> {
            reply.put("quota", result.body().getJsonArray("result").getJsonObject(0).getLong("quota"));
            handler.handle(reply);
        });
    }

    @Override
    public void launchImport(String userId, String userLogin, String userName, String importId, String importPath,
      String locale, String host, JsonObject apps)
    {
        userImports.put(importId, new UserImport(apps.size()));
        JsonObject j = new JsonObject()
                .put("action", handlerActionName)
                .put("importId", importId)
                .put("userId", userId)
                .put("userLogin", userLogin)
                .put("userName", userName)
                .put("locale", locale)
                .put("host", host)
                .put("apps", apps)
                .put("path", importPath);
        eb.publish("user.repository", j);
    }

    @Override
    public void imported(String importId, String app, JsonObject importRapport) {
        UserImport userImport = userImports.get(importId);
        if (userImport == null) {
            JsonObject jo = new JsonObject()
                    .put("status", "error");
            eb.send(getImportBusAddress(importId), jo);
            deleteArchive(importId);
        } else {
            final boolean finished = userImport.addAppResult(app, importRapport);
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

  private Future<Long> recursiveSize(String path) {
        Future<Long> size = Future.future();
        fs.props(path, handler -> {
            if (handler.succeeded()) {
                FileProps props = handler.result();
                if (props.isDirectory()) {
                    fs.readDir(path, res -> {
                       if (res.failed()) {
                           size.complete(props.size());
                       } else {
                           List<Future> childrenSize = res.result().stream().map(this::recursiveSize).collect(Collectors.toList());
                           CompositeFuture.join(childrenSize).setHandler(compositeFutureAsyncResult -> {
                               if (compositeFutureAsyncResult.succeeded()) {
                                   Long l = compositeFutureAsyncResult.result().list().stream().mapToLong(lo -> (Long)lo).sum();
                                   size.complete(l + props.size());
                               } else {
                                   size.complete(props.size());
                               }
                           });
                       }
                    });
                } else {
                    size.complete(props.size());
                }
            } else {
                size.complete(0l);
            }
        });

        return size;
  }

}
