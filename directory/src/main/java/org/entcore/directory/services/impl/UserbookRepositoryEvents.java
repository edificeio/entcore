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

package org.entcore.directory.services.impl;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.RepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UserbookRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(UserbookRepositoryEvents.class);

	@Override
	public void exportResources(String exportId, String userId, JsonArray groups, String exportPath,
			String locale, String host, Handler<Boolean> handler) {

	}

	@Override
	public void deleteGroups(JsonArray groups) {

	}

	@Override
	public void deleteUsers(JsonArray users) {
		String query =
				"MATCH (u:UserBook)-[r]-(n) " +
				"WHERE (n:Hobby OR n:UserBook) AND NOT(u<--(:User)) " +
				"DETACH DELETE u, r, n";
		StatementsBuilder b = new StatementsBuilder().add(query);
		query = "MATCH (p:UserAppConf) " +
				"WHERE NOT(p<--(:User)) " +
				"DETACH DELETE p";
		b.add(query);
		b.add("MATCH (sb:ShareBookmark) WHERE NOT(sb<--(:User)) DELETE sb");
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
