package org.entcore.common.storage.impl;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.s3.S3Client;
import org.entcore.common.storage.FallbackStorage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class S3FallbackS3S3Storage implements FallbackStorage {

    private final S3Client s3Client;
    private final S3Client s3FallbackClient;

    private final String bucket;
    private final int bucketMaxAge;

    private static final Logger log = LoggerFactory.getLogger(S3FallbackS3S3Storage.class);

    public S3FallbackS3S3Storage(Vertx vertx, JsonObject s3, JsonObject s3fallback) {
        this.s3Client = new S3Client(vertx, URI.create(s3.getString("uri")), s3.getString("accessKey"), s3.getString("secretKey"), s3.getString("region"), s3.getString("bucket"), s3.getString("ssec"));
        this.s3FallbackClient = new S3Client(vertx, URI.create(s3fallback.getString("uri")), s3fallback.getString("accessKey"), s3fallback.getString("secretKey"), s3fallback.getString("region"), s3fallback.getString("bucket"), s3fallback.getString("ssec"));

        this.bucket = s3fallback.getString("bucket");
        this.bucketMaxAge = s3fallback.getInteger("bucketMaxAge", 2);
    }

    @Override
    public void downloadFile(String file,  String destination, Handler<AsyncResult<String>> handler) {
        downloadFile(file, new AtomicInteger(0), handler);
    }

    private void downloadFile(String file, AtomicInteger retryIndex, Handler<AsyncResult<String>> handler) {
        String fileId = S3Client.getPath(file);

        Calendar calendar = Calendar.getInstance();
        if (retryIndex.get() > 0) {
            calendar.add(Calendar.MONTH, retryIndex.get()*-1);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");

        String bucketName = bucket + "-" + dateFormat.format(calendar.getTime());

        s3FallbackClient.readFile(fileId, bucketName, readAr -> {
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
                if (retryIndex.incrementAndGet() >= bucketMaxAge) {
                    handler.handle(Future.failedFuture("Object " + fileId + " not found"));
                }
                else {
                    downloadFile(fileId, retryIndex, handler);
                }
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
