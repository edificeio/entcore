/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.common.sql;

import org.entcore.common.bus.ErrorMessage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Sql {

	private String address;
	private EventBus eb;

	private Sql() {}

	private static class SqlHolder {
		private static final Sql instance = new Sql();
	}

	public static Sql getInstance() {
		return SqlHolder.instance;
	}

	public void init(EventBus eb, String address) {
		this.address = address;
		this.eb = eb;
	}

	public void prepared(String query, JsonArray values, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "prepared")
				.put("statement", query)
				.put("values", values);
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

	public void raw(String query, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "raw")
				.put("command", query);
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

	public void insert(String table, JsonObject params, Handler<Message<JsonObject>> handler) {
		insert(table, params, null, handler);
	}

	public void insert(String table, JsonObject params, String returning, Handler<Message<JsonObject>> handler) {
		if (params == null) {
			handler.handle(new ErrorMessage("invalid.parameters"));
			return;
		}
		JsonArray fields = new JsonArray();
		JsonArray values = new JsonArray();
		for (String attr : params.fieldNames()) {
			fields.add(attr);
			values.add(params.getValue(attr));
		}
		insert(table, fields, new JsonArray().add(values), returning, handler);
	}

	public void insert(String table, JsonArray fields, JsonArray values, Handler<Message<JsonObject>> handler) {
		insert(table, fields, values, null, handler);
	}

	public void insert(String table, JsonArray fields, JsonArray values, String returning,
			Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "insert")
				.put("table", table)
				.put("fields", fields)
				.put("values", values);
		if (returning != null && !returning.trim().isEmpty()) {
			j.put("returning", returning);
		}
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

	public void select(String table, JsonArray fields, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "select")
				.put("table", table)
				.put("fields", fields);
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

	public void transaction(JsonArray statements, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "transaction")
				.put("statements", statements);
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

	public static String upsert(String table, String updateQuery, String insertQuery) {
		return  "LOCK TABLE " + table + " IN SHARE ROW EXCLUSIVE MODE; " +
				"WITH upsert AS ("+ updateQuery + " RETURNING *) " +
				insertQuery +" WHERE NOT EXISTS (SELECT * FROM upsert);";
	}

	public static String listPrepared(List array) {
		return listPrepared(array.toArray());
	}

	public static String listPrepared(Object[] array) {
		StringBuilder sb = new StringBuilder("(");
		if (array != null && array.length > 0) {
			for (int i = 0; i< array.length; i++) {
				sb.append("?,");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.append(")").toString();
	}

	public static String arrayPrepared(Object[] array) {
		return arrayPrepared(array, false);
	}

	public static String arrayPrepared(Object[] array, Boolean useUnaccentFunction) {
		StringBuilder sb = new StringBuilder("(ARRAY[");
		final String token = (useUnaccentFunction != null && useUnaccentFunction) ? "unaccent(?)," : "?,";
		if (array != null && array.length > 0) {
			for (int i = 0; i< array.length; i++) {
				sb.append(token);
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.append("])").toString();
	}

	public static Object parseId(String id) {
		try {
			return Integer.valueOf(id);
		} catch (NumberFormatException e) {
			return id;
		}
	}

}
