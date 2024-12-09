package org.entcore.common.storage.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.entcore.common.storage.FallbackStorage;

import fr.wseduc.webutils.security.AWS4Signature;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class S3FallbackStorage implements FallbackStorage {

    private final FileSystem fs;
    private final HttpClient httpClient;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String name;
    private final String host;

    private static final Logger log = LoggerFactory.getLogger(S3FallbackStorage.class);

    public S3FallbackStorage(Vertx vertx, String host, String name,
            String region, String accessKey, String secretKey) {
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.name = name;
        this.host = host;
        this.fs = vertx.fileSystem();
        HttpClientOptions options = new HttpClientOptions()
            .setDefaultHost(host)
            .setDefaultPort(443)
            .setMaxPoolSize(16)
            .setSsl(true)
            .setConnectTimeout(1000)
            .setKeepAlive(false);
        this.httpClient = vertx.createHttpClient(options);
    }

    @Override
    public void downloadFile(String file,  String destination, Handler<AsyncResult<String>> handler) {
        downloadFile(file, destination, 0, 3, handler);
    }

    private void downloadFile(String file, String destination, int monthDelta, int retryIndex, Handler<AsyncResult<String>> handler) {
        final String uri = generateUri(file, monthDelta);

        HttpClientRequest req = httpClient.get(uri);
        req.setHost(this.host);
        req.handler(resp -> {
            if (resp.statusCode() == 200) {
                resp.pause();
                FileStorage.mkdirsIfNotExists(fs, file, destination, folderAr -> {
                    if (folderAr.succeeded()) {
                        this.fs.open(destination, new OpenOptions(), asyncFile -> {
                            if (asyncFile.succeeded()) {
                                resp.pipeTo(asyncFile.result(), h -> {
                                    handler.handle(Future.succeededFuture(destination));
                                });
                            } else {
                                handler.handle(Future.failedFuture(asyncFile.cause()));
                            }
                            resp.resume();
                        });
                    } else {
                        handler.handle(Future.failedFuture(folderAr.cause()));
                    }
                });
            } else {
                if (resp.statusCode() == 404 && monthDelta + 1 < 2) {
                    downloadFile(file, destination, monthDelta+1, 3, handler);
                }
                else {
                    if (resp.statusCode() != 404 && retryIndex - 1 > 0) {
                        log.error("S3Fallback error downloading " + file + " (" + resp.statusCode() + "), retryIndex " + retryIndex);
                        downloadFile(file, destination, monthDelta, retryIndex-1, handler);
                    }
                    else {
                        resp.bodyHandler(body -> log.error("S3Fallback error - " + file + " - " + resp.statusCode() + " - " + body.toString().trim()));
                        handler.handle(Future.failedFuture(new FileNotFoundException("S3Fallback - Not found file : " + file + " , statusCode: " + resp.statusCode())));
                    }
                }
            }
        });

        try {
            AWS4Signature.sign(req, region, accessKey, secretKey, null);
            req.end();
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException
                | UnsupportedEncodingException e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    private String generateUri(String fileName, int monthDelta) {
        String file;
        if (fileName.contains(File.separator)) {
            file = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        } else {
            file = fileName;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, monthDelta*-1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
        String date = dateFormat.format(calendar.getTime());

        final String bucketName = name + "-" + date;

        final int l = file.length();
        if (l < 4) {
            file = "0000".substring(0, 4 - l) + file;
        }
        return "/" + bucketName + "/" + file.substring(l - 2) + "/" + file.substring(l - 4, l - 2) + "/" + file;
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
