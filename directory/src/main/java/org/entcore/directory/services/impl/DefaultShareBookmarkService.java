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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.getOrElse;

import static org.entcore.common.neo4j.Neo4jResult.*;
import org.entcore.common.user.UserUtils;
import static org.entcore.common.validation.StringValidation.cleanId;

import io.vertx.core.eventbus.EventBus;


public class DefaultShareBookmarkService implements ShareBookmarkService {

	private final EventBus eb;
	private static final Neo4j neo4j = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultShareBookmarkService.class);

	public DefaultShareBookmarkService(EventBus eb) {
		this.eb = eb;
	}

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

	private void get(String userId, String id, Handler<Either<String, JsonObject>> handler) {
		final String cleanId = cleanId(id);
		final String query =
				"MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) " +
				"UNWIND TAIL(sb." + cleanId + ") as vid " +
				"MATCH (v:Visible {id : vid}) " +
				"WHERE not(has(v.deleteDate)) " +
				"RETURN \"" + cleanId + "\" as id, HEAD(sb." + cleanId + ") as name, " +
				"COLLECT(DISTINCT {id : v.id, name : v.name, displayName :  v.displayName, groupType : labels(v), " +
				"groupProfile : v.filter, nbUsers : v.nbUsers, profile : HEAD(v.profiles), activationCode : has(v.activationCode) }) as members;";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void get(String userId, String id, boolean onlyVisibles, Handler<Either<String, JsonObject>> handler) {
		get(userId, id, r -> {
			if (r.isLeft() || !onlyVisibles) {
				handler.handle(r);
				return;
			}

			// If onlyVisibles is true, we need to filter the members of the share bookmark
			// to keep only those that are visible to the user.
			final JsonObject res = r.right().getValue();
			final JsonArray members = res.getJsonArray("members");
			if (members == null || members.isEmpty()) {
				handler.handle(r);
				return;
			}

			// Map members by their ID
			Map<String, JsonObject> membersMap = new HashMap(members.size());
			members.stream()
				.map(JsonObject.class::cast)
				.forEach(member -> membersMap.put(member.getString("id"), member));

			// Check bookmarked members' visibility
			final Set<String> membersMapIds = membersMap.keySet();
			UserUtils.filterFewOrGetAllVisibles(
				eb, 
				userId,
				new JsonArray( membersMapIds.stream().collect(Collectors.toList()) )
			)
			.onSuccess( visibleInfos -> {
				List<String> visibleIds = visibleInfos.stream()
					.map(JsonObject.class::cast)
					.map(jo -> jo.getString("id"))
					.collect(Collectors.toList());
				// Keep only visible users, in the *membersMap*
				membersMapIds.retainAll(visibleIds);
				// Update members array
				members.clear();
				membersMap.values().stream().forEach( member -> members.add(member) );
			})
			.onComplete( ar -> {
				// Filtered or not, handle the results anyway.
				handler.handle(r);
			});
		});
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
					final JsonObject r = new JsonObject();
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
