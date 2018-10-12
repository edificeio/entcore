/*
 * Copyright © "Open Digital Education", 2018
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
				"WHERE not(has(v.deleteDate)) " +
				"RETURN \"" + cleanId + "\" as id, HEAD(sb." + cleanId + ") as name, " +
				"COLLECT(DISTINCT {id : v.id, name : v.name, displayName :  v.displayName, groupType : labels(v), " +
				"groupProfile : v.filter, nbUsers : v.nbUsers, profile : HEAD(v.profiles), activationCode : v.activationCode }) as members;";
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
