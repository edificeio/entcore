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

package org.entcore.auth.users;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.RepositoryEvents;

public class AuthRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(AuthRepositoryEvents.class);

	private NewDeviceWarningTask NDWTask;

	public AuthRepositoryEvents(NewDeviceWarningTask NDWTask)
	{
		this.NDWTask = NDWTask;
	}

	@Override
	public void deleteGroups(JsonArray groups)
	{
		// Nothing
	}

	@Override
	public void deleteUsers(JsonArray users)
	{
		if(this.NDWTask != null)
		{
			String[] userIds = new String[users.size()];
			for(int i = users.size(); i-- > 0;)
				userIds[i] = users.getJsonObject(i).getString("id");

			this.NDWTask.clearUsersDevices(userIds);
		}
	}

	@Override
	public void transition(JsonObject structure) {
		final String query =
				"MATCH (s:Structure {id: {id}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {source: {source}}) " +
				"WHERE not(has(u.deleteDate)) " +
				"SET u.disappearanceDate = {disappearanceDate} ";
		final JsonObject params = new JsonObject()
				.put("id", structure.getString("id"))
				.put("source", "SSO")
				.put("disappearanceDate", 1342);
		Neo4j.getInstance().execute(query, params, Neo4jResult.validEmptyHandler(res -> {
			if (res.isLeft()) {
				log.error("Error when set disappearanceDate to SSO users : " + res.left().getValue());
			}
		}));
	}

}
