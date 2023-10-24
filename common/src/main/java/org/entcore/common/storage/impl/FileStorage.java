/*
 * Copyright © "Open Digital Education", 2016
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

import org.entcore.common.utils.FileUtils;
import fr.wseduc.webutils.DefaultAsyncResult;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import fr.wseduc.webutils.http.ETag;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemProps;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.to.UploadedFileMessage;
import org.entcore.common.storage.AntivirusClient;
import org.entcore.common.storage.BucketStats;
import org.entcore.common.storage.FallbackStorage;
import org.entcore.common.storage.FileStats;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.FileValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileStorage implements Storage {

	private static final Logger log = LoggerFactory.getLogger(FileStorage.class);
	private final List<String> basePaths;
	private final int lastBucketIdx;
	private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");
	private final FileSystem fs;
	private final boolean flat;
	private final IMessagingClient messagingClient;
	private AntivirusClient antivirus;
	private FileValidator validator;
	private FallbackStorage fallbackStorage;
	private static final String STORAGE_ID = "file";

	public FileStorage(Vertx vertx, String basePath, boolean flat, final IMessagingClient messagingClient) {
		this(vertx, new JsonArray().add(basePath), flat, messagingClient, new StorageFileAnalyzer.Configuration());
	}
	public FileStorage(Vertx vertx, String basePath, boolean flat, final IMessagingClient messagingClient, final StorageFileAnalyzer.Configuration configuration) {
		this(vertx, new JsonArray().add(basePath), flat, messagingClient, configuration);
	}

	public FileStorage(Vertx vertx, JsonArray bP, boolean flat, final IMessagingClient messagingClient) {
		this(vertx, bP, flat, messagingClient, new StorageFileAnalyzer.Configuration());
	}
	public FileStorage(Vertx vertx, JsonArray bP, boolean flat, final IMessagingClient messagingClient, final StorageFileAnalyzer.Configuration configuration) {
		this.flat = flat;
		this.fs = vertx.fileSystem();
		this.basePaths = new ArrayList<>();
		for (Object o: bP) {
			if (!(o instanceof String)) continue;
			final String basePath = (String) o;
			this.basePaths.add(((!basePath.endsWith("/")) ? basePath + "/" : basePath));
		}
		this.lastBucketIdx = this.basePaths.size() - 1;
		this.messagingClient = messagingClient;
		final String verticleIdt = vertx.getOrCreateContext().config().getString("main");
		if(this.messagingClient.canListen()) {
			this.messagingClient.startListening(new StorageFileAnalyzer(vertx, this, configuration))
					.onSuccess(e -> log.info(verticleIdt + " started listening to analyze files"))
					.onFailure(th -> log.error(verticleIdt + " encountered an error while trying to listen for incoming files to analyze", th));
		} else {
			log.info(verticleIdt + " won't listen to files to analyze");
		}
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
		writeUploadFile(request, null, handler);
	}

	@Override
	public void writeUploadFile(final HttpServerRequest request, final Long maxSize, final Handler<JsonObject> handler) {
		writeUploadFile(request, null, maxSize, handler);
	}

	@Override
	public void writeUploadToFileSystem(HttpServerRequest request, String path, Handler<JsonObject> handler) {
		writeUploadFile(request, path, null, handler);
	}

	private void writeUploadFile(final HttpServerRequest request, final String uploadPath, final Long maxSize,
								final Handler<JsonObject> handler) {
		request.pause();
		final String id = UUID.randomUUID().toString();
		final String path;
		final JsonObject res = new JsonObject();
		if (uploadPath == null) {
			try {
				path = getWritePath(id);
			} catch (FileNotFoundException e) {
				handler.handle(res.put("status", "error").put("message", "invalid.path"));
				log.warn(e.getMessage(), e);
				return;
			}
		} else {
			path = uploadPath;
		}
		request.setExpectMultipart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload upload) {
				request.pause();
				final JsonObject metadata = FileUtils.metadata(upload);
				if (validator != null) {
					validator.process(metadata, new JsonObject().put("maxSize", maxSize), new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.succeeded()) {
								doUpload(upload, metadata);
							} else {
								handler.handle(res.put("status", "error")
										.put("message", event.cause().getMessage()));
							}
						}
					});
				} else {
					doUpload(upload, metadata);
				}
			}

			private void doUpload(final HttpServerFileUpload upload, final JsonObject metadata) {
				upload.handler(buffer -> log.info(buffer.toJson().toString()));
				upload.streamToFileSystem(path).onSuccess(e -> {
					if (metadata.getLong("size") == 0l) {
						metadata.put("size", upload.size());
						if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
							handler.handle(res.put("status", "error")
									.put("message", "file.too.large"));
							try {
								fs.delete(getWritePath(id), new Handler<AsyncResult<Void>>() {
									@Override
									public void handle(AsyncResult<Void> event) {
										if (event.failed()) {
											log.error(event.cause().getMessage(), event.cause());
										}
									}
								});
							} catch (FileNotFoundException exc) {
								log.error("Cannot delete file " + id, exc);
							}
							return;
						}
					}
					handler.handle(res.put("_id", id)
							.put("status", "ok")
							.put("metadata", metadata));
					sendFileMetadataForSecurityThreatsAnalysis(id, metadata);
					scanFile(path);
				})
				.onFailure(th ->  {
					handler.handle(res.put("status", "error"));
					log.error("Cannot write to filesystem", th);
				});
				request.resume();
			}
		});
		mkdirsIfNotExists(id, path, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					request.resume();
				} else {
					handler.handle(res.put("status", "error"));
					log.error(event.cause().getMessage(), event.cause());
				}
			}
		});
	}

	private void sendFileMetadataForSecurityThreatsAnalysis(final String id,
															final JsonObject metadata) {
		final UploadedFileMessage uploadedFileMessage = new UploadedFileMessage(
				id,
				metadata.getString("name"),
				metadata.getString("filename"),
				metadata.getString("content-type"),
				metadata.getString("content-transfer-encoding"),
				metadata.getString("charset"),
				metadata.getLong("size"),
				System.currentTimeMillis(),
				STORAGE_ID
		);
		messagingClient.pushMessages(uploadedFileMessage)
			.onSuccess(e -> log.debug("Successfully sent message to analyze file " + uploadedFileMessage.getId()))
			.onFailure(th -> log.warn("Could not send message to analyze file " + uploadedFileMessage.getId(), th));
	}

	@Override
	public void scanFile(String path) {
		if (antivirus != null) {
			antivirus.scan(path);
		}
	}

	private Future<Void> mkdirsIfNotExists(String id, String path) {
		Promise<Void> promise = Promise.promise();
		mkdirsIfNotExists(fs, id, path, event -> {
			if (event.failed()) {
				String message = String.format("[entcore@%s - mkdirsIfNotExists] Failed to proceed : %s",
						this.getClass().getSimpleName(), event.cause().getMessage());
				log.error(message);
				promise.fail(event.cause().getMessage());
			} else {
				promise.complete();
			}
		});
		return promise.future();
	}

	private void mkdirsIfNotExists(String id, String path, final Handler<AsyncResult<Void>> h) {
		mkdirsIfNotExists(fs, id, path, h);
	}

	static void mkdirsIfNotExists(FileSystem fs, String id, String path, final Handler<AsyncResult<Void>> h) {
		final String dir = org.entcore.common.utils.FileUtils.getParentPath(path);
		fs.exists(dir, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> event) {
				if (event.succeeded()) {
					if (Boolean.FALSE.equals(event.result())) {
						fs.mkdirs(dir, new Handler<AsyncResult<Void>>() {
							@Override
							public void handle(AsyncResult<Void> event) {
								h.handle(event);
							}
						});
					} else {
						h.handle(new DefaultAsyncResult<>((Void) null));
					}
				} else {
					h.handle(new DefaultAsyncResult<Void>(event.cause()));
				}
			}
		});
	}

	@Override
	public void writeBuffer(Buffer buff, String contentType, String filename, Handler<JsonObject> handler) {
		writeBuffer(UUID.randomUUID().toString(), buff, contentType, filename, handler);
	}

	@Override
	public void writeBuffer(final String id, final Buffer buff, final String contentType, final String filename,
							final Handler<JsonObject> handler) {
		try {
			writeBuffer(getWritePath(id), id, buff, contentType, filename, true, handler);
		} catch (FileNotFoundException e) {
			handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void writeBuffer(final String path, final String id, final Buffer buff, final String contentType, final String filename,
							final Handler<JsonObject> handler) {
		writeBuffer(path, id, buff, contentType, filename, false, handler);
	}


	@Override
	public void writeBuffer(final String path, final String id, final Buffer buff, final String contentType, final String filename,
							final boolean safe, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		mkdirsIfNotExists(id, path, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				fs.writeFile(path, buff, new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.succeeded()) {
							final JsonObject metadata = new JsonObject().put("content-type", contentType)
									.put("filename", filename).put("size", buff.length());
							res.put("status", "ok").put("_id", id).put("metadata", metadata);
							if(!safe) {
								sendFileMetadataForSecurityThreatsAnalysis(id, metadata);
								scanFile(path);
							}
						} else {
							res.put("status", "error").put("message", event.cause().getMessage());
						}
						handler.handle(res);
					}
				});
			}
		});
	}

	@Override
	public Future<JsonObject> writeBufferStream(ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
		return writeBufferStream(UUID.randomUUID().toString(), bufferReadStream, contentType, filename);
	}

	@Override
	public Future<JsonObject> writeBufferStream(String id, ReadStream<Buffer> bufferReadStream, String contentType, String filename) {
		try {
			return writeBufferStream(getWritePath(id), id, bufferReadStream, contentType, filename);
		} catch (FileNotFoundException e) {
			log.warn(e.getMessage(), e);
			return Future.failedFuture(String.format("%s: invalid.path", e.getMessage()));
		}
	}
	private Future<JsonObject> writeBufferStream(final String path, final String id, final ReadStream<Buffer> bufferReadStream,
								  final String contentType, final String filename) {
		Promise<JsonObject> promise = Promise.promise();
		final JsonObject res = new JsonObject();
		mkdirsIfNotExists(id, path)
				.compose(v -> streamBufferToFileSystem(path, bufferReadStream))
				.onSuccess(streamedFileResult -> {
					final JsonObject metadata = new JsonObject()
							.put("content-type", contentType)
							.put("filename", filename)
							.put("size", streamedFileResult.getWritePos());
					res.put("status", "ok")
							.put("_id", id)
							.put("metadata", metadata);
					promise.complete(res);
					sendFileMetadataForSecurityThreatsAnalysis(id, metadata);
					scanFile(path);
				})
				.onFailure(err -> {
					String message = String.format("[entcore@%s - streamFile - writeBufferStream] " +
									"Failed to mkdirsIfNotExists or streamBufferToFileSystem : %s",
							this.getClass().getSimpleName(), err.getMessage());
					log.error(message);
					promise.fail(err.getMessage());
				});
		return promise.future();
	}

	/**
	 * open/create a file and use buffer in read stream to pipe its content to the new file
	 * close buffer read stream after transfer
	 *
	 * return the piped file {@link AsyncFile}
	 *
	 * @param path				identifier chosen for the file
	 * @param bufferReadStream	Buffer in read stream
	 * @return Future {@link AsyncFile } streamed file
	 */
	private Future<AsyncFile> streamBufferToFileSystem(final String path, final ReadStream<Buffer> bufferReadStream) {
		Promise<AsyncFile> promise = Promise.promise();
		bufferReadStream.pause();
		this.fs.open(path, new OpenOptions(), fileResult -> {
			if (fileResult.failed()) {
				String message = String.format("[entcore@%s - streamFile] Failed to open/create file system : %s",
						this.getClass().getSimpleName(), fileResult.cause().getMessage());
				log.error(message);
				promise.fail(fileResult.cause().getMessage());
				bufferReadStream.pipe().close();
			} else {
				AsyncFile file = fileResult.result();
				try {
					bufferReadStream.pipe().to(file, pipeResult -> {
						if (pipeResult.failed()) {
							String message = String.format("[entcore@%s - streamFile] Failed to pipe used buffer to new file : %s",
									this.getClass().getSimpleName(), pipeResult.cause().getMessage());
							log.error(message);
							promise.fail(pipeResult.cause().getMessage());
							this.fs.delete(path, deleteRes -> {
								if (deleteRes.failed()) {
									log.error(String.format("[entcore@%s - streamFile - delete] Failed to delete new file : %s",
											this.getClass().getSimpleName(), deleteRes.cause().getMessage()));
								}
							});
							bufferReadStream.pipe().close();
						} else {
							promise.complete(file);
						}
					});
				} catch (IllegalStateException e) {
					String message = String.format("[entcore@%s - streamFile] Failed to init Pipe in buffer : %s",
							this.getClass().getSimpleName(), e.getMessage());
					log.error(message);
					promise.fail(e.getMessage());
					this.fs.delete(path, deleteRes -> {
						if (deleteRes.failed()) {
							log.error(String.format("[entcore@%s - streamFile - delete] Failed to delete new file : %s",
									this.getClass().getSimpleName(), deleteRes.cause().getMessage()));
						}
					});
				}
			}
		});
		return promise.future();
	}

	@Override
	public void writeFsFile(final String filename, final Handler<JsonObject> handler)
	{
		writeFsFile(UUID.randomUUID().toString(), filename, handler);
	}

	@Override
	public void writeFsFile(final String id, final String filename, final Handler<JsonObject> handler) {
		try {
			final String path = getWritePath(id);
			mkdirsIfNotExists(id, path, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						copyFilePath(filename, path, handler);
					} else {
						handler.handle(new JsonObject().put("status", "error")
								.put("message", event.cause().getMessage()));
						log.error(event.cause().getMessage(), event.cause());
					}
				}
			});
		} catch (FileNotFoundException e) {
			handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void findByFilenameEndingWith(String name, Handler<AsyncResult<JsonArray>> handler) {
		getReadPath(name, ar -> {
			if (ar.succeeded()) {
				String path = StringUtils.substringBeforeLast(ar.result(), File.separator);
				fs.readDir(path, String.format("(.*)%s", name), event -> {
					if (event.succeeded()) {
						JsonArray json = event.result().stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
						handler.handle(new DefaultAsyncResult<>(json));
					} else {
						handler.handle(new DefaultAsyncResult<>(event.cause()));
					}
				});
			} else {
				handler.handle(new DefaultAsyncResult<>(ar.cause()));
			}
		});
	}

	@Override
	public Future<byte[]> readFileToMemory(final UploadedFileMessage uploadedFileMessage) {
		final String id = uploadedFileMessage.getId();
		final Promise<byte[]> onFileRead = Promise.promise();
		getReadPath(id, pathResult -> {
			if (pathResult.succeeded()) {
				final String path = pathResult.result();
				// NB : Reading the whole file will fail if the file is too heavy (2Go)
				fs.readFile(path, result -> {
					if(result.succeeded()) {
						onFileRead.complete(result.result().getBytes());
					} else {
						onFileRead.fail(result.cause());
					}
				});
			} else {
				onFileRead.fail(pathResult.cause());
			}
		});
		return onFileRead.future();
	}

	@Override
	public void fileStats(String id, Handler<AsyncResult<FileStats>> handler) {
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				fs.props(ar.result(), res -> {
					if (res.succeeded()) {
						FileProps props = res.result();
						handler.handle(new DefaultAsyncResult<>(
								new FileStats(props.creationTime(), props.lastModifiedTime(), props.size())));
					} else {
						handler.handle(new DefaultAsyncResult<>(res.cause()));
						//log.error(res.cause().getMessage(), res.cause());
					}
				});
			} else {
				handler.handle(new DefaultAsyncResult<>(ar.cause()));
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}

	@Override
	public void readFile(String id, final Handler<Buffer> handler) {
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				fs.readFile(ar.result(), new Handler<AsyncResult<Buffer>>() {
					@Override
					public void handle(AsyncResult<Buffer> event) {
						if (event.succeeded()) {
							handler.handle(event.result());
						} else {
							handler.handle(null);
							log.error(event.cause().getMessage(), event.cause());
						}
					}
				});
			} else {
				handler.handle(null);
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}

	/**
	 * Allows the user to get a document from their workspace
	 * Same as readFile but returns a ReadStream<Buffer> rather than a buffer
	 * @param id : id of the attachment
	 * @param handler
	 */
	@Override
	public void readStreamFile(String id, final Handler<ReadStream<Buffer>> handler) {
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				fs.open(ar.result(), new OpenOptions(), fileRes -> {
					if (fileRes.succeeded()) {
						ReadStream<Buffer> fileStream = fileRes.result();
						handler.handle(fileStream);
					} else {
						handler.handle(null);
						log.error(fileRes.cause().getMessage(), fileRes.cause());
					}
				});
			} else {
				handler.handle(null);
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}



	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
		sendFile(id, downloadName, request, inline, metadata, null);
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline,
			JsonObject metadata, Handler<AsyncResult<Void>> resultHandler) {
		final HttpServerResponse resp = request.response();
		getReadPath(id, ar2 -> {
			if (ar2.succeeded()) {
				final String path = ar2.result();
				if (!inline) {
					String name = FileUtils.getNameWithExtension(downloadName, metadata);
					resp.putHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
				}
				if(metadata != null && metadata.containsKey("ETag")) {
					ETag.addHeader(resp, metadata.getString("ETag"));
				} else {
					ETag.addHeader(resp, id);
				}
				if (metadata != null && metadata.getString("content-type") != null) {
					resp.putHeader("Content-Type", metadata.getString("content-type"));
				}
				long offset = 0;
				long length = Long.MAX_VALUE;
				final String rangeHeader = request.getHeader("Range");
				if (rangeHeader != null && metadata != null) {
					final Matcher m = RANGE.matcher(rangeHeader);
					if (m.matches()) {
						try {
							final long size = metadata.getLong("size");
							long lastByte = size - 1;
							offset = Long.parseLong(m.group(1));
							final String suffix = m.group(2);
							if (isNotEmpty(suffix)) {
								lastByte = Math.min(Long.parseLong(suffix), lastByte);
							}
							length = lastByte + 1 - offset;
							request.response().putHeader("Content-Range", "bytes " + offset + "-" + lastByte + "/" + size);
							request.response().setStatusCode(206).setStatusMessage("Partial Content");
						} catch (Exception re) {
							log.error("Error Range Not Satisfiable", re);
						}
					}
				}
				if (resultHandler != null) {
					resp.sendFile(path, offset, length, resultHandler);
				} else {
					resp.sendFile(path, offset, length, ar -> {
						if (ar.failed() && !request.response().ended()) {
							Renders.notFound(request);
						}
					});
				}
			} else {
				resp.setStatusCode(404).setStatusMessage("Not Found").end();
				if (resultHandler != null) {
					resultHandler.handle(new DefaultAsyncResult<>((Void) null));
				}
				log.warn(ar2.cause().getMessage(), ar2.cause());
			}
		});
	}


	@Override
	public void removeFile(String id, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				final String path =	ar.result();
				fs.delete(path, new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.succeeded()) {
							res.put("status", "ok");
						} else {
							res.put("status", "error").put("message", event.cause().getMessage());
						}
						handler.handle(res);
					}
				});
			} else {
				handler.handle(res.put("status", "error").put("message", "invalid.path"));
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}

	@Override
	public void removeFiles(JsonArray ids, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		final AtomicInteger count = new AtomicInteger(ids.size());
		final JsonArray errors = new JsonArray();
		for (final Object o: ids) {
			if (o == null) {
				decrementRemove(count, errors, handler, res);
				continue;
			}
			getReadPath(o.toString(), ar -> {
				if (ar.succeeded()) {
					final String path = ar.result();
					fs.delete(path, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.failed()) {
								errors.add(new JsonObject().put("id", o.toString())
										.put("message", event.cause().getMessage()));
							}
							decrementRemove(count, errors, handler, res);
						}
					});
				} else {
					errors.add(new JsonObject().put("id", o.toString())
							.put("message", "invalid.path"));
					decrementRemove(count, errors, handler, res);
					log.warn(ar.cause().getMessage(), ar.cause());
				}
			});
		}
	}

	private void decrementRemove(AtomicInteger count, JsonArray errors, Handler<JsonObject> handler, JsonObject res) {
		if (count.decrementAndGet() <= 0) {

			if (errors.size() == 0) {
				handler.handle(res.put("status", "ok"));
			} else {
				handler.handle(res.put("status", "error").put("errors", errors));
			}
		}
	}

	@Override
	public void copyFile(final String id, final Handler<JsonObject> handler) {
		try {
			final String newId = UUID.randomUUID().toString();
			final String path = getWritePath(newId);
			mkdirsIfNotExists(newId, path, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						copyFileId(id, path, newId, handler);
					} else {
						handler.handle(new JsonObject().put("status", "error")
								.put("message", event.cause().getMessage()));
						log.error(event.cause().getMessage(), event.cause());
					}
				}
			});
		} catch (FileNotFoundException e) {
			handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void copyFileId(String id, final String to, final Handler<JsonObject> handler) {
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				copyFilePath(ar.result(), to, null, handler);
			} else {
				handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}

	@Override
	public void copyFilePath(String path, final String to, final Handler<JsonObject> handler) {
		copyFilePath(path, to, null, handler);
	}

	private void copyFileId(String id, final String to, final String newId, final Handler<JsonObject> handler) {
		getReadPath(id, ar -> {
			if (ar.succeeded()) {
				copyFilePath(ar.result(), to, newId, handler);
			} else {
				handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
				log.warn(ar.cause().getMessage(), ar.cause());
			}
		});
	}

	private void copyFilePath(String path, final String to, final String newId, final Handler<JsonObject> handler)
	{
		final JsonObject res = new JsonObject();

		fs.mkdirs(org.entcore.common.utils.FileUtils.getPathWithoutFilename(to), new Handler<AsyncResult<Void>>()
		{
			@Override
			public void handle(AsyncResult<Void> event)
			{
				if(event.succeeded() == true)
				{
					fs.copy(path, to, new Handler<AsyncResult<Void>>()
					{
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.succeeded()) {
								res.put("status", "ok").put("_id", (isNotEmpty(newId) ? newId : to));
							} else {
								res.put("status", "error").put("message", event.cause().getMessage());
								log.error(event.cause().getMessage(), event.cause());
							}
							handler.handle(res);
						}
					});
				}
				else
				{
					res.put("status", "error").put("message", event.cause().getMessage());
					log.error(event.cause().getMessage(), event.cause());
					handler.handle(res);
				}
			}
		});
	}

	@Override
	public void writeToFileSystem(String[] ids, String destinationPath, JsonObject alias,
			final Handler<JsonObject> handler) {
		final AtomicInteger count = new AtomicInteger(ids.length);
		final JsonArray errors = new JsonArray();
		for (final String id: ids) {
			if (id == null || id.isEmpty()) {
				decrementWriteToFS(count, errors, handler);
				continue;
			}
			final String d = destinationPath + File.separator + alias.getString(id, id);

			copyFileId(id, d, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject event) {
					if (!"ok".equals(event.getString("status"))) {
						errors.add(event.put("fileId",id));
					}
					decrementWriteToFS(count, errors, handler);
				}
			});
		}
	}

	private void decrementWriteToFS(AtomicInteger count, JsonArray errors, Handler<JsonObject> handler) {
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

	@Override
	public String getProtocol() {
		return "file";
	}

	@Override
	public String getBucket() {
		return basePaths.get(lastBucketIdx);
	}

	@Override
	public void stats(final Handler<AsyncResult<BucketStats>> handler) {
		fs.fsProps(getBucket(), new Handler<AsyncResult<FileSystemProps>>() {
			@Override
			public void handle(AsyncResult<FileSystemProps> event) {
				if (event.succeeded()) {
					final FileSystemProps fsProps = event.result();
					final BucketStats bucketStats = new BucketStats();
					bucketStats.setStorageSize(fsProps.totalSpace() - fsProps.usableSpace());
					handler.handle(new DefaultAsyncResult<>(bucketStats));
				} else {
					handler.handle(new DefaultAsyncResult<BucketStats>(event.cause()));
				}
			}
		});
	}

	private String getWritePath(String file) throws FileNotFoundException {
		return getFilePath(file, getBucket());
	}

	private String getFilePath(String file, final String bucket) throws FileNotFoundException {
		if (isNotEmpty(file)) {
			if (flat) {
				return bucket + file;
			} else {
				final int startIdx = file.lastIndexOf(File.separatorChar) + 1;
				final int extIdx = file.lastIndexOf('.');
				String filename = (extIdx > 0) ? file.substring(startIdx, extIdx) : file.substring(startIdx);
				if (isNotEmpty(filename)) {
					final int l = filename.length();
					if (l < 4) {
						filename = "0000".substring(0, 4 - l) + filename;
					}
					return bucket + filename.substring(l - 2) + File.separator + filename.substring(l - 4, l - 2) +
							File.separator + filename;
				}
			}
		}
		throw new FileNotFoundException("Invalid file : " + file);
	}

	private void getReadPath(String file, Handler<AsyncResult<String>> handler) {
		if (lastBucketIdx == 0) {
			try {
				final String path = getWritePath(file);
				if (fallbackStorage != null) {
					fallbackStorage.downloadFileIfNotExists(file, path, handler);
				} else {
					handler.handle(Future.succeededFuture(path));
				}
			} catch (FileNotFoundException e) {
				handler.handle(Future.failedFuture(e));
			}
		} else {
			getReadPath(file, 0, handler);
		}
	}

	private void getReadPath(final String file, final int idx, final Handler<AsyncResult<String>> handler) {
		final String p;
		try {
			p = getFilePath(file, basePaths.get(idx));
		} catch (FileNotFoundException e) {
			handler.handle(Future.failedFuture(e));
			return;
		}
		fs.exists(p, ar -> {
			if (ar.succeeded() && Boolean.TRUE.equals(ar.result())) {
				handler.handle(Future.succeededFuture(p));
			} else if (idx < lastBucketIdx) {
				getReadPath(file, idx + 1, handler);
			} else if (fallbackStorage != null) {
				fallbackStorage.downloadFile(file, p, handler);
			} else {
				handler.handle(Future.failedFuture(new FileNotFoundException("Not found file : " + file)));
			}
		});
	}

	@Override
	public Future<List<FileInfo>> deleteByFilter(final String directory, final Function<FileInfo, Boolean> filter) {
		final Promise<List<FileInfo>> rootPromise = Promise.promise();
		fs.readDir(directory, result -> {
			if (result.succeeded()) {
				final List<Future> futures = new ArrayList<>();
				for (final String file : result.result()) {
					final Promise<FileInfo> promise = Promise.promise();
					futures.add(promise.future());
					fs.props(file, propsResult -> {
						if (propsResult.succeeded()) {
							final FileInfo stats = new FileInfo(file, propsResult.result(), false);
							if (filter.apply(stats)) {
								// deleting...
								fs.deleteRecursive(file, true, deleteResult -> {
									if (deleteResult.succeeded()) {
										log.info("File deleted successfully:" + file);
										promise.complete(new FileInfo(file, propsResult.result(), true));
									} else {
										log.error("Could not delete file: "+ file, deleteResult.cause());
										promise.fail(deleteResult.cause());
									}
								});
							} else {
								promise.complete(stats);
							}
						} else {
							log.error("Could not get props of file: "+ file, propsResult.cause());
							promise.fail(propsResult.cause());
						}
					});
				}
				CompositeFuture.all(futures).onComplete(e -> {
					if(e.succeeded()){
						rootPromise.complete(e.result().list());
					}else{
						rootPromise.fail(e.cause());
					}
				});
			}else{
				rootPromise.fail(result.cause());
			}
		});
		return rootPromise.future();
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

	public void setFallbackStorage(FallbackStorage fallbackStorage) {
		this.fallbackStorage = fallbackStorage;
	}

}
