/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.bus;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.AsyncResult;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.logging.Logger;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Date;

import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncError;

public class WorkspaceHelper {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceHelper.class);
	private static final String WORKSPACE_ADDRESS = "org.entcore.workspace";
	private final EventBus eb;
	private final Storage storage;

	public WorkspaceHelper(EventBus eb, Storage storage) {
		this.storage = storage;
		this.eb = eb;
	}

	public void addDocument(JsonObject uploaded, UserInfos userInfos, String name, String application,
			boolean protectedContent, JsonArray thumbs, Handler<AsyncResult<Message<JsonObject>>> handler) {
		if (userInfos == null) {
			if (handler != null) {
				handleAsyncError("invalid.user", handler);
			}
			return;
		}
		JsonObject doc = new JsonObject();
		String now = MongoDb.formatDate(new Date());
		doc.put("created", now);
		doc.put("modified", now);
		doc.put("owner", userInfos.getUserId());
		doc.put("ownerName", userInfos.getUsername());
		if (application != null && !application.trim().isEmpty() && protectedContent) {
			doc.put("protected", true);
		}
		JsonObject m = new JsonObject()
				.put("action", "addDocument")
				.put("document", doc)
				.put("uploaded", uploaded)
				.put("name", name)
				.put("application", application)
				.put("thumbs", thumbs);
		eb.send(WORKSPACE_ADDRESS, m, handler);
	}

	public void updateDocument(String id, JsonObject uploaded, String name, JsonArray thumbs,
			Handler<AsyncResult<Message<JsonObject>>> handler) {
		if (id == null || id.trim().isEmpty()) {
			if (handler != null) {
				handleAsyncError("invalid.document.id", handler);
			}
			return;
		}
		JsonObject m = new JsonObject()
				.put("action", "updateDocument")
				.put("id", id)
				.put("uploaded", uploaded)
				.put("name", name)
				.put("thumbs", thumbs);
		eb.send(WORKSPACE_ADDRESS, m, handler);
	}

	private void uploadDocument(final HttpServerRequest request, final String id,
			final String name, final String application, final boolean protectedContent,
			final JsonArray thumbs, final Handler<AsyncResult<Message<JsonObject>>> handler) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					request.resume();
					storage.writeUploadFile(request, new Handler<JsonObject>() {
						@Override
						public void handle(final JsonObject up) {
							if (up != null && !"error".equals(up.getString("status"))) {
								Handler<AsyncResult<Message<JsonObject>>> h = message -> {
									if (message.failed()) {
										storage.removeFile(up.getString("_id"), new Handler<JsonObject>() {
												@Override
												public void handle(JsonObject event) {
													if (!"ok".equals(event.getString("status"))) {
														log.error("Error removing file " + up.getString("_id") + " : "
																+ event.getString("message"));
													}
												}
											});
									}
									if (handler != null) {
										handler.handle(message);
									}
								};
								if (id != null && !id.trim().isEmpty()) {
									updateDocument(id, up, name, thumbs, h);
								} else {
									addDocument(up, userInfos, name, application, protectedContent, thumbs, h);
								}
							} else {
								if (handler != null) {
									handleAsyncError("upload.error", handler);
								}
							}
						}
					});
				} else {
					if (handler != null) {
						handleAsyncError("invalid.user", handler);
					}
				}
			}
		});
	}

	public void createDocument(final HttpServerRequest request, final String name, final String application,
			final boolean protectedContent, final JsonArray thumbs, final Handler<AsyncResult<Message<JsonObject>>> handler) {
		uploadDocument(request, null, name, application, protectedContent, thumbs, handler);
	}

	public void updateDocument(final HttpServerRequest request, final String id, final String name,
			final JsonArray thumbs, final Handler<AsyncResult<Message<JsonObject>>> handler) {
		uploadDocument(request, id, name, null, false, thumbs, handler);
	}

	public void getDocument(String id, Handler<AsyncResult<Message<JsonObject>>> handler) {
		JsonObject m = new JsonObject()
				.put("action", "getDocument")
				.put("id", id);
		eb.send(WORKSPACE_ADDRESS, m, handler);
	}

	public void readFile(String id, Handler<Buffer> handler) {
		storage.readFile(id, handler);
	}

	public void readDocument(String documentId, final Handler<Document> handler) {
		getDocument(documentId, new Handler<AsyncResult<Message<JsonObject>>>() {
			@Override
			public void handle(AsyncResult<Message<JsonObject>> event) {
				if (event.failed()) {
					handler.handle(null);
					return;
				}
				JsonObject res = event.result().body();
				String status = res.getString("status");
				final JsonObject result = res.getJsonObject("result");
				if ("ok".equals(status) && result != null) {
					String file = result.getString("file");
					if (file != null && !file.trim().isEmpty()) {
						readFile(file, new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								if (event != null) {
									handler.handle(new Document(result, event));
								} else {
									handler.handle(null);
								}
							}
						});
					} else {
						handler.handle(null);
					}
				} else {
					handler.handle(null);
				}
			}
		});
	}

	public static class Document {
		private final JsonObject document;
		private final Buffer data;

		public Document(JsonObject document, Buffer data) {
			this.document = document;
			this.data = data;
		}

		public Buffer getData() {
			return data;
		}

		public JsonObject getDocument() {
			return document;
		}
	}

}
