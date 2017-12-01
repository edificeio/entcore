/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;

import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface UserService {

	void createInStructure(String structureId, JsonObject user, Handler<Either<String, JsonObject>> result);

	void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result);

	void update(String id, JsonObject user, Handler<Either<String, JsonObject>> result);

	void sendUserCreatedEmail(HttpServerRequest request, String userId, Handler<Either<String, Boolean>> result);

	void get(String id, boolean getManualGroups, Handler<Either<String, JsonObject>> result);

	void list(String structureId, String classId, JsonArray expectedProfiles, Handler<Either<String, JsonArray>> results);

	void list(String profileGroupId, boolean itSelf, String userId, Handler<Either<String, JsonArray>> handler);

	void list(JsonArray groupIds, JsonArray userIds, boolean itSelf, String userId,
			Handler<Either<String, JsonArray>> handler);

	void listIsolated(String structureId, List<String> profile, Handler<Either<String, JsonArray>> results);

	void listAdmin(String structureId, String classId, String groupId, JsonArray expectedProfiles,
			UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void listAdmin(String structureId, String classId, String groupId, JsonArray expectedProfiles,
			String filterActivated, String nameFilter, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void delete(List<String> users, Handler<Either<String, JsonObject>> result);

	void restore(List<String> users, Handler<Either<String, JsonObject>> result);

	void addFunction(String id, String functionCode, JsonArray scope, String inherit,
			Handler<Either<String, JsonObject>> result);

	void removeFunction(String id, String functionCode, Handler<Either<String, JsonObject>> result);

	void listFunctions(String userId, Handler<Either<String, JsonArray>> handler);

	void addGroup(String id, String groupId, Handler<Either<String, JsonObject>> result);

	void removeGroup(String id, String groupId, Handler<Either<String, JsonObject>> result);

	void listAdml(String structureId, Handler<Either<String,JsonArray>> result);

	void getInfos(String userId, Handler<Either<String,JsonObject>> eitherHandler);

	void relativeStudent(String relativeId, String studentId, Handler<Either<String,JsonObject>> eitherHandler);

	void unlinkRelativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler);

	void ignoreDuplicate(String userId1, String userId2, Handler<Either<String, JsonObject>> result);

	void listDuplicates(JsonArray structures, boolean inherit, Handler<Either<String, JsonArray>> results);

	void mergeDuplicate(String userId1, String userId2, Handler<Either<String,JsonObject>> handler);

	void listByUAI(List<String> UAI,JsonArray expectedTypes,boolean isExportFull, JsonArray fields, Handler<Either<String, JsonArray>> results);

	void generateMergeKey(String userId, Handler<Either<String,JsonObject>> handler);

	void mergeByKey(String userId, JsonObject body, Handler<Either<String,JsonObject>> handler);

}
