/*
 * Copyright © WebServices pour l'Éducation, 2018
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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.directory.services.ShareBookmarkService;

import java.util.UUID;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.neo4j.Neo4jResult.fullNodeMergeHandler;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.validation.StringValidation.cleanId;

public class DefaultShareBookmarkService implements ShareBookmarkService {

	private static final Neo4j neo4j = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultShareBookmarkService.class);

	@Override
	public void create(String userId, JsonObject bookmark, Handler<Either<String, JsonObject>> handler) {
		final String id = generateId();
		update(userId, id, bookmark, event -> {
			if (event.isRight()) {
				handler.handle(new Either.Right<>(new JsonObject().put("id", id)));
			} else {
				handler.handle(event);
			}
		});
	}

	@Override
	public void update(String userId, String id, JsonObject bookmark, Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (u:User {id:{userId}}) " +
				"MERGE u-[:HAS_SB]->(sb:ShareBookmark) " +
				"SET sb." + cleanId(id) + " = {bookmark} ";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		params.put("bookmark", new JsonArray().add(bookmark.getString("name"))
				.addAll(getOrElse(bookmark.getJsonArray("members"), new JsonArray())));
		neo4j.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void delete(String userId, String id, Handler<Either<String, JsonObject>> handler) {
		final String cleanId = cleanId(id);
		final String query =
				"MATCH (u:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) " +
				"REMOVE sb." + cleanId;
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void get(String userId, String id, Handler<Either<String, JsonObject>> handler) {
		final String cleanId = cleanId(id);
		final String query =
				"MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) " +
				"UNWIND TAIL(sb." + cleanId + ") as vid " +
				"MATCH (v:Visible {id : vid}) " +
				"RETURN \"" + cleanId + "\" as id, HEAD(sb." + cleanId + ") as name, " +
				"COLLECT(DISTINCT {id : v.id, name : v.name, displayName :  v.displayName, groupType : labels(v), " +
				"groupProfile : v.filter, nbUsers : v.nbUsers, profile : HEAD(v.profiles) }) as members;";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void list(String userId, Handler<Either<String, JsonArray>> handler) {
		final String query = "MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) return sb";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, fullNodeMergeHandler("sb", node -> {
			if (node.isRight()) {
				final JsonObject j = node.right().getValue();
				final JsonArray result = new JsonArray();
				for (String id: j.fieldNames()) {
					final JsonArray value = j.getJsonArray(id);
					if (value == null || value.size() < 2) {
						delete(userId, id, dres -> {
							if (dres.isLeft()) {
								log.error("Error deleting sharebookmark " + id + " : " + dres.left().getValue());
							}
						});
						continue;
					}
					final JsonObject r = new fr.wseduc.webutils.collections.JsonObject();
					r.put("id", id);
					r.put("name", value.remove(0));
					//r.put("membersIds", value);
					result.add(r);
				}
				handler.handle(new Either.Right<>(result));
			} else {
				handler.handle(new Either.Left<>(node.left().getValue()));
			}
		}));
	}

	private String generateId() {
		return "_" + UUID.randomUUID().toString().replaceAll("-","");
	}

}
