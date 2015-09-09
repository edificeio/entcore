/*
 * Copyright © WebServices pour l'Éducation, 2015
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

import fr.wseduc.swift.SwiftClient;
import fr.wseduc.swift.storage.StorageObject;
import org.entcore.common.storage.Storage;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class SwiftStorage implements Storage {

	private final SwiftClient swiftClient;
	private final String container;

	public SwiftStorage(Vertx vertx, URI uri, String container, String user, String password) {
		this.container = container;
		this.swiftClient = new SwiftClient(vertx, uri, container);
		this.swiftClient.authenticate(user, password, new AsyncResultHandler<Void>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.failed()) {
					event.cause().printStackTrace();
				}
			}
		});
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
		swiftClient.uploadFile(request, handler);
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler) {
		swiftClient.uploadFile(request, maxSize, handler);
	}

	@Override
	public void writeBuffer(Buffer buff, String contentType, String filename, final Handler<JsonObject> handler) {
		StorageObject o = new StorageObject(buff, filename, contentType);
		writeStorageObject(handler, o);
	}

	@Override
	public void writeBuffer(String id, Buffer buff, String contentType, String filename,
			final Handler<JsonObject> handler) {
		StorageObject o = new StorageObject(id, buff, filename, contentType);
		writeStorageObject(handler, o);
	}

	@Override
	public void writeFsFile(String id, String filename, Handler<JsonObject> handler) {
		swiftClient.writeFromFileSystem(id, filename, container, handler);
	}

	private void writeStorageObject(final Handler<JsonObject> handler, StorageObject o) {
		swiftClient.writeFile(o, new AsyncResultHandler<String>() {
			@Override
			public void handle(AsyncResult<String> event) {
				JsonObject j = new JsonObject();
				if (event.succeeded()) {
					j.putString("status", "ok").putString("_id", event.result());
				} else {
					j.putString("status", "error").putString("message", event.cause().getMessage());
				}
				handler.handle(j);
			}
		});
	}

	@Override
	public void readFile(String id, final Handler<Buffer> handler) {
		swiftClient.readFile(id, new AsyncResultHandler<StorageObject>() {
			@Override
			public void handle(AsyncResult<StorageObject> event) {
				if (event.succeeded()) {
					handler.handle(event.result().getBuffer());
				} else {
					handler.handle(null);
				}
			}
		});
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
		swiftClient.downloadFile(id, request, inline, downloadName, metadata, id);
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata,
			Handler<AsyncResult<Void>> resultHandler) {
		swiftClient.downloadFile(id, request, inline, downloadName, metadata, id, resultHandler);
	}

	@Override
	public void removeFile(String id, final Handler<JsonObject> handler) {
		swiftClient.deleteFile(id, new AsyncResultHandler<Void>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				JsonObject j = new JsonObject();
				if (event.succeeded()) {
					j.putString("status", "ok");
				} else {
					j.putString("status", "error").putString("message", event.cause().getMessage());
				}
				handler.handle(j);
			}
		});
	}

	@Override
	public void removeFiles(JsonArray ids, final Handler<JsonObject> handler) {
		final AtomicInteger count = new AtomicInteger(ids.size());
		final JsonArray errors = new JsonArray();
		for (final Object o: ids) {
			swiftClient.deleteFile(o.toString(), new AsyncResultHandler<Void>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.failed()) {
						errors.add(new JsonObject().putString("id", o.toString())
								.putString("message", event.cause().getMessage()));
					}
					if (count.decrementAndGet() <= 0) {
						JsonObject j = new JsonObject();
						if (errors.size() == 0) {
							handler.handle(j.putString("status", "ok"));
						} else {
							handler.handle(j.putString("status", "error").putArray("errors", errors));
						}
					}
				}
			});
		}
	}

	@Override
	public void copyFile(String id, final Handler<JsonObject> handler) {
		swiftClient.copyFile(id, new AsyncResultHandler<String>() {
			@Override
			public void handle(AsyncResult<String> event) {
				JsonObject j = new JsonObject();
				if (event.succeeded()) {
					j.putString("status", "ok").putString("_id", event.result());
				} else {
					j.putString("status", "error").putString("message", event.cause().getMessage());
				}
				handler.handle(j);
			}
		});
	}

	@Override
	public void writeToFileSystem(String [] ids, String destinationPath, JsonObject alias,
			final Handler<JsonObject> handler) {
		final AtomicInteger count = new AtomicInteger(ids.length);
		final JsonArray errors = new JsonArray();
		for (final String id: ids) {
			if (id == null || id.isEmpty()) {
				count.decrementAndGet();
				continue;
			}
			String d = destinationPath + File.separator + alias.getString(id, id);
			swiftClient.writeToFileSystem(id, d, new AsyncResultHandler<String>() {
				@Override
				public void handle(AsyncResult<String> event) {
					if (event.failed()) {
						errors.add(new JsonObject().putString("id", id)
								.putString("message", event.cause().getMessage()));
					}
					if (count.decrementAndGet() <= 0) {
						JsonObject j = new JsonObject();
						if (errors.size() == 0) {
							handler.handle(j.putString("status", "ok"));
						} else {
							handler.handle(j.putString("status", "error").putArray("errors", errors)
									.putString("message", errors.encode()));
						}
					}
				}
			});
		}
	}

	@Override
	public String getProtocol() {
		return "swift";
	}

	@Override
	public String getBucket() {
		return container;
	}

}
