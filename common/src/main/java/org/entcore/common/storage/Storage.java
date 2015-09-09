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

package org.entcore.common.storage;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface Storage {

	void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler);

	void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler);

	void writeBuffer(Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeFsFile(String id, String filename, Handler<JsonObject> handler);

	void readFile(String id, Handler<Buffer> handler);

	void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata);

	void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata,
			Handler<AsyncResult<Void>> resultHandler);

	void removeFile(String id, Handler<JsonObject> handler);

	void removeFiles(JsonArray ids, Handler<JsonObject> handler);

	void copyFile(String id, final Handler<JsonObject> handler);

	void writeToFileSystem(String [] ids, String destinationPath, JsonObject alias, Handler<JsonObject> handler);

	String getProtocol();

	String getBucket();

}
