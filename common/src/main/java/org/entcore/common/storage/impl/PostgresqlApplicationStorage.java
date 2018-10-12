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

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.FileInfos;
import org.entcore.common.storage.StorageException;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.sql.SqlResult.validResult;

public class PostgresqlApplicationStorage extends AbstractApplicationStorage {

	private final Sql sql = Sql.getInstance();
	private final String table;
	private final String application;
	private final JsonObject mapping;
	private final String keys;

	public PostgresqlApplicationStorage(String table, String application) {
		this(table, application, null);
	}

	public PostgresqlApplicationStorage(String table, String application, JsonObject mapping) {
		this.table = table;
		this.application = application;
		this.mapping = new JsonObject();
		if (mapping != null) {
			this.mapping.mergeIn(mapping);
		}
		this.keys = this.mapping.getString("owner", "owner") + "," + this.mapping.getString("name", "name") + "," +
				this.mapping.getString("size", "size");
	}

	@Override
	public void getInfo(final String fileId, final Handler<AsyncResult<FileInfos>> handler) {
		getInfoProcess(fileId, handler);
	}

	protected void getInfoProcess(final String fileId, final Handler<AsyncResult<FileInfos>> handler) {
		sql.prepared(getInfoQuery(), new fr.wseduc.webutils.collections.JsonArray().add(fileId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Either<String, JsonArray> r = validResult(event);
				if (r.isRight()) {
					final JsonArray result = r.right().getValue();
					if (result.size() == 1) {
						final JsonObject res = result.getJsonObject(0);
						final FileInfos fi = new FileInfos();
						fi.setApplication(application);
						fi.setId(fileId);
						fi.setName(res.getString(mapping.getString("name", "name")));
						fi.setOwner(res.getString(mapping.getString("owner", "owner")));
						fi.setSize(res.getInteger(mapping.getString("size", "size")));
						handler.handle(new DefaultAsyncResult<>(fi));
					} else {
						handler.handle(new DefaultAsyncResult<>((FileInfos) null));
					}
				} else {
					handler.handle(new DefaultAsyncResult<FileInfos>(
							new StorageException(r.left().getValue())));
				}
			}
		});
	}

	protected String getInfoQuery() {
		return "select " + keys + " from " + table + " where " + mapping.getString("id", "id") + " = ?;";
	}

	@Override
	public void updateInfo(String fileId, FileInfos fileInfos, final Handler<AsyncResult<Integer>> handler) {
		JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
		final String query =
				"update " + table +
				" set " + generateColumns(fileInfos.toJsonExcludeEmpty(mapping), params) +
				"where " + mapping.getString("id", "id") + " = ?;" ;
		params.add(fileId);
		sql.prepared(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Integer count = event.body().getInteger("rows");
				if (count != null) {
					handler.handle(new DefaultAsyncResult<>(count));
				} else {
					handler.handle(new DefaultAsyncResult<Integer>(
							new StorageException(event.body().getString("message"))));
				}
			}
		});
	}

	private String generateColumns(JsonObject j, JsonArray params) {
		StringBuilder sb = new StringBuilder();
		for (String attr : j.fieldNames()) {
			sb.append(attr).append("= ?, ");
			params.add(j.getValue(attr));
		}
		return sb.toString().substring(0, sb.length() - 2);
	}

}
