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

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class SqlStatementsBuilder {

	private final JsonArray statements;

	public SqlStatementsBuilder() {
		this.statements = new JsonArray();
	}

	public SqlStatementsBuilder raw(String query) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.putString("action", "raw")
					.putString("command", query);
			statements.addObject(statement);
		}
		return this;
	}

	public SqlStatementsBuilder prepared(String query, JsonArray values) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.putString("action", "prepared")
					.putString("statement", query)
					.putArray("values", values);
			statements.addObject(statement);
		}
		return this;
	}

	public SqlStatementsBuilder insert(String table, JsonObject params) {
		return insert(table, params, null);
	}

	public SqlStatementsBuilder insert(String table, JsonObject params, String returning) {
		if (params == null) {
			return this;
		}
		JsonArray fields = new JsonArray();
		JsonArray values = new JsonArray();
		for (String attr : params.getFieldNames()) {
			fields.add(attr);
			values.add(params.getValue(attr));
		}
		insert(table, fields, new JsonArray().addArray(values), returning);
		return this;
	}

	public SqlStatementsBuilder insert(String table, JsonArray fields, JsonArray values) {
		return insert(table, fields, values, null);
	}

	public SqlStatementsBuilder insert(String table, JsonArray fields, JsonArray values, String returning) {
		if (table != null && !table.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.putString("action", "insert")
					.putString("table", table)
					.putArray("fields", fields)
					.putArray("values", values);
			if (returning != null && !returning.trim().isEmpty()) {
				statement.putString("returning", returning);
			}
			statements.addObject(statement);
		}
		return this;
	}

	public SqlStatementsBuilder select(String table, JsonArray fields) {
		if (table != null && !table.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.putString("action", "select")
					.putString("table", table)
					.putArray("fields", fields);
			statements.addObject(statement);
		}
		return this;
	}

	public JsonArray build() {
		return statements;
	}

}
