package org.entcore.common.folders.impl;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.service.impl.MongoDbRepositoryEvents;
import org.entcore.common.storage.AntivirusClient;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.MimeTypeUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.FileValidator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public class FolderImporterZip {
    static final Logger logger = LoggerFactory.getLogger(FolderImporterZip.class);
    private final Vertx vertx;
    private final FolderManager manager;
    private final List<String> encodings = new ArrayList<>();
    private final Optional<AntivirusClient> antivirusClient;
    private final Optional<FileValidator> fileValidator;

    public FolderImporterZip(final Vertx v, final FolderManager aManager) {
        this.vertx = v;
        this.manager = aManager;
        this.fileValidator = FileValidator.create(v);
        this.antivirusClient =  AntivirusClient.create(v);
        try {
            encodings.add("UTF-8");
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
            logger.warn("An error occurred while initializing importer", e);
        }
    }

    public Future<Optional<String>> getGuessedEncoding(FolderImporterZipContext context) {
        if (context.guessedEncodingCache == null) {
            final Promise<Optional<String>> future = Promise.promise();
            FileUtils.guessZipEncondig(vertx, context.zipPath, encodings, r -> {
                if (r.succeeded()) {
                    context.guessedEncodingCache = Optional.ofNullable(r.result().getEncoding());
                } else {
                    context.guessedEncodingCache = Optional.empty();
                }
                future.complete(context.guessedEncodingCache);
            });
            return future.future();
        } else {
            return Future.succeededFuture(context.guessedEncodingCache);
        }
    }

    public static Future<FolderImporterZipContext> createContext(Vertx vertx, UserInfos user, HttpServerRequest request) {
        final String message = I18n.getInstance().translate("workspace.invalidfile.placeholder", Renders.getHost(request), I18n.acceptLanguage(request));
        final Promise<FolderImporterZipContext> future = Promise.promise();
        request.uploadHandler(upload -> {
            createContext(vertx, user, upload, message).onComplete(future);
        });
        request.exceptionHandler(res -> {
            future.tryFail("folder.zip.upload.notfound");
        });
        return future.future();
    }

    public static Future<FolderImporterZipContext> createContext(Vertx vertx, UserInfos user, ReadStream<Buffer> buffer, String invalidMessage) {
        final Promise<FolderImporterZipContext> future = Promise.promise();
        final String name = UUID.randomUUID() + ".zip";
        final String importPath = getImportPath(vertx);
        final String zipPath = Paths.get(importPath, name).normalize().toString();
        buffer.pause();
        vertx.fileSystem().open(zipPath, new OpenOptions().setTruncateExisting(true).setCreate(true).setWrite(true), fileRes -> {
            if (fileRes.succeeded()) {
                final AsyncFile file = fileRes.result();
                final Pump pump = Pump.pump(buffer, file);
                buffer.endHandler(r -> {
                    file.end();
                    future.complete(new FolderImporterZipContext(zipPath, importPath, user, invalidMessage));
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
        return future.future();
    }

    /**
     * Looks into the configuration to fetch the path to import the archive in the following order :
     * - 'import-path' in the configuration of the module
     * - 'import-path' in the shared configuration of the ENT
     * - 'java.io.tmpdir' environment variable if all else fails
     * @param vertx Vertx instance of the called
     * @return The path to import the zip file
     */
    private static String getImportPath(Vertx vertx) {
        String importPath = vertx.getOrCreateContext().config().getString("import-path");
        if(org.apache.commons.lang3.StringUtils.isEmpty(importPath)) {
            final LocalMap<Object, Object> localMap = vertx.sharedData().getLocalMap("server");
            importPath = (String)localMap.getOrDefault("import-path", System.getProperty("java.io.tmpdir"));
        }
        return importPath;
    }

    public Future<Void> doPrepare(final FolderImporterZipContext context) {
        if (context.isPrepared()) {
            return context.prepare.future();
        }
        context.prepare = Promise.promise();
        final Promise<Void> futureAntivirus = Promise.promise();
        if(antivirusClient.isPresent()){
            antivirusClient.get().scan(context.zipPath, futureAntivirus);
        } else {
            logger.warn("Could not check zip because antivirus client is missing");
            futureAntivirus.complete();
        }
        futureAntivirus.future()
        .compose( r-> getGuessedEncoding(context))
        .onComplete(r -> {
            if(r.failed()){
                final Throwable e = r.cause();
                logger.warn("Failed to analyze zip :" + e.getMessage());
                context.addError("", null, "workspace.import.zip.invalid", e.getMessage());
                context.prepare.fail("workspace.import.zip.invalid");
                return;
            }
            final Optional<String> guessedEncoding = r.result();
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
            }, e -> {
                if(e.succeeded()){
                    if(this.fileValidator.isPresent()){
                        final List<Future> futures = new ArrayList<>();
                        for(final Map.Entry<String, FileInfo> entry : context.docToInsertById.entrySet()){
                            final FileInfo info = entry.getValue();
                            final String key = entry.getKey();
                            final String name = Paths.get(info.path).getFileName().toString();
                            final JsonObject meta = new JsonObject().put("filename", name).put("size", info.size);
                            final JsonObject vContext = new JsonObject();
                            final Promise future = Promise.promise();
                            this.fileValidator.get().process(meta,vContext, valid ->{
                                if(valid.failed()){
                                    final FileInfo copy = new FileInfo(info.data, info.size, info.path, true);
                                    context.docToInsertById.put(key, copy);
                                }
                                future.complete();
                            });
                            futures.add(future.future());
                        }
                        CompositeFuture.all(futures).onComplete((ee)-> {
                            if(ee.succeeded()){
                                context.prepare.complete();
                            } else {
                                context.prepare.fail(ee.cause());
                            }
                        });
                    } else {
                        context.prepare.complete();
                    }
                } else {
                    context.prepare.fail(e.cause());
                }
            });
        });
        return context.prepare.future();
    }

    private Future<List<FileInfo>> copyToTemp(FolderImporterZipContext context, Optional<String> guessedEncoding) {
        final Promise<List<FileInfo>> future = Promise.promise();
        final Collection<FileInfo> infos = context.docToInsertById.values();
        if (infos.isEmpty()) {
            future.complete(new ArrayList<>());
            return future.future();
        }
        final String fileName = Paths.get(context.zipPath).getFileName().toString().replaceAll(".zip", "");
        final Path tempDir = Paths.get(context.tmpPath, fileName).normalize();
        vertx.fileSystem().mkdir(tempDir.toString(), dir -> {
            if (dir.succeeded()) {
                context.toClean.add(tempDir.toString());
                final List<FileInfo> newFiles = new ArrayList<>();
                FileUtils.visitZip(vertx, context.zipPath, guessedEncoding, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        try {
                            if (path.getFileName() != null) {
                                final String strPath = path.toString();
                                final Optional<FileInfo> found = infos.stream().filter(e -> e.path.equals(strPath)).findFirst();
                                if (found.isPresent()) {
                                    final FileInfo info = found.get();
                                    final String currentName = path.getFileName().toString();
                                    final Path newPath = tempDir.resolve(currentName);
                                    final long size = info.getRealSize(context);
                                    final Path destPath;
                                    if(info.invalid){
                                        destPath = newPath.resolveSibling(newPath+".txt");
                                        Files.write(destPath, context.invalidMessage.getBytes());
                                        //update filename and size
                                        DocumentHelper.setFileName(info.data, destPath.getFileName().toString());
                                        DocumentHelper.setFileSize(info.data, size);
                                    } else {
                                        destPath = newPath;
                                        Files.copy(path, destPath);
                                    }
                                    newFiles.add(new FileInfo(info.data, size, destPath.toString(), info.invalid));
                                } else {
                                    logger.warn("Could not found path from prepared list:" + strPath);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to copy temp file: " + e.getMessage());
                        }
                        return super.visitFile(path, attrs);
                    }
                }, r -> {
                    future.handle(new DefaultAsyncResult<>(newFiles));
                });
            } else {
                future.fail(dir.cause());
            }
        });
        return future.future();
    }

    public Future<JsonObject> doFinalize(final FolderImporterZipContext context) {
        final Promise<JsonObject> futureFinal = Promise.promise();
        getGuessedEncoding(context).onComplete(rGuess -> {
            final Optional<String> guess = rGuess.result();
            doPrepare(context).compose(prep -> {
                if (context.hasErrors()) {
                    context.cancel();
                    Promise<JsonObject> failure = Promise.promise();
                    //failure.complete(context.getResult());
                    failure.fail(context.errors.getJsonObject(0).getString("message"));
                    return failure.future();
                }
                return copyToTemp(context, guess).compose(newFiles -> {
                    final List<Future> futures = new ArrayList<>();
                    for (final FileInfo newFile : newFiles) {
                        final Promise<Void> future = Promise.promise();
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
                        futures.add(future.future());
                    }
                    return CompositeFuture.all(futures).compose(res -> {
                        final List<JsonObject> list = context.getAllObjects();
                        final Promise<JsonObject> future = Promise.promise();
                        MongoDbRepositoryEvents.importDocuments("documents", list, "", false, r -> {
                            future.complete(context.getResult());
                        });
                        return future.future();
                    });
                });
            }).onComplete(r -> {
                futureFinal.handle(r);
                clean(context);
            });
        });
        return futureFinal.future();
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
            final Promise<Long> future = Promise.promise();
            long total = 0;
            for (final FileInfo info : context.docToInsertById.values()) {
                total += info.getRealSize(context);
            }
            future.complete(total);
            return future.future();
        });
    }

    public static class FileInfo {
        private final JsonObject data;
        private final Long size;
        private final String path;
        private final boolean invalid;

        FileInfo(JsonObject d, Long s, String p, boolean invalid) {
            this.data = d;
            this.size = s;
            this.path = p;
            this.invalid = invalid;
        }

        public long getRealSize(final FolderImporterZipContext context){
            if(this.invalid){
                return context.invalidMessage.length();
            }else{
                return this.size;
            }
        }
    }

    public static class FolderImporterZipContext {
        private final String zipPath;
        private final String tmpPath;
        private final String userId;
        private final String userName;
        private final Map<String, FileInfo> docToInsertById = new HashMap<>();
        private final Map<String, JsonObject> dirToInsertById = new HashMap<>();
        private final JsonArray errors = new JsonArray();
        private final Stack<JsonObject> ancestors = new Stack<>();
        private final Set<String> toClean = new HashSet<>();
        private Promise<Void> prepare;
        private boolean cleanZip = false;
        private final String invalidMessage;
        private Optional<String> guessedEncodingCache;

        public FolderImporterZipContext(final String aZipPath, final String tmpPath, UserInfos user, String invalidMessage) {
            this(aZipPath, tmpPath, user.getUserId(), user.getUsername(), invalidMessage);
        }

        public FolderImporterZipContext(final String aZipPath, final String tmpPath, final String aUserId, final String aUserName, final String invalidMessage) {
            this.zipPath = aZipPath;
            this.userId = aUserId;
            this.userName = aUserName;
            this.invalidMessage = invalidMessage;
            this.tmpPath = isEmpty(tmpPath) ? System.getProperty("java.io.tmpdir") : tmpPath;
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
            docToInsertById.put(id, new FileInfo(document, size, path.toString(), false));
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
