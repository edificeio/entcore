/*
 * Copyright © "Open Digital Education", 2017
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

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import org.entcore.common.storage.FileInfos;
import org.entcore.common.storage.StorageException;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MongoDBApplicationStorage extends AbstractApplicationStorage {

	protected final MongoDb mongo = MongoDb.getInstance();
	protected final String collection;
	protected final String application;
	protected final JsonObject keys;
	protected final JsonObject mapping;

	public MongoDBApplicationStorage(String collection, String application) {
		this(collection, application, null);
	}

	public MongoDBApplicationStorage(String collection, String application, JsonObject mapping) {
		this.collection = collection;
		this.application = application;
		this.mapping = new JsonObject()
				.put("title", "name")
				.put("name", "metadata.filename")
				.put("size", "metadata.size")
				.put("contentType", "metadata.content-type");
		if (mapping != null) {
			this.mapping.mergeIn(mapping);
		}
		this.keys = new JsonObject()
				.put(this.mapping.getString("owner", "owner"), 1)
				.put(this.mapping.getString("title", "title"), 1);
	}

	@Override
	public void getInfo(final String fileId, final Handler<AsyncResult<FileInfos>> handler) {
		final JsonObject query = new JsonObject().put(mapping.getString("file", "file"), fileId);
		mongo.findOne(collection, query, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					JsonObject res = event.body().getJsonObject("result");
					if (res != null) {
						final FileInfos fi = new FileInfos();
						fi.setApplication(application);
						fi.setId(fileId);
						fi.setName(res.getString(mapping.getString("title", "title")));
						fi.setOwner(res.getString(mapping.getString("owner", "owner")));
						handler.handle(new DefaultAsyncResult<>(fi));
					} else {
						handler.handle(new DefaultAsyncResult<>((FileInfos) null));
					}
				} else {
					handler.handle(new DefaultAsyncResult<FileInfos>(
							new StorageException(event.body().getString("message"))));
				}
			}
		});
	}

	@Override
	public void updateInfo(String fileId, FileInfos fileInfos, final Handler<AsyncResult<Integer>> handler) {
		final JsonObject query = new JsonObject().put(mapping.getString("file", "file"), fileId);
		final JsonObject modifier = new JsonObject().put("$set", fileInfos.toJsonExcludeEmpty(mapping));
		mongo.update(collection, query, modifier, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					handler.handle(new DefaultAsyncResult<>(event.body().getInteger("number")));
				} else {
					handler.handle(new DefaultAsyncResult<Integer>(
							new StorageException(event.body().getString("message"))));
				}
			}
		});
	}

}
