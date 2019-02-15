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

package org.entcore.registry.services;

import fr.wseduc.webutils.Either;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

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

	void setLevelsOfEducation(String applicationId, List<Integer> levelsOfEducations, Handler<Either<String, JsonObject>> handler);

	void setRoleDistributions(String roleId, List<String> distributions, Handler<Either<String, JsonObject>> handler);

	void applicationAllowedUsers(String application, JsonArray users, JsonArray groups,
			Handler<Either<String, JsonArray>> handler);

	void applicationAllowedProfileGroups(String application, Handler<Either<String, JsonArray>> handler);

	void setDefaultClassRoles(String classId, Handler<Either<String, JsonObject>> handler);

	void listCasConnectors(Handler<Either<String, JsonArray>> handler);

	void massAuthorize(String structureId, List<String> profiles, List<String> rolesId, Handler<Either<String, JsonObject>> handler);

	void massUnauthorize(String structureId, List<String> profiles, List<String> rolesId, Handler<Either<String, JsonObject>> handler);
}
