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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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
				.putNumber("size", size)
				.putNumber("threshold", threshold);
		if (!neo4jPlugin) {
			String query =
					"MATCH (u:UserBook { userid : {userId}}) " +
					"SET u.storage = u.storage + {size} " +
					"WITH u, u.alertSize as oldAlert " +
					"SET u.alertSize = ((100.0 * u.storage / u.quota) > {threshold}) " +
					"RETURN u.storage as storage, (u.alertSize = true AND oldAlert <> u.alertSize) as notify ";
			params.putString("userId", userId);
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
				" MATCH (ub:UserBook { userid : {userId}})<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(s:Structure) " +
						" with min(s.quota-s.storage ) as minimum, collect(DISTINCT(ub)) as ub2  " +
						" match (ub:UserBook { userid : {userId}})<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(s:Structure) " +
						" where (s.quota-s.storage) = minimum RETURN DISTINCT(u.displayName), ub.quota as quota, ub.storage as storage, " +
						" s.quota as quotastructure, s.storage as storagestructure";

		JsonObject params = new JsonObject().putString("userId", userId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void quotaAndUsageStructure(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (s:Structure {id : {structureId}})<-[:DEPENDS]-(:ProfileGroup)" +
				"<-[:IN]-(:User)-[:USERBOOK]->(u:UserBook) " +
				"RETURN sum(u.quota) as quota, sum(u.storage) as storage ";
		JsonObject params = new JsonObject().putString("structureId", structureId);
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
				.putArray("users", users)
				.putNumber("quota", quota);
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
				.putString("profile", profile);
		List<String> p = new ArrayList<>();
		if (maxQuota != null) {
			p.add("p.maxQuota = {maxQuota}");
			params.putNumber("maxQuota", maxQuota);
		}
		if (defaultQuota != null) {
			p.add("p.defaultQuota = {defaultQuota}");
			params.putNumber("defaultQuota", defaultQuota);
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
		JsonObject params = new JsonObject().putString("userId", userId);
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

	@Override
	public void updateQuotaForProfile(JsonObject profile, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject();
		params.putString("id", profile.getString("id"));
		params.putNumber("quota",
				Float.parseFloat(profile.getField("quota").toString()) * Float.parseFloat(profile.getField("unit").toString())
		);
		params.putNumber("maxquota",
				Float.parseFloat(profile.getField("maxquota").toString()) * Float.parseFloat(profile.getField("unit").toString())
		);

		String query = "match (pg:ProfileGroup) where pg.id = {id} SET pg.quota = {quota}, pg.maxquota = {maxquota} return pg";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}

	@Override

	// updates all the userbooks from the given structure and profile, but doesn't update if actual storage > quota.
	// if the user is in 2 structures, we have to check the greater given quota
	public void updateQuotaUserBooks(JsonObject profile, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject();
		params.putString("id", profile.getString("id"));
		params.putNumber("quota",
				Float.parseFloat(profile.getField("quota").toString()) * Float.parseFloat(profile.getField("unit").toString())
		);

		String query = "match (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup) where pg.id = {id} " +
				" WITH ub MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup) with max(pg.quota) as maxiquota, " +
				" ub MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup) where ub.storage < maxiquota set ub.quota = maxiquota " +
				" return ub, u, collect(DISTINCT(pg)), max(pg.quota) ";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void updateQuotaForStructure(JsonObject structure, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject();
		params.putString("id", structure.getString("id"));
		params.putNumber("quota",
				Float.parseFloat(structure.getField("quota").toString()) * Float.parseFloat(structure.getField("unit").toString())
		);

		String query = "match (s:Structure) where s.id = {id} set s.quota = {quota} return s";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void updateQuotaForUser(JsonObject user, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject();
		params.putString("id", user.getString("id"));
		params.putNumber("quota",
				Float.parseFloat(user.getField("quota").toString()) * Float.parseFloat(user.getField("unit").toString())
		);

		String query = "match (u:User)-[USERBOOK]->(ub:UserBook) where u.id = {id} set ub.quota = {quota} return u";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void listUsersQuotaActivity( String structureId, int quotaFilterNbusers, String quotaFilterSortBy, String quotaFilterOrderBy, String quotaFilterProfile,
										Float quotaFilterPercentageLimit, Handler<Either<String, JsonArray>> results) {

		JsonObject params = new JsonObject();
		params.putString("structureId", structureId);
		params.putNumber("quotaFilterNbusers", quotaFilterNbusers);
		if( !"".equals(quotaFilterProfile) ) {
			params.putString("quotaFilterProfile",quotaFilterProfile);
		}

		if( quotaFilterPercentageLimit > 0 ){
			Float nb = quotaFilterPercentageLimit/100;
			params.putNumber("quotaFilterPercentageLimit", nb);
		}

		if( "sortpercentage".equals(quotaFilterSortBy) ){
			params.putString("quotaFilterSortBy", "percentage");
		} else {
			params.putString("quotaFilterSortBy", "storage" );
		}

		String query =
				"match (pro:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[USERBOOK]->(ub:UserBook), (s:Structure)<-[DEPENDS]-(pg:ProfileGroup) " +
						"where has (ub.storage) and  ub.quota > 0 and s.id = {structureId} ";

		// Profile filter
		if( !"".equals(quotaFilterProfile) ) {
			query += " and pro.name = {quotaFilterProfile} ";
		}

		// percentage limit filter
		if( quotaFilterPercentageLimit > 0 ) {
			query += " and(ub.storage / ub.quota) > {quotaFilterPercentageLimit} ";
		}
		// values returned
		query += "return collect(distinct(u.id)) as uid, ub.storage as storage, ub.quota as quota, (ub.storage / ub.quota) as percentage, " +
				"pro.name as profile, u.displayName as name, u.id as id, pg.maxquota as maxquota ";

		// order by
		if( "sortdcecreasing".equals(quotaFilterOrderBy) ) {
			query += " order by {quotaFilterSortBy} desc ";
		} else {
			query += " order by {quotaFilterSortBy} asc ";
		}

		// number of results
		query += " limit {quotaFilterNbusers}";

		neo4j.execute(query, params, validResultHandler(results));
	}

	@Override
	public void updateStructureStorageInitialize( Handler<Either<String, JsonObject>> result ) {
		JsonObject params = new JsonObject();
		String query = "match (s:Structure) set s.storage = 0 return s.name;";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void updateStructureStorage( Handler<Either<String, JsonObject>> result ) {
		JsonObject params = new JsonObject();
		String query = "match (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(s:Structure) with ub, collect(distinct ID(u)) as u2, s, (sum(ub.storage)/count(ub.storage)) as total match (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(s:Structure) with ub, collect(distinct ID(u)) as u2, total ,s SET s.storage = s.storage + total return s.name";
		neo4j.execute(query, params, validUniqueResultHandler(result));
	}
}
