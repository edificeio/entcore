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

 package org.entcore.common.storage.impl;

import com.google.common.collect.Lists;
import io.edifice.storage.common.BucketStats;
import io.edifice.storage.common.messaging.UploadedFileMessage;
import io.edifice.storage.common.validation.FileValidator;
import io.edifice.storage.s3.DefaultAsyncResult;
import io.edifice.storage.s3.S3Client;
import io.edifice.storage.s3.StorageObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;


import org.entcore.common.storage.AntivirusClient;

import org.entcore.common.storage.FallbackStorage;
import io.edifice.storage.common.FileStats;
import io.edifice.storage.common.Storage;


import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.edifice.storage.s3.S3Client.encodeUrlPath;

public class S3Storage implements Storage {
    
    private final S3Client s3Client;
    private final String bucket;
    private final FileSystem fs;

    private AntivirusClient antivirus;
    private FileValidator validator;

    private FallbackStorage fallbackStorage;

    private static final int DOWNLOAD_TO_FS_BATCH_SIZE = 10;

    private static final Logger log = LoggerFactory.getLogger(S3Storage.class);
    
    public S3Storage(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec, boolean keepAlive, int timeout, int threshold, long openDelay, int poolSize) {
        this.bucket = bucket;
        this.s3Client = new S3Client(vertx, uri, accessKey, secretKey, region, bucket, ssec, keepAlive, timeout, threshold, openDelay, poolSize);
        this.fs = vertx.fileSystem();
    }

    @Override
    public void stats(final Handler<AsyncResult<BucketStats>> handler) {
        final BucketStats bucketStats = new BucketStats();
        bucketStats.setStorageSize(Integer.MAX_VALUE);
        handler.handle(Future.succeededFuture(bucketStats));
    }
    
    @Override
    public void fileStats(String id, Handler<AsyncResult<FileStats>> handler) {
        s3Client.getFileStats(id, handler);
    }
    
    @Override
    public void findByFilenameEndingWith(String name, Handler<AsyncResult<JsonArray>> handler) {
        s3Client.getObjectsEndingWith(name, ar -> {
            if (ar.succeeded()) {
                handler.handle(new DefaultAsyncResult<>(new JsonArray(ar.result())));
            }
            else {
                handler.handle(Future.failedFuture("Error listing files with findByFilenameEndingWith method"));
            }
        });
    }
    
    @Override
    public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
        writeUploadFile(request, null, handler);
    }
    
    @Override
    public void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler) {
        s3Client.uploadFile(request, maxSize, validator, ar -> {
            handler.handle(ar);
            if (ar.getString("status").equals("ok")) {
                scanFile(S3Client.getPath(ar.getString("_id")));
            }
        });
    }
    
    @Override
    public void writeBuffer(Buffer buff, String contentType, String filename, final Handler<JsonObject> handler) {
        writeBuffer(null, buff, contentType, filename, handler);
    }
    
    @Override
    public void writeBuffer(String id, Buffer buff, String contentType, String filename, final Handler<JsonObject> handler) {
        writeBuffer(null, id, buff, contentType, filename, handler);
    }
    
    @Override
    public void writeBuffer(String basePath, String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler) {
        writeBuffer(null, id, buff, contentType, filename, false, handler);
    }
    
    @Override
    public void writeBuffer(final String path, final String id, final Buffer buff, final String contentType, final String filename, final boolean safe, final Handler<JsonObject> handler) {
        StorageObject o = new StorageObject(id, buff, filename, contentType);
        writeStorageObject(handler, o);
    }

    private void writeStorageObject(final Handler<JsonObject> handler, final StorageObject o) {
        s3Client.writeFile(o, ar -> {
            JsonObject j = new JsonObject();

            if (ar.succeeded()) {
                final JsonObject metadata = new JsonObject().put("content-type", o.getContentType())
                .put("filename", o.getFilename()).put("size", o.getBuffer().length());
                j.put("status", "ok").put("_id", ar.result()).put("metadata", metadata);
            } else {
                j.put("status", "error").put("message", ar.cause().getMessage());
            }

            handler.handle(j);
            if (j.getString("status") == "ok") {
                scanFile(S3Client.getPath(j.getString("_id")));
            }
        });
    }
    
    @Override
    public Future<JsonObject> writeBufferStream(ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
        return writeBufferStream(S3Client.getPath(UUID.randomUUID().toString()), bufferReadStream, contentType, filename);
    }
    
    @Override
    public Future<JsonObject> writeBufferStream(String id, ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
        Promise<JsonObject> promise = Promise.promise();
        
        s3Client.writeBufferStream(id, bufferReadStream, contentType, filename, ar -> {
            if (ar.succeeded()) {
                JsonObject result = ar.result();

                promise.complete(result);
                if (result.getString("status").equals("ok")) {
                    scanFile(S3Client.getPath(result.getString("_id")));
                }
            }
            else {
                promise.fail("An error as occured");
            }
        });
        
        return promise.future();
    }
    
    @Override
    public void writeFsFile(String filename, Handler<JsonObject> handler)
    {
        writeFsFile(S3Client.getPath(UUID.randomUUID().toString()), filename, handler);
    }
    
    @Override
    public void writeFsFile(String id, String filename, Handler<JsonObject> handler) {
        s3Client.writeFromFileSystem(id, filename, bucket, json -> {
            if (json.getString("status") == "ok") {
                scanFile(S3Client.getPath(json.getString("_id")));
            }
            handler.handle(json);
        });
    }

    @Override
    public void writeToFileSystem(String [] ids, String destinationPath, JsonObject alias,
    final Handler<JsonObject> handler) {
        final AtomicInteger count = new AtomicInteger(ids.length);
        final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
        for (final String id: ids) {
            if (id == null || id.isEmpty()) {
                count.decrementAndGet();
                continue;
            }
            String d = destinationPath + File.separator + alias.getString(id, id);

            getReadPath(id, ar -> {
                s3Client.writeToFileSystem(ar.result(), d, new Handler<AsyncResult<String>>() {
                    @Override
                    public void handle(AsyncResult<String> event) {
                        if (event.failed()) {
                            errors.add(new JsonObject().put("id", ar.result())
                            .put("message", event.cause().getMessage()));
                        }
                        if (count.decrementAndGet() <= 0) {
                            JsonObject j = new JsonObject();
                            if (errors.size() == 0) {
                                handler.handle(j.put("status", "ok"));
                            } else {
                                handler.handle(j.put("status", "error").put("errors", errors)
                                .put("message", errors.encode()));
                            }
                        }
                    }
                });
            });
        }
    }
    
    @Override
    public void readFile(String id, final Handler<Buffer> handler) {
        getReadPath(id, ar -> {
            s3Client.readFile(ar.result(), new Handler<AsyncResult<StorageObject>>() {
                @Override
                public void handle(AsyncResult<StorageObject> event) {
                    if (event.succeeded()) {
                        handler.handle(event.result().getBuffer());
                    } else {
                        handler.handle(null);
                    }
                }
            });
        });
    }
    
    @Override
    public void readStreamFile(String id, Handler<ReadStream<Buffer>> handler) {
        getReadPath(id, arId -> {
            s3Client.readFileStream(arId.result(), ar -> {
                if (ar.succeeded()) {
                    HttpClientResponse response = ar.result();
                    if (response.statusCode() == 200) {
                        handler.handle(response);
                    }
                    else {
                        handler.handle(null);
                    }
                }
                else {
                    handler.handle(null);
                }
            });
        });
    }
    
    @Override
    public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
        sendFile(id, downloadName, request, inline, metadata, aVoid -> {});
    }
    
    @Override
    public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata, Handler<AsyncResult<Void>> resultHandler) {
        getReadPath(id, ar -> {
            s3Client.downloadFile(ar.result(), request, inline, downloadName, metadata, null, resultHandler);
        });
    }

    @Override
    public Future<byte[]> readFileToMemory(final UploadedFileMessage uploadedFileMessage) {
        final String id = uploadedFileMessage.getId();
        final Promise<byte[]> onFileRead = Promise.promise();
        
        getReadPath(id, ar -> {
            s3Client.readFile(ar.result(), storageObject -> {
                if (storageObject.succeeded()) {
                    onFileRead.complete(storageObject.result().getBuffer().getBytes());
                }
                else {
                    onFileRead.fail(storageObject.cause());
                }
            });
        });
        
        return onFileRead.future();
    }

    @Override
    public Future<Void> deleteRecursive(String srcDir) {
        return s3Client.listFilesByPrefix(srcDir.charAt(0) == '/' ? srcDir.replaceFirst("/", "") : srcDir)
                .compose(s3FilePaths -> deleteFromS3(s3FilePaths, 0));
    }

    private Future<Void> deleteFromS3(List<S3Client.S3FileInfo> s3FilePaths, int index) {
        if(index < 0 || index >= s3FilePaths.size()) {
            return Future.succeededFuture();
        } else {
            final S3Client.S3FileInfo path = s3FilePaths.get(index);
            return Future.future(p -> {
                s3Client.deleteFileWithPath(path.getPath(), s3Client.getDefaultBucket(), e -> {
                    deleteFromS3(s3FilePaths, index + 1).onComplete(p);
                });
            });
        }
    }

    @Override
    public Future<Void> moveDirectoryToFs(String srcDir, String targetDir) {
      return this.fs.mkdirs(targetDir)
        .compose(e -> s3Client.listFilesByPrefix(srcDir.charAt(0) == '/' ? srcDir.replaceFirst("/", "") : srcDir))
        .compose(s3FilePaths -> downloadToFs(s3FilePaths, srcDir, targetDir, true));
    }

    @Override
    public Future<Void> copyDirectoryToFs(String srcDir, String targetDir) {
        return this.fs.mkdirs(targetDir)
            .compose(e -> s3Client.listFilesByPrefix(encodeUrlPath(srcDir.charAt(0) == '/' ? srcDir.replaceFirst("/", "") : srcDir)))
            .compose(s3FilePaths -> downloadToFs(s3FilePaths, srcDir, targetDir, false));
    }

    private Future<Void> downloadToFs(final List<S3Client.S3FileInfo> s3FilePaths, String srcDir, String targetDir, final boolean deleteAfterMove) {
    final List<List<S3Client.S3FileInfo>> batches = Lists.partition(s3FilePaths, DOWNLOAD_TO_FS_BATCH_SIZE);
    return downloadBatchToFs(srcDir, targetDir, batches, 0, deleteAfterMove);
  }

  private Future<Void> downloadBatchToFs(final String srcDir,
                                         final String targetDir,
                                         final List<List<S3Client.S3FileInfo>> batches,
                                         final int batchIndex,
                                         final boolean deleteAfterMove) {
      if(batchIndex >= batches.size()) {
        return Future.succeededFuture();
      }
      // Download all files of a batch then move on to the next
      final List<Future<Void>> futures = batches.get(batchIndex).stream().map(fileInfo -> {
        final Promise<Void> promise = Promise.promise();
        final String path = fileInfo.getPath();
        this.s3Client.writeToFileSystemWithId(path, getPathInTargetDirectory(File.separatorChar + fileInfo.getPath(), srcDir, targetDir), e -> {
          if(e.succeeded()) {
            log.debug("Successfully downloaded file " + path);
            if(deleteAfterMove) {
                this.s3Client.deleteFileWithPath(path, x -> {
                    if(x.succeeded()) {
                        log.debug("Successfully deleted file " + path);
                    } else {
                        log.warn("Could not delete file " + path);
                    }
                });
            }
            promise.complete();
          } else {
            log.error("could not download file " + path + ": ", e.cause());
            promise.fail(e.cause());
          }
        });
        return promise.future();
      }).collect(Collectors.toList());
      return Future.all(futures)
        .flatMap(e -> downloadBatchToFs(srcDir, targetDir, batches, batchIndex + 1, deleteAfterMove));
  }

  private String getPathInTargetDirectory(final String path, final String srcDir, final String targetDir) {
      final String src = srcDir.charAt(0) == '/' ? srcDir : '/' + srcDir;
      return path.replaceFirst(src, targetDir);
  }

  @Override
    public void copyFile(String id, final Handler<JsonObject> handler) {
        s3Client.copyFile(id, new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                JsonObject j = new JsonObject();
                if (event.succeeded()) {
                    j.put("status", "ok").put("_id", event.result());
                } else {
                    j.put("status", "error").put("message", event.cause().getMessage());
                }
                handler.handle(j);
            }
        });
    }
    
    @Override
    public void removeFile(String id, final Handler<JsonObject> handler) {
        s3Client.deleteFile(id, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                JsonObject j = new JsonObject();
                if (event.succeeded()) {
                    j.put("status", "ok");
                } else {
                    j.put("status", "error").put("message", event.cause().getMessage());
                }
                handler.handle(j);
            }
        });
    }

    @Override
    public void removeFiles(JsonArray ids, final Handler<JsonObject> handler) {
        final AtomicInteger count = new AtomicInteger(ids.size());
        final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
        for (final Object o: ids) {
            s3Client.deleteFile(o.toString(), new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    if (event.failed()) {
                        errors.add(new JsonObject().put("id", o.toString())
                        .put("message", event.cause().getMessage()));
                    }
                    if (count.decrementAndGet() <= 0) {
                        JsonObject j = new JsonObject();
                        if (errors.size() == 0) {
                            handler.handle(j.put("status", "ok"));
                        } else {
                            handler.handle(j.put("status", "error").put("errors", errors));
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public String getProtocol() {
        return "s3";
    }
    
    @Override
    public String getBucket() {
        return bucket;
    }
    
    @Override
    public void scanFile(String id) {
        if (antivirus != null) {
			antivirus.scanS3(id, bucket);
		}
    }

    public void setAntivirus(AntivirusClient antivirus) {
		this.antivirus = antivirus;
	}

    public void setValidator(FileValidator validator) {
		this.validator = validator;
	}
    
    @Override
    public FileValidator getValidator() {
        return validator;
    }

    public void getReadPath(String id, Handler<AsyncResult<String>> handler) {
        final String idPath = S3Client.getPath(id);

		if (fallbackStorage != null) {
			fallbackStorage.downloadFileIfNotExists(idPath, idPath, new Handler<AsyncResult<String>>() {
				@Override
				public void handle(AsyncResult<String> event) {
					handler.handle(Future.succeededFuture(idPath));
				}
			});
		}
		else {
			handler.handle(Future.succeededFuture(idPath));
		}
	}

  public void setFallbackStorage(FallbackStorage fallbackStorage) {
        this.fallbackStorage = fallbackStorage;
	}


    @Override
  public Future<Void> moveFsDirectory(final String srcPath, final String destPath) {
    log.debug("Copying from " + srcPath + " to " + destPath);
    return fs.readDir(srcPath)
      .compose(children -> {
        final Promise<Void> promise = Promise.promise();
        final String s3Path = destPath.charAt(0) == '/' ? destPath.substring(1) : destPath;
        moveFsEntriesToS3(children, s3Path, 0, promise);
        return promise.future();
      })
      .compose(e -> fs.deleteRecursive(srcPath, true))
      .mapEmpty();
  }

  private void moveFsEntriesToS3(final List<String> children, final String prefix, int childIndex, final Promise<Void> promise) {
    if(childIndex >= children.size()) {
      promise.complete();
    } else {
      final String childPathOnFs = children.get(childIndex);
      final String child = Paths.get(childPathOnFs).getFileName().toString();
      fs.props(childPathOnFs)
        .compose(props -> {
          final Promise<Void> onChildCopied = Promise.promise();
          final String s3Path = prefix + File.separatorChar + child;
          if(props.isDirectory()) {
            moveFsDirectory(childPathOnFs, s3Path).onComplete(onChildCopied);
          } else {
            log.debug("Copying " + childPathOnFs + " to s3 " + s3Path);
            s3Client.writeFromFileSystem(s3Path, childPathOnFs).onSuccess(e -> {
              if("ok".equals(e.getString("status"))) {
                onChildCopied.complete();
              } else {
                onChildCopied.fail(e.getString("message"));
              }
            }).onFailure(onChildCopied::fail);
          }
          return onChildCopied.future();
        })
        .onSuccess(e -> moveFsEntriesToS3(children, prefix, childIndex + 1, promise))
        .onFailure(promise:: fail);
    }
  }
}