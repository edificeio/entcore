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
import fr.wseduc.webutils.FileUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

public class WorkspaceHelper {

	private static final String WORKSPACE_ADDRESS = "org.entcore.workspace";
	private final String gridfsAddress;
	private final EventBus eb;

	public WorkspaceHelper(String gridfsAddress, EventBus eb) {
		this.gridfsAddress = gridfsAddress;
		this.eb = eb;
	}

	public void addDocument(JsonObject uploaded, UserInfos userInfos, String name, String application,
			boolean protectedContent, JsonArray thumbs, Handler<Message<JsonObject>> handler) {
		if (userInfos == null) {
			if (handler != null) {
				handler.handle(new ErrorMessage("invalid.user"));
			}
			return;
		}
		JsonObject doc = new JsonObject();
		String now = MongoDb.formatDate(new Date());
		doc.putString("created", now);
		doc.putString("modified", now);
		doc.putString("owner", userInfos.getUserId());
		doc.putString("ownerName", userInfos.getUsername());
		if (application != null && !application.trim().isEmpty() && protectedContent) {
			doc.putBoolean("protected", true);
		}
		JsonObject m = new JsonObject()
				.putString("action", "addDocument")
				.putObject("document", doc)
				.putObject("uploaded", uploaded)
				.putString("name", name)
				.putString("application", application)
				.putArray("thumbs", thumbs);
		eb.send(WORKSPACE_ADDRESS, m, handler);
	}

	public void updateDocument(String id, JsonObject uploaded, String name, JsonArray thumbs,
			Handler<Message<JsonObject>> handler) {
		if (id == null || id.trim().isEmpty()) {
			if (handler != null) {
				handler.handle(new ErrorMessage("invalid.document.id"));
			}
			return;
		}
		JsonObject m = new JsonObject()
				.putString("action", "updateDocument")
				.putString("id", id)
				.putObject("uploaded", uploaded)
				.putString("name", name)
				.putArray("thumbs", thumbs);
		eb.send(WORKSPACE_ADDRESS, m, handler);
	}

	private void uploadDocument(final HttpServerRequest request, final String id,
			final String name, final String application, final boolean protectedContent,
			final JsonArray thumbs, final Handler<Message<JsonObject>> handler) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					request.resume();
					FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, new Handler<JsonObject>() {
						@Override
						public void handle(final JsonObject up) {
							if (up != null && !"error".equals(up.getString("status"))) {
								Handler<Message<JsonObject>> h = new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										if (!"ok".equals(message.body().getString("status"))) {
											FileUtils.gridfsRemoveFile(up.getString("_id"), eb, gridfsAddress, null);
										}
										if (handler != null) {
											handler.handle(message);
										}
									}
								};
								if (id != null && !id.trim().isEmpty()) {
									updateDocument(id, up, name, thumbs, h);
								} else {
									addDocument(up, userInfos, name, application, protectedContent, thumbs, h);
								}
							} else {
								if (handler != null) {
									handler.handle(new ErrorMessage("upload.error"));
								}
							}
						}
					});
				} else {
					if (handler != null) {
						handler.handle(new ErrorMessage("invalid.user"));
					}
				}
			}
		});
	}

	public void createDocument(final HttpServerRequest request, final String name, final String application,
			final boolean protectedContent, final JsonArray thumbs, final Handler<Message<JsonObject>> handler) {
		uploadDocument(request, null, name, application, protectedContent, thumbs, handler);
	}

	public void updateDocument(final HttpServerRequest request, final String id, final String name,
			final JsonArray thumbs, final Handler<Message<JsonObject>> handler) {
		uploadDocument(request, id, name, null, false, thumbs, handler);
	}

}
