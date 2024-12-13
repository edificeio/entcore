/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.AdmlResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class AdmlOfStructuresByExternalId extends AdmlResourcesProvider {

	@Override
	public void authorizeAdml(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, UserInfos.Function adminLocal, Handler<Boolean> handler) {
		List<String> structures = resourceRequest.params().getAll("structureExternalId");
		if (structures == null || structures.isEmpty()) {
			handler.handle(false);
			return;
		}
		String query =
				"MATCH (s:Structure) " +
				"WHERE s.externalId IN {structures} " +
				"WITH COLLECT(s.id) as sIds " +
				"RETURN LENGTH(FILTER(sId IN sIds WHERE sId IN {scope})) = LENGTH(sIds) as exists ";
		JsonObject params = new JsonObject()
				.put("structures", new JsonArray(structures))
				.put("scope", new JsonArray(adminLocal.getScope()));
		validateQuery(resourceRequest, handler, query, params);
	}

}
