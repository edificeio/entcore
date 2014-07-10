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

package org.entcore.communication.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.communication.services.CommunicationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultCommunicationService implements CommunicationService {

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void addLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (g1:Group {id : {startGroupId}}), (g2:Group {id : {endGroupId}}) " +
				"SET g1.communiqueWith = coalesce(g1.communiqueWith, []) + {endGroupId} " +
				"CREATE UNIQUE g1-[:COMMUNIQUE]->g2 " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject()
				.putString("startGroupId", startGroupId)
				.putString("endGroupId", endGroupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (g1:Group {id : {startGroupId}})-[r:COMMUNIQUE]->(g2:Group {id : {endGroupId}}) " +
				"SET g1.communiqueWith = FILTER(gId IN g1.communiqueWith WHERE gId <> {endGroupId}) " +
				"DELETE r " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject()
				.putString("startGroupId", startGroupId)
				.putString("endGroupId", endGroupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void addLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		String createRelationship;
		switch (direction) {
			case INCOMING:
				createRelationship = "g<-[:COMMUNIQUE]-u ";
				break;
			case OUTGOING:
				createRelationship = "g-[:COMMUNIQUE]->u ";
				break;
			default:
				createRelationship = "g<-[:COMMUNIQUE]-u, g-[:COMMUNIQUE]->u ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}})<-[:IN]-(u:User) " +
				"SET g.communiqueUsers = {direction} " +
				"CREATE UNIQUE " + createRelationship +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().putString("groupId", groupId).putString("direction", direction.name());
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		String relationship;
		String set;
		switch (direction) {
			case INCOMING:
				relationship = "g<-[r:COMMUNIQUE]-(u:User) ";
				set = "SET g.communiqueUsers = CASE WHEN g.communiqueUsers = 'INCOMING' THEN null ELSE 'OUTGOING' END ";
				break;
			case OUTGOING:
				relationship = "g-[r:COMMUNIQUE]->(u:User) ";
				set = "SET g.communiqueUsers = CASE WHEN g.communiqueUsers = 'OUTGOING' THEN null ELSE 'INCOMING' END ";
				break;
			default:
				relationship = "g-[r:COMMUNIQUE]-(u:User) ";
				set = "REMOVE g.communiqueUsers ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
				set +
				"WITH g " +
				"MATCH " + relationship +
				"DELETE r " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().putString("groupId", groupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void communiqueWith(String groupId, Handler<Either<String, JsonObject>> handler) {
//		String optional;
//		switch (filter) {
//			case GROUPS:
//				optional = "OPTIONAL MATCH g-[:COMMUNIQUE]->(vg:Group) ";
//				break;
//			case USERS:
//				optional = "OPTIONAL MATCH g-[:COMMUNIQUE]->(vu:User) ";
//				break;
//			default:
//				optional =
//						"OPTIONAL MATCH g-[:COMMUNIQUE]->(vg:Group) " +
//						"OPTIONAL MATCH g-[:COMMUNIQUE]->(vu:User) ";
//
//		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
				"OPTIONAL MATCH g-[:COMMUNIQUE]->(g1:Group) " +
				"RETURN g as group, COLLECT(g1) as communiqueWith ";
		JsonObject params = new JsonObject().putString("groupId", groupId);
		neo4j.execute(query, params, fullNodeMergeHandler("group", handler, "communiqueWith"));
	}

	@Override
	public void addLinkBetweenRelativeAndStudent(String groupId, Direction direction,
			Handler<Either<String, JsonObject>> handler) {
		String createRelationship;
		switch (direction) {
			case INCOMING:
				createRelationship = "u<-[:COMMUNIQUE_DIRECT]-s ";
				break;
			case OUTGOING:
				createRelationship = "u-[:COMMUNIQUE_DIRECT]->s ";
				break;
			default:
				createRelationship = "u<-[:COMMUNIQUE_DIRECT]-s, u-[:COMMUNIQUE_DIRECT]->s ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}})<-[:IN]-(u:User)<-[:RELATED]-(s:User) " +
				"SET g.relativeCommuniqueStudent = {direction} " +
				"CREATE UNIQUE " + createRelationship +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().putString("groupId", groupId).putString("direction", direction.name());
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLinkBetweenRelativeAndStudent(String groupId, Direction direction,
			Handler<Either<String, JsonObject>> handler) {
		String relationship;
		String set;
		switch (direction) {
			case INCOMING:
				relationship = "g<-[:IN]-(u:User)<-[r:COMMUNIQUE_DIRECT]-(s:User) ";
				set = "SET g.relativeCommuniqueStudent = " +
					"CASE WHEN g.relativeCommuniqueStudent = 'INCOMING' THEN null ELSE 'OUTGOING' END ";
				break;
			case OUTGOING:
				relationship = "g<-[:IN]-(u:User)-[r:COMMUNIQUE_DIRECT]->(s:User) ";
				set = "SET g.relativeCommuniqueStudent = " +
					"CASE WHEN g.relativeCommuniqueStudent = 'OUTGOING' THEN null ELSE 'INCOMING' END ";
				break;
			default:
				relationship = "g<-[:IN]-(u:User)-[r:COMMUNIQUE_DIRECT]-(s:User) ";
				set = "REMOVE g.relativeCommuniqueStudent ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
				"WHERE HAS(g.relativeCommuniqueStudent) " +
				set +
				"WITH g " +
				"MATCH " + relationship +
				"DELETE r " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().putString("groupId", groupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void applyDefaultRules(JsonArray structureIds, JsonArray defaultRules,
			Handler<Either<String, JsonObject>> handler) {
		// = container.config().getArray("defaultCommunicationRules");
		if (defaultRules == null || defaultRules.size() == 0 || structureIds == null || structureIds.size() == 0) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameter"));
			return;
		}
		StatementsBuilder b = new StatementsBuilder();
		for (Object s : structureIds) {
			if (!(s instanceof String)) continue;
			String schoolId = (String) s;
			final JsonObject params = new JsonObject().putString("schoolId", schoolId);
			for (Object o: defaultRules) {
				if (!(o instanceof String) || ((String) o).trim().isEmpty()) continue;
				if (((String) o).contains("RELATED")) {
					b.add("MATCH " + o + " CREATE UNIQUE start-[:COMMUNIQUE_DIRECT]->end", params);
				} else {
					if (((String) o).contains("startStructureGroup") && ((String) o).contains("endStructureGroup")) {
						b.add(
								"MATCH (s:Structure)<-[:DEPENDS]-(startStructureGroup:ProfileGroup)" +
								"-[:HAS_PROFILE]-(startProfile:Profile), " +
								"s<-[:DEPENDS]-(endStructureGroup:ProfileGroup)-[:HAS_PROFILE]-(endProfile:Profile) " +
								"WHERE s.id = {schoolId} " + o +
								"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
						);
					} else if (((String) o).contains("endProfile")) {
						b.add(
								"MATCH (s:Structure)<-[:BELONGS]-(c:Class), " +
								"c<-[:DEPENDS]-(startClassGroup:ProfileGroup)-[:DEPENDS]->" +
								"(startStructureGroup:ProfileGroup)-[:HAS_PROFILE]-(startProfile:Profile), " +
								"c<-[:DEPENDS]-(endClassGroup:ProfileGroup)-[:DEPENDS]->" +
								"(endStructureGroup:ProfileGroup)-[:HAS_PROFILE]-(endProfile:Profile) " +
								"WHERE s.id = {schoolId} " + o +
								"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
						);
					} else if (((String) o).contains("userClass")) {
						b.add(
								"MATCH (s:Structure)<-[:BELONGS]-(c:Class), " +
								"c<-[:DEPENDS]-(classGroup:ProfileGroup)-[:DEPENDS]->" +
								"(structureGroup:ProfileGroup)-[:HAS_PROFILE]-(profile:Profile), " +
								"classGroup<-[:IN]-(userClass:User) " +
								"WHERE s.id = {schoolId} " + o +
								"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
						);
					} else if (((String) o).contains("userStructure")) {
						b.add(
								"MATCH (s:Structure)<-[:DEPENDS]-(structureGroup:ProfileGroup)" +
								"-[:HAS_PROFILE]-(profile:Profile), " +
								"structureGroup<-[:IN]-(userStructure:User) " +
								"WHERE s.id = {schoolId} " + o +
								"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
						);
					}
				}
			}
			b.add(
					"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN*0..1]-(v), v<-[:COMMUNIQUE|COMMUNIQUE_DIRECT]-() " +
					"WHERE s.id = {schoolId} AND NOT(v:Visible) " +
					"WITH DISTINCT v " +
					"SET v:Visible ", params
			);
		}
		neo4j.executeTransaction(b.build(), null, true, validEmptyHandler(handler));
	}

	@Override
	public void removeRules(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query;
		JsonObject params =  new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			query = "MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)-[r:COMMUNIQUE]-() " +
					"WHERE s.id = {schoolId} " +
					"OPTIONAl MATCH s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]" +
					"-(u:User)-[r1:COMMUNIQUE_DIRECT]->() " +
					"DELETE r, r1";
			params.putString("schoolId", structureId);
		} else {
			query = "MATCH ()-[r:COMMUNIQUE]->() " +
					"OPTIONAL MATCH ()-[r1:COMMUNIQUE_DIRECT]->() " +
					"DELETE r, r1 ";
		}
		neo4j.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf,
			boolean myGroup, boolean profile, String customReturn, JsonObject additionnalParams,
			final Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String condition = itSelf ? "" : "AND m.id <> {userId} ";
		if (structureId != null && !structureId.trim().isEmpty()) {
			query.append("MATCH (n:User)-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->(s:Structure {id:{schoolId}})"); //TODO manage leaf
			params.putString("schoolId", structureId);
		} else {
			String l = (myGroup) ? "" : " AND length(p) >= 2";
			query.append(" MATCH p=(n:User)-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]->t-[:COMMUNIQUE*0..1]->ipg" +
					"-[:COMMUNIQUE*0..1]->g<-[:DEPENDS*0..1]-m ");
			condition += "AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
					"XOR (type(r) = 'COMMUNIQUE'"+ l +
					" AND (length(p) < 3 OR (ipg:ProfileGroup AND length(p) = 3)))) ";
		}
		query.append("WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ").append(condition);
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND (");
			StringBuilder types = new StringBuilder();
			for (Object o: expectedTypes) {
				if (!(o instanceof String)) continue;
				String t = (String) o;
				types.append(" OR m:").append(t);
			}
			query.append(types.substring(4)).append(") ");
		}
		String pcr = " ";
		String pr = "";
		if (profile) {
			query.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			pcr = ", profile ";
			pr = "profile.name as type, ";
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH DISTINCT m as visibles").append(pcr);
			query.append(customReturn);
		} else {
			query.append("RETURN distinct m.id as id, m.name as name, "
					+ "m.login as login, m.displayName as username, ").append(pr)
					.append("m.lastName as lastName, m.firstName as firstName "
							+ "ORDER BY name, username ");
		}
		params.putString("userId", userId);
		if (additionnalParams != null) {
			params.mergeIn(additionnalParams);
		}
		neo4j.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void usersCanSeeMe(String userId, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH p=(n:User)<-[:COMMUNIQUE*0..2]-t<-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]-(m:User) " +
				"WHERE n.id = {userId} AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
				"XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) AND m.id <> {userId} " +
				"OPTIONAL MATCH m-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct m.id as id, m.login as login, " +
				"m.displayName as username, profile.name as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.putString("userId", userId);
		neo4j.execute(query, params, validResultsHandler(handler));
	}

	@Override
	public void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams,
			Handler<Either<String, JsonArray>> handler) {
		String r;
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			r = "WITH gp as profileGroup, profile " + customReturn;
		} else {
			r = "RETURN distinct gp.id as id, gp.name as name, profile.name as type, " +
				"gp.groupDisplayName as groupDisplayName " +
				"ORDER BY type DESC, name ";
		}
		JsonObject params =
				(additionnalParams != null) ? additionnalParams : new JsonObject();
		params.putString("userId", userId);
		String query =
				"MATCH (n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(gp:ProfileGroup) " +
				"WHERE n.id = {userId} " +
				"OPTIONAL MATCH gp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				r;
		neo4j.execute(query, params, validResultHandler(handler));
	}

}
