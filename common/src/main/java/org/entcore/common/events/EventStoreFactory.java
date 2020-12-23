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

package org.entcore.common.events;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.events.impl.BusEventStoreFactory;
import org.entcore.common.events.impl.MongoDbEventStoreFactory;
import org.entcore.common.http.BaseServer;

import java.util.ServiceLoader;

public abstract class EventStoreFactory {
	static final Logger logger = LoggerFactory.getLogger(EventStoreFactory.class);

	protected Vertx vertx;

	private static class EventStoreFactoryHolder {

		private static final EventStoreFactory factory;
		private static final BusEventStoreFactory busFactory = new BusEventStoreFactory();

		static {
			ServiceLoader<EventStoreFactory> eventStoreFactories = ServiceLoader.load(EventStoreFactory.class);
			if (eventStoreFactories != null && eventStoreFactories.iterator().hasNext()) {
				factory = eventStoreFactories.iterator().next();
			} else {
				factory = new MongoDbEventStoreFactory();
			}
		}
	}

	public static EventStoreFactory getFactory() {
		//running multiple pgclient in worker context cause issues (netty emit data to each inbound handler causing NPE)
		final Context context = Vertx.currentContext();
		if(context != null && context.isWorkerContext()){
			final String name = BaseServer.getModuleName();
			logger.info((name!=null ? name : "") + " init a bus eventstore for worker...");
			return EventStoreFactoryHolder.busFactory;
		}
		return EventStoreFactoryHolder.factory;
	}

	public abstract EventStore getEventStore(String module);

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
