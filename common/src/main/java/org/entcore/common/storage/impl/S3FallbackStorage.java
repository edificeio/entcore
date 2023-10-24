package org.entcore.common.storage.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.http.*;
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
    private final boolean multiBuckets;
    private final int nbStorageFolder;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String name;
    private final String host;

    private static final Logger log = LoggerFactory.getLogger(S3FallbackStorage.class);

    public S3FallbackStorage(Vertx vertx, String host, String name, boolean multiBuckets,
            int nbStorageFolder, String region, String accessKey, String secretKey) {
        this.multiBuckets = multiBuckets;
        this.nbStorageFolder = nbStorageFolder;
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
        downloadFile(file, destination, 1, 3, handler);
    }

    private void downloadFile(String file, String destination, int storageIdx, int retryIndex, Handler<AsyncResult<String>> handler) {
        final String uri = generateUri(file, storageIdx);
        httpClient.request(new RequestOptions()
            .setMethod(HttpMethod.GET)
            .setURI(uri)
            .setHost(this.host)
        ).onSuccess(req -> {
            try {
                AWS4Signature.sign(req, region, accessKey, secretKey, null);
                req.send().onSuccess(resp -> {
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
                        if (nbStorageFolder > 1 && storageIdx < nbStorageFolder) {
                    downloadFile(file, destination, storageIdx+1, retryIndex, handler);
                        } else {
                    if (retryIndex - 1 > 0) {
                        log.error("S3Fallback error downloading " + file + " (" + resp.statusCode() + "), retryIndex " + retryIndex);
                        downloadFile(file, destination, storageIdx, retryIndex-1, handler);
                    }
                    else {
                        resp.bodyHandler(body -> log.error("S3Fallback error - " + file + " - " + resp.statusCode() + " - " + body.toString().trim()));
                            handler.handle(Future.failedFuture(new FileNotFoundException("S3Fallback - Not found file : " + file + " , statusCode: " + resp.statusCode())));
                        }
                    }
            }
                });
            } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException
                     | UnsupportedEncodingException e) {
                handler.handle(Future.failedFuture(e));
            }
        });
    }

    private String generateUri(String fileName, int storageIdx) {
        String file;
        if (fileName.contains(File.separator)) {
            file = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        } else {
            file = fileName;
        }
        final String bucketName;
        if (multiBuckets) {
            bucketName = name + "-" + file.substring(file.length() - 2);
        } else {
            bucketName = name;
        }
        final String storageNb = (nbStorageFolder > 1 && storageIdx > 1) ? "" + storageIdx : "";
        final int l = file.length();
        if (l < 4) {
            file = "0000".substring(0, 4 - l) + file;
        }
        return "/" + bucketName + "/storage" + storageNb + "/" + file.substring(l - 2) + "/" + file.substring(l - 4, l - 2) + "/" + file;
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
