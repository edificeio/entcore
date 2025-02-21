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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.bus.ErrorMessage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Set;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Sql implements ISql {

	private String address;
	private EventBus eb;

	private Sql() {}

	private static class SqlHolder {
		private static final Sql instance = new Sql();
	}

	public static Sql getInstance() {
		return SqlHolder.instance;
	}

	/**
	 * Warning : use in some specifics case as two verticle with postgres in same module.
	 * In others cases use getInstance() method.
	 */
	public static Sql createInstance(EventBus eb, String address) {
		return new Sql().init(eb, address);
	}

	public Sql init(EventBus eb, String address) {
		this.address = address;
		this.eb = eb;
		return this;
	}

	@Override
	public void prepared(String query, JsonArray values, Handler<Message<JsonObject>> handler) {
		prepared(query, values, new DeliveryOptions(), handler);
	}

	@Override
	public void prepared(String query, JsonArray values, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "prepared")
				.put("statement", query)
				.put("values", values);
		eb.request(address, j, deliveryOptions, handlerToAsyncHandler(handler));
	}

	@Override
	public Future<Message<JsonObject>> prepared(String query, JsonArray values, DeliveryOptions deliveryOptions) {
		Promise<Message<JsonObject>> responseMessagePromise = Promise.promise();
		JsonObject message = new JsonObject()
				.put("action", "prepared")
				.put("statement", query)
				.put("values", values);
		eb.request(address, message, deliveryOptions, handlerToAsyncHandler(responseMessagePromise::complete));
		return responseMessagePromise.future();
	}

	@Override
	public void raw(String query, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "raw")
				.put("command", query);
		eb.request(address, j, handlerToAsyncHandler(handler));
	}

	@Override
	public void insert(String table, JsonObject params, Handler<Message<JsonObject>> handler) {
		insert(table, params, null, handler);
	}

	@Override
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

	@Override
	public void insert(String table, JsonArray fields, JsonArray values, Handler<Message<JsonObject>> handler) {
		insert(table, fields, values, null, handler);
	}

	@Override
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
		eb.request(address, j, handlerToAsyncHandler(handler));
	}

	@Override
	public void upsert(String table, JsonArray fields, JsonArray values, JsonArray conflictFields,
										 JsonArray updateFields, String returning,
										 Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "upsert")
				.put("table", table)
				.put("fields", fields)
				.put("values", values)
				.put("conflictFields", conflictFields)
				.put("updateFields", updateFields);
		if (returning != null && !returning.trim().isEmpty()) {
			j.put("returning", returning);
		}
		eb.request(address, j, handlerToAsyncHandler(handler));
	}

	@Override
	public void select(String table, JsonArray fields, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "select")
				.put("table", table)
				.put("fields", fields);
		eb.request(address, j, handlerToAsyncHandler(handler));
	}

	@Override
	public void transaction(JsonArray statements, Handler<Message<JsonObject>> handler) {
		transaction(statements, new DeliveryOptions(), handler);
	}

	@Override
	public void transaction(JsonArray statements, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("action", "transaction")
				.put("statements", statements);
		eb.request(address, j, deliveryOptions, handlerToAsyncHandler(handler));
	}

	public static String upsert(String table, String updateQuery, String insertQuery) {
		return  "LOCK TABLE " + table + " IN SHARE ROW EXCLUSIVE MODE; " +
				"WITH upsert AS ("+ updateQuery + " RETURNING *) " +
				insertQuery +" WHERE NOT EXISTS (SELECT * FROM upsert);";
	}

	public static String listPrepared(JsonArray array) {
		return listPrepared(array.getList().toArray());
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

	/**
	 * Creates a placeholder for IN clause and updates the query params accordingly.
	 * @param items the set of string items to be checked by the IN clause
	 * @param params the query params to be updated
	 * @return the placeholder
	 */
	public static String preparePlaceholderAndParamsForInClause(Set<String> items, JsonArray params) {
		StringBuilder placeholder = new StringBuilder("(");
		items.forEach(item -> {
			placeholder.append("?,");
			params.add(item);
		});
		placeholder.deleteCharAt(placeholder.length() - 1);
		return placeholder.append(")").toString();
	}

	public static String arrayPrepared(JsonArray array) {
		return arrayPrepared(array.getList().toArray(), false);
	}

	public static String arrayPrepared(List array) {
		return arrayPrepared(array.toArray(), false);
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

	public static String escapeField(String str) {
		return "\"" + str.replace("\"", "\"\"") + "\"";
	}

	public static String escapeValue(Object v) {
		if (v == null) {
			return "NULL";
		} else if (v instanceof Integer || v instanceof Boolean) {
			return v.toString();
		} else {
			return "'" + v.toString().replace("'", "''") + "'";
		}
	}

	public static String insertQuery(String table, JsonObject json) {
		final Set<String> fields = json.fieldNames();
		if (table == null || table.isEmpty() || fields == null || fields.isEmpty()) {
			return null;
		}
		final StringBuilder sb = new StringBuilder("INSERT INTO ")
				.append(table)
				.append(" (");
		final StringBuilder sb2 = new StringBuilder();
		for (String o : fields) {
			sb.append(escapeField(o)).append(",");
			sb2.append(escapeValue(json.getValue(o))).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb2.deleteCharAt(sb2.length()-1);
		sb.append(") VALUES (").append(sb2.toString()).append(")");
		return sb.toString();
	}

}
