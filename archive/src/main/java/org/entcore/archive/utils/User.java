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

package org.entcore.archive.utils;


import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class User {

	public static void getOldGroups(String userId, final Handler<JsonArray> handler) {
		String query =
				"MATCH (u:User { id : {userId}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
				"RETURN b.IN_OUTGOING as groups ";
		JsonObject params = new JsonObject().put("userId", userId);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Either<String, JsonObject> r = Neo4jResult.validUniqueResult(message);
				JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
				if (r.isRight()) {
					a = r.right().getValue().getJsonArray("groups", a);
				}
				handler.handle(a);
			}
		});
	}

}
