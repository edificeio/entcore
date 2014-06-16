/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.services.impl;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.RepositoryEvents;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class UserbookRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(UserbookRepositoryEvents.class);

	@Override
	public void deleteGroups(JsonArray groups) {

	}

	@Override
	public void deleteUsers(JsonArray users) {
		String query =
				"MATCH (u:UserBook)-[r]-(n) " +
				"WHERE (n:Hobby OR n:UserBook) AND NOT(u<--(:User)) " +
				"DELETE u, r, n";
		StatementsBuilder b = new StatementsBuilder().add(query);
		query = "MATCH (p:UserAppConf) " +
				"WHERE NOT(p<--(:User)) " +
				"DELETE p";
		b.add(query);
		Neo4j.getInstance().executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error deleting userbook data : " + event.body().encode());
				}
			}
		});
	}

}
