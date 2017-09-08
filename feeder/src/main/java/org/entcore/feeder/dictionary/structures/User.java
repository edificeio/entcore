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

package org.entcore.feeder.dictionary.structures;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.HashSet;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class User {

	public static class DeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final EventBus eb;
		private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());

		public DeleteTask(long delay, EventBus eb) {
			this.delay = delay;
			this.eb = eb;
		}

		@Override
		public void handle(Long event) {
			log.info("Execute task delete user.");
			try {
				TransactionHelper tx = TransactionManager.getInstance().begin();
				User.delete(delay, tx);
				tx.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						JsonArray results = m.body().getArray("results");
						if ("ok".equals(m.body().getString("status")) && results != null) {
							JsonArray r = results.get(0);
							if (r != null && r.size() > 0) {
								log.info("Delete users : " + r.encode());
								eb.publish(Feeder.USER_REPOSITORY, new JsonObject()
										.putString("action", "delete-users")
										.putArray("old-users", r));
								eventStore.createAndStoreEvent(Feeder.FeederEvent.DELETE_USER.name(),
										(UserInfos) null, new JsonObject().putArray("old-users", r));
							} else if (r == null) {
								log.error("User delete task return null array.");
							}
						} else {
							log.error(m.body().encode());
						}
					}
				});
			} catch (Exception e) {
				log.error("Delete task error");
				log.error(e.getMessage(), e);
			}
		}

	}

	public static class PreDeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final String profile;

		public PreDeleteTask(long delay) {
			this(delay, null);
		}

		public PreDeleteTask(Long delay, String profile) {
			this.delay = delay;
			this.profile = profile;
		}

		@Override
		public void handle(Long event) {
			log.info("Execute task pre-delete user.");
			JsonObject params = new JsonObject().putNumber("date", System.currentTimeMillis() - delay);
			String filter = "";
			if (profile != null) {
				params.putString("profile", profile);
				filter = "AND head(u.profiles) = {profile} ";
			}
			String query =
					"MATCH (u:User) " +
					"WHERE HAS(u.disappearanceDate) AND NOT(HAS(u.deleteDate)) AND u.disappearanceDate < {date} " +
					filter +
					"RETURN u.id as id ";
			TransactionManager.getInstance().getNeo4j().execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					final JsonArray res = message.body().getArray("result");
					if ("ok".equals(message.body().getString("status")) && res != null && res.size() > 0) {
						preDeleteUsers(res, null);
					}
				}
			});
		}

		public void findMissingUsersInStructure(String structureExternalId, String source, JsonArray existingUsers,
				JsonArray profiles, Handler<Message<JsonObject>> handler) {
			final String query =
					"MATCH (s:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
					"WHERE u.source = {source} AND HEAD(u.profiles) IN {profiles} AND NOT(u.externalId IN {existingUsers}) " +
					"RETURN u.id as id, u.externalId as externalId, u.lastName as lastName, " +
							"u.firstName as firstName, HEAD(u.profiles) as profile";
			final JsonObject params = new JsonObject()
					.putString("structureExternalId", structureExternalId)
					.putString("source", source)
					.putArray("existingUsers", existingUsers)
					.putArray("profiles", profiles);
			TransactionManager.getInstance().getNeo4j().execute(query, params, handler);
		}

		public void preDeleteMissingUsersInStructure(String structureExternalId, String source, JsonArray existingUsers,
				JsonArray profiles, final Handler<Message<JsonObject>> handler) {
			findMissingUsersInStructure(structureExternalId, source, existingUsers, profiles, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray res = event.body().getArray("result");
					if ("ok".equals(event.body().getString("status")) && res != null && res.size() > 0) {
						preDeleteUsers(res, handler);
					} else {
						handler.handle(event);
					}
				}
			});
		}

		private void preDeleteUsers(final JsonArray users, final Handler<Message<JsonObject>> handler) {
			try {
				TransactionHelper tx = TransactionManager.getInstance().begin();
				for (Object o : users) {
					if (!(o instanceof JsonObject)) continue;
					String userId = ((JsonObject) o).getString("id");
					backupRelationship(userId, tx);
					preDelete(userId, tx);
				}
				tx.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						if ("ok".equals(m.body().getString("status"))) {
							log.info("PreDelete users : " + users.encode());
						} else {
							log.error(m.body().encode());
						}
						if (handler != null) {
							handler.handle(m);
						}
					}
				});
			} catch (Exception e) {
				log.error("PreDelete task error");
				log.error(e.getMessage(), e);
			}
		}

	}

	public static void backupRelationship(String userId, TransactionHelper transaction) {
		JsonObject params = new JsonObject().putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN]->(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.IN_OUTGOING = coalesce(b.IN_OUTGOING, []) + ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:COMMUNIQUE]->(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.COMMUNIQUE_OUTGOING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})<-[r:COMMUNIQUE]-(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.COMMUNIQUE_INCOMING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:COMMUNIQUE_DIRECT]->(n) " +
				"WHERE HAS(n.id) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.COMMUNIQUE_DIRECT_OUTGOING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})<-[r:COMMUNIQUE_DIRECT]-(n) " +
				"WHERE HAS(n.id) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.COMMUNIQUE_DIRECT_INCOMING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:RELATED]->(n) " +
				"WHERE HAS(n.id) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.RELATED_OUTGOING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})<-[r:RELATED]-(n) " +
				"WHERE HAS(n.id) " +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.RELATED_INCOMING = ids ";
		transaction.add(query, params);
	}

	public static void preDelete(String userId, TransactionHelper transaction) {
		JsonObject params = new JsonObject().putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}}), (dg:DeleteGroup) " +
				"OPTIONAL MATCH u-[r:IN|COMMUNIQUE|COMMUNIQUE_DIRECT|RELATED|DUPLICATE]-() " +
				"SET u.deleteDate = timestamp(), u.IDPN = null " +
				"DELETE r " +
				"CREATE UNIQUE dg<-[:IN]-u";
		transaction.add(query, params);
	}

	public static void restorePreDeleted(String userId, TransactionHelper transaction){
		JsonObject params = new JsonObject().putString("userId", userId);
		String query =
			"MATCH (u:User {id: {userId}})-[r:IN]->(:DeleteGroup), u-[r2:HAS_RELATIONSHIPS]->(b:Backup) " +
			"REMOVE u.disappearanceDate, u.deleteDate WITH r, r2, b, u MATCH (g:Group) " +
			"WHERE g.id IN b.IN_OUTGOING " +
			"CREATE UNIQUE u-[:IN]->g WITH r, r2, b, u MATCH (g:Group) " +
			"WHERE g.id IN b.COMMUNIQUE_OUTGOING " +
			"CREATE UNIQUE u-[:COMMUNIQUE]->g WITH r, r2, b, u MATCH (g:Group) " +
			"WHERE g.id IN b.COMMUNIQUE_INCOMING " +
			"CREATE UNIQUE u<-[:COMMUNIQUE]-g DELETE r, r2, b ";
		transaction.add(query, params);
	}

	public static void transition(String userId, TransactionHelper transaction) {
		JsonObject params = new JsonObject().putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"DELETE r ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:FunctionalGroup)-[:DEPENDS]->(c:Structure) " +
				"DELETE r ";
		transaction.add(query, params);
	}

	public static void delete(long delay, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject().putNumber("date", System.currentTimeMillis() - delay);
		String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} " +
				"OPTIONAL MATCH (fgroup:FunctionalGroup) " +
				"WHERE fgroup.externalId IN u.groups " +
				"OPTIONAL MATCH (c:Class) " +
				"WHERE c.externalId IN u.classes " +
				"OPTIONAL MATCH (s:Structure) " +
				"WHERE s.externalId IN u.structures " +
				"RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, u.externalId as externalId, u.displayName as displayName, " +
				"HEAD(u.profiles) as type, " +
				"CASE WHEN c IS NULL THEN [] ELSE collect(distinct c.id) END as classIds, " +
				"CASE WHEN fgroup IS NULL THEN [] ELSE collect(distinct fgroup.id) END as functionalGroupsIds, " +
				"CASE WHEN s IS NULL THEN [] ELSE collect(distinct s.id) END as structureIds";
		transactionHelper.add(query, params);
		query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} " +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE u,b,r,rb ";
		transactionHelper.add(query, params);
	}

	public static void addFunction(String userId, String functionCode, JsonArray s,
			TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}}), (f) " +
				"WHERE (f:Function OR f:Functions) AND f.externalId = {functionCode} " +
				"MERGE u-[rf:HAS_FUNCTION]->f ";

		JsonArray scope = null;
		JsonObject params = new JsonObject()
				.putString("userId", userId)
				.putString("functionCode", functionCode);
		if (s != null) {
			query += "SET rf.scope = {scope} ";
			scope = new JsonArray(new HashSet<String>(s.toList()).toArray());
			params.putArray("scope", scope);
		}
		transactionHelper.add(query, params);
		if(scope != null){
			String query2 =
				"MATCH (n), (f) " +
				"WHERE (n:Structure OR n:Class) AND n.id IN {scope} AND " +
				"(f:Function OR f:Functions) AND f.externalId = {functionCode} " +
				"WITH n, f " +
				"MERGE (fg:Group:FunctionGroup { externalId : n.id + '-' + {functionCode}}) " +
				"ON CREATE SET fg.id = id(fg) + '-' + timestamp(), fg.name = n.name + '-' + f.name, fg.displayNameSearchField = lower(n.name) " +
				"CREATE UNIQUE n<-[:DEPENDS]-fg " +
				"WITH fg " +
				"MATCH (u:User { id : {userId}}) " +
				"CREATE UNIQUE fg<-[:IN {source:'MANUAL'}]-u";
			JsonObject p2 = new JsonObject()
				.putArray("scope", scope)
				.putString("functionCode", functionCode)
				.putString("userId", userId);
			transactionHelper.add(query2, p2);
		}
	}

	public static void removeFunction(String userId, String functionCode, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}})-[r:HAS_FUNCTION]->(f) " +
				"WHERE (f:Function OR f:Functions) AND f.externalId = {functionCode} " +
				"WITH r.scope as scope, r, u, f " +
				"DELETE r " +
				"WITH coalesce(scope, []) as ids, u, f " +
				"UNWIND ids as s " +
				"MATCH (fg:FunctionGroup {externalId : s + '-' + f.externalId})<-[r:IN]-u " +
				"DELETE r";
		JsonObject params = new JsonObject()
				.putString("userId", userId)
				.putString("functionCode", functionCode);
		transactionHelper.add(query, params);
	}

	public static void addGroup(String userId, String groupId, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}}), (f:Group {id : {groupId}}) " +
				"WHERE 'ManualGroup' IN labels(f) OR 'FunctionalGroup' IN labels(f) " +
				"CREATE UNIQUE u-[:IN {source:'MANUAL'}]->f " +
				"WITH f, u " +
				"WHERE 'FunctionalGroup' IN labels(f) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> f.externalId) + f.externalId ";
		JsonObject params = new JsonObject()
				.putString("userId", userId)
				.putString("groupId", groupId);
		transactionHelper.add(query, params);
	}

	public static void removeGroup(String userId, String groupId, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(f:Group {id : {groupId}}) " +
				"WHERE 'ManualGroup' IN labels(f) OR 'FunctionalGroup' IN labels(f) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> f.externalId) " +
				"DELETE r ";
		JsonObject params = new JsonObject()
				.putString("userId", userId)
				.putString("groupId", groupId);
		transactionHelper.add(query, params);
	}

	public static void count(String exportType, JsonArray profiles, TransactionHelper transactionHelper) {
		String query;
		JsonObject params = new JsonObject();
		if (profiles != null && profiles.size() > 0) {
			query = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"WHERE HEAD(u.profiles) IN {profiles} ";
			params.putArray("profiles", profiles);
			if (isNotEmpty(exportType)) {
				query += "AND ";
			}
		} else {
			query = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) ";
			if (isNotEmpty(exportType)) {
				query += "WHERE ";
			}
		}
		if (isNotEmpty(exportType)) {
			query += "HAS(s.exports) AND {exportType} IN s.exports ";
			params.putString("exportType", exportType);
		}
		query += "RETURN count(distinct u) as nb";
		transactionHelper.add(query, params);
	}

	public static void list(String exportType, JsonArray profiles, JsonArray attributes, Integer skip, Integer limit,
			TransactionHelper transactionHelper) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String filter = "";
		if (isNotEmpty(exportType)) {
			filter = "AND HAS(s0.exports) AND {exportType} IN s0.exports ";
			params.putString("exportType", exportType);
		}
		if (profiles != null && profiles.size() > 0) {
			query.append("MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s0:Structure) " +
					"WHERE HEAD(u.profiles) IN {profiles} AND NOT(HAS(u.deleteDate)) ").append(filter).append(
					"OPTIONAL MATCH u-[:IN]->(g:ManualGroup)-[:DEPENDS]->(s:Structure) " +
					"WITH u, COLLECT(DISTINCT s.externalId + '$' + g.id + '$' + g.name) as manualGroups ");
			params.putArray("profiles", profiles);
		} else {
			query.append("MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s0:Structure) WHERE NOT(HAS(u.deleteDate)) ")
					.append(filter);
		}
		if (attributes != null && attributes.size() > 0) {
			query.append("RETURN DISTINCT");
			for (Object attribute : attributes) {
				if ("manualGroups".equals(attribute)) {
					query.append(" manualGroups");
				} else {
					query.append(" u.").append(attribute);
				}
				query.append(" as ").append(attribute).append(",");
			}
			query.deleteCharAt(query.length() - 1);
			query.append(" ");
		} else {
			query.append("RETURN DISTINCT u ");
		}
		if (skip != null && limit != null) {
			query.append("ORDER BY externalId ASC " +
					"SKIP {skip} " +
					"LIMIT {limit} ");
			params.putNumber("skip", skip);
			params.putNumber("limit", limit);
		}
		transactionHelper.add(query.toString(), params);
	}

	public static void listByFunctions(String exportType, JsonArray functions, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.putArray("functions", functions);
		String filter;
		if(isNotEmpty(exportType)) {
			filter = "-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"WHERE f.externalId IN {functions} AND HAS(s.exports) AND {exportType} IN s.exports ";
			params.putString("exportType", exportType);
		} else {
			filter = " WHERE f.externalId IN {functions} ";
		}
		String query =
				"MATCH (f:Function)<-[:CONTAINS_FUNCTION*0..1]-()<-[rf:HAS_FUNCTION]-u" +
				filter +
				"WITH DISTINCT u.externalId as externalId, rf.scope as scope, f " +
				"MATCH (s:Structure) " +
				"WHERE s.id in scope " +
				"WITH externalId, COLLECT(distinct s.externalId) as structs, f " +
				"RETURN externalId, COLLECT(distinct [f.externalId, structs]) as functions ";
		transactionHelper.add(query, params);
	}


	public static void relativeStudent(String relativeId, String studentId, TransactionHelper tx) {
		String query =
				"MATCH (r:User {id : {relativeId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"WITH DISTINCT r " +
				"MATCH (s:User {id : {studentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(st:Structure), " +
				"s-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Student'}), " +
				"c<-[:DEPENDS]-(rcpg:ProfileGroup)-[:DEPENDS]->(rspg:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"MERGE s-[:RELATED]->r " +
				"WITH s, r, st, rspg, rcpg " +
				"MERGE r-[:IN]->rspg " +
				"WITH s, r, st, rcpg " +
				"MERGE r-[:IN]->rcpg " +
				"SET s.relative = CASE WHEN r.externalId IN s.relative THEN " +
				"s.relative ELSE coalesce(s.relative, []) + (r.externalId + '$1$1$1$1$0') END " +
				"RETURN COLLECT(st.id) as structures ";
		JsonObject params = new JsonObject()
				.putString("relativeId", relativeId)
				.putString("studentId", studentId);
		tx.add(query, params);
	}

	public static void unlinkRelativeStudent(String relativeId, String studentId, TransactionHelper tx){
		String query =
				"MATCH (r:User {id : {relativeId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"WITH r " +
				"MATCH (s:User {id : {studentId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Student'}), " +
				"s-[relations]-r " +
				"SET s.relative = FILTER(rId IN s.relative WHERE NOT(rId =~ (r.externalId + '.*'))) " +
				"DELETE relations";
		String query2 =
				"MATCH (s:User {id : {studentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class), " +
				"(r:User {id : {relativeId}})-[r2:IN]->(:ProfileGroup)-[:DEPENDS]->c " +
				"WHERE NOT(r<-[:RELATED]-(:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->c) " +
				"DELETE r2";
		JsonObject params = new JsonObject()
			.putString("relativeId", relativeId)
			.putString("studentId", studentId);
		tx.add(query, params);
		tx.add(query2, params);
	}

}
