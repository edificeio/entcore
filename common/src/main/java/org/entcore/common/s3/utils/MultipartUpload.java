package org.entcore.common.s3.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;

import org.entcore.common.s3.dataclasses.CompleteMultipartUpload;
import org.entcore.common.s3.dataclasses.CompletePart;
import org.entcore.common.s3.dataclasses.InitiateMultipartUploadResult;
import org.entcore.common.s3.exception.SignatureException;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MultipartUpload {

    private static final Logger log = LoggerFactory.getLogger(MultipartUpload.class);

    protected final Vertx vertx;
    protected final ResilientHttpClient httpClient;

    protected final String endPoint;
    protected final String accessKey;
    protected final String secretKey;
    protected final String region;
    protected final String bucket;
    protected final String ssec;

    public MultipartUpload(final Vertx vertx, final ResilientHttpClient httpClient, final String endPoint,final String accessKey, final String secretKey, final String region, final String bucket, final String ssec) {
        this.vertx = vertx;
        this.httpClient = httpClient;

        this.endPoint = endPoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;

        this.bucket = bucket;
        this.ssec = ssec;
    }

    public void upload(final String filepath, final String id, final Handler<JsonObject> handler) {
        init(id, Paths.get(filepath).getFileName().toString(), AwsUtils.getContentType(filepath), uploadId -> {
            if (uploadId == null) {
                handler.handle(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Initial multipart upload request has failed")
                );
                return;
            }
            
            uploadParts(id, filepath, uploadId, eTags -> {
                if (eTags.isEmpty()) {
                    handler.handle(
                        new JsonObject()
                            .put("status", "error")
                            .put("message", "Some parts of the multipart upload have failed")
                    );
                    return;
                }

                complete(id, uploadId, eTags, result -> {
                    if (Boolean.TRUE.equals(result)) {
                        handler.handle(new JsonObject().put("status", "ok"));
                    }
                    else {
                        handler.handle(
                            new JsonObject()
                                .put("status", "error")
                                .put("message", "Final multipart upload request has failed")
                        );
                    }
                });
            });
        });        
    }

    public void init(final String id, final Handler<String> handler) {
        init(id, null, null, handler);
    }

    public void init(final String id, final String filename, final String contentType, final Handler<String> handler) {
        RequestOptions requestOptions = new RequestOptions()
            .setMethod(HttpMethod.POST)
            .setHost(endPoint)
            .setURI("/" + bucket + "/" + id + "?uploads=");

        httpClient.request(requestOptions)
            .flatMap(req -> {
                if (filename != null) {
                    try {
                        req.putHeader("x-amz-meta-filename", new QuotedPrintableCodec().encode(filename));
                    } catch (EncoderException e) {
                        req.putHeader("x-amz-meta-filename", filename);
                    }
                }
                if (contentType != null) {
                    req.putHeader("Content-Type", contentType);
                }

                AwsUtils.setSSEC(req, ssec);
                if (!sign(req, null)) {
                    log.error("MultipartUpload init, signature failed");
                    return Future.failedFuture("MultipartUpload init, signature failed");
                }

                return req.send();
            })
            .onSuccess(response -> {
                if (response.statusCode() == 200) {
                    response.bodyHandler(bodyBuffer -> {
                        final StringReader stringReader = new StringReader(bodyBuffer.toString());

                        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
                        try {
                            final JAXBContext jaxbContext = JAXBContext.newInstance(InitiateMultipartUploadResult.class);
                            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                            initiateMultipartUploadResult = (InitiateMultipartUploadResult) unmarshaller.unmarshal(stringReader);
                        } catch (JAXBException e) {
                            handler.handle(null);
                            return;
                        }

                        if (initiateMultipartUploadResult.getUploadId() == null) {
                            handler.handle(null);
                            return;
                        }

                        handler.handle(initiateMultipartUploadResult.getUploadId());
                    });
                }
                else {
                    handler.handle(null);
                }
            })
            .onFailure(exception -> {
                handler.handle(null);
            });
    }

    protected void uploadParts(final String id, final String filepath, final String uploadId, final Handler<List<String>> handler) {
        vertx.fileSystem().open(filepath, new OpenOptions(), ar -> {
            if (ar.succeeded()) {
                AsyncFile asyncFile = ar.result();

                List<String> eTags = new ArrayList<>();
                Chunk chunk = new Chunk();

                asyncFile.handler(buff -> {
                    chunk.appendBuffer(buff);

                    if (chunk.getChunkSize() >= chunk.getMaxSize()) {
                        asyncFile.pause();

                        uploadPart(id, uploadId, chunk, eTag -> {
                            if (eTag == null) {
                                cancel(id, uploadId);
                                handler.handle(new ArrayList<>());
                                return;
                            }
                            eTags.add(eTag);

                            chunk.nextChunk();
                            asyncFile.resume();
                        });
                    }
                });

                asyncFile.endHandler(aVoid -> {
                    if (chunk.getChunkSize() > 0) {
                        uploadPart(id, uploadId, chunk, eTag -> {
                            if (eTag == null) {
                                cancel(id, uploadId);
                                handler.handle(new ArrayList<>());
                                return;
                            }
                            eTags.add(eTag);

                            handler.handle(eTags);
                        });
                    }
                    else {
                        handler.handle(eTags);
                    }
                });
            }
            else {
                cancel(id, uploadId);
                handler.handle(new ArrayList<>());
            }
        });
    }

    public void uploadPart(final String id, final String uploadId, final Chunk chunk, final Handler<String> handler) {
        RequestOptions requestOptions = new RequestOptions()
            .setMethod(HttpMethod.PUT)
            .setHost(endPoint)
            .setURI("/" + bucket + "/" + id + "?partNumber=" + chunk.getChunkNumber() + "&uploadId=" + uploadId);

        httpClient.request(requestOptions)
            .flatMap(req -> {
                AwsUtils.setSSEC(req, ssec);
                if (!sign(req, AwsUtils.getDigest(chunk.getBuffer()))) {
                    log.error("MultipartUpload uploadPart, signature failed");
                    return Future.failedFuture("MultipartUpload uploadPart, signature failed");
                }

                return req.send(chunk.getBuffer());
            })
            .onSuccess(response -> {
                if (response.statusCode() == 200) {
                    handler.handle(response.headers().get("ETag"));
                }
                else {
                    chunk.incrementRetryIndex();

                    log.warn("MultiPart upload failed: " + response.statusCode() + " - " + chunk.getChunkNumber());
                    if (response.headers() != null) {
                        response.headers().forEach(header -> {
                            log.warn("MultiPart upload failed: " + header.getKey() + " - " + header.getValue());
                        });
                    }
                    response.bodyHandler(body -> {
                        if(log.isDebugEnabled()) {
                            log.debug(body.toString());
                        }
                    });

                    if (chunk.getRetryIndex() < 6) {
                        uploadPart(id, uploadId, chunk, handler);
                    }
                    else {
                        log.warn("The upload of file=" + id + " with uploadId=" + uploadId + " was retried too many times (" + chunk.getRetryIndex() + ")");
                        handler.handle(null);
                    }
                }
            })
            .onFailure(exception -> {
                log.warn("An exception occurred while uploading file=" + id + " with uploadId=" + uploadId, exception);
                handler.handle(null);
            });
    }

    public void complete(final String id, final String uploadId, final List<String> eTags, final Handler<Boolean> handler) {
        RequestOptions requestOptions = new RequestOptions()
            .setMethod(HttpMethod.POST)
            .setHost(endPoint)
            .setURI("/" + bucket + "/" + id + "?uploadId=" + uploadId);

        httpClient.request(requestOptions)
            .flatMap(req -> {
                CompleteMultipartUpload completeMultipartUpload = new CompleteMultipartUpload();
                for (int i = 0; i < eTags.size(); i++) {
                    CompletePart part = new CompletePart(eTags.get(i), i + 1);
                    completeMultipartUpload.addPart(part);
                }

                StringWriter bodyStringWriter = new StringWriter();
                try {
                    final JAXBContext jaxbContext = JAXBContext.newInstance(CompleteMultipartUpload.class);
                    final Marshaller marshaller = jaxbContext.createMarshaller();

                    marshaller.marshal(completeMultipartUpload, bodyStringWriter);
                } catch (JAXBException e) {
                    log.error("MultipartUpload complete, XML parser failed", e);
                    return Future.failedFuture("MultipartUpload complete, XML parser failed");
                }

                String body = bodyStringWriter.toString();
                if (body.isEmpty()) {
                    log.error("MultipartUpload complete, empty body");
                    return Future.failedFuture("MultipartUpload complete, empty body");
                }

                AwsUtils.setSSEC(req, ssec);
                if (!sign(req, AwsUtils.getDigest(body.getBytes()))) {
                    log.error("MultipartUpload complete, signature failed");
                    return Future.failedFuture("MultipartUpload complete, signature failed");
                }

                return req.send(body);
            })
            .onSuccess(response -> {
                handler.handle(response.statusCode() == 200);
            })
            .onFailure(exception -> {
                handler.handle(false);
            });
    }

    public void cancel(final String id, final String uploadId) {
        RequestOptions requestOptions = new RequestOptions()
                .setMethod(HttpMethod.DELETE)
                .setHost(endPoint)
                .setURI("/" + bucket + "/" + id + "?uploadId=" + uploadId);

        httpClient.request(requestOptions).flatMap(req -> {
            if (!sign(req, null)) {
                log.error("MultipartUpload cancel, signature failed");
                return Future.failedFuture("MultipartUpload cancel, signature failed");
            }

            return req.send();
        });
    }

    protected boolean sign(HttpClientRequest req, String hash) {
        if (req == null) {
            return false;
        }

        try {
            AwsUtils.sign(req, accessKey, secretKey, region, hash);
        } catch (SignatureException e) {
            return false;
        }

        return true;
    }

}
