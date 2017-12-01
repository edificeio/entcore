/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
