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

 import io.vertx.core.AsyncResult;
 import io.vertx.core.Future;
 import io.vertx.core.Handler;
 import io.vertx.core.Vertx;
 import io.vertx.core.buffer.Buffer;
 import io.vertx.core.http.HttpServerRequest;
 import io.vertx.core.json.JsonArray;
 import io.vertx.core.json.JsonObject;
 import io.vertx.core.streams.ReadStream;
 import org.apache.commons.lang3.NotImplementedException;
 import org.entcore.common.messaging.to.UploadedFileMessage;
import org.entcore.common.s3.S3Client;
import org.entcore.common.s3.storage.StorageObject;
import org.entcore.common.storage.BucketStats;
 import org.entcore.common.storage.FileStats;
 import org.entcore.common.storage.Storage;
 import org.entcore.common.validation.FileValidator;
 
 import java.io.File;
 import java.net.URI;
 import java.util.concurrent.atomic.AtomicInteger;
 
 public class S3Storage implements Storage {
 
     private final S3Client s3Client;
     private final String bucket;
 
     public S3Storage(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket) {
         this.bucket = bucket;
         this.s3Client = new S3Client(vertx, uri, accessKey, secretKey, region, bucket);
     }
 
     @Override
     public void fileStats(String id, Handler<AsyncResult<FileStats>> handler) {
         // TODO to implement
         throw new UnsupportedOperationException("not yet implemented");
     }
 
     @Override
     public void findByFilenameEndingWith(String name, Handler<AsyncResult<JsonArray>> handler) {
         // TODO to implement
         throw new UnsupportedOperationException("not yet implemented");
     }
 
     @Override
     public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
         s3Client.uploadFile(request, handler);
     }
 
     @Override
     public void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler) {
         s3Client.uploadFile(request, maxSize, handler);
     }
 
     @Override
     public void writeBuffer(Buffer buff, String contentType, String filename, final Handler<JsonObject> handler) {
         StorageObject o = new StorageObject(buff, filename, contentType);
         writeStorageObject(handler, o);
     }
 
     @Override
     public void writeBuffer(String id, Buffer buff, String contentType, String filename,
             final Handler<JsonObject> handler) {
         StorageObject o = new StorageObject(id, buff, filename, contentType);
         writeStorageObject(handler, o);
     }
 
     @Override
     public void writeBuffer(String basePath, String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler) {
         StorageObject o = new StorageObject(id, buff, filename, contentType);
         writeStorageObject(handler, o);
     }
 
     @Override
     public void writeBuffer(final String path, final String id, final Buffer buff, final String contentType, final String filename, final boolean safe, final Handler<JsonObject> handler) {
         StorageObject o = new StorageObject(id, buff, filename, contentType);
         writeStorageObject(handler, o);
     }
 
     @Override
     public Future<JsonObject> writeBufferStream(ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
         return writeBufferStream(null, bufferReadStream, contentType, filename);
     }
 
     @Override
     public Future<JsonObject> writeBufferStream(String id, ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
         throw new NotImplementedException("writeBufferStream not implemented");
     }
 
     @Override
     public void writeFsFile(String filename, Handler<JsonObject> handler)
     {
         // TODO to implement
         throw new UnsupportedOperationException("not yet implemented");
     }
 
     @Override
     public void writeFsFile(String id, String filename, Handler<JsonObject> handler) {
         s3Client.writeFromFileSystem(id, filename, bucket, handler);
     }
 
     private void writeStorageObject(final Handler<JsonObject> handler, final StorageObject o) {
         s3Client.writeFile(o, new Handler<AsyncResult<String>>() {
             @Override
             public void handle(AsyncResult<String> event) {
                 JsonObject j = new JsonObject();
                 if (event.succeeded()) {
                     final JsonObject metadata = new JsonObject().put("content-type", o.getContentType())
                             .put("filename", o.getFilename()).put("size", o.getBuffer().length());
                     j.put("status", "ok").put("_id", event.result()).put("metadata", metadata);
                 } else {
                     j.put("status", "error").put("message", event.cause().getMessage());
                 }
                 handler.handle(j);
             }
         });
     }
 
     @Override
     public void readFile(String id, final Handler<Buffer> handler) {
         s3Client.readFile(id, new Handler<AsyncResult<StorageObject>>() {
             @Override
             public void handle(AsyncResult<StorageObject> event) {
                 if (event.succeeded()) {
                     handler.handle(event.result().getBuffer());
                 } else {
                     handler.handle(null);
                 }
             }
         });
     }
 
     @Override
     public void readStreamFile(String id, Handler<ReadStream<Buffer>> handler) {
         throw new NotImplementedException("Error. Not Implemented exception");
     }
 
     @Override
     public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
         s3Client.downloadFile(id, request, inline, downloadName, metadata, id);
     }
 
     @Override
     public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata,
             Handler<AsyncResult<Void>> resultHandler) {
         s3Client.downloadFile(id, request, inline, downloadName, metadata, id, resultHandler);
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
             s3Client.writeToFileSystem(id, d, new Handler<AsyncResult<String>>() {
                 @Override
                 public void handle(AsyncResult<String> event) {
                     if (event.failed()) {
                         errors.add(new JsonObject().put("id", id)
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
     public void scanFile(String path) {
         throw new NotImplementedException("ScanFile not implemented");
     }
 
     @Override
     public void stats(final Handler<AsyncResult<BucketStats>> handler) {
        //  s3Client.headContainer(bucket, new Handler<AsyncResult<JsonObject>>() {
        //      @Override
        //      public void handle(AsyncResult<JsonObject> event) {
        //          if (event.succeeded()) {
        //              BucketStats bucketStats = new BucketStats();
        //              bucketStats.setObjectNumber(event.result().getLong("X-Container-Object-Count"));
        //              bucketStats.setStorageSize(event.result().getLong("X-Container-Bytes-Used"));
        //              handler.handle(new DefaultAsyncResult<>(bucketStats));
        //          } else {
        //              handler.handle(new DefaultAsyncResult<BucketStats>(event.cause()));
        //          }
        //      }
        //  });
        handler.handle(Future.failedFuture(new RuntimeException("Not implemented yet")));
     }
 
     @Override
     public Future<byte[]> readFileToMemory(final UploadedFileMessage uploadedFileMessage) {
         throw new NotImplementedException();
     }
 
     @Override
     public FileValidator getValidator() {
         return null;
     }
 
 }