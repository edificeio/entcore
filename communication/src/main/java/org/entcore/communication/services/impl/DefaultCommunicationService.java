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
import fr.wseduc.webutils.collections.Joiner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.communication.services.CommunicationService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultCommunicationService implements CommunicationService {

	private final Neo4j neo4j = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultCommunicationService.class);

	@Override
	public void addLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (g1:Group {id : {startGroupId}}), (g2:Group {id : {endGroupId}}) " +
				"SET g1.communiqueWith = coalesce(g1.communiqueWith, []) + {endGroupId} " +
				"CREATE UNIQUE g1-[:COMMUNIQUE { source: 'MANUAL'}]->g2 " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject()
				.put("startGroupId", startGroupId)
				.put("endGroupId", endGroupId);
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
				.put("startGroupId", startGroupId)
				.put("endGroupId", endGroupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void addLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		String createRelationship;
		switch (direction) {
			case INCOMING:
				createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u ";
				break;
			case OUTGOING:
				createRelationship = "g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
				break;
			default:
				createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u, g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
				"SET g.users = {direction} " +
				"WITH g " +
				"MATCH g<-[:IN]-(u:User) " +
				"CREATE UNIQUE " + createRelationship +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().put("groupId", groupId).put("direction", direction.name());
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		String relationship;
		String set;
		switch (direction) {
			case INCOMING:
				relationship = "g<-[r:COMMUNIQUE]-(u:User) ";
				set = "SET g.users = CASE WHEN g.users = 'INCOMING' THEN null ELSE 'OUTGOING' END ";
				break;
			case OUTGOING:
				relationship = "g-[r:COMMUNIQUE]->(u:User) ";
				set = "SET g.users = CASE WHEN g.users = 'OUTGOING' THEN null ELSE 'INCOMING' END ";
				break;
			default:
				relationship = "g-[r:COMMUNIQUE]-(u:User) ";
				set = "REMOVE g.users ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
				set +
				"WITH g " +
				"MATCH " + relationship +
				"DELETE r " +
				"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().put("groupId", groupId);
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
		JsonObject params = new JsonObject().put("groupId", groupId);
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
		JsonObject params = new JsonObject().put("groupId", groupId).put("direction", direction.name());
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
		JsonObject params = new JsonObject().put("groupId", groupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules,
			final Handler<Either<String, JsonObject>> handler) {
		final StatementsBuilder s1 = new StatementsBuilder();
		final StatementsBuilder s2 = new StatementsBuilder();
		final StatementsBuilder s3 = new StatementsBuilder();
		s3.add(
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup) " +
				"WHERE NOT(HAS(g.communiqueWith)) " +
				"SET g.communiqueWith = [] "
		).add(
				"MATCH (fg:FunctionGroup) " +
				"WHERE fg.name ENDS WITH 'AdminLocal' " +
				"SET fg.users = 'BOTH' "
		).add(
				"MATCH (ag:FunctionalGroup) " +
				"SET ag.users = 'BOTH' "
		);
		for (String attr : defaultRules.fieldNames()) {
			initDefaultRules(structureIds, attr, defaultRules.getJsonObject(attr), s1, s2);
		}
		neo4j.executeTransaction(s1.build(), null, false, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					Integer transactionId = event.body().getInteger("transactionId");
					neo4j.executeTransaction(s2.build(), transactionId, false, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								Integer transactionId = event.body().getInteger("transactionId");
								neo4j.executeTransaction(s3.build(), transactionId, true,
										new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										if ("ok".equals(message.body().getString("status"))) {
											handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
											log.info("Default communication rules initialized.");
										} else {
											handler.handle(new Either.Left<String, JsonObject>(
													message.body().getString("message")));
											log.error("Error init default com rules : " +
													message.body().getString("message"));
										}
									}
								});
							} else {
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
								log.error("Error init default com rules : " + event.body().getString("message"));
							}
						}
					});
				} else {
					handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
					log.error("Error init default com rules : " + event.body().getString("message"));
				}
			}
		});
	}

	private void initDefaultRules(JsonArray structureIds, String attr, JsonObject defaultRules,
			final StatementsBuilder existingGroups, final StatementsBuilder newGroups) {
		final String [] a = attr.split("\\-");
		final String c = "Class".equals(a[0]) ? "*2" : "";
		String relativeStudent = defaultRules.getString("Relative-Student"); // TODO check type in enum
		if (relativeStudent != null && "Relative".equals(a[1])) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup) " +
					"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) " +
					"AND cg.name =~ {profile} " +
					"SET cg.relativeCommuniqueStudent = {direction} ";
			JsonObject params = new JsonObject()
					.put("structures", structureIds)
					.put("direction", relativeStudent)
					.put("profile", "^.*?" + a[1] + "$");
			newGroups.add(query, params);
		}
		String users = defaultRules.getString("users"); // TODO check type in enum
		if (users != null) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup) " +
					"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) " +
					"AND cg.name =~ {profile} " +
					"SET cg.users = {direction} ";
			JsonObject params = new JsonObject()
					.put("structures", structureIds)
					.put("direction", users)
					.put("profile", "^.*?" + a[1] + "$");
			newGroups.add(query, params);
		}
		JsonArray communiqueWith = defaultRules.getJsonArray("communiqueWith", new fr.wseduc.webutils.collections.JsonArray());
		Set<String> classes = new HashSet<>();
		Set<String> structures = new HashSet<>();
		StringBuilder groupLabelSB = new StringBuilder("g:ProfileGroup");
		for (Object o : communiqueWith) {
			if (!(o instanceof String)) continue;
			String [] s = ((String) o).split("\\-");
			if ("Class".equals(s[0]) && "Structure".equals(a[0])) {
				log.warn("Invalid default configuration " + attr + "->" + o.toString());
			} else if ("Class".equals(s[0])) {
				if ("HeadTeacher".equals(s[1])) {
					groupLabelSB.append(" OR g:HTGroup");
				}
				classes.add(s[1]);
			} else {
				if ("Func".equals(s[1]) || "Discipline".equals(s[1])) {
					groupLabelSB.append(" OR g:FunctionGroup");
				} else if ("HeadTeacher".equals(s[1])) {
					groupLabelSB.append(" OR g:HTGroup");
				}
				structures.add(s[1]);
			}
		}
		final String groupLabel = groupLabelSB.toString();
		JsonObject params = new JsonObject()
				.put("structures", structureIds)
				.put("profile", "^.*?" + a[1] + "$");
		if (!classes.isEmpty()) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
					"WHERE s.id IN {structures} AND HAS(cg.communiqueWith) AND cg.name =~ {profile} " +
					"WITH cg, c " +
					"MATCH c<-[:DEPENDS]-(g) " +
					"WHERE (" + groupLabel + ") AND NOT(HAS(g.communiqueWith)) AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = FILTER(gId IN cg.communiqueWith WHERE gId <> g.id) + g.id ";
			String query2 =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
					"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) AND cg.name =~ {profile} " +
					"WITH cg, c, s " +
					"MATCH c<-[:DEPENDS]-(g) " +
					"WHERE  (" + groupLabel + ") AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + g.id ";
			if (!structures.isEmpty()) {
				query2 +=
						"WITH DISTINCT s, cg " +
						"MATCH s<-[:DEPENDS]-(sg:ProfileGroup) " +
						"WHERE sg.name =~ {structureProfile} " +
						"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + sg.id ";
			}
			JsonObject p = params.copy();
			p.put("otherProfile", "^.*?(" + Joiner.on("|").join(classes) + ")$");
			p.put("structureProfile", "^.*?(" + Joiner.on("|").join(structures) + ")$");
			existingGroups.add(query, p);
			newGroups.add(query2, p);
		}
		if (!structures.isEmpty() && "Structure".equals(a[0])) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup), s<-[:DEPENDS]-(g) " +
					"WHERE s.id IN {structures} AND HAS(cg.communiqueWith) AND cg.name =~ {profile} " +
					"AND  (" + groupLabel + ") AND NOT(HAS(g.communiqueWith)) AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = FILTER(gId IN cg.communiqueWith WHERE gId <> g.id) + g.id ";
			String query2 =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup), s<-[:DEPENDS]-(g) " +
					"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) AND cg.name =~ {profile} " +
					"AND (" + groupLabel + ") AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + g.id ";
			params.put("otherProfile", "^.*?(" + Joiner.on("|").join(structures) + ")$");
			existingGroups.add(query, params);
			newGroups.add(query2, params);
		}
	}

	@Override
	public void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler) {
		StatementsBuilder s = new StatementsBuilder();
		JsonObject params = new JsonObject().put("structures", structureIds);
		String query =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup) " +
				"WHERE s.id IN {structures} AND HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 " +
				"WITH DISTINCT g " +
				"MATCH (pg:Group) " +
				"WHERE pg.id IN g.communiqueWith " +
				"MERGE g-[:COMMUNIQUE]->pg ";
		s.add(query, params);
		String usersIncoming =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN]-(u:User) " +
				"WHERE s.id IN {structures} AND HAS(g.users) AND (g.users = 'INCOMING' OR g.users = 'BOTH') " +
				"MERGE g<-[:COMMUNIQUE]-u ";
		s.add(usersIncoming, params);
		String usersOutgoing =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN]-(u:User) " +
				"WHERE s.id IN {structures} AND HAS(g.users) AND (g.users = 'OUTGOING' OR g.users = 'BOTH') " +
				"MERGE g-[:COMMUNIQUE]->u ";
		s.add(usersOutgoing, params);
		String relativeIncoming =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
				"WHERE s.id IN {structures} AND HAS(g.relativeCommuniqueStudent) " +
				"AND (g.relativeCommuniqueStudent = 'INCOMING' OR g.relativeCommuniqueStudent = 'BOTH') " +
				"MERGE r<-[:COMMUNIQUE_DIRECT]-u ";
		s.add(relativeIncoming, params);
		String relativeOutgoing =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
				"WHERE s.id IN {structures} AND HAS(g.relativeCommuniqueStudent) " +
				"AND (g.relativeCommuniqueStudent = 'OUTGOING' OR g.relativeCommuniqueStudent = 'BOTH') " +
				"MERGE r-[:COMMUNIQUE_DIRECT]->u ";
		s.add(relativeOutgoing, params);
		String setVisible =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN*0..1]-(v), " +
				"v-[:COMMUNIQUE|COMMUNIQUE_DIRECT]-() " +
				"WHERE s.id IN {structures} AND NOT(v:Visible) " +
				"WITH DISTINCT v " +
				"SET v:Visible ";
		s.add(setVisible, params);
		String setVisible2 =
				"MATCH (s:Structure)<-[:DEPENDS]-(g:Group)<-[:COMMUNIQUE]-(), " +
				"g<-[:DEPENDS]-(v)" +
				"WHERE s.id IN {structures} AND NOT(v:Visible) " +
				"WITH DISTINCT v " +
				"SET v:Visible ";
		s.add(setVisible2, params);
		neo4j.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
	}

	@Override
	public void applyRules(String groupId, Handler<Either<String, JsonObject>> handler) {
		StatementsBuilder s = new StatementsBuilder();
		JsonObject params = new JsonObject().put("groupId", groupId);
		String query =
				"MATCH (g:Group {id : {groupId}}) " +
				"WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 " +
				"WITH g " +
				"MATCH (pg:Group) " +
				"WHERE pg.id IN g.communiqueWith " +
				"MERGE g-[:COMMUNIQUE]->pg ";
		s.add(query, params);
		String usersIncoming =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(u:User) " +
				"WHERE HAS(g.users) AND (g.users = 'INCOMING' OR g.users = 'BOTH') " +
				"MERGE g<-[:COMMUNIQUE]-u ";
		s.add(usersIncoming, params);
		String usersOutgoing =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(u:User) " +
				"WHERE HAS(g.users) AND (g.users = 'OUTGOING' OR g.users = 'BOTH') " +
				"MERGE g-[:COMMUNIQUE]->u ";
		s.add(usersOutgoing, params);
		String relativeIncoming =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
				"WHERE HAS(g.relativeCommuniqueStudent) " +
				"AND (g.relativeCommuniqueStudent = 'INCOMING' OR g.relativeCommuniqueStudent = 'BOTH') " +
				"MERGE r<-[:COMMUNIQUE_DIRECT]-u ";
		s.add(relativeIncoming, params);
		String relativeOutgoing =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
				"WHERE HAS(g.relativeCommuniqueStudent) " +
				"AND (g.relativeCommuniqueStudent = 'OUTGOING' OR g.relativeCommuniqueStudent = 'BOTH') " +
				"MERGE r-[:COMMUNIQUE_DIRECT]->u ";
		s.add(relativeOutgoing, params);
		String setVisible =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(v), " +
				"v-[:COMMUNIQUE|COMMUNIQUE_DIRECT]-() " +
				"WHERE NOT(v:Visible) " +
				"WITH DISTINCT v " +
				"SET v:Visible ";
		s.add(setVisible, params);
		neo4j.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
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
			params.put("schoolId", structureId);
		} else {
			query = "MATCH ()-[r:COMMUNIQUE]->() " +
					"OPTIONAL MATCH ()-[r1:COMMUNIQUE_DIRECT]->() " +
					"DELETE r, r1 ";
		}
		neo4j.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf,
			boolean myGroup, boolean profile, String preFilter, String customReturn, JsonObject additionnalParams,
			final Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String condition = itSelf ? "" : "AND m.id <> {userId} ";
		StringBuilder union = null;
		String conditionUnion = itSelf ? "" : "AND m.id <> {userId} ";
		if (structureId != null && !structureId.trim().isEmpty()) {
			query.append("MATCH (n:User)-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->(s:Structure {id:{schoolId}})"); //TODO manage leaf
			params.put("schoolId", structureId);
		} else {
			String l = (myGroup) ? " (length(p) >= 2 OR m.users <> 'INCOMING')" : " length(p) >= 2";
			query.append(" MATCH p=(n:User)-[:COMMUNIQUE*0..2]->ipg" +
					"-[:COMMUNIQUE*0..1]->g<-[:DEPENDS*0..1]-m ");
			condition += "AND (("+ l +
					" AND (length(p) < 3 OR (ipg:Group AND (m:User OR g<-[:DEPENDS]-m) AND length(p) = 3)))) ";
			union = new StringBuilder("MATCH p=(n:User)-[COMMUNIQUE_DIRECT]->m " +
					"WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ");
		}
		query.append("WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ");
		if (preFilter != null) {
			query.append(preFilter);
			if (union != null) {
				union.append(preFilter);
				union.append(conditionUnion);
			}
		}
		query.append(condition);
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND (");
			StringBuilder types = new StringBuilder();
			for (Object o: expectedTypes) {
				if (!(o instanceof String)) continue;
				String t = (String) o;
				types.append(" OR m:").append(t);
			}
			query.append(types.substring(4)).append(") ");
			if (union != null) {
				union.append("AND (").append(types.substring(4)).append(") ");
			}
		}
		String pcr = " ";
		String pr = "";
		if (profile) {
			query.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			pcr = ", profile ";
			pr = "profile.name as type, ";
			if (union != null) {
				union.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			}
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH DISTINCT m as visibles").append(pcr);
			query.append(customReturn);
			if (union != null) {
				union.append("WITH DISTINCT m as visibles").append(pcr);
				union.append(customReturn);
			}
		} else {
			query.append("RETURN distinct m.id as id, m.name as name, "
					+ "m.login as login, m.displayName as username, ").append(pr)
					.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
							+ "ORDER BY name, username ");
			if (union != null) {
				union.append("RETURN distinct m.id as id, m.name as name, "
						+ "m.login as login, m.displayName as username, ").append(pr)
						.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
								+ "ORDER BY name, username ");
			}
		}
		params.put("userId", userId);
		if (additionnalParams != null) {
			params.mergeIn(additionnalParams);
		}
		String q;
		if (union != null) {
			q = query.append(" union ").append(union.toString()).toString();
		} else {
			q = query.toString();
		}
		neo4j.execute(q, params, validResultHandler(handler));
	}

	@Override
	public void usersCanSeeMe(String userId, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH p=(n:User)<-[:COMMUNIQUE*0..2]-t<-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]-(m:User) " +
				"WHERE n.id = {userId} AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
				"XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) AND m.id <> {userId} " +
				"RETURN distinct m.id as id, m.login as login, " +
				"m.displayName as username, HEAD(m.profiles) as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams,
			String preFilter, Handler<Either<String, JsonArray>> handler) {
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
		params.put("userId", userId);
		String query =
				"MATCH p=(n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(gp:Group) " +
				"WHERE n.id = {userId} AND (length(p) > 1 OR gp.users <> 'INCOMING') " + (preFilter != null ? preFilter : "") +
				"OPTIONAL MATCH gp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				r;
		neo4j.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams,
			Handler<Either<String, JsonArray>> handler) {
		String r;
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			r = "WITH mg as manualGroup " + customReturn;
		} else {
			r = "RETURN distinct mg.id as id, mg.name as name, " +
				"mg.groupDisplayName as groupDisplayName " +
				"ORDER BY type DESC, name ";
		}
		JsonObject params =
				(additionnalParams != null) ? additionnalParams : new JsonObject();
		params.put("userId", userId);
		String query =
				"MATCH p=(n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(mg:ManualGroup) " +
				"WHERE n.id = {userId} AND (length(p) > 1 OR mg.users <> 'INCOMING') " +
				r;
		neo4j.execute(query, params, validResultHandler(handler));
	}

}
