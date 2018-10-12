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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.directory.Directory;
import org.entcore.directory.services.ProfileService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultProfileService implements ProfileService {

	private final EventBus eb;
	private Neo4j neo4j = Neo4j.getInstance();

	public DefaultProfileService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void createFunction(String profile, JsonObject function, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-create-function")
				.put("profile", profile)
				.put("data", function);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
	}

	@Override
	public void deleteFunction(String functionCode, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-delete-function")
				.put("functionCode", functionCode);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(handler)));
	}

	@Override
	public void createFunctionGroup(JsonArray functionsCodes, String name, String externalId,
			Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-create-function-group")
				.put("functions", functionsCodes)
				.put("externalId", externalId)
				.put("name", name);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, result)));
	}

	@Override
	public void deleteFunctionGroup(String functionGroupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-delete-function-group")
				.put("groupId", functionGroupId);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void listFunctions(Handler<Either<String, JsonArray>> result) {
		String query =
				"MATCH (f:Function) RETURN f.name as name, f.externalId as externalId " +
				"UNION " +
				"MATCH (f:Functions) RETURN f.name as name, f.externalId as externalId ";
		neo4j.execute(query, (JsonObject) null, validResultHandler(result));
	}

	@Override
	public void listProfiles(Handler<Either<String, JsonArray>> result) {
		final String query =
				"MATCH (p:Profile) RETURN DISTINCT p.name as name, p.blocked as blocked";
		neo4j.execute(query, (JsonObject) null, validResultHandler(result));
	}

	@Override
	public void blockProfiles(JsonObject profiles, Handler<Either<String, JsonObject>> handler) {
		final String query = "MATCH (p:Profile {name : {name}}) set p.blocked = {blocked}";
		final StatementsBuilder sb =  new StatementsBuilder();
		for (String profile : profiles.fieldNames()) {
			sb.add(query, new JsonObject().put("name", profile)
					.put("blocked", (profiles.getBoolean(profile, false) ? true : null)));
		}
		neo4j.executeTransaction(sb.build(), null, true, validEmptyHandler(handler));
	}

}
