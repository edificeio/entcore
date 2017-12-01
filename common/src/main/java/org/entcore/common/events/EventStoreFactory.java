/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.events;

import org.entcore.common.events.impl.MongoDbEventStoreFactory;
import io.vertx.core.Vertx;

import java.util.ServiceLoader;

public abstract class EventStoreFactory {

	protected Vertx vertx;

	private static class EventStoreFactoryHolder {

		private static final EventStoreFactory factory;

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
		return EventStoreFactoryHolder.factory;
	}

	public abstract EventStore getEventStore(String module);

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
