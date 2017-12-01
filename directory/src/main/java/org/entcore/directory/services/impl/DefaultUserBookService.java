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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.directory.services.UserBookService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collections;

import static org.entcore.common.neo4j.Neo4jResult.fullNodeMergeHandler;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultUserBookService implements UserBookService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void update(String userId, JsonObject userBook, final Handler<Either<String, JsonObject>> result) {
		JsonObject u = Utils.validAndGet(userBook, UPDATE_USERBOOK_FIELDS, Collections.<String>emptyList());
		if (Utils.defaultValidationError(u, result, userId)) return;
		StatementsBuilder b = new StatementsBuilder();
		String query =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub:UserBook) " +
				"SET " + nodeSetPropertiesFromJson("ub", u);
		if (u.size() > 0) {
			b.add(query, u.put("id", userId));
		}
		String q2 =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub:UserBook)" +
				"-[:PUBLIC|PRIVE]->(h:`Hobby` { category : {category}}) " +
				"SET h.values = {values} ";
		JsonArray hobbies = userBook.getJsonArray("hobbies");
		if (hobbies != null) {
			for (Object o : hobbies) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject j = (JsonObject) o;
				b.add(q2, j.put("id", userId));
			}
		}
		neo.executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				if ("ok".equals(r.body().getString("status"))) {
					result.handle(new Either.Right<String, JsonObject>(new JsonObject()));
				} else {
					result.handle(new Either.Left<String, JsonObject>(
							r.body().getString("message", "update.error")));
				}
			}
		});
	}

	@Override
	public void get(String userId, Handler<Either<String, JsonObject>> result) {
		String query =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub: UserBook)" +
				"OPTIONAL MATCH ub-[:PUBLIC|PRIVE]->(h:Hobby) " +
				"RETURN ub, COLLECT(h) as hobbies ";
		neo.execute(query, new JsonObject().put("id", userId),
				fullNodeMergeHandler("ub", result, "hobbies"));
	}

}
