/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.neo4j;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class StatementsBuilder {

	private final JsonArray statements;

	public StatementsBuilder() {
		this.statements = new fr.wseduc.webutils.collections.JsonArray();
	}

	public StatementsBuilder add(String query, JsonObject params) {
		if (query != null && !query.trim().isEmpty()) {
			JsonObject statement = new JsonObject().put("statement", query);
			if (params != null) {
				statement.put("parameters", params);
			}
			statements.add(statement);
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
