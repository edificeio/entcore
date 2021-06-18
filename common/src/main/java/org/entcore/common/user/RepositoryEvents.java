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

package org.entcore.common.user;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface RepositoryEvents {

	default void exportResources(boolean exportDocuments, String exportId, String userId, JsonArray groups, String exportPath,
						 String locale, String host, Handler<Boolean> handler) {
		exportResources(null,exportDocuments,exportId,userId,groups,exportPath,locale,host,handler);
	};

	default void exportResources(JsonArray resourcesIds, boolean exportDocuments, String exportId, String userId, JsonArray groups, String exportPath,
			String locale, String host, Handler<Boolean> handler) {}

	default void importResources(String importId, String userId, String userLogin, String userName, String importPath,
		String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {}

	void deleteGroups(JsonArray groups);

	void deleteUsers(JsonArray users);

	default void usersClassesUpdated(JsonArray updates) {}

	default void transition(JsonObject structure) {}

	default void mergeUsers(String keepedUserId, String deletedUserId) {
	};

	default void removeShareGroups(JsonArray oldGroups) {
		deleteGroups(oldGroups);
	}

	default void tenantsStructuresUpdated(JsonArray addedTenantsStructures, JsonArray deletedTenantsStructures) {}

}
