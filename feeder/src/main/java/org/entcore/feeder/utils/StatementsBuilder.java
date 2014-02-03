package org.entcore.feeder.utils;


import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

public class StatementsBuilder {

	private final JsonArray statements;

	public StatementsBuilder() {
		this.statements = new JsonArray();
	}

	public StatementsBuilder add(String query, JsonObject params) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject().putString("statement", query);
			if (params != null) {
				statement.putObject("parameters", params);
			}
			statements.addObject(statement);
		}
		return this;
	}

	public StatementsBuilder add(String query, Map<String, Object> params) {
		return add(query, new JsonObject(params));
	}

	public StatementsBuilder add(String query) {
		return add(query, (JsonObject) null);
	}

	public JsonArray build() {
		return statements;
	}

}
