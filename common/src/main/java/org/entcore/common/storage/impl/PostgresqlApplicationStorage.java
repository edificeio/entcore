/*
 * Copyright © WebServices pour l'Éducation, 2017
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
		sql.prepared(getInfoQuery(), new JsonArray().add(fileId), new Handler<Message<JsonObject>>() {
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
		JsonArray params = new JsonArray();
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
