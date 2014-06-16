/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import org.entcore.common.http.filter.HttpActionFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.RepositoryHandler;

public abstract class BaseServer extends Server {

	private ResourcesProvider resourceProvider = null;
	private RepositoryHandler repositoryHandler = new RepositoryHandler();

	@Override
	public void start() {
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

}
