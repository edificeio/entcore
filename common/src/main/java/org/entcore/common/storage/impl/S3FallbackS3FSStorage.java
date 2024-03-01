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
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class S3FallbackS3FSStorage implements FallbackStorage {

    private final S3Client s3Client;
    private final FileSystem fs;

    private final String bucket;
    private final int bucketMaxAge;

    private static final Logger log = LoggerFactory.getLogger(S3FallbackS3FSStorage.class);

    public S3FallbackS3FSStorage(Vertx vertx, String uri, String bucket, String region, String accessKey, String secretKey, String ssecKey, int bucketMaxAge) {
        this.bucket = bucket;
        this.bucketMaxAge = bucketMaxAge;

        this.s3Client = new S3Client(vertx, URI.create(uri), accessKey, secretKey, region, bucket, ssecKey);
        this.fs = vertx.fileSystem();
    }

    @Override
    public void downloadFile(String file,  String destination, Handler<AsyncResult<String>> handler) {
        FileStorage.mkdirsIfNotExists(fs, file, destination, folderAr -> {
            if (folderAr.succeeded()) {
                downloadFile(file, destination, new AtomicInteger(0), handler);
            }
            else {
                handler.handle(Future.failedFuture(folderAr.cause()));
            }
        });
    }

    private void downloadFile(String file, String destination, AtomicInteger retryIndex, Handler<AsyncResult<String>> handler) {
        String filePath = S3Client.getPath(file);

        if (bucketMaxAge > 0) {
            Calendar calendar = Calendar.getInstance();
            if (retryIndex.get() > 0) {
                calendar.add(Calendar.MONTH, retryIndex.get()*-1);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");

            String bucketName = bucket + "-" + dateFormat.format(calendar.getTime());

            s3Client.writeToFileSystem(filePath, destination, bucketName, s3ar -> {
                if (s3ar.succeeded()) {
                    handler.handle(Future.succeededFuture(s3ar.result()));
                }
                else {
                    if (retryIndex.incrementAndGet() >= bucketMaxAge) {
                        handler.handle(Future.failedFuture("Object " + filePath + " not found"));
                    }
                    else {
                        downloadFile(filePath, destination, retryIndex, handler);
                    }
                }
            });
        }
        else {
            s3Client.writeToFileSystem(filePath, destination, bucket, s3ar -> {
                if (s3ar.succeeded()) {
                    handler.handle(Future.succeededFuture(s3ar.result()));
                }
                else {
                    handler.handle(Future.failedFuture("Object " + filePath + " not found"));
                }
            });
        }
    }

    @Override
    public void downloadFileIfNotExists(String file, String destination, Handler<AsyncResult<String>> handler) {
        fs.exists(destination, ar -> {
			if (ar.succeeded() && Boolean.TRUE.equals(ar.result())) {
				handler.handle(Future.succeededFuture(destination));
            } else {
                downloadFile(file, destination, handler);
            }
        });
    }

}
