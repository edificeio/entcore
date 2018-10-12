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

package org.entcore.common.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface CrudService {

	void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void retrieve(String id, Handler<Either<String, JsonObject>> handler);

	void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);

	void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void delete(String id, Handler<Either<String, JsonObject>> handler);

	void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void list(Handler<Either<String, JsonArray>> handler);

	void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler);

	void isOwner(String id, UserInfos user, final Handler<Boolean> handler);

}
