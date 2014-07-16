/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.http;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import org.entcore.common.http.filter.HttpActionFilter;
import org.entcore.common.http.filter.ResourceProviderFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.RepositoryHandler;

public abstract class BaseServer extends Server {

	private ResourcesProvider resourceProvider = null;
	private RepositoryHandler repositoryHandler = new RepositoryHandler();

	@Override
	public void start() {
		if (resourceProvider == null) {
			setResourceProvider(new ResourceProviderFilter());
		}
		super.start();
		Neo4j.getInstance().init(getEventBus(vertx),
				config.getString("neo4j-address", "wse.neo4j.persistor"));
		MongoDb.getInstance().init(getEventBus(vertx),
				config.getString("mongo-address", "wse.mongodb.persistor"));
		if (config.getString("integration-mode","BUS").equals("HTTP")) {
			addFilter(new HttpActionFilter(securedUriBinding, config, vertx, resourceProvider));
		} else {
			addFilter(new ActionFilter(securedUriBinding, getEventBus(vertx), resourceProvider));
		}
		vertx.eventBus().registerHandler("user.repository", repositoryHandler);
	}

	protected BaseServer setResourceProvider(ResourcesProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
		return this;
	}

	protected BaseServer setRepositoryEvents(RepositoryEvents repositoryEvents) {
		repositoryHandler.setRepositoryEvents(repositoryEvents);
		return this;
	}

	protected BaseServer setDefaultResourceFilter(ResourcesProvider resourceProvider) {
		if (this.resourceProvider instanceof ResourceProviderFilter) {
			((ResourceProviderFilter) this.resourceProvider).setDefault(resourceProvider);
		}
		return this;
	}

}
