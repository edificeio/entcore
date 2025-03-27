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

import java.util.List;

import org.entcore.common.user.UserInfos;
import org.entcore.directory.pojo.TransversalSearchQuery;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface UserService {

	void createInStructure(String structureId, JsonObject user, final UserInfos caller, Handler<Either<String, JsonObject>> result);

	void createInClass(String classId, JsonObject user, final UserInfos caller, Handler<Either<String, JsonObject>> result);

	void update(String id, JsonObject user, final UserInfos caller, Handler<Either<String, JsonObject>> result);

	void updateLogin(String id, String newLogin, Handler<Either<String, JsonObject>> result);

	void sendUserCreatedEmail(HttpServerRequest request, String userId, Handler<Either<String, Boolean>> result);

	void getForExternalService(String id, Handler<Either<String, JsonObject>> result);

	void getForETude(String id, Handler<Either<String, JsonObject>> result);

	Future<JsonObject> getForSaooti(String id);

	void get(String id, boolean getManualGroups, boolean filterNullReturn, Handler<Either<String, JsonObject>> result);

	void get(String id, boolean getManualGroups, JsonArray filterAttributes, boolean filterNullReturn, Handler<Either<String, JsonObject>> result);

	void get(String id, boolean getManualGroups, boolean filterNullReturn, boolean withClasses, Handler<Either<String, JsonObject>> result);

	void get(String id, boolean getManualGroups, JsonArray filterAttributes, boolean filterNullReturn, boolean withClasses, Handler<Either<String, JsonObject>> result);

	void getClasses(String id, Handler<Either<String, JsonObject>> result);

	void getGroups(String id, Handler<Either<String, JsonArray>> results);

	void list(String structureId, String classId, JsonArray expectedProfiles, Handler<Either<String, JsonArray>> results);

	void list(String profileGroupId, boolean itSelf, String userId, Handler<Either<String, JsonArray>> handler);

	void list(JsonArray groupIds, JsonArray userIds, boolean itSelf, String userId,
			Handler<Either<String, JsonArray>> handler);

	/**
	 * List users who are not attached to a structure or a class.
	 * All parameters are optional and cumulative.
	 * 
	 * @param structureId If defined, return users attached to this structure but with no class.
	 * @param profile If defined, return users having one of the profiles.
	 * @param sortingField If defined, sort users by this field, defaults to (profile DESC, displayName ASC).
	 * @param sortingOrder Sort order ASC or DESC (defaults to "ASC").
	 * @param fromIndex If defined, return users by ommitting results before the index.
	 * @param limitResult If defined, returned resultset will contain users up to this number. 
	 * @param searchType If defined with searchTerm, filter results on this field.
	 * @param searchTerm If defined with searchType, filter results on field containing this value.
	 * @param results the resultset
	 */
	void listIsolated(
		final String structureId, 
		final List<String> profile,
		final String sortingField,
		final String sortingOrder,
		final Integer fromIndex,
		final Integer limitResult,
		final String searchType,
		final String searchTerm,
		Handler<Either<String, JsonArray>> results);

	void listAdmin(String structureId, boolean includeSubStructure, String classId, String groupId, JsonArray expectedProfiles,
			UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void listAdmin(String structureId, boolean includeSubStructure, String classId, String groupId, JsonArray expectedProfiles,
				   String filterActivated, final TransversalSearchQuery searchQuery, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void delete(List<String> users, Handler<Either<String, JsonObject>> result);

	void restore(List<String> users, Handler<Either<String, JsonObject>> result);

	void addFunction(String id, String functionCode, JsonArray scope, String inherit,
			Handler<Either<String, JsonObject>> result);

	void addHeadTeacherManual(String id, String structureExternalId, String classExternalId, Handler<Either<String, JsonObject>> result);

	void updateHeadTeacherManual(String id, String structureExternalId, String classExternalId, Handler<Either<String, JsonObject>> result);

	void addDirectionManual(String id, String structureExternalId, Handler<Either<String, JsonObject>> result);

	void removeDirectionManual(String id, String structureExternalId, Handler<Either<String, JsonObject>> result);

	void removeFunction(String id, String functionCode, Handler<Either<String, JsonObject>> result);

	void listFunctions(String userId, Handler<Either<String, JsonArray>> handler);

	void addGroup(String id, String groupId, Handler<Either<String, JsonObject>> result);

	void removeGroup(String id, String groupId, Handler<Either<String, JsonObject>> result);

	void listAdml(String structureId, Handler<Either<String,JsonArray>> result);

	void getInfos(String userId, Handler<Either<String,JsonObject>> eitherHandler);

	void getUserStructuresClasses(String userId, Handler<Either<String, JsonObject>> result);

	void relativeStudent(String relativeId, String studentId, Handler<Either<String,JsonObject>> eitherHandler);

	void unlinkRelativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler);

	void ignoreDuplicate(String userId1, String userId2, Handler<Either<String, JsonObject>> result);

	void listDuplicates(JsonArray structures, boolean inherit, Handler<Either<String, JsonArray>> results);

	/**
	 * Merge two duplicated user.
	 * @param userId1 Id of one of the duplicated user
	 * @param userId2 Id of the other duplicated user
	 * @param keepRelations {@code true} if the relationships of the disappearing user should be copied to the remaining user
	 * @param handler Downstream action after merger
	 */
	void mergeDuplicate(String userId1, String userId2, final boolean keepRelations, final Handler<Either<String,JsonObject>> handler);

	void listByUAI(List<String> UAI,JsonArray expectedTypes,boolean isExportFull, JsonArray fields, Handler<Either<String, JsonArray>> results);

	void generateMergeKey(String userId, Handler<Either<String,JsonObject>> handler);

	void mergeByKey(String userId, JsonObject body, Handler<Either<String,JsonObject>> handler);

	void unmergeByLogins(JsonObject body, Handler<Either<String,JsonObject>> handler);

	void listChildren(String userId, Handler<Either<String,JsonArray>> eitherHandler);

	void getUserInfos(String userId, Handler<Either<String,JsonObject>> handler);

	void listByLevel(String levelContains, String levelNotContains, String profile, String structureId, boolean stream,
					 Handler<Either<String, JsonArray>> handler);

	void getMainStructure(String userId, JsonArray structuresToExclude, Handler<Either<String, JsonObject>> result);

	/**
	 * Get some details about the structures that a list of users are attached to.
	 * @param userIds IDs of users
	 * @param fields fields of the structure nodes, to be returned in details. Default to ["id"]
	 * @return an array of JsonObjects, such as 
	 * 		{ userId: "ID of the user", structures: [{id: "ID of the structure", ... other queried fields...}] }
	 */
	void getUsersStructures(JsonArray userIds, JsonArray fields, Handler<Either<String, JsonArray>> handler);

	void getAttachmentSchool(String userId, JsonArray structuresToExclude, Handler<Either<String, JsonObject>> result);

	Future<JsonObject> getUsersDisplayNames(JsonArray userIds);
	
	public void listUsersByStructure(List<String> structures, Handler<Either<String, JsonArray>> results);
	void getUserStructuresGroup(String id, Handler<Either<String, JsonObject>> result);

	Future<JsonArray> getAttachmentInfos(JsonArray userIds, JsonArray structuresSources);

}
