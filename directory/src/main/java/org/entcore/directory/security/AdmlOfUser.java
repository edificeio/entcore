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
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class AdmlOfUser extends AdmlResourcesProvider {

	@Override
	public void authorizeAdml(final HttpServerRequest resourceRequest, final Binding binding,
			final UserInfos user, final UserInfos.Function adminLocal, final Handler<Boolean> handler) {
		final String userId = resourceRequest.params().get("userId");
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		resourceRequest.pause();
		additionnalsChecks(resourceRequest, binding, user, adminLocal, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.FALSE.equals(event)) {
					handler.handle(false);
					return;
				}
				String query =
						"MATCH (u:User {id: {userId}})-[:IN]->(:Group)-[:DEPENDS]->(s:Structure) " +
						"WHERE s.id IN {structures} " +
						"RETURN count(*) > 0 as exists ";

				JsonObject params = new JsonObject()
						.put("structures", new JsonArray(adminLocal.getScope()))
						.put("userId", userId);
				validateQuery(resourceRequest, handler, query, params);
			}
		});

	}

	protected void additionnalsChecks(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, UserInfos.Function adminLocal, Handler<Boolean> handler) {
		handler.handle(true);
	}

}
