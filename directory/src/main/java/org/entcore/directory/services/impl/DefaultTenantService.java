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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.directory.Directory;
import org.entcore.directory.services.TenantService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultTenantService implements TenantService {

	private final EventBus eb;
	private final Neo4j neo4j = Neo4j.getInstance();

	public DefaultTenantService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void create(JsonObject tenant, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-create-tenant")
				.put("data", tenant);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
	}

	@Override
	public void get(String id, Handler<Either<String, JsonObject>> handler) {
		final String filter = "WHERE t.id = {id} ";
		getOrList(filter, new JsonObject().put("id", id), validUniqueResultHandler(handler));
	}

	private void getOrList(String filter, JsonObject params, Handler<Message<JsonObject>> h) {
		final String query =
				"MATCH (t:Tenant)<-[:HAS_STRUCT]-(s:Structure) " + filter +
				"RETURN t.id as id, t.name as name, " +
				"COLLECT(DISTINCT {id: s.id, UAI: s.UAI, externalId: s.externalId, name: s.name}) as structures ";
		neo4j.execute(query, params, h);
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		getOrList("", new JsonObject(), validResultHandler(handler));
	}

}
