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
import org.entcore.directory.Directory;
import org.entcore.directory.services.ProfileService;
import org.entcore.directory.services.TenantService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultTenantService implements TenantService {

	private final EventBus eb;

	public DefaultTenantService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void create(JsonObject tenant, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-create-tenant")
				.put("data", tenant);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
	}

}
