package org.entcore.common.neo4j;

import io.vertx.core.json.JsonObject;

/**
 * Utility class that bears a neo4j query and its associated valued params
 */
public class Neo4jQueryAndParams {

	private String query;
	private JsonObject params;

	public Neo4jQueryAndParams(String query, JsonObject params) {
		this.query = query;
		this.params = params;
	}

	public String getQuery() {
		return query;
	}

	public JsonObject getParams() {
		return params;
	}
}
