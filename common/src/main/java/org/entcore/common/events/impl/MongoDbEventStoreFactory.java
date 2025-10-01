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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Server;
import io.vertx.core.json.JsonObject;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;

public class MongoDbEventStoreFactory extends EventStoreFactory {

	@Override
	public EventStore getEventStore(String module) {
		final MongoDbEventStore eventStore =  new MongoDbEventStore();
		eventStore.setEventBus(Server.getEventBus(vertx));
		eventStore.setModule(module);
		eventStore.setVertx(vertx);

		vertx.sharedData().<String, String>getAsyncMap("server")
            .compose(serverMap -> serverMap.get("event-store"))
            .onSuccess(eventStoreConf -> {
                if (eventStoreConf != null) {
					final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
					if (eventStoreConfig.containsKey("postgresql")) {
						final PostgresqlEventStore pgEventStore =  new PostgresqlEventStore();
						pgEventStore.setEventBus(Server.getEventBus(vertx));
						pgEventStore.setModule(module);
						pgEventStore.setVertx(vertx);
						pgEventStore.init();
						eventStore.setPostgresqlEventStore(pgEventStore);
					}
				}
            })
            .onFailure(ex -> logger.error("Error when set pg event-store", ex));

		return eventStore;
	}

}
