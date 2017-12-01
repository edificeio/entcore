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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Server;
import io.vertx.core.http.HttpClientOptions;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import io.vertx.core.http.HttpClient;

public class HttpLocalhostEventStoreFactory extends EventStoreFactory {

	@Override
	public EventStore getEventStore(String module) {
		final HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(8001)
				.setKeepAlive(false);
		HttpClient httpClient = vertx.createHttpClient(options);

		HttpLocalhostEventStore eventStore = new HttpLocalhostEventStore(httpClient);
		eventStore.setEventBus(Server.getEventBus(vertx));
		eventStore.setModule(module);
		return eventStore;
	}

}
