/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.neo4j;

import org.vertx.java.core.json.JsonObject;

public class Neo4jUtils {

	public static String nodeSetPropertiesFromJson(String nodeAlias, JsonObject json) {
		StringBuilder sb = new StringBuilder();
		for (String attr: json.getFieldNames()) {
			sb.append(", ").append(nodeAlias).append(".").append(attr).append(" = {").append(attr).append("}");
		}
		if (sb.length() > 2) {
			return sb.append(" ").substring(2);
		}
		return " ";
	}

}
