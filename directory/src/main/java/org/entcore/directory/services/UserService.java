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

	void get(String id, boolean getManualGroups, JsonArray filterAttributes, Handler<Either<String, JsonObject>> result);

	void getGroups(String id, Handler<Either<String, JsonArray>> results);

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

	void addHeadTeacherManual(String id, String structureExternalId, String classExternalId, String structureName,
							  Handler<Either<String, JsonObject>> result);

	void updateHeadTeacherManual(String id, String structureExternalId, String classExternalId, Handler<Either<String, JsonObject>> result);

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

	void listChildren(String userId, Handler<Either<String,JsonArray>> eitherHandler);

	void getUserInfos(String userId, Handler<Either<String,JsonObject>> handler);

	void listByLevel(String levelContains, String levelNotContains, String profile, boolean stream,
					 Handler<Either<String, JsonArray>> handler);

}
