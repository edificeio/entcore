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

package org.entcore.registry.services;

import fr.wseduc.webutils.Either;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface AppRegistryService {

	void listApplications(String structureId, Handler<Either<String, JsonArray>> handler);
	
	void listRoles(String structureId, Handler<Either<String, JsonArray>> handler);

	void listRolesWithActions(String structureId, Handler<Either<String, JsonArray>> handler);

	void listActions(String application, Handler<Either<String, JsonArray>> handler);

	void listGroupsWithRoles(String structureId, boolean classGroups, Handler<Either<String, JsonArray>> handler);

	void listApplicationsWithActions(String structureId, String actionType, Handler<Either<String, JsonArray>> handler);
	
	void listApplicationRolesWithGroups(String structureId, String appId, Handler<Either<String, JsonArray>> handler);

	// if structureId is null => global role
	void createRole(String structureId, JsonObject role, JsonArray actions, Handler<Either<String, JsonObject>> handler);

	void updateRole(String roleId, JsonObject role, JsonArray actions, Handler<Either<String, JsonObject>> handler);

	void deleteRole(String roleId, Handler<Either<String, JsonObject>> handler);

	void linkRolesToGroup(String groupId, JsonArray rolesIds, Handler<Either<String, JsonObject>> handler);

	void addGroupLink(String groupId, String roleId, Handler<Either<String, JsonObject>> handler);

	void deleteGroupLink(String groupId, String roleId, Handler<Either<String, JsonObject>> handler);

	// if structureId is null => global application
	void createApplication(String structureId, JsonObject application, JsonArray actions,
			Handler<Either<String, JsonObject>> handler);

	void getApplication(String applicationId, Handler<Either<String, JsonObject>> handler);

	void updateApplication(String applicationId, JsonObject application, Handler<Either<String, JsonObject>> handler);

	void deleteApplication(String applicationId, Handler<Either<String, JsonObject>> handler);

	void applicationAllowedUsers(String application, JsonArray users, JsonArray groups,
			Handler<Either<String, JsonArray>> handler);

	void applicationAllowedProfileGroups(String application, Handler<Either<String, JsonArray>> handler);

	void setDefaultClassRoles(String classId, Handler<Either<String, JsonObject>> handler);

	void listCasConnectors(Handler<Either<String, JsonArray>> handler);

}
