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

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import org.entcore.common.storage.Storage;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.FileUtils.*;

public class GridfsStorage implements Storage {

	private final EventBus eb;
	private final String gridfsAddress;
	private final String bucket;

	public GridfsStorage(Vertx vertx, EventBus eb, String gridfsAddress) {
		this(vertx, eb, gridfsAddress, "fs");
	}

	public GridfsStorage(Vertx vertx, EventBus eb, String gridfsAddress, String bucket) {
		this.eb = eb;
		String node = (String) vertx.sharedData().getMap("server").get("node");
		if (node == null) {
			node = "";
		}
		this.gridfsAddress = node + gridfsAddress;
		this.bucket = bucket;
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler) {
		gridfsWriteUploadFile(request, eb, gridfsAddress, handler);
	}

	@Override
	public void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler) {
		gridfsWriteUploadFile(request,eb, gridfsAddress, maxSize, handler);
	}

	@Override
	public void writeBuffer(Buffer buff, String contentType, String filename, Handler<JsonObject> handler) {
		gridfsWriteBuffer(buff, contentType, filename, eb, handler, gridfsAddress);
	}

	@Override
	public void readFile(String id, Handler<Buffer> handler) {
		gridfsReadFile(id, eb, gridfsAddress, handler);
	}

	@Override
	public void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata) {
		gridfsSendFile(id, downloadName, eb, gridfsAddress, request.response(), inline, metadata);
	}

	@Override
	public void removeFile(String id, Handler<JsonObject> handler) {
		gridfsRemoveFile(id, eb, gridfsAddress, handler);
	}

	@Override
	public void removeFiles(JsonArray ids, Handler<JsonObject> handler) {
		gridfsRemoveFiles(ids, eb, gridfsAddress, handler);
	}

	@Override
	public void copyFile(String id, Handler<JsonObject> handler) {
		gridfsCopyFile(id, eb, gridfsAddress, handler);
	}

	@Override
	public void writeToFileSystem(String [] ids, String destinationPath, JsonObject alias,
			final Handler<JsonObject> handler) {
		QueryBuilder q = QueryBuilder.start("_id").in(ids);
		JsonObject e = new JsonObject()
				.putString("action", "write")
				.putString("path", destinationPath)
				.putObject("alias", alias)
				.putObject("query", MongoQueryBuilder.build(q));
		eb.send(gridfsAddress + ".json", e, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(event.body());
			}
		});
	}

	@Override
	public String getProtocol() {
		return "gridfs";
	}

	@Override
	public String getBucket() {
		return bucket;
	}

}
