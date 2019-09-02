package org.entcore.archive.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.archive.services.ImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class DefaultImportService implements ImportService {

    private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);


    private final Vertx vertx;
    private final FileSystem fs;
    private final Storage storage;
    private final String importPath;

    public DefaultImportService(Vertx vertx, Storage storage, String importPath) {
        this.storage = storage;
        this.importPath = importPath + File.separator + "import";
        this.vertx = vertx;
        this.fs = vertx.fileSystem();
    }

    @Override
    public void uploadArchive(HttpServerRequest request, Handler<Either<String, String>> handler) {
        /*storage.writeUploadFile(request, uploaded -> {
            if ("ok".equals(uploaded.getString("status"))) {
                String[] id = {uploaded.getString("_id")};
                JsonObject aliases = new JsonObject().put(id[0], id[0]);
                String fileImportPath = importPath + File.separator + id[0];
                fs.mkdirs(fileImportPath, created -> {
                    storage.writeToFileSystem(id, fileImportPath, aliases, written -> {
                        if ("ok".equals(written.getString("status"))) {
                            handler.handle(new Either.Right<>(id[0]));
                        } else {
                            handler.handle(new Either.Left<>(written.getString("message")));
                        }
                    });
                });

            } else {
                handler.handle(new Either.Left<>(uploaded.getString("message")));
            }
        });*/
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
    public void analyzeArchive(String importId, Handler<Either<String, JsonObject>> handler) {
        final String filePath = importPath + File.separator + importId + File.separator + importId;
        try {
            FileUtils.unzip(filePath, FileUtils.getParentPath(filePath));
        } catch (IOException ioe) {
            deleteArchive(importId, res -> {
                if (res.isLeft()) {
                    log.error("[Archive] - Import could not be deleted.", res.left().getValue());
                }
            });
            handler.handle(new Either.Left<>(ioe.getMessage()));
            return;
        }
        storage.readFile(importId, buffer -> {
            handler.handle(new Either.Right<>(new JsonObject()));

        });
    }

    public void deleteArchive(String importId, Handler<Either<String, JsonObject>> handler) {
        fs.deleteRecursive(importPath + File.separator + importId, true, deleted -> {
            if (deleted.succeeded()) {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            } else {
                handler.handle(new Either.Left<>(deleted.cause().getMessage()));
            }
        });
    }

}
