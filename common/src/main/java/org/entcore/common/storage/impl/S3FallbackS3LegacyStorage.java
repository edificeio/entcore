package org.entcore.common.storage.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.s3.S3Client;
import org.entcore.common.storage.FallbackStorage;

import java.net.URI;

/**
 * S3 fallback,  S3 to S3.
 * The source bucket is fixed, not date suffix added...
 */

public class S3FallbackS3LegacyStorage implements FallbackStorage {

    private final S3Client s3Client;
    private final S3Client s3FallbackClient;

    private final String bucket;

    private static final Logger log = LoggerFactory.getLogger(S3FallbackS3LegacyStorage.class);

    public S3FallbackS3LegacyStorage(Vertx vertx, JsonObject s3, JsonObject s3fallback) {
        this.s3Client = new S3Client(vertx, URI.create(s3.getString("uri")), s3.getString("accessKey"), s3.getString("secretKey"), s3.getString("region"), s3.getString("bucket"), s3.getString("ssec"));
        this.s3FallbackClient = new S3Client(vertx, URI.create(s3fallback.getString("uri")), s3fallback.getString("accessKey"), s3fallback.getString("secretKey"), s3fallback.getString("region"), s3fallback.getString("bucket"), s3fallback.getString("ssec"));

        this.bucket = s3fallback.getString("bucket");
    }

    @Override
    public void downloadFile(String file,  String destination, Handler<AsyncResult<String>> handler) {
        String fileId = S3Client.getPath(file);

        s3FallbackClient.readFile(fileId, bucket, readAr -> {
            if (readAr.succeeded()) {
                s3Client.writeFile(readAr.result(), writeAr -> {
                    if (writeAr.succeeded()) {
                        handler.handle(Future.succeededFuture(writeAr.result()));
                    }
                    else {
                        handler.handle(Future.failedFuture("Object upload" + fileId + " failed"));
                    }
                });
            }
            else {
                handler.handle(Future.failedFuture("Object " + fileId + " not found"));
            }
        });
    }

    @Override
    public void downloadFileIfNotExists(String file, String destination, Handler<AsyncResult<String>> handler) {
        String fileId = S3Client.getPath(file);

        s3Client.getFileStats(fileId, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(destination));
            }
            else {
                downloadFile(fileId, destination, handler);
            }

        });
    }

}
