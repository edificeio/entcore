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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import org.entcore.common.messaging.to.UploadedFileMessage;
import org.entcore.common.validation.FileValidator;

import static fr.wseduc.webutils.Utils.isNotEmpty;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.function.Function;

public interface Storage {

	void writeUploadFile(HttpServerRequest request, Handler<JsonObject> handler);

	void writeUploadFile(HttpServerRequest request, Long maxSize, Handler<JsonObject> handler);

    default void writeUploadToFileSystem(HttpServerRequest request, String path, Handler<JsonObject> handler){};

	void writeBuffer(Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String basePath, String id, Buffer buff, String contentType, String filename, Handler<JsonObject> handler);

	void writeBuffer(String path, String id, Buffer buff, String contentType, String filename,
					 boolean safe, Handler<JsonObject> handler);

	/**
	 * write file in file system with buffer stream
	 *
	 * @param bufferReadStream	Buffer in read stream
	 * @param contentType		content type data of the read buffer
	 * @param filename			filename
	 * @return Future {@link Future <JsonObject>} containing "_id" which corresponds to the written file identifier and its "metadata" content
	 */
	Future<JsonObject> writeBufferStream(ReadStream<Buffer> bufferReadStream, String contentType, String filename);

	/**
	 * write file in file system with buffer stream
	 *
	 * @param id				identifier chosen for the file
	 * @param bufferReadStream	Buffer in read stream
	 * @param contentType		content type data of the read buffer
	 * @param filename			filename
	 * @return Future {@link Future <JsonObject>} containing "_id" which corresponds to the written file identifier and its "metadata" content
	 */
	Future<JsonObject> writeBufferStream(String id, ReadStream<Buffer> bufferReadStream, String contentType, String filename);

	void writeFsFile(String filename, Handler<JsonObject> handler);

	void writeFsFile(String id, String filename, Handler<JsonObject> handler);

	void readFile(String id, Handler<Buffer> handler);

	void readStreamFile(String id, Handler<ReadStream<Buffer>> handler);

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

	void scanFile(String path);

	void stats(Handler<AsyncResult<BucketStats>> handler);

	void fileStats(String id, Handler<AsyncResult<FileStats>> handler);

	FileValidator getValidator();

	void findByFilenameEndingWith(String endsWith, Handler<AsyncResult<JsonArray>> handler);

	/**
	 * <p>
	 *     Reads the whole content of the file into memory.
	 * </p>
	 *<p>
	 * NB : depending on the underlying implementation and the size of the file, a call to this method
	 * can fail if the file is too heavy
	 *</p>
	 * @param uploadedFileMessage Description of the file to read
	 * @return The raw content of the file
	 */
    Future<byte[]> readFileToMemory(UploadedFileMessage uploadedFileMessage);

	/**
	 * Delete all children matching filter
	 * @param parent directory
	 * @param filter returning true if child should be deleted
	 * @return list of deleted file
	 */
	default Future<List<FileInfo>> deleteByFilter(final String parent, final Function<FileInfo, Boolean> filter) {
		throw new UnsupportedOperationException("Not supported yet");
	}

	static String getFilePath(String file, final String bucket, boolean flat) throws FileNotFoundException {
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

	class FileInfo{
		public final String path;
		public final FileProps props;
		public final boolean deleted;

		public FileInfo(String path, FileProps props, boolean deleted) {
			this.path = path;
			this.props = props;
			this.deleted = deleted;
		}
	}
}
