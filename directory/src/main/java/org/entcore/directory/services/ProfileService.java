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

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public interface ProfileService {

	void createFunction(String profile, JsonObject function, Handler<Either<String, JsonObject>> handler);

	void deleteFunction(String functionCode, Handler<Either<String, JsonObject>> handler);

	void createFunctionGroup(JsonArray functionsCodes, String name, String externalId,
			Handler<Either<String, JsonObject>> result);

	void deleteFunctionGroup(String functionGroupId, Handler<Either<String, JsonObject>> result);

	void listFunctions(Handler<Either<String,JsonArray>> result);

	void listProfiles(Handler<Either<String,JsonArray>> eitherHandler);

	void blockProfiles(JsonObject profiles, Handler<Either<String,JsonObject>> handler);

}
