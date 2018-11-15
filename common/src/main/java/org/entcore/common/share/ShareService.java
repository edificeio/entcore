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

package org.entcore.common.share;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ShareService {

	void inheritShareInfos(String userId, String resourceId, String acceptLanguage, String search,
			Handler<Either<String, JsonObject>> handler);

	void shareInfos(String userId, String resourceId, String acceptLanguage, String search,
			Handler<Either<String, JsonObject>> handler);

	void groupShare(String userId, String groupShareId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

	void userShare(String userId, String userShareId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

	void removeGroupShare(String groupId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

	void removeUserShare(String userId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

	void share(String userId, String resourceId, JsonObject share, Handler<Either<String, JsonObject>> handler);

	/**
	 * 
	 * @param resourceId id of the resource
	 * @param userId     id of current user
	 * @param actions	 optional actions list to filter only users having action
	 * @param h          handler that emit the set of users id concerned by the
	 *                   share array
	 */
	void findUserIdsForShare(String resourceId, String userId, Optional<Set<String>> actions,
			Handler<AsyncResult<Set<String>>> h);

	void findUserIdsForInheritShare(String resourceId, String userId, Optional<Set<String>> actions,
			Handler<AsyncResult<Set<String>>> h);
}
