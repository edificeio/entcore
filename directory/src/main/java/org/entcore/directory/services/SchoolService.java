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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface SchoolService {

	void create(JsonObject school, Handler<Either<String, JsonObject>> result);

	void get(String id, Handler<Either<String, JsonObject>> result);

	void getByClassId(String classId, Handler<Either<String, JsonObject>> result);

	void listByUserId(String userId, Handler<Either<String, JsonArray>> results);

	void listAdmin(UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void link(String structureId, String userId, Handler<Either<String, JsonObject>> result);

	void unlink(String structureId, String userId, Handler<Either<String, JsonObject>> result);

	void defineParent(String structureId, String parentStructureId, Handler<Either<String,JsonObject>> handler);

	void removeParent(String structureId, String parentStructureId, Handler<Either<String,JsonObject>> handler);

	void list(JsonArray fields, Handler<Either<String, JsonArray>> results);

	void update(String structureId, JsonObject body, Handler<Either<String,JsonObject>> eitherHandler);

	void getLevels(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void setLevelsOfEducation(String structureId, List<Integer> levelsOfEducations, Handler<Either<String, JsonObject>> handler);

	void setDistributions(String structureId, List<String> distributions, Handler<Either<String, JsonObject>> handler);

	void getMetrics(String structureId, Handler<Either<String, JsonObject>> results);

	void listSources(String structureId, Handler<Either<String, JsonArray>> result);
	
	void listAafFunctions(String structureId, Handler<Either<String, JsonArray>> result);
	
	void quickSearchUsers(String structureId, String input, Handler<Either<String, JsonArray>> handler);
	
	void userList(String structureId, Handler<Either<String, JsonArray>> handler);

	void blockUsers(String structureId, String profile, boolean block, Handler<JsonObject> handler);

	void searchCriteria(List<String> structures, boolean getClassesForMonoEtabOnly, Handler<Either<String, JsonObject>> handler);

	void getClasses(String structureId, Handler<Either<String, JsonObject>> handler);

}
