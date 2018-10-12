/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.common.sql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SqlStatementsBuilder {

	private final JsonArray statements;

	public SqlStatementsBuilder() {
		this.statements = new fr.wseduc.webutils.collections.JsonArray();
	}

	public SqlStatementsBuilder raw(String query) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.put("action", "raw")
					.put("command", query);
			statements.add(statement);
		}
		return this;
	}

	public SqlStatementsBuilder prepared(String query, JsonArray values) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.put("action", "prepared")
					.put("statement", query)
					.put("values", values);
			statements.add(statement);
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
		JsonArray fields = new fr.wseduc.webutils.collections.JsonArray();
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		for (String attr : params.fieldNames()) {
			fields.add(attr);
			values.add(params.getValue(attr));
		}
		insert(table, fields, new fr.wseduc.webutils.collections.JsonArray().add(values), returning);
		return this;
	}

	public SqlStatementsBuilder insert(String table, JsonArray fields, JsonArray values) {
		return insert(table, fields, values, null);
	}

	public SqlStatementsBuilder insert(String table, JsonArray fields, JsonArray values, String returning) {
		if (table != null && !table.trim().isEmpty() && values != null && !values.isEmpty()) {
			JsonObject statement = new JsonObject()
					.put("action", "insert")
					.put("table", table)
					.put("fields", fields)
					.put("values", values);
			if (returning != null && !returning.trim().isEmpty()) {
				statement.put("returning", returning);
			}
			statements.add(statement);
		}
		return this;
	}

	public SqlStatementsBuilder select(String table, JsonArray fields) {
		if (table != null && !table.trim().isEmpty()) {
			JsonObject statement = new JsonObject()
					.put("action", "select")
					.put("table", table)
					.put("fields", fields);
			statements.add(statement);
		}
		return this;
	}

	public JsonArray build() {
		return statements;
	}

}
