/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.storage.impl;

import fr.wseduc.swift.utils.FileUtils;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.http.ETag;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.storage.AntivirusClient;
import org.entcore.common.storage.BucketStats;
import org.entcore.common.storage.Storage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemProps;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.validation.FileValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class FileStorage implements Storage {

	private static final Logger log = LoggerFactory.getLogger(FileStorage.class);
	private final String basePath;
	private final FileSystem fs;
	private final boolean flat;
	private AntivirusClient antivirus;
	private FileValidator validator;

	public FileStorage(Vertx vertx, String basePath, boolean flat) {
		this.basePath = (basePath != null && !basePath.endsWith("/")) ? basePath + "/" : basePath;
		this.flat = flat;
		this.fs = vertx.fileSystem();
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
		writeUploadFile(request, null, handler);
	}

	@Override
	public void writeUploadFile(final HttpServerRequest request, final Long maxSize, final Handler<JsonObject> handler) {
		request.pause();
		final String id = UUID.randomUUID().toString();
		final String path;
		final JsonObject res = new JsonObject();
		try {
			path = getPath(id);
		} catch (FileNotFoundException e) {
			handler.handle(res.put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
			return;
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
				upload.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						if (metadata.getLong("size") == 0l) {
							metadata.put("size", upload.size());
							if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
								handler.handle(res.put("status", "error")
										.put("message", "file.too.large"));
								try {
									fs.delete(getPath(id), new Handler<AsyncResult<Void>>() {
										@Override
										public void handle(AsyncResult<Void> event) {
											if (event.failed()) {
												log.error(event.cause().getMessage(), event.cause());
											}
										}
									});
								} catch (FileNotFoundException e) {
									log.error(e.getMessage(), e);
								}
							}
						}
						handler.handle(res.put("_id", id)
								.put("status", "ok")
								.put("metadata", metadata));
						scanFile(path);
					}
				});
				upload.exceptionHandler(new Handler<Throwable>() {
					@Override
					public void handle(Throwable event) {
						handler.handle(res.put("status", "error"));
						log.error(event.getMessage(), event);
					}
				});
				upload.streamToFileSystem(path);
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

	private void scanFile(String path) {
		if (antivirus != null) {
			antivirus.scan(path);
		}
	}

	private void mkdirsIfNotExists(String id, String path, final Handler<AsyncResult<Void>> h) {
		final String dir = path.substring(0, path.length() - id.length());
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
			writeBuffer(getPath(id), id, buff, contentType, filename, handler);
		} catch (FileNotFoundException e) {
			handler.handle(new JsonObject().put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void writeBuffer(final String path, final String id, final Buffer buff, final String contentType, final String filename,
			final Handler<JsonObject> handler) {
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
							scanFile(path);
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
	public void writeFsFile(final String id, final String filename, final Handler<JsonObject> handler) {
		try {
			final String path = getPath(id);
			mkdirsIfNotExists(id, path, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						copyFile(filename, path, handler);
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
	public void readFile(String id, final Handler<Buffer> handler) {
		try {
			fs.readFile(getPath(id), new Handler<AsyncResult<Buffer>>() {
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
		} catch (FileNotFoundException e) {
			handler.handle(null);
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
		sendFile(id, downloadName, request, inline, metadata, null);
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline,
			JsonObject metadata, Handler<AsyncResult<Void>> resultHandler) {
		final HttpServerResponse resp = request.response();
		try {
			final String path = getPath(id);
			if (!inline) {
				String name = FileUtils.getNameWithExtension(downloadName, metadata);
				resp.putHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
			}
			ETag.addHeader(resp, id);
			if (metadata != null && metadata.getString("content-type") != null) {
				resp.putHeader("Content-Type", metadata.getString("content-type"));
			}
			if (resultHandler != null) {
				resp.sendFile(path, resultHandler);
			} else {
				resp.sendFile(path, ar -> {
					if (ar.failed() && !request.response().ended()) {
						Renders.notFound(request);
					}
				});
			}
		} catch (FileNotFoundException e) {
			resp.setStatusCode(404).setStatusMessage("Not Found").end();
			if (resultHandler != null) {
				resultHandler.handle(new DefaultAsyncResult<>((Void) null));
			}
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void removeFile(String id, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		try {
			final String path = getPath(id);
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
		} catch (FileNotFoundException e) {
			handler.handle(res.put("status", "error").put("message", "invalid.path"));
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void removeFiles(JsonArray ids, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		final AtomicInteger count = new AtomicInteger(ids.size());
		final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
		for (final Object o: ids) {
			if (o == null) {
				decrementRemove(count, errors, handler, res);
				continue;
			}
			try {
				final String path = getPath(o.toString());
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
			} catch (FileNotFoundException e) {
				errors.add(new JsonObject().put("id", o.toString())
						.put("message", "invalid.path"));
				decrementRemove(count, errors, handler, res);
				log.warn(e.getMessage(), e);
			}
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
			final String path = getPath(newId);
			final String sourcePath = getPath(id);
			mkdirsIfNotExists(newId, path, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						copyFile(sourcePath, path, newId, handler);
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

	private void copyFile(String id, final String to, final Handler<JsonObject> handler) {
		copyFile(id, to, null, handler);
	}

	private void copyFile(String id, final String to, final String newId, final Handler<JsonObject> handler) {
		final JsonObject res = new JsonObject();
		fs.copy(id, to, new Handler<AsyncResult<Void>>() {
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

	@Override
	public void writeToFileSystem(String[] ids, String destinationPath, JsonObject alias,
			final Handler<JsonObject> handler) {
		final AtomicInteger count = new AtomicInteger(ids.length);
		final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
		for (final String id: ids) {
			if (id == null || id.isEmpty()) {
				decrementWriteToFS(count, errors, handler);
				continue;
			}
			final String d = destinationPath + File.separator + alias.getString(id, id);
			try {
				copyFile(getPath(id), d, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject event) {
						if (!"ok".equals(event.getString("status"))) {
							errors.add(event);
						}
						decrementWriteToFS(count, errors, handler);
					}
				});
			} catch (FileNotFoundException e) {
				errors.add(new JsonObject().put("status", "error").put("message", "invalid.path"));
				decrementWriteToFS(count, errors, handler);
				log.warn(e.getMessage(), e);
			}
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
		return basePath;
	}

	@Override
	public void stats(final Handler<AsyncResult<BucketStats>> handler) {
		fs.fsProps(basePath, new Handler<AsyncResult<FileSystemProps>>() {
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

	private String getPath(String file) throws FileNotFoundException {
		if (isNotEmpty(file)) {
			if (flat) {
				return basePath + file;
			} else {
				final int startIdx = file.lastIndexOf(File.separatorChar) + 1;
				final int extIdx = file.lastIndexOf('.');
				String filename = (extIdx > 0) ? file.substring(startIdx, extIdx) : file.substring(startIdx);
				if (isNotEmpty(filename)) {
					final int l = filename.length();
					if (l < 4) {
						filename = "0000".substring(0, 4 - l) + filename;
					}
					return basePath + filename.substring(l - 2) + File.separator + filename.substring(l - 4, l - 2) +
							File.separator + filename;
				}
			}
		}
		throw new FileNotFoundException("Invalid file : " + file);
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

}
