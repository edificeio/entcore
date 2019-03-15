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

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface GroupService {

	void listAdmin(String structureId, UserInfos userInfos, JsonArray expectedTypes,
			Handler<Either<String, JsonArray>> results);

	void createOrUpdateManual(JsonObject group, String structureId, String classId,
			Handler<Either<String, JsonObject>> result);

	void deleteManual(String groupId, Handler<Either<String, JsonObject>> result);

	void list(String structureId, String type, boolean subGroups, Handler<Either<String, JsonArray>> results);
	
	void addUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result);
	
	void removeUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result);

	void getInfos(String groupId, Handler<Either<String,JsonObject>> handler);

	void getGroupsReachableByGroup(String id, Handler<Either<String, JsonArray>> results);
}
