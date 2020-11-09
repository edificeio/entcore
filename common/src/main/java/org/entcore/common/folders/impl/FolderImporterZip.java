package org.entcore.common.folders.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.service.impl.MongoDbRepositoryEvents;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.MimeTypeUtils;
import org.entcore.common.utils.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class FolderImporterZip {
    static final Logger logger = LoggerFactory.getLogger(FolderImporterZip.class);
    private final Vertx vertx;
    private final FolderManager manager;
    private final List<String> encodings = new ArrayList<>();
    private Optional<String> guessedEncoding = Optional.empty();

    public FolderImporterZip(final Vertx v, final FolderManager aManager) {
        this.vertx = v;
        this.manager = aManager;
        try {
            encodings.add("UTF-8");
            encodings.add("ISO-8859-1");
            final String encodingList = (String) v.sharedData().getLocalMap("server").get("encoding-available");
            if (encodingList != null) {
                final JsonArray encodingJson = new JsonArray(encodingList);
                for (final Object o : encodingJson) {
                    if (!encodings.contains(o.toString())) {
                        encodings.add(o.toString());
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    public static Future<FolderImporterZipContext> createContext(Vertx vertx, UserInfos user, HttpServerRequest request) {
        final Future<FolderImporterZipContext> future = Future.future();
        request.uploadHandler(upload -> {
            if (!future.isComplete()) {
                createContext(vertx, user, upload).setHandler(future.completer());
            }
        });
        request.exceptionHandler(res -> {
            if (!future.isComplete()) {
                future.fail("folder.zip.upload.notfound");
            }
        });
        return future;
    }

    public static Future<FolderImporterZipContext> createContext(Vertx vertx, UserInfos user, ReadStream<Buffer> buffer) {
        final Future<FolderImporterZipContext> future = Future.future();
        final String name = UUID.randomUUID().toString() + ".zip";
        final String zipPath = Paths.get(System.getProperty("java.io.tmpdir"), name).normalize().toString();
        buffer.pause();
        vertx.fileSystem().open(zipPath, new OpenOptions().setTruncateExisting(true).setCreate(true).setWrite(true), fileRes -> {
            if (fileRes.succeeded()) {
                final AsyncFile file = fileRes.result();
                final Pump pump = Pump.pump(buffer, file);
                buffer.endHandler(r -> {
                    file.end();
                    future.complete(new FolderImporterZipContext(zipPath, user));
                });
                buffer.exceptionHandler(e -> {
                    file.end();
                    future.fail(e);
                });
                pump.start();
                buffer.resume();
            } else {
                future.fail(fileRes.cause());
            }
        });
        return future;
    }

    public Future<Void> doPrepare(final FolderImporterZipContext context) {
        if (context.isPrepared()) {
            return context.prepare;
        }
        context.prepare = Future.future();
        FileUtils.guessZipEncondig(vertx, context.zipPath, encodings, resGuess -> {
            if(resGuess.succeeded()){
                guessedEncoding = Optional.ofNullable(resGuess.result().getEncoding());
            }
            FileUtils.visitZip(vertx, context.zipPath, guessedEncoding, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        context.createDocument(file, attrs.size());
                    } catch (Exception e) {
                        logger.warn("Failed to visitFile :" + e.getMessage());
                        context.addError("", null, "workspace.import.zip.error.encoding", e.getMessage());
                    }
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    try {
                        if (!dir.toString().equals("/")) {
                            final JsonObject res = context.createDirectory(dir);
                            context.pushAncestor(res);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to visitDir :" + e.getMessage());
                        context.addError("", null, "workspace.import.zip.error.encoding", e.getMessage());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    context.popAncestor();
                    return super.postVisitDirectory(dir, exc);
                }
            }, context.prepare.completer());
        });
        return context.prepare;
    }

    private Future<List<FileInfo>> copyToTemp(FolderImporterZipContext context) {
        final Future<List<FileInfo>> future = Future.future();
        final Collection<FileInfo> infos = context.docToInsertById.values();
        if (infos.isEmpty()) {
            future.complete(new ArrayList<>());
            return future;
        }
        final String fileName = Paths.get(context.zipPath).getFileName().toString().replaceAll(".zip", "");
        final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), fileName).normalize();
        vertx.fileSystem().mkdir(tempDir.toString(), dir -> {
            if (dir.succeeded()) {
                context.toClean.add(tempDir.toString());
                FileUtils.executeInZipFileSystem(vertx, context.zipPath, guessedEncoding, fs -> {
                    final List<FileInfo> newFiles = new ArrayList<>();
                    for (final FileInfo info : infos) {
                        try {
                            final Path pathFile = fs.getPath(info.path);
                            final String currentName = pathFile.getFileName().toString();
                            final Path newPath = tempDir.resolve(currentName);
                            Files.copy(pathFile, newPath);
                            newFiles.add(new FileInfo(info.data, info.size, newPath.toString()));
                        } catch (Exception e) {
                            logger.warn("Failed to copy temp file: " + e.getMessage());
                        }
                    }
                    return newFiles;
                }).setHandler(r -> {
                    future.handle(r);
                });
            } else {
                future.fail(dir.cause());
            }
        });
        return future;
    }

    public Future<JsonObject> doFinalize(final FolderImporterZipContext context) {
        final Future<JsonObject> futureFinal = Future.future();
        doPrepare(context).compose(prep -> {
            if (context.hasErrors()) {
                context.cancel();
                Future<JsonObject> failure = Future.future();
                //failure.complete(context.getResult());
                failure.fail(context.errors.getJsonObject(0).getString("message"));
                return failure;
            }
            return copyToTemp(context).compose(newFiles -> {
                final List<Future> futures = new ArrayList<>();
                for (final FileInfo newFile : newFiles) {
                    final Future<Void> future = Future.future();
                    manager.importFile(newFile.path, null, context.userId, writtenFile -> {
                        if (writtenFile.getString("status").equals("ok")) {
                            final String storageId = DocumentHelper.getId(writtenFile);
                            DocumentHelper.setFileId(newFile.data, storageId);
                            DocumentHelper.setThumbnails(newFile.data, new JsonObject());
                            final Long size = writtenFile.getJsonObject("metadata", new JsonObject()).getLong("size");
                            if (size != null) {
                                DocumentHelper.getMetadata(newFile.data).put("size", size);
                            }
                            final String extension = StringUtils.getFileExtension(newFile.path);
                            final String mime = MimeTypeUtils.getContentTypeForExtension(extension).orElse(MimeTypeUtils.OCTET_STREAM);
                            DocumentHelper.setContentType(newFile.data, mime);
                            future.complete();
                        } else {
                            final String error = writtenFile.getString("message");
                            final String docId = DocumentHelper.getId(newFile.data);
                            context.addError(newFile.path, docId, "Failed to write the archived file", error);
                            future.fail(error);
                        }
                    });
                    futures.add(future);
                }
                return CompositeFuture.all(futures).compose(res -> {
                    final List<JsonObject> list = context.getAllObjects();
                    final Future<JsonObject> future = Future.future();
                    MongoDbRepositoryEvents.importDocuments("documents", list, "", false, r -> {
                        future.complete(context.getResult());
                    });
                    return future;
                });
            });
        }).setHandler(r -> {
            futureFinal.handle(r);
            clean(context);
        });
        return futureFinal;
    }

    private void clean(FolderImporterZipContext context) {
        final Set<String> clean = new HashSet<>(context.toClean);
        if (context.cleanZip) {
            clean.add(context.zipPath);
        }
        for (final String path : clean) {
            vertx.fileSystem().deleteRecursive(path, true, resDel -> {
                if (resDel.failed()) {
                    logger.error("Failed to clean path: " + path, resDel.cause());
                }
            });
        }
    }

    public Future<Long> getTotalSize(FolderImporterZipContext context) {
        return doPrepare(context).compose(res -> {
            final Future<Long> future = Future.future();
            long total = 0;
            for (final FileInfo info : context.docToInsertById.values()) {
                total += info.size;
            }
            future.complete(total);
            return future;
        });
    }

    public static class FileInfo {
        private final JsonObject data;
        private final Long size;
        private final String path;

        FileInfo(JsonObject d, Long s, String p) {
            this.data = d;
            this.size = s;
            this.path = p;
        }
    }

    public static class FolderImporterZipContext {
        private final String zipPath;
        private final String userId;
        private final String userName;
        private final Map<String, FileInfo> docToInsertById = new HashMap<>();
        private final Map<String, JsonObject> dirToInsertById = new HashMap<>();
        private final JsonArray errors = new JsonArray();
        private final Stack<JsonObject> ancestors = new Stack<>();
        private final Set<String> toClean = new HashSet<>();
        private Future<Void> prepare;
        private boolean cleanZip = false;

        public FolderImporterZipContext(final String aZipPath, UserInfos user) {
            this(aZipPath, user.getUserId(), user.getUsername());
        }

        public FolderImporterZipContext(final String aZipPath, final String aUserId, final String aUserName) {
            this.zipPath = aZipPath;
            this.userId = aUserId;
            this.userName = aUserName;
        }

        public FolderImporterZipContext setRootFolder(final JsonObject parentFolder) {
            ancestors.add(parentFolder);
            return this;
        }

        public JsonObject createDocument(final Path path, final Long size) {
            final String fileName = path.getFileName().toString();
            final JsonObject document = DocumentHelper.initFile(null, userId, userName, fileName, "media-library");
            if (!ancestors.isEmpty()) {
                final JsonObject parent = ancestors.lastElement();
                final String parentId = DocumentHelper.getId(parent);
                DocumentHelper.setParent(document, parentId);
                InheritShareComputer.mergeShared(parent, document, true);
            }
            DocumentHelper.setFileName(document, fileName);
            final String id = UUID.randomUUID().toString();
            DocumentHelper.setId(document, id);
            docToInsertById.put(id, new FileInfo(document, size, path.toString()));
            return document;
        }

        public JsonObject createDirectory(final Path path) {
            final String dirName = path.getFileName().toString().replaceAll("/$", "");
            final JsonObject directory = DocumentHelper.initFolder(null, userId, userName, dirName, "media-library");
            if (!ancestors.isEmpty()) {
                final JsonObject parent = ancestors.lastElement();
                final String parentId = DocumentHelper.getId(parent);
                DocumentHelper.setParent(directory, parentId);
                InheritShareComputer.mergeShared(parent, directory, true);
            }
            final String id = UUID.randomUUID().toString();
            DocumentHelper.setId(directory, id);
            dirToInsertById.put(id, directory);
            return directory;
        }

        public void pushAncestor(final JsonObject dir) {
            ancestors.push(dir);
        }

        public JsonObject popAncestor() {
            if (ancestors.isEmpty()) {
                return null;
            }
            return ancestors.pop();
        }

        public void addError(final String path, final String docId, final String message, final String details) {
            errors.add(
                    new JsonObject()
                            .put("path", path)
                            .put("message", message)
                            .put("details", details)
            );
            this.docToInsertById.remove(docId);
        }

        public boolean hasErrors() {
            return this.errors.size() > 0;
        }

        public void cancel() {
            this.docToInsertById.clear();
            this.dirToInsertById.clear();
        }

        public List<JsonObject> getAllObjects() {
            final List<JsonObject> all = new ArrayList<>();
            all.addAll(dirToInsertById.values());
            all.addAll(docToInsertById.values().stream().map(e -> e.data).collect(Collectors.toList()));
            return all;
        }

        public boolean isPrepared() {
            return this.prepare != null;
        }

        public String getUserId() {
            return userId;
        }

        public JsonObject getResult() {
            final JsonObject result = new JsonObject();
            result.put("errors", this.errors);
            result.put("saved", new JsonArray(getAllObjects()));
            if (!ancestors.isEmpty()) {
                result.put("root", ancestors.firstElement());
            }
            return result;
        }

        public FolderImporterZipContext setCleanZip(boolean clean) {
            this.cleanZip = clean;
            return this;
        }
    }
}
