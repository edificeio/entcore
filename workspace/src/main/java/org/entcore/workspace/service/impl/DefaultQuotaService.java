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

package org.entcore.workspace.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.workspace.service.QuotaService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultQuotaService implements QuotaService {

	private final Neo4j neo4j = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultQuotaService.class);
	private final boolean neo4jPlugin;

	public DefaultQuotaService(boolean neo4jPlugin) {
		this.neo4jPlugin = neo4jPlugin;
	}

	@Override
	public void incrementStorage(String userId, Long size, int threshold,
			final Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject()
				.put("size", size)
				.put("threshold", threshold);
		if (!neo4jPlugin) {
			String query =
					"MATCH (u:UserBook { userid : {userId}}) " +
					"SET u.storage = u.storage + {size} " +
					"WITH u, u.alertSize as oldAlert " +
					"SET u.alertSize = ((100.0 * u.storage / u.quota) > {threshold}) " +
					"RETURN u.storage as storage, (u.alertSize = true AND oldAlert <> u.alertSize) as notify ";
			params.put("userId", userId);
			neo4j.execute(query, params, validUniqueResultHandler(handler));
		} else {
			neo4j.unmanagedExtension("put", "/entcore/quota/storage/" + userId, params.encode(),
					new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						handler.handle(new Either.Right<String, JsonObject>(
								new JsonObject(event.body().getString("result"))));
					} else {
						handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
					}
				}
			});
		}
	}

	@Override
	public void decrementStorage(String userId, Long size, int threshold, Handler<Either<String, JsonObject>> handler) {
		incrementStorage(userId, -1l * size, threshold, handler);
	}

	@Override
	public void quotaAndUsage(String userId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (u:UserBook { userid : {userId}}) " +
				"RETURN u.quota as quota, u.storage as storage ";
		JsonObject params = new JsonObject().put("userId", userId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void quotaAndUsageStructure(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (s:Structure {id : {structureId}})<-[:DEPENDS]-(:ProfileGroup)" +
				"<-[:IN]-(:User)-[:USERBOOK]->(u:UserBook) " +
				"RETURN sum(u.quota) as quota, sum(u.storage) as storage ";
		JsonObject params = new JsonObject().put("structureId", structureId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));

	}

	@Override
	public void quotaAndUsageGlobal(Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (u:UserBook) " +
				"RETURN sum(u.quota) as quota, sum(u.storage) as storage ";
		JsonObject params = new JsonObject();
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void update(JsonArray users, long quota,
			Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (u:UserBook)<-[:USERBOOK]-(:User)-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE u.userid IN {users} AND u.storage < {quota} AND {quota} < coalesce(p.maxQuota, 1073741824) " +
				"SET u.quota = {quota}, u.alertSize = false " +
				"RETURN u.userid as id ";
		JsonObject params = new JsonObject()
				.put("users", users)
				.put("quota", quota);
		neo4j.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void updateQuotaDefaultMax(String profile, Long defaultQuota, Long maxQuota,
			Handler<Either<String, JsonObject>> handler) {
		if (defaultQuota == null && maxQuota == null) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.params"));
			return;
		}
		JsonObject params = new JsonObject()
				.put("profile", profile);
		List<String> p = new ArrayList<>();
		if (maxQuota != null) {
			p.add("p.maxQuota = {maxQuota}");
			params.put("maxQuota", maxQuota);
		}
		if (defaultQuota != null) {
			p.add("p.defaultQuota = {defaultQuota}");
			params.put("defaultQuota", defaultQuota);
		}
		String query =
				"MATCH (p:Profile { name : {profile}}) " +
				"SET " + Joiner.on(", ").join(p) +
				" RETURN p.id as id ";
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void getDefaultMaxQuota(Handler<Either<String, JsonArray>> handler) {
		String query = "MATCH (p:Profile) RETURN p.name as name, coalesce(p.maxQuota, 1073741824) as maxQuota";
		neo4j.execute(query, new JsonObject(), validResultHandler(handler));
	}

	@Override
	public void init(final String userId) {
		String query =
				"MATCH (n:User {id : {userId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WITH n, sum(CASE WHEN has(p.defaultQuota) THEN p.defaultQuota ELSE 104857600 END) as quota " +
				"MERGE (m:UserBook { userid : {userId}}) " +
				"SET m.quota = quota, m.storage = 0, m.alertSize = false " +
				"WITH m, n "+
				"CREATE UNIQUE n-[:USERBOOK]->m";
		JsonObject params = new JsonObject().put("userId", userId);
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if (!"ok".equals(message.body().getString("status"))) {
					log.error("Error initializing quota for user " + userId + " : " +
							message.body().getString("message"));
				}
			}
		});
	}

}
