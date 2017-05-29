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
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface GroupService {

	void listAdmin(String structureId, UserInfos userInfos, JsonArray expectedTypes,
			Handler<Either<String, JsonArray>> results);

	void createOrUpdateManual(JsonObject group, String structureId, String classId,
			Handler<Either<String, JsonObject>> result);

	void deleteManual(String groupId, Handler<Either<String, JsonObject>> result);

	void list(String structureId, String type, boolean subGroups, Handler<Either<String, JsonArray>> results);
	
	void addUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result);
	
	void removeUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result);
}
