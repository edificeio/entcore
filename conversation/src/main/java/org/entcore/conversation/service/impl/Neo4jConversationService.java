/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.conversation.service.impl;

import java.util.HashSet;
import java.util.Set;

import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Neo4jConversationService {

	private Neo4j neo;

	public Neo4jConversationService(){
		this.neo = Neo4j.getInstance();
	}

	public void addDisplayNames(final JsonObject message, final JsonObject parentMessage, final Handler<JsonObject> handler) {
		if(!displayNamesCondition(message)){
			handler.handle(message);
			return;
		}

		String query =
			"MATCH (v:Visible) " +
			"WHERE v.id IN {ids} " +
			"RETURN COLLECT(distinct (v.id + '$' + coalesce(v.displayName, ' ') + '$' + " +
			"coalesce(v.name, ' ') + '$' + coalesce(v.groupDisplayName, ' '))) as displayNames ";

		Set<String> ids = new HashSet<>();
		ids.addAll(message.getArray("to", new JsonArray()).toList());
		ids.addAll(message.getArray("cc", new JsonArray()).toList());
		if (message.containsField("from")) {
			ids.add(message.getString("from"));
		}
		if(parentMessage != null){
			ids.addAll(parentMessage.getArray("to", new JsonArray()).toList());
			ids.addAll(parentMessage.getArray("cc", new JsonArray()).toList());
			if(parentMessage.containsField("from"))
				ids.add(parentMessage.getString("from"));
		}
		neo.execute(query, new JsonObject().putArray("ids", new JsonArray(ids.toArray())),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray r = m.body().getArray("result");
				if ("ok".equals(m.body().getString("status")) && r != null && r.size() == 1) {
					JsonObject j = r.get(0);
					JsonArray d = j.getArray("displayNames");
					if (d != null && d.size() > 0) {
						message.putArray("displayNames", d);
					}
				}
				handler.handle(message);
			}
		});
	}

	private boolean displayNamesCondition(JsonObject message) {
		return message != null && (
				(message.containsField("from") && !message.getString("from").trim().isEmpty()) ||
				(message.containsField("to") && message.getArray("to").size() > 0) ||
				(message.containsField("cc") && message.getArray("cc").size() > 0));
	}

	public void findInactives(final JsonObject message, long size, final Handler<JsonObject> handler){
		Set<Object> dest = new HashSet<>();
		dest.addAll(message.getArray("to", new JsonArray()).toList());
		dest.addAll(message.getArray("cc", new JsonArray()).toList());

		JsonObject params = new JsonObject().putArray("dest", new JsonArray(dest.toArray()));

		String returnClause = "";
		if(size > 0){
			returnClause =
				"RETURN " +
				"[t IN targets WHERE t.quotaLeft IS NULL OR t.quotaLeft < {attachmentsSize} | t.users.displayName] as undelivered, " +
				"[t IN targets WHERE t.quotaLeft IS NOT NULL AND t.quotaLeft >= {attachmentsSize} | t.users.id] as userTargets ";
			params.putNumber("attachmentsSize", size);
		} else {
			returnClause =
				"RETURN " +
				"[t IN targets WHERE t.users.activationCode IS NOT NULL | t.users.displayName] as inactives, " +
				"EXTRACT(t IN targets | t.users.id) as userTargets ";
		}

		String query =
			"MATCH (v:Visible)<-[:IN*0..1]-(u:User) " +
			"WHERE v.id IN {dest} " +
			"OPTIONAL MATCH (u)-[:USERBOOK]->(ub:UserBook) " +
			"WITH COLLECT(DISTINCT {users: u, quotaLeft: (ub.quota - ub.storage)}) as targets " +
			returnClause;

		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				JsonArray r = event.body().getArray("result");

				JsonObject formattedResult = new JsonObject()
					.putArray("inactives", new JsonArray())
					.putArray("actives", new JsonArray())
					.putArray("allUsers", new JsonArray());

				if ("ok".equals(event.body().getString("status")) && r != null && r.size() == 1) {
					JsonObject j = r.get(0);
					formattedResult.putArray("inactives", j.getArray("inactives", new JsonArray()));
					formattedResult.putArray("undelivered", j.getArray("undelivered", new JsonArray()));
					formattedResult.putArray("allUsers", j.getArray("userTargets", new JsonArray()));
				}

				handler.handle(formattedResult);
			}
		});
	}

}
