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

import org.entcore.common.s3.exception.SignatureException;
import org.entcore.common.s3.exception.StorageException;
import org.entcore.common.s3.storage.DefaultAsyncResult;
import org.entcore.common.s3.storage.StorageObject;
import org.entcore.common.s3.utils.*;
import org.entcore.common.storage.FileStats;
import org.entcore.common.storage.Storage;
import org.entcore.common.validation.FileValidator;

import io.vertx.core.file.OpenOptions;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class S3Client {

	private static final Logger log = LoggerFactory.getLogger(S3Client.class);
	private final Vertx vertx;
	private final ResilientHttpClient httpClient;
	private final String host;
	private final String defaultBucket;
	private String accessKey;
	private String secretKey;
	private String region;
	private String ssec;

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec) {
		this(vertx, uri, accessKey, secretKey, region, bucket, ssec, false);
	}

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec, boolean keepAlive) {
		this(vertx, uri, accessKey, secretKey, region, bucket, ssec, keepAlive, 10000, 100, 10000l);
	}

	public S3Client(Vertx vertx, URI uri, String accessKey, String secretKey, String region, String bucket, String ssec, boolean keepAlive,
					int timeout, int threshold, long openDelay) {
		this.vertx = vertx;
		this.host = uri.getHost();
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
		this.defaultBucket = bucket;
		this.ssec = ssec;
		this.httpClient = new ResilientHttpClient(vertx, uri, keepAlive, timeout, threshold, openDelay);
	}

	public void getFileStats(String id, Handler<AsyncResult<FileStats>> handler) {
		getFileStats(id, defaultBucket, handler);
	}

	public void getFileStats(String id, String bucket, Handler<AsyncResult<FileStats>> handler) {
		id = getPath(id);

		HttpClientRequest req = httpClient.head("/" + bucket + "/" + id, response -> {
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
			} else {
				handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
			}
		});

		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSEC(req, ssec);
		try {
			AwsUtils.sign(req, accessKey, secretKey, region);
		} catch (SignatureException e) {
			log.error("getFileStats signature failed", e);
			handler.handle(Future.failedFuture("getFileStats signature failed"));
			return;
		}

		req.end();
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
		AtomicLong size = new AtomicLong(0l);
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
					});
				}
				else {
					if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
						handler.handle(
							new JsonObject()
								.put("status", "error")
								.put("message", "file.too.large")
						);
						return;
					}
				}

				multipartUpload.init(id, metadata.getString("filename"), metadata.getString("content-type"), uploadId -> {
					multipartUploadId.append(uploadId);
					upload.resume();
				});
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

				if (metadata.getLong("size") == 0l) {
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
		id = getPath(id);

		final HttpServerResponse resp = request.response();
		if (!inline) {
			String name = FileUtils.getNameWithExtension(downloadName, metadata);
			resp.putHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
		}

		HttpClientRequest req = httpClient.get("/" + bucket + "/" + id, response -> {
			response.pause();

			if (response.statusCode() == 200 || response.statusCode() == 304) {
				resp.putHeader("ETag", ((eTag != null) ? eTag : response.headers().get("ETag")));
				resp.putHeader("Content-Type", response.headers().get("Content-Type"));
			}

			if (response.statusCode() == 200) {
				resp.setChunked(true);

				response.handler(buff -> resp.write(buff));

				response.endHandler(aVoid -> {
					resp.end();
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
		});
		
		if (req == null) {
			resultHandler.handle(new DefaultAsyncResult<>((Void) null));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSEC(req, ssec);
        try {
            AwsUtils.sign(req, accessKey, secretKey, region);
        } catch (SignatureException e) {
			log.error("downloadFile signature failed", e);
			resultHandler.handle(Future.failedFuture("downloadFile signature failed"));
			return;
        }

		req.end();
	}

	public void readFile(final String id, final Handler<AsyncResult<StorageObject>> handler) {
		readFile(id, defaultBucket, handler);
	}

	public void readFile(final String id, String bucket, final Handler<AsyncResult<StorageObject>> handler) {
		final String idPrefixed = getPath(id);

		HttpClientRequest req = httpClient.get("/" + bucket + "/" + idPrefixed, response -> {
			response.pause();
			if (response.statusCode() == 200) {
				final Buffer buffer = Buffer.buffer();
				response.handler(buffer::appendBuffer);
				response.endHandler(event -> {
						String filename = response.headers().get("x-amz-meta-filename");
						if (filename != null) {
							try {
								filename = DecoderUtil.decodeEncodedWords(
										filename, DecodeMonitor.SILENT);
							} catch (IllegalArgumentException e) {
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
		});

		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSEC(req, ssec);
		try {
			AwsUtils.sign(req, accessKey, secretKey, region);
		} catch (SignatureException e) {
			log.error("readFile signature failed", e);
			handler.handle(Future.failedFuture("readFile signature failed"));
			return;
		}

		req.end();
	}

	public void readFileStream(final String id, final Handler<AsyncResult<HttpClientResponse>> handler) {
		readFileStream(id, defaultBucket, handler);
	}

	public void readFileStream(String id, String bucket, final Handler<AsyncResult<HttpClientResponse>> handler) {
		id = getPath(id);

		HttpClientRequest req = httpClient.get("/" + bucket + "/" + id, response -> {
			response.pause();
			response.endHandler(event -> handler.handle(Future.succeededFuture(response)));
			response.resume();
		});

		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSEC(req, ssec);
		try {
			AwsUtils.sign(req, accessKey, secretKey, region);
		} catch (SignatureException e) {
			log.error("readFileStream signature failed", e);
			handler.handle(Future.failedFuture("readFile signature failed"));
			return;
		}

		req.end();
	}

	public void writeFile(StorageObject object, final Handler<AsyncResult<String>> handler) {
		writeFile(object, defaultBucket, handler);
	}

	public void writeFile(StorageObject object, String bucket, final Handler<AsyncResult<String>> handler) {
		final String id = (object.getId() != null) ? getPath(object.getId()) : getPath(UUID.randomUUID().toString());
		
		final HttpClientRequest req = httpClient.put("/" + bucket + "/" + id, response -> {
			if (response.statusCode() == 200) {
				handler.handle(new DefaultAsyncResult<>(id));
			} else {
				handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
			}
		});
		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		req.putHeader("Content-Type", object.getContentType());
		AwsUtils.setSSEC(req, ssec);
        try {
            AwsUtils.sign(req, accessKey, secretKey, region, object);
        } catch (SignatureException e) {
			log.error("writeFile signature failed", e);
			handler.handle(Future.failedFuture("writeFile signature failed"));
			return;
        }

        req.end(object.getBuffer());
		
	}

	public void deleteFile(String id, final Handler<AsyncResult<Void>> handler) {
		deleteFile(id, defaultBucket, handler);
	}

	public void deleteFile(String id, String bucket, final Handler<AsyncResult<Void>> handler) {
		id = getPath(id);

		final HttpClientRequest req = httpClient.delete("/" + bucket + "/" + id, response -> {
			if (response.statusCode() == 204) {
				handler.handle(new DefaultAsyncResult<>((Void) null));
			} else {
				handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
			}
		});

		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
        try {
            AwsUtils.sign(req, accessKey, secretKey, region);
        } catch (SignatureException e) {
			log.error("deleteFile signature failed", e);
			handler.handle(Future.failedFuture("deleteFile signature failed"));
			return;
        }

		req.end();
	}

	public void copyFile(String from, final Handler<AsyncResult<String>> handler) {
		copyFile(from, defaultBucket, handler);
	}

	public void copyFile(String from, String bucket, final Handler<AsyncResult<String>> handler) {
		final String uuid = UUID.randomUUID().toString();
		final String id = getPath(uuid);

		final HttpClientRequest req = httpClient.put("/" + bucket + "/" + id, response -> {
			if (response.statusCode() == 200) {
				handler.handle(new DefaultAsyncResult<>(uuid));
			} else {
				handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
			}
		});

		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSECCopy(req, ssec);
        try {
            AwsUtils.sign(req, accessKey, secretKey, region);
        } catch (SignatureException e) {
			log.error("copyFile signature failed", e);
			handler.handle(Future.failedFuture("copyFile signature failed"));
			return;
        }
		
		req.putHeader("X-Copy-From", "/" + bucket + "/" + getPath(from));
		req.end();
	}

	public void writeToFileSystem(String id, String destination, Handler<AsyncResult<String>> handler) {
		writeToFileSystem(id, destination, defaultBucket, handler);
	}

	public void writeToFileSystem(String id, final String destination, String bucket,
			final Handler<AsyncResult<String>> handler) {
		id = getPath(id);

		HttpClientRequest req = httpClient.get("/" + bucket + "/" + id, response -> {
			response.pause();
			if (response.statusCode() == 200) {
				vertx.fileSystem().open(destination, new OpenOptions(), ar -> {
					if (ar.succeeded()) {
						response.endHandler(aVoid -> {
							ar.result().close();
							handler.handle(new DefaultAsyncResult<>(destination));
						});
						Pump p = Pump.pump(response, ar.result());
						p.start();

						response.resume();
					} else {
						handler.handle(new DefaultAsyncResult<>(ar.cause()));
					}
				});
			} else {
				handler.handle(new DefaultAsyncResult<>(new StorageException(response.statusMessage())));
			}
		});
		
		if (req == null) {
			handler.handle(Future.failedFuture("Request is null"));
			return;
		}

		req.setHost(host);
		AwsUtils.setSSEC(req, ssec);
        try {
            AwsUtils.sign(req, accessKey, secretKey, region);
        } catch (SignatureException e) {
			log.error("writeToFileSystem signature failed", e);
			handler.handle(Future.failedFuture("writeToFileSystem signature failed"));
			return;
        }

		req.end();
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

}
