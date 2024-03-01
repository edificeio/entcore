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

package org.entcore.common.s3.storage;

import io.vertx.core.buffer.Buffer;

public class StorageObject {

	private final String id;
	private final Buffer buffer;
	private final String filename;
	private final String contentType;

	public StorageObject(Buffer buffer, String filename, String contentType) {
		this(null, buffer, filename, contentType);
	}

	public StorageObject(String id, Buffer buffer, String filename, String contentType) {
		this.id = id;
		this.buffer = buffer;
		this.filename = filename;
		this.contentType = contentType;
	}

	public Buffer getBuffer() {
		return buffer;
	}

	public String getFilename() {
		return filename;
	}

	public String getContentType() {
		return contentType;
	}

	public String getId() {
		return id;
	}

}
