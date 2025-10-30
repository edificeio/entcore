/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.entcore.common.s3;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.entcore.common.s3.exception.SignatureException;
import org.entcore.common.s3.exception.StorageException;
import org.entcore.common.s3.storage.DefaultAsyncResult;
import org.entcore.common.s3.storage.StorageObject;
import org.entcore.common.s3.utils.*;
import org.entcore.common.storage.FileStats;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.FileValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class S3Client {

	private static final Logger log = LoggerFactory.getLogger(S3Client.class);
	private final Vertx vertx;
	private final ResilientHttpClient httpClient;
	private final String host;
	private final String defaultBucket;
	private final String accessKey;
	private final String secretKey;
	private final String region;
	private final String ssec;

	private static final long S3_DOWNLOAD_TIMEOUT_IN_MILLISECONDS = TimeUnit.HOURS.toMillis(1L);

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec) {
		this(vertx, uri, accessKey, secretKey, region, bucket, ssec, false);
	}

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec, boolean keepAlive) {
		this(vertx, uri, accessKey, secretKey, region, bucket, ssec, keepAlive, 10000, 100, 10000L, 16);
	}

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec, boolean keepAlive,
					int timeout, int threshold, long openDelay, int poolSize) {
		this.vertx = vertx;
		this.host = uri.getHost();
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
		this.defaultBucket = bucket;
		this.ssec = ssec;
		this.httpClient = new ResilientHttpClient(vertx, uri, keepAlive, timeout, threshold, openDelay, poolSize);
	}

	public void getFileStats(String id, Handler<AsyncResult<FileStats>> handler) {
		getFileStats(id, defaultBucket, handler);
	}

	public void getFileStats(String id, String bucket, Handler<AsyncResult<FileStats>> handler) {
		id = getPath(id);

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.HEAD)
			.setHost(host)
			.setURI("/" + bucket + "/" + id);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client getFileStats, signature failed", e);
					return Future.failedFuture("S3Client getFileStats, signature failed");
				}
				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				if (response.statusCode() == 200) {
					SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
					format.setTimeZone(TimeZone.getTimeZone("GMT"));

					Date modified;
					try {
						modified = format.parse(response.getHeader("Last-Modified"));
					} catch (ParseException e) {
						handler.handle(Future.failedFuture("Parsing error"));
						return;
					}
					long size = Long.parseLong(response.getHeader("Content-Length"));

					handler.handle(new DefaultAsyncResult<>(new FileStats(modified, modified, size)));
					response.resume();
				}
				else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusCode() + " - " + response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void uploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
		uploadFile(request, defaultBucket, null, handler);
	}

	public void uploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler) {
		uploadFile(request, defaultBucket, maxSize, handler);
	}

	public void uploadFile(HttpServerRequest request, Long maxSize, FileValidator validator, Handler<JsonObject> handler) {
		uploadFile(request, defaultBucket, maxSize, validator, handler);
	}

	public void uploadFile(final HttpServerRequest request, final String bucket, final Long maxSize, final Handler<JsonObject> handler) {
		uploadFile(request, bucket, maxSize, null, handler);
	}

	public void uploadFile(final HttpServerRequest request, final String bucket, final Long maxSize, FileValidator validator, final Handler<JsonObject> handler) {
		final String uuid = UUID.randomUUID().toString();
		final String id = getPath(uuid);

		MultipartUpload multipartUpload = new MultipartUpload(vertx, httpClient, host, accessKey, secretKey, region, bucket, ssec);
		JsonObject metadata = new JsonObject();
		AtomicLong size = new AtomicLong(0L);
		List<String> eTags = new ArrayList<>();
		Chunk chunk = new Chunk();
		StringBuilder multipartUploadId = new StringBuilder();

		request.setExpectMultipart(true);
		request.uploadHandler(upload -> {
			upload.pause();

			if (multipartUploadId.toString().isEmpty()) {
				metadata.mergeIn(FileUtils.metadata(upload));
				if (validator != null) {
					validator.process(metadata, new JsonObject().put("maxSize", maxSize), ar -> {
						if (ar.failed()) {
							handler.handle(
								new JsonObject()
									.put("status", "error")
									.put("message", ar.cause().getMessage())
							);
							return;
						}
						else {
							multipartUpload.init(id, metadata.getString("filename"), metadata.getString("content-type"), uploadId -> {
								multipartUploadId.append(uploadId);
								upload.resume();
							});
						}
					});
				}
				else {
					multipartUpload.init(id, metadata.getString("filename"), metadata.getString("content-type"), uploadId -> {
						multipartUploadId.append(uploadId);
						upload.resume();
					});
				}
			}

			upload.handler(buff -> {
				chunk.appendBuffer(buff);

				if (chunk.getChunkSize() >= chunk.getMaxSize()) {
					upload.pause();
					String uploadId = multipartUploadId.toString();

					multipartUpload.uploadPart(id, uploadId, chunk, eTag -> {
						if (eTag == null) {
							multipartUpload.cancel(id, uploadId);
							handler.handle(
								new JsonObject()
									.put("status", "error")
									.put("message", "Upload part failed")
							);
							return;
						}
						eTags.add(eTag);
						size.addAndGet(chunk.getChunkSize());

						chunk.nextChunk();
						upload.resume();
					});
				}
			});

			upload.endHandler(aVoid -> {
				String uploadId = multipartUploadId.toString();

				if (metadata.getLong("size") == 0L) {
					metadata.put("size", size.get() + chunk.getChunkSize());
				}

				if (chunk.getChunkSize() > 0) {
					multipartUpload.uploadPart(id, uploadId, chunk, eTag -> {
						if (eTag == null) {
							multipartUpload.cancel(id, uploadId);
							handler.handle(
								new JsonObject()
									.put("status", "error")
									.put("message", "Upload part failed")
							);
							return;
						}
						eTags.add(eTag);

						if (maxSize != null && maxSize < metadata.getLong("size", 0L)) {
							multipartUpload.cancel(id, uploadId);
							handler.handle(
								new JsonObject()
									.put("status", "error")
									.put("message", "file.too.large")
							);
							return;
						}

						multipartUpload.complete(id, uploadId, eTags, result -> {
							handler.handle(
								new JsonObject()
									.put("_id", uuid)
									.put("status", "ok")
									.put("metadata", metadata)
							);
						});
					});
				}
				else {
					if (maxSize != null && maxSize < metadata.getLong("size", 0L)) {
						multipartUpload.cancel(id, uploadId);
						handler.handle(
							new JsonObject()
								.put("status", "error")
								.put("message", "file.too.large")
						);
						return;
					}

					multipartUpload.complete(id, uploadId, eTags, result -> {
						handler.handle(
							new JsonObject()
								.put("_id", uuid)
								.put("status", "ok")
								.put("metadata", metadata)
						);
					});
				}
			});
		});
	}

	public void downloadFile(String id, final HttpServerRequest request) {
		downloadFile(id, request, defaultBucket, true, null, null, null);
	}

	public void downloadFile(String id, String bucket, final HttpServerRequest request) {
		downloadFile(id, request, bucket, true, null, null, null);
	}

	public void downloadFile(String id, final HttpServerRequest request,
		 boolean inline, String downloadName, JsonObject metadata, final String eTag) {
		downloadFile(id, request, defaultBucket, inline, downloadName, metadata, eTag);
	}

	public void downloadFile(String id, final HttpServerRequest request, String bucket,
			boolean inline, String downloadName, JsonObject metadata, final String eTag) {
		downloadFile(id, request, bucket, inline, downloadName, metadata, eTag, null);
	}

	public void downloadFile(String id, final HttpServerRequest request, boolean inline,
			String downloadName, JsonObject metadata, final String eTag, Handler<AsyncResult<Void>> resultHandler) {
		downloadFile(id, request, defaultBucket, inline, downloadName, metadata, eTag, resultHandler);
	}

	public void downloadFile(String id, final HttpServerRequest request, String bucket,
			boolean inline, String downloadName, JsonObject metadata, final String eTag,
			final Handler<AsyncResult<Void>> resultHandler) {
		final String fileId = getPath(id);

		final HttpServerResponse resp = request.response();
		if (!inline) {
			String name = FileUtils.getNameWithExtension(downloadName, metadata);
			resp.putHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
		}

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setTimeout(S3_DOWNLOAD_TIMEOUT_IN_MILLISECONDS)
			.setURI("/" + bucket + "/" + fileId);
		final String range = request.getHeader("Range");
		if (!StringUtils.isEmpty(range)) {
			requestOptions.addHeader("Range", range);
		}
		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client downloadFile, signature failed", e);
					return Future.failedFuture("S3Client downloadFile, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				response.pause();

				if (response.statusCode() >= 200 && response.statusCode() < 300 || response.statusCode() == 304) {
					if(resp.headWritten()) {
						log.warn("Client response Headers have already been written : " + resp.getStatusCode() + " \n" + resp.headers());
					} else {
						resp.putHeader("ETag", ((eTag != null) ? eTag : response.headers().get("ETag")));
						if (metadata != null && metadata.getString("content-type") != null) {
							resp.putHeader("Content-Type", metadata.getString("content-type"));
						}
						else {
							resp.putHeader("Content-Type", response.headers().get("Content-Type"));
						}
						if(!StringUtils.isEmpty(range)) {
							resp.putHeader("Content-Range", response.getHeader("Content-Range"));
						}
						resp.putHeader("Content-Length", response.headers().get("Content-Length"));
					}
				}

				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					response.pipeTo(resp).onComplete(aVoid -> {
						if(aVoid.failed()) {
							Throwable cause = aVoid.cause();
							if (cause instanceof ClosedChannelException) {
								log.debug("Client closed the connection downloading file " + id);
							}
							else {
								log.error("An error occurred while piping an s3 file with id=" + id, cause);
								resp.setStatusCode(500).setStatusMessage("Error downloading file").end();
							}

							try {
								response.request().connection().close();
							} catch (Exception e) {
								log.warn("Failed to close S3 connection for file id=" + id, e);
							}
						}

						if (resultHandler != null) {
							resultHandler.handle(new DefaultAsyncResult<>((Void) null));
						}
					});

					response.resume();
				} else {
					resp.setStatusCode(response.statusCode()).setStatusMessage(response.statusMessage()).end();
					if (resultHandler != null) {
						resultHandler.handle(new DefaultAsyncResult<>((Void) null));
					}
				}
			})
			.onFailure(exception -> {
				resp.setStatusCode(500).setStatusMessage(exception.getMessage()).end();
				if (resultHandler != null) {
					resultHandler.handle(new DefaultAsyncResult<>((Void) null));
				}
			});
	}

	public void readFile(final String id, final Handler<AsyncResult<StorageObject>> handler) {
		readFile(id, defaultBucket, handler);
	}

	public void readFile(final String id, String bucket, final Handler<AsyncResult<StorageObject>> handler) {
		final String idPrefixed = getPath(id);

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setURI("/" + bucket + "/" + idPrefixed);

		httpClient.request(requestOptions)
				.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client readFile, signature failed", e);
					return Future.failedFuture("S3Client readFile, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				if (response.statusCode() == 200) {
					final Buffer buffer = Buffer.buffer();
					response.handler(buffer::appendBuffer);
					response.endHandler(event -> {
						String filename = response.headers().get("x-amz-meta-filename");
						if (filename != null) {
							try {
								filename = new QuotedPrintableCodec().decode(filename);
							} catch (DecoderException e) {
								log.error(e.getMessage(), e);
							}
						}
						StorageObject o = new StorageObject(
								id,
								buffer,
								filename,
								response.headers().get("Content-Type")
						);
						handler.handle(new DefaultAsyncResult<>(o));
					});
					response.resume();
				} else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void readFileStream(final String id, final Handler<AsyncResult<HttpClientResponse>> handler) {
		readFileStream(id, defaultBucket, handler);
	}

	public void readFileStream(String id, String bucket, final Handler<AsyncResult<HttpClientResponse>> handler) {
		final String fileId = getPath(id);

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setURI("/" + bucket + "/" + fileId);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client readFileStream, signature failed", e);
					return Future.failedFuture("S3Client readFileStream, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				response.endHandler(event -> handler.handle(Future.succeededFuture(response)));
				response.resume();
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void writeFile(StorageObject object, final Handler<AsyncResult<String>> handler) {
		writeFile(object, defaultBucket, handler);
	}

	public void writeFile(StorageObject object, String bucket, final Handler<AsyncResult<String>> handler) {
		final String id = (object.getId() != null) ? getPath(object.getId()) : getPath(UUID.randomUUID().toString());

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.PUT)
			.setHost(host)
			.setURI("/" + bucket + "/" + id);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				req.putHeader("Content-Type", object.getContentType());
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region, object);
				} catch (SignatureException e) {
					log.error("S3Client writeFile, signature failed", e);
					return Future.failedFuture("S3Client writeFile, signature failed");
				}

				return req.send(object.getBuffer());
			})
			.onSuccess(response -> {
				if (response.statusCode() == 200) {
					handler.handle(new DefaultAsyncResult<>(getUuid(id)));
				} else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void deleteFile(String id, final Handler<AsyncResult<Void>> handler) {
		deleteFile(id, defaultBucket, handler);
	}

    public void deleteFile(String id, String bucket, final Handler<AsyncResult<Void>> handler) {
        deleteFileWithId(getPath(id), bucket, handler);
    }

	public void deleteFileWithId(String id, String bucket, final Handler<AsyncResult<Void>> handler) {
		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.DELETE)
			.setHost(host)
			.setURI("/" + bucket + "/" + id);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client deleteFile, signature failed", e);
					return Future.failedFuture("S3Client deleteFile, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				if (response.statusCode() == 204) {
					handler.handle(new DefaultAsyncResult<>((Void) null));
				} else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void copyFile(String from, final Handler<AsyncResult<String>> handler) {
		copyFile(from, defaultBucket, handler);
	}

	public void copyFile(String from, String bucket, final Handler<AsyncResult<String>> handler) {
		final String uuid = UUID.randomUUID().toString();
		final String id = getPath(uuid);

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.PUT)
			.setHost(host)
			.setURI("/" + bucket + "/" + id);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSECCopy(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client copyFile, signature failed", e);
					return Future.failedFuture("S3Client copyFile, signature failed");
				}
				req.putHeader("x-amz-copy-source", "/" + bucket + "/" + getPath(from));

				return req.send();
			})
			.onSuccess(response -> {
				if (response.statusCode() == 200) {
					handler.handle(new DefaultAsyncResult<>(uuid));
				} else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void writeToFileSystem(String id, String destination, Handler<AsyncResult<String>> handler) {
		writeToFileSystem(id, destination, defaultBucket, handler);
	}

	public void writeToFileSystem(String id, final String destination, String bucket,
			final Handler<AsyncResult<String>> handler) {
    final String fileId = getPath(id);
    writeToFileSystemWithId(fileId, destination, bucket, handler);
  }


  public void writeToFileSystemWithId(String id, final String destination,
                                      final Handler<AsyncResult<String>> handler) {
    writeToFileSystemWithId(id, destination, defaultBucket, handler);
  }
  public void writeToFileSystemWithId(String id, final String destination, String bucket,
                                final Handler<AsyncResult<String>> handler) {
		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setURI("/" + bucket + "/" + id);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client writeToFileSystem, signature failed", e);
					return Future.failedFuture("S3Client writeToFileSystem, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				if (response.statusCode() == 200) {
          final String dest = decodePath(destination);
          createParentsIfNeeded(dest)
          .onSuccess(e -> {
            vertx.fileSystem().open(dest, new OpenOptions(), ar -> {
              if (ar.succeeded()) {
                response.pipeTo(ar.result(), aVoid -> {
                  if (aVoid.succeeded()) {
                    log.info(id + " file successfully downloaded from S3 to " + dest);
                    handler.handle(new DefaultAsyncResult<>(dest));
                  } else {
                    final String message = "An error occurred while piping " + id + " to " + dest;
                    log.error(message, aVoid.cause());
                    handler.handle(new DefaultAsyncResult<>(new StorageException(message, aVoid.cause())));
                  }
                });
              } else {
                handler.handle(new DefaultAsyncResult<>(ar.cause()));
              }
            });
          })
          .onFailure(th -> handler.handle(new DefaultAsyncResult<>(th)));
				} else {
					handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

  private Future<Void> createParentsIfNeeded(final String destination) {
    final File file = new File(destination);
    final File parent = file.getParentFile();
    return vertx.fileSystem().mkdirs(parent.getAbsolutePath());
  }

  public void writeFromFileSystem(final String id, String path, final Handler<JsonObject> handler) {
		writeFromFileSystem(id, path, defaultBucket, handler);
	}

	public void writeFromFileSystem(final String id, String path, final String bucket, final Handler<JsonObject> handler) {
		if (id == null || id.trim().isEmpty() || path == null || path.trim().isEmpty() || path.endsWith(File.separator)) {
			handler.handle(
				new JsonObject()
					.put("status", "error")
					.put("message", "invalid.parameter")
			);
			return;
		}

		final String idPrefixed = getPath(id);

		MultipartUpload multipartUpload = new MultipartUpload(vertx, httpClient, host, accessKey, secretKey, region, bucket, ssec);
		multipartUpload.upload(path, idPrefixed, result -> {
			handler.handle(new JsonObject().put("_id", id).put("status", "ok"));
		});
	}

  public Future<JsonObject> writeFromFileSystem(final String s3Path, String fsPath) {
    final Promise<JsonObject> promise = Promise.promise();
    MultipartUpload multipartUpload = new MultipartUpload(vertx, httpClient, host, accessKey, secretKey, region, defaultBucket, ssec);
    final String id = encodeUrlPath(s3Path);
    multipartUpload.upload(fsPath, id, result -> promise.complete(new JsonObject().put("_id", id).put("status", result.getString("status")).put("message", result.getValue("message"))));
    return promise.future();
  }


  public static String encodeUrlPath(String path) {
    String[] segments = path.split("/");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        result.append("/");
      }
      // Encode each segment separately
      try {
        result.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        result.append(URLEncoder.encode(segments[i]));
      }
    }

    return result.toString();
  }


  public static String decodePath(String path) {
    String[] segments = path.split("/");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        result.append("/");
      }
      // Encode each segment separately
      try {
        result.append(URLDecoder.decode(segments[i], StandardCharsets.UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        result.append(URLDecoder.decode(segments[i]));
      }
    }

    return result.toString();
  }

	public void writeBufferStream(final String id, ReadStream<Buffer> bufferReadStream, String contentType, String filename, Handler<AsyncResult<JsonObject>> handler) {
		bufferReadStream.pause();
		final String idPrefixed = getPath(id);

		final JsonObject res = new JsonObject();
		final JsonObject metadata = new JsonObject();
		final AtomicLong fileSize = new AtomicLong(0);

		MultipartUpload multipartUpload = new MultipartUpload(vertx, httpClient, host, accessKey, secretKey, region, defaultBucket, ssec);
		List<String> eTags = new ArrayList<>();
		Chunk chunk = new Chunk();
		StringBuilder multipartUploadId = new StringBuilder();

		if (multipartUploadId.toString().isEmpty()) {
			multipartUpload.init(idPrefixed, filename, contentType, uploadId -> {
				multipartUploadId.append(uploadId);
				bufferReadStream.resume();
			});
		}

		bufferReadStream.handler(buff -> {
			chunk.appendBuffer(buff);

			if (chunk.getChunkSize() >= chunk.getMaxSize()) {
				bufferReadStream.pause();
				String uploadId = multipartUploadId.toString();

				multipartUpload.uploadPart(idPrefixed, uploadId, chunk, eTag -> {
					if (eTag == null) {
						multipartUpload.cancel(idPrefixed, uploadId);
						handler.handle(new DefaultAsyncResult<>(
							new JsonObject()
								.put("status", "error")
								.put("message", "Upload part failed")
						));
						return;
					}
					eTags.add(eTag);
					fileSize.addAndGet(chunk.getChunkSize());

					chunk.nextChunk();
					bufferReadStream.resume();
				});
			}
		});

		bufferReadStream.endHandler(aVoid -> {
			String uploadId = multipartUploadId.toString();

			if (chunk.getChunkSize() > 0) {
				multipartUpload.uploadPart(idPrefixed, uploadId, chunk, eTag -> {
					if (eTag == null) {
						multipartUpload.cancel(idPrefixed, uploadId);
						handler.handle(new DefaultAsyncResult<>(
							new JsonObject()
								.put("status", "error")
								.put("message", "Upload part failed")
						));
						return;
					}
					fileSize.addAndGet(chunk.getChunkSize());
					eTags.add(eTag);

					multipartUpload.complete(idPrefixed, uploadId, eTags, result -> {
						metadata.put("size", fileSize);
						res.put("status", "ok")
							.put("_id", id)
							.put("metadata", metadata);

						handler.handle(new DefaultAsyncResult<>(res));
					});
				});
			}
			else {
				multipartUpload.complete(idPrefixed, uploadId, eTags, result -> {
					metadata.put("size", fileSize);
					res.put("status", "ok")
						.put("_id", id)
						.put("metadata", metadata);

					handler.handle(new DefaultAsyncResult<>(res));
				});
			}
		});
	}

	public void listBucket(final String prefix, String continuationToken, final Handler<AsyncResult<List<String>>> handler) {
		List<String> objects = new ArrayList<>();

		String url = "/" + defaultBucket + "/?list-type=2";
		if (prefix != null) {
			try {
				url += "&prefix=" + URLEncoder.encode(prefix, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				handler.handle(Future.failedFuture("Error escaping prefix in listBuck method"));
				return;
			}
		}
		if (continuationToken != null) {
			url += "&continuation-token=" + continuationToken;
		}

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setURI(url);

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client listBucket, signature failed", e);
					return Future.failedFuture("S3Client listBucket, signature failed");
				}

				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				if (response.statusCode() == 200) {
					response.bodyHandler(body -> {
						Document document = null;
						try {
							DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
							document = documentBuilder.parse(new ByteArrayInputStream(body.toString().getBytes()));
							document.getDocumentElement().normalize();
						} catch (ParserConfigurationException | SAXException | IOException e) {
							String message = "Error parsing S3 ListObjectsV2 response";
							log.error(message);
							handler.handle(Future.failedFuture(message));
							return;
						}

						NodeList contents = document.getElementsByTagName("Contents");
						for (int i = 0; i < contents.getLength(); i++) {
							NodeList childs = contents.item(i).getChildNodes();
							for (int j = 0; j < childs.getLength(); j++) {
								Node node = childs.item(j);
								String nodeName = node.getNodeName();
								if (nodeName.equals("Key")) {
									objects.add(node.getTextContent());
									break;
								}
							}
						}

						boolean isTruncated = false;
						String token = null;
						NodeList truncateds = document.getElementsByTagName("IsTruncated");
						if (truncateds.getLength() >= 1) {
							Node truncated = truncateds.item(0);
							isTruncated = Boolean.parseBoolean(truncated.getTextContent());

							if (isTruncated) {
								listBucket(prefix, token, results -> {
									if (results.succeeded()) {
										objects.addAll(results.result());
									}
									else {
										handler.handle(results);
										return;
									}
								});
							}
						}

						handler.handle(new DefaultAsyncResult<>(objects));
					});

					response.resume();
				} else {
					handler.handle(Future.failedFuture("HTTP status code : " + response.statusCode()));
				}
			})
			.onFailure(exception -> {
				handler.handle(new DefaultAsyncResult<>(new StorageException(exception.getMessage())));
			});
	}

	public void getObjectsEndingWith(final String endsWith, final Handler<AsyncResult<List<String>>> handler) {
		if (endsWith.length() < 4) {
			handler.handle(Future.failedFuture("endsWith must be 4 characters or higher"));
			return;
		}
		String path = getPath(endsWith);
		String prefix = path.substring(0, path.lastIndexOf("/")+1);

		listBucket(prefix, null, results -> {
			if (results.succeeded()) {
				List<String> matchingObjects = new ArrayList<>();
				for(String objectId: results.result()) {
					if (objectId.endsWith(endsWith)) {
						matchingObjects.add(getUuid(objectId));
					}
				}

				handler.handle(new DefaultAsyncResult<>(matchingObjects));
			}
			else {
				handler.handle(results);
			}
		});
	}

	private String getContentType(String p) {
		try {
			Path source = Paths.get(p);
			return Files.probeContentType(source);
		} catch (IOException e) {
			return "";
		}
	}

	public void close() {
		if (httpClient != null) {
			httpClient.close();
		}
	}

	public static String getPath(final String id) {
		String path;

		if (id.charAt(2) == File.separator.charAt(0) && id.charAt(5) == File.separator.charAt(0)) {
			return id;
		}

		try {
			path = Storage.getFilePath(id, "", false);
		} catch (FileNotFoundException e) {
			log.error("File not found");
			return "";
		}

		return path;
	}

	public static String getUuid(final String path) {
		final int separatorIndex = path.lastIndexOf(File.separator);
		return (separatorIndex < 0) ? path : path.substring(separatorIndex+1);
	}

  /**
   *
   * @param prefix
   * @return
   */
	public Future<List<S3FileInfo>> listFilesByPrefix(final String prefix) {
		Promise<List<S3FileInfo>> promise = Promise.promise();
		List<S3FileInfo> allObjects = new ArrayList<>();
		
		listBucketRecursive(prefix, null, allObjects, promise);
		
		return promise.future();
	}
	
	private void listBucketRecursive(final String prefix, String continuationToken, 
			List<S3FileInfo> allObjects, Promise<List<S3FileInfo>> promise) {
		
		final StringBuilder url = new StringBuilder().append('/').append(defaultBucket).append("/?list-type=2");
		if (prefix != null) {
      url.append("&prefix=").append(encodeUrlPath(prefix));
		}
		if (continuationToken != null) {
			url.append("&continuation-token=").append(continuationToken);
		}

		RequestOptions requestOptions = new RequestOptions()
			.setMethod(HttpMethod.GET)
			.setHost(host)
			.setURI(url.toString());

		httpClient.request(requestOptions)
			.flatMap(req -> {
				AwsUtils.setSSEC(req, ssec);
				try {
					AwsUtils.sign(req, accessKey, secretKey, region);
				} catch (SignatureException e) {
					log.error("S3Client listFilesByPrefix, signature failed: " + e.getMessage(), e);
					return Future.failedFuture("S3Client listFilesByPrefix, signature failed");
				}
				return req.send();
			})
			.onSuccess(response -> {
				response.pause();
				if (response.statusCode() == 200) {
					response.bodyHandler(body -> {
						try {
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							DocumentBuilder builder = factory.newDocumentBuilder();
							Document document = builder.parse(new ByteArrayInputStream(body.getBytes()));

							NodeList contents = document.getElementsByTagName("Contents");
							for (int i = 0; i < contents.getLength(); i++) {
								Node content = contents.item(i);
								NodeList children = content.getChildNodes();
								for (int j = 0; j < children.getLength(); j++) {
									Node child = children.item(j);
									if ("Key".equals(child.getNodeName())) {
										String key = child.getTextContent();
										if (key != null && !key.trim().isEmpty()) {
											allObjects.add(new S3FileInfo(getUuid(key), key));
										}
									}
								}
							}

							// Check if there are more objects to retrieve
							NodeList isTruncatedNodes = document.getElementsByTagName("IsTruncated");
							boolean isTruncated = false;
							if (isTruncatedNodes.getLength() > 0) {
								isTruncated = "true".equals(isTruncatedNodes.item(0).getTextContent());
							}

							if (isTruncated) {
								NodeList nextContinuationTokenNodes = document.getElementsByTagName("NextContinuationToken");
								if (nextContinuationTokenNodes.getLength() > 0) {
									String nextToken = nextContinuationTokenNodes.item(0).getTextContent();
									// Recursive call to get the next batch
									listBucketRecursive(prefix, nextToken, allObjects, promise);
								} else {
									promise.complete(allObjects);
								}
							} else {
								promise.complete(allObjects);
							}

						} catch (ParserConfigurationException | SAXException | IOException e) {
							promise.fail(new StorageException("Error parsing response: " + e.getMessage()));
						}
					});
					response.resume();
				} else {
					promise.fail(new StorageException(response.statusCode() + " - " + response.statusMessage()));
				}
			})
			.onFailure(exception -> {
				promise.fail(new StorageException(exception.getMessage()));
			});
	}

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public static class S3FileInfo {
    private final String id;
    private final String path;

    public S3FileInfo(String id, String path) {
      this.id = id;
      this.path = path;
    }

    public String getId() {
      return id;
    }

    public String getPath() {
      return path;
    }

  }

}
