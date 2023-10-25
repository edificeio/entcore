package org.entcore.archive.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.RSA;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.Archive;
import org.entcore.archive.controllers.ArchiveController;

import io.vertx.core.buffer.Buffer;
import org.entcore.archive.services.ImportService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    private final PublicKey verifyKey;
    private final boolean forceEncryption;

    private final Neo4j neo = Neo4j.getInstance();

    private final Map<String, UserImport> userImports;

    public DefaultImportService(Vertx vertx, final JsonObject config, Storage storage, String importPath, String customHandlerActionName,
                                PublicKey verifyKey, boolean forceEncryption) {
        this.vertx = vertx;
        this.storage = storage;
        this.importPath = importPath;
        this.fs = vertx.fileSystem();
        this.eb = vertx.eventBus();
        this.userImports = new HashMap<>();
        this.handlerActionName = customHandlerActionName == null ? "import" : customHandlerActionName;
        this.verifyKey = verifyKey;
        this.forceEncryption = forceEncryption;
    }

    @Override
    public boolean isUserAlreadyImporting(String userId) {
        return userImports.keySet().stream().anyMatch(id -> id.endsWith(userId));
    }

    @Override
    public void uploadArchive(HttpServerRequest request, UserInfos user, Handler<Either<String, String>> handler) {
        final String importId = System.currentTimeMillis() + "_" + user.getUserId();
        final String filePath = getImportPath(importId);
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
      storage.copyFileId(archiveId, getImportPath(archiveId), new Handler<JsonObject>()
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
        final String filePath = getImportPath(importId);
        final String unzippedPath = getUnzippedImportPath(importId);
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
                                                verifyImport(user, importId, filePath, locale, config, files.result(), handler);
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

            String filePath = getImportPath(importId);
            log.debug("[Archive] - Deleting import located at " + filePath);

            fs.deleteRecursive(filePath, true, deleted -> {
                if (deleted.failed()) {
                    log.error("[Archive] - Import could not be deleted - " + deleted.cause().getMessage());
                }
            });
            fs.deleteRecursive(getUnzippedImportPath(importId), true, deleted -> {
                if (deleted.failed()) {
                    log.error("[Archive] - Import could not be deleted - " + deleted.cause().getMessage());
                }
            });
        }
    }

    private void verifyImport(UserInfos user, String importId, String path, String locale, JsonObject config,
                              List<String> folders, Handler<Either<String, JsonObject>> handler)
    {
      Optional<String> signaturePath = folders.stream().filter(f -> f.endsWith(ArchiveController.SIGNATURE_NAME)).findFirst();

      if(this.verifyKey == null)
      {
        if(forceEncryption == false)
            parseFolders(user, importId, path, locale, config, folders, handler);
        else
            deleteAndHandleError(importId, "No verify key", handler);
        return;
      }

      if(signaturePath.isPresent() == false)
      {
        if(forceEncryption == false)
            parseFolders(user, importId, path, locale, config, folders, handler);
        else
            deleteAndHandleError(importId, "Archive file not recognized - Missing '" + ArchiveController.SIGNATURE_NAME + "'", handler);
        return;
      }

      fs.readFile(signaturePath.get(), res ->
      {
        if(res.failed())
        {
          deleteAndHandleError(importId, "Archive file not recognized - Missing '" + ArchiveController.SIGNATURE_NAME + "'", handler);
          return;
        }

        JsonObject contentHashes = new JsonObject(res.result().toString());

        for(String folder : folders)
        {
          String fname = FileUtils.getFilename(folder);
          if(folder.endsWith(ArchiveController.SIGNATURE_NAME) == false)
          {
            String signature = contentHashes.getString(fname);

            if(signature == null || StringUtils.isEmpty(signature) == true)
            {
              deleteAndHandleError(importId, "Archive signature does not list the folder " + fname, handler);
              return;
            }
            else
            {
              try
              {
                if(RSA.verifyFile(folder, signature, verifyKey) == false)
                {
                  deleteAndHandleError(importId, "The folder " + fname + " does not match the signature", handler);
                  return;
                }
              }
              catch(Exception e)
              {
                deleteAndHandleError(importId, "Could not verify the folder " + fname, handler);
                return;
              }
            }
          }
        }

        parseFolders(user, importId, path, locale, config, folders, handler);
      });
    }

    private void parseFolders(UserInfos user, String importId, String path, String locale, JsonObject config,
                              List<String> folders, Handler<Either<String, JsonObject>> handler) {
      String manifestPath = folders.stream().filter(f -> f.endsWith("Manifest.json")).findFirst().get();
        fs.readFile(manifestPath, res -> {
           if (res.failed()) {
               deleteAndHandleError(importId, "Archive file not recognized - Missing 'Manifest.json'", handler);
           } else {
               JsonObject reply = new JsonObject().put("importId", importId);
               JsonObject foundApps = new JsonObject();
               JsonObject apps = res.result().toJsonObject();
               eb.request("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), map -> {
                   if (map.succeeded()) {

                       JsonObject i18n = (JsonObject)(map.result().body());

                       apps.fieldNames().forEach(key -> {

                           String appName = key.substring(key.lastIndexOf(".") + 1).replace("-", "");

                           String workflow = config.getJsonObject("publicConf").getJsonObject("apps").getString(appName);
                           if (user != null && user.getAuthorizedActions().stream().noneMatch(action -> action.getName().equals(workflow))) {
                               return;
                           }

                           Object o = apps.getValue(key);

                           JsonObject minimumImportVersions = config.getJsonObject("minimum-import-version");
                           if(minimumImportVersions == null)
                            minimumImportVersions = new JsonObject();

                           if (o instanceof JsonObject) {
                               // case where Manifest contains folder name

                               for (Map.Entry<String, Object> app: minimumImportVersions.getMap().entrySet()) {
                                   if (app.getKey().equals(appName) &&
                                           StringUtils.versionComparator.compare(((JsonObject)o).getString("version"), app.getValue().toString()) < 0) {
                                       return;
                                   }
                               }

                               JsonObject jo = (JsonObject)o;
                               String folderName = jo.getString("folder");
                               if (folders.stream().anyMatch(f -> f.endsWith(folderName))) {
                                   foundApps.put(appName, folderName);
                               }
                           } else {
                               // case where Manifest doesn't contain folder name

                               for (Map.Entry<String, Object> app: minimumImportVersions.getMap().entrySet()) {
                                   if (app.getKey().equals(appName) &&
                                           StringUtils.versionComparator.compare((String)o, app.getValue().toString()) < 0) {
                                       return;
                                   }
                               }

                               String i = i18n.getString(appName);
                               String translated = StringUtils.stripAccents(i == null ? appName : i);
                               if (folders.stream().anyMatch(f -> f.endsWith(translated))) {
                                   foundApps.put(appName, translated);
                               }
                           }

                       });

                       final JsonObject foundAppsWithSize = new JsonObject();
                       final List<Future<?>> getFoldersSize = new ArrayList<>();

                       foundApps.fieldNames().forEach(app -> {

                           String folder = foundApps.getString(app);
                           String folderBase = folders.stream().filter(f -> f.endsWith(folder)).findFirst().get();
                           String folderPath;
                           if ("workspace".equals(app) || "rack".equals(app)) {
                               folderPath = folderBase;
                           } else {
                               folderPath = folderBase + File.separator + "Documents";
                           }
                         Promise<Long> size = Promise.promise();
                           fs.readDir(folderBase, files ->
                           {
                               if(files.result().size() > 0) // Ignore empty folders
                               {
                                fs.exists(folderPath, exist -> {
                                    if (exist.result()) {
                                        Future<Long> promise = recursiveSize(folderPath);
                                        promise.onComplete(result -> {
                                            foundAppsWithSize.put(app, new JsonObject()
                                                    .put("folder", folder).put("size", result.result()));
                                            size.complete(result.result());
                                        });
                                    } else {
                                        foundAppsWithSize.put(app, new JsonObject()
                                                .put("folder", folder).put("size", 0L));
                                        size.complete(0L);
                                    }
                                });
                               }
                               else
                                size.complete(0L);
                           });
                           getFoldersSize.add(size.future());

                       });
                       Future.join(getFoldersSize).onComplete(completed -> {
                           reply.put("apps", foundAppsWithSize);
                           if(user != null)
                               getQuota(user, reply, replyWithQuota -> {
                                   handler.handle(new Either.Right<>(replyWithQuota));
                               });
                            else
                                handler.handle(new Either.Right<>(reply));
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
    public void launchImport(String userId, String userLogin, String userName, String importId,
      String locale, String host, JsonObject apps)
    {
        userImports.put(importId, new UserImport(apps.size()));

        fs.readDir(getUnzippedImportPath(importId), results -> {
            if (results.succeeded()) {
                if (results.result().size() == 1)
                {
                    JsonObject j = new JsonObject()
                            .put("action", handlerActionName)
                            .put("importId", importId)
                            .put("userId", userId)
                            .put("userLogin", userLogin)
                            .put("userName", userName)
                            .put("locale", locale)
                            .put("host", host)
                            .put("apps", apps)
                            .put("path", results.result().get(0));
                    eb.publish("user.repository", j);
                }
                else {
                    deleteArchive(importId);
                    log.error("[Archive] - Import could not be launched - wrong number of unzipped files");
                }
            } else {
                deleteArchive(importId);
                log.error("[Archive] - Import could not be deleted - no unzipped filed");
            }
        });
    }

    private String getImportPath(String importId)
    {
        return importPath + File.separator + importId;
    }

    private String getUnzippedImportPath(String importId)
    {
        return getImportPath(importId) + "_unzip";
    }

    @Override
    public void importFromFile(String fileName, String userId, String userLogin, String userName, String locale, String host, JsonObject archiveConfig)
    {
        String importId = fileName;
        analyzeArchive(null, importId, locale, archiveConfig, new Handler<Either<String, JsonObject>>()
        {
            @Override
            public void handle(Either<String, JsonObject> either)
            {
                if(either.isRight())
                {
                    JsonObject apps = either.right().getValue().getJsonObject("apps");
                    if (apps.isEmpty()) {
                        final String address = getImportBusAddress(importId);
                        JsonObject jo = new JsonObject()
                                .put("status", "ok")
                                .put("result", new JsonObject());
                        eb.request(address, jo);
                    } else {
                        launchImport(userId, userLogin, userName, importId, locale, host, apps);
                    }
                }
                else
                {
                    log.error("[Archive] - Import from file failure - analyze failed");
                }
            }
        });
    }

    @Override
    public void imported(String importId, String app, JsonObject importRapport) {
        UserImport userImport = userImports.get(importId);
        if (userImport == null) {
            JsonObject jo = new JsonObject()
                    .put("status", "error");
            eb.request(getImportBusAddress(importId), jo);
            deleteArchive(importId);
        } else {
            final boolean finished = userImport.addAppResult(app, importRapport);
            if (finished) {
                JsonObject jo = new JsonObject()
                        .put("status", "ok")
                        .put("result", userImport.getResults());
                eb.request(getImportBusAddress(importId), jo);
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
    Promise<Long> size = Promise.promise();
        fs.props(path, handler -> {
            if (handler.succeeded()) {
                FileProps props = handler.result();
                if (props.isDirectory()) {
                    fs.readDir(path, res -> {
                       if (res.failed()) {
                           size.complete(props.size());
                       } else {
                           List<Future> childrenSize = res.result().stream().map(this::recursiveSize).collect(Collectors.toList());
                           CompositeFuture.join(childrenSize).onComplete(compositeFutureAsyncResult -> {
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

        return size.future();
  }

}
