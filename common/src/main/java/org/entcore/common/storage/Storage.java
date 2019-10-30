/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.storage;

import org.entcore.common.validation.FileValidator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface Storage {

	void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler);

	void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler);

    default void writeUploadToFileSystem(HttpServerRequest request, String path, Handler<JsonObject> handler){};

	void writeBuffer(Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String basePath, String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeFsFile(String filename, Handler<JsonObject> handler);

	void writeFsFile(String id, String filename, Handler<JsonObject> handler);

	void readFile(String id, Handler<Buffer> handler);

	void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata);

	void sendFile(String id, String downloadName, HttpServerRequest request, boolean inline, JsonObject metadata,
			Handler<AsyncResult<Void>> resultHandler);

	void removeFile(String id, Handler<JsonObject> handler);

	void removeFiles(JsonArray ids, Handler<JsonObject> handler);

	void copyFile(String id, final Handler<JsonObject> handler);

	default void copyFileId(String id, String to, final Handler<JsonObject> handler)
	{
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	};

	default void copyFilePath(String path, String to, final Handler<JsonObject> handler)
	{
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	};

	void writeToFileSystem(String [] ids, String destinationPath, JsonObject alias, Handler<JsonObject> handler);

	String getProtocol();

	String getBucket();

	void stats(Handler<AsyncResult<BucketStats>> handler);

	void fileStats(String id, Handler<AsyncResult<FileStats>> handler);

	FileValidator getValidator();

	void findByFilenameEndingWith(String endsWith, Handler<AsyncResult<JsonArray>> handler);

}
