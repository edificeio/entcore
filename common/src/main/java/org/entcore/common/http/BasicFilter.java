/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.http;

import fr.wseduc.webutils.collections.Joiner;
import fr.wseduc.webutils.request.filter.AbstractBasicFilter;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BasicFilter extends AbstractBasicFilter {

	@Override
	protected void validateClientScope(String clientId, String secret, final Handler<String> handler) {
		String query =
				"MATCH (n:Application {name: {clientId}, secret: {secret}, grantType: 'Basic'}) " +
				"RETURN n.scope as scope";
		JsonObject params = new JsonObject().put("clientId", clientId).put("secret", secret);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					handler.handle(Joiner.on(" ").join(res.getJsonObject(0).getJsonArray("scope")));
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
