/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class User {

	public static class DeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final EventBus eb;
		private final Vertx vertx;
		private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
		private static final int LIMIT = 1000;
		private int page;

		public DeleteTask(long delay, EventBus eb, Vertx vertx) {
			this.delay = delay;
			this.eb = eb;
			this.vertx = vertx;
		}

		@Override
		public void handle(Long event) {
			log.info("Execute task delete user.");
			page = 1;
			delete();
		}

		protected void delete() {
			try {
				TransactionHelper tx = TransactionManager.getInstance().begin();
				User.getDelete(delay, LIMIT, tx);
				tx.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						JsonArray results = m.body().getJsonArray("results");
						if ("ok".equals(m.body().getString("status")) && results != null) {
							final JsonArray r = results.getJsonArray(0);
							if (r != null && r.size() > 0) {
								final JsonArray deleteUsers = new fr.wseduc.webutils.collections.JsonArray();
								for (Object o : r) {
									if (!(o instanceof JsonObject)) continue;
									deleteUsers.add(((JsonObject) o).getString("id"));
								}
								try {
									TransactionHelper tx = TransactionManager.getInstance().begin();
									User.delete(deleteUsers, tx);
									tx.commit(new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> m2) {
											if ("ok".equals(m2.body().getString("status"))) {
												log.info("Delete users : " + r.encode());
												eb.publish(Feeder.USER_REPOSITORY, new JsonObject()
														.put("action", "delete-users")
														.put("old-users", r));
												eventStore.createAndStoreEvent(Feeder.FeederEvent.DELETE_USER.name(),
														(UserInfos) null, new JsonObject().put("old-users", r));
												if (r.size() == LIMIT) {
													vertx.setTimer(LIMIT * 100l, new Handler<Long>() {
														@Override
														public void handle(Long event) {
															log.info("Delete page " + ++page);
															delete();
														}
													});
												}
											} else {
												log.error("Error deleting user : " + m2.body().getString("message"));
											}
										}
									});
								} catch (Exception e) {
									log.error("Delete task error");
									log.error(e.getMessage(), e);
								}
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
		private static final int LIMIT = 5000;
		private int page;

		public PreDeleteTask(long delay) {
			this(delay, null);
		}

		public PreDeleteTask(Long delay, String profile) {
			this.delay = delay;
			this.profile = profile;
		}

		@Override
		public void handle(Long event) {
			launchPreDelete();
		}

		private void launchPreDelete() {
			log.info("Execute task pre-delete user.");
			JsonObject params = new JsonObject().put("date", System.currentTimeMillis() - delay).put("limit", LIMIT);
			String filter = "";
			if (profile != null) {
				params.put("profile", profile);
				filter = "AND head(u.profiles) = {profile} ";
			}
			String query =
					"MATCH (u:User) " +
					"WHERE HAS(u.disappearanceDate) AND NOT(HAS(u.deleteDate)) AND u.disappearanceDate < {date} " +
					filter +
					"RETURN u.id as id " +
					"LIMIT {limit} ";
			TransactionManager.getInstance().getNeo4j().execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					final JsonArray res = message.body().getJsonArray("result");
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
					.put("structureExternalId", structureExternalId)
					.put("source", source)
					.put("existingUsers", existingUsers)
					.put("profiles", profiles);
			TransactionManager.getInstance().getNeo4j().execute(query, params, handler);
		}

		public void preDeleteMissingUsersInStructure(String structureExternalId, String source, JsonArray existingUsers,
				JsonArray profiles, final Handler<Message<JsonObject>> handler) {
			findMissingUsersInStructure(structureExternalId, source, existingUsers, profiles, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray res = event.body().getJsonArray("result");
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
						} else {
							launchPreDelete();
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
		JsonObject params = new JsonObject().put("userId", userId);
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
		query =
				"MATCH (u:User { id : {userId}})-[:IN]->(pg: ProfileGroup)-[:DEPENDS]->(s: Structure), " +
				" (u)-[:HAS_RELATIONSHIPS]->(b: Backup) " +
				"WITH b, COLLECT(s.id) as sIds " +
				"SET b.structureIds = sIds";
		transaction.add(query, params);
	}

	public static void preDelete(String userId, TransactionHelper transaction) {
		JsonObject params = new JsonObject().put("userId", userId);
		String mQuery =
				"MATCH (u:User { id : {userId}})<-[r:MERGED]-(um:User), " +
				"u<-[:RELATED]-(us:User) " +
				"WHERE has(us.relative) AND LENGTH(FILTER(eId IN us.relative WHERE eId STARTS WITH um.externalId)) > 0 " +
				"REMOVE um.mergedWith, u.mergedLogins " +
				"CREATE UNIQUE um<-[:RELATED]-us " +
				"DELETE r " +
				"WITH um, us " +
				"MATCH us-[:IN]->(scg:ProfileGroup)-[:DEPENDS]->(c:Structure)<-[:DEPENDS]-(rcg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE p.name = head(um.profiles) " +
				"CREATE UNIQUE um-[:IN]->rcg ";
		transaction.add(mQuery, params);
		String query =
				"MATCH (u:User { id : {userId}}), (dg:DeleteGroup) " +
				"OPTIONAL MATCH u-[r:IN|COMMUNIQUE|COMMUNIQUE_DIRECT|RELATED|DUPLICATE]-() " +
				"SET u.deleteDate = timestamp(), u.IDPN = null " +
				"DELETE r " +
				"CREATE UNIQUE dg<-[:IN]-u";
		transaction.add(query, params);
	}

	public static void restorePreDeleted(String userId, TransactionHelper transaction){
		JsonObject params = new JsonObject().put("userId", userId);

		String query =
			"MATCH (u:User {id: {userId}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.IN_OUTGOING CREATE UNIQUE u-[:IN]->g";
		transaction.add(query, params);

		query =
			"MATCH (u:User {id: {userId}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.COMMUNIQUE_OUTGOING CREATE UNIQUE u-[:COMMUNIQUE]->g";
		transaction.add(query, params);

		query =
			"MATCH (u:User {id: {userId}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.COMMUNIQUE_INCOMING CREATE UNIQUE u<-[:COMMUNIQUE]-g";
		transaction.add(query, params);

		query =
			"MATCH (u:User {id: {userId}})-[r:IN]->(:DeleteGroup), u-[r2:HAS_RELATIONSHIPS]->(b:Backup) " +
			"REMOVE u.disappearanceDate, u.deleteDate " +
			"DELETE r, r2, b";
		transaction.add(query, params);
	}

	public static void transition(String userId, TransactionHelper transaction) {
		JsonObject params = new JsonObject().put("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"DELETE r ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:FunctionalGroup)-[:DEPENDS]->(c:Structure) " +
				"DELETE r ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}}) WHERE HAS(u.headTeacherManual) " +
				"REMOVE u.headTeacherManual ";
		transaction.add(query, params);
	}

	public static void getDelete(long delay, int limit, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("date", System.currentTimeMillis() - delay)
				.put("limit", limit);
		String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User)-[:HAS_RELATIONSHIPS]->(b:Backup) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} " +
				"OPTIONAL MATCH (fgroup:FunctionalGroup) " +
				"WHERE fgroup.externalId IN u.groups " +
				"OPTIONAL MATCH (mgroup:ManualGroup) " +
				"WHERE mgroup.id IN b.IN_OUTGOING " +
				"OPTIONAL MATCH (c:Class) " +
				"WHERE c.externalId IN u.classes " +
				"OPTIONAL MATCH (s:Structure) " +
				"WHERE s.externalId IN u.structures " +
				"RETURN DISTINCT u.id as id, u.firstName as firstName, u.lastName as lastName, " +
				"u.deleteDate as deleteDate, u.birthDate as birthDate," +
				"u.externalId as externalId, u.displayName as displayName, " +
				"HEAD(u.profiles) as type, " +
				"CASE WHEN c IS NULL THEN [] ELSE collect(distinct c.id) END as classIds, " +
				"CASE WHEN fgroup IS NULL THEN [] ELSE collect(distinct fgroup.id) END as functionalGroupsIds, " +
				"CASE WHEN mgroup IS NULL THEN [] ELSE collect(distinct mgroup.id) END as manualGroupsIds, " +
				"CASE WHEN s IS NULL THEN [] ELSE collect(distinct s.id) END as structureIds " +
				"LIMIT {limit} ";
		transactionHelper.add(query, params);
	}

	public static void delete(JsonArray deleteUsers, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject().put("deleteUsers", deleteUsers);
		final String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE u.id IN {deleteUsers} " +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE u,b,r,rb ";
		transactionHelper.add(query, params);
	}

	public static void addHeadTeacherManual(String userId,String structureExternalId, String classExternalId, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("classExternalId", classExternalId)
				.put("structureExternalId", structureExternalId);

		String query =
				"MERGE (u:User { id: {userId} })" +
				"FOREACH(x in CASE WHEN {classExternalId} in u.headTeacherManual THEN [] ELSE [1] END | " +
				"SET u.headTeacherManual = coalesce(u.headTeacherManual,[]) + {classExternalId} " +
				") " +
				"RETURN u.headTeacherManual";

		transactionHelper.add(query, params);

		String query2 =
				"MATCH (u:User { id : {userId}}), (c:Class {externalId : {classExternalId}})<-[:DEPENDS]-(g:Group:HTGroup)  " +
				"MERGE u-[:IN {source:'MANUAL'}]->g ";


		transactionHelper.add(query2, params);

		String query3 =
				"MATCH (u:User { id : {userId}}), (s:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(g:Group:HTGroup) " +
				"MERGE u-[:IN]->g " +
				"MERGE g-[:COMMUNIQUE]->u";
		;
		transactionHelper.add(query3, params);
	}


	public static void updateHeadTeacherManual(String userId,String structureExternalId , String classExternalId, TransactionHelper transactionHelper) {

		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("classExternalId", classExternalId)
				.put("structureExternalId", structureExternalId);

		String query =
				"MATCH (u:User) " +
				"WHERE HAS(u.headTeacherManual) AND u.id = {userId} " +
				"SET u.headTeacherManual = FILTER(x IN u.headTeacherManual WHERE x <> {classExternalId}) " +
				"RETURN u.headTeacherManual";

		transactionHelper.add(query, params);

		String query2 =
				"MATCH (u:User { id : {userId}})-[r:IN]->(g:Group:HTGroup)-[:DEPENDS]->(c:Class {externalId : {classExternalId}}) " +
				"DELETE r ";

		transactionHelper.add(query2, params);

		String query3 =
				"MATCH (u:User { id : {userId}})-[r:IN |COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(s:Structure {externalId : {structureExternalId}}) " +
				"WHERE length(u.headTeacherManual) = 0 AND (u.headTeacher IS NULL OR length(u.headTeacher) = 0) " +
				"DELETE r ";;

		transactionHelper.add(query3, params);
	}

	public static void addFunction(String userId, String functionCode, JsonArray s,
			TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}}), (f) " +
				"WHERE (f:Function OR f:Functions) AND f.externalId = {functionCode} " +
				"MERGE u-[rf:HAS_FUNCTION]->f ";

		JsonArray scope = null;
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("functionCode", functionCode);
		if (s != null) {
			query += "SET rf.scope = {scope} ";
			scope = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(new HashSet<String>(s.getList())));
			params.put("scope", scope);
		}
		transactionHelper.add(query, params);
		if(scope != null){
			String query2 =
				"MATCH (n), (f) " +
				"WHERE (n:Structure OR n:Class) AND n.id IN {scope} AND " +
				"(f:Function OR f:Functions) AND f.externalId = {functionCode} " +
				"WITH n, f " +
				"MERGE (fg:Group:FunctionGroup { externalId : n.id + '-' + {functionCode}}) " +
				"ON CREATE SET fg.id = id(fg) + '-' + timestamp(), fg.name = n.name + '-' + f.name, fg.displayNameSearchField = lower(n.name) + lower(f.name), fg.filter = f.name " +
				"CREATE UNIQUE n<-[:DEPENDS]-fg " +
				"WITH fg " +
				"MATCH (u:User { id : {userId}}) " +
				"CREATE UNIQUE fg<-[:IN {source:'MANUAL'}]-u " +
				"RETURN fg.id as groupId ";
			JsonObject p2 = new JsonObject()
				.put("scope", scope)
				.put("functionCode", functionCode)
				.put("userId", userId);
			transactionHelper.add(query2, p2);
			countUsersInGroups(null, "FunctionGroup", transactionHelper);
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
				"MATCH (fg:FunctionGroup {externalId : s + '-' + f.externalId})<-[r:IN|COMMUNIQUE]-u " +
				"DELETE r";
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("functionCode", functionCode);
		transactionHelper.add(query, params);
		countUsersInGroups(null, "FunctionGroup", transactionHelper);
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
				.put("userId", userId)
				.put("groupId", groupId);
		transactionHelper.add(query, params);
		countUsersInGroups(groupId, null, transactionHelper);
	}

	public static void removeGroup(String userId, String groupId, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(f:Group {id : {groupId}}) " +
				"WHERE 'ManualGroup' IN labels(f) OR 'FunctionalGroup' IN labels(f) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> f.externalId) " +
				"DELETE r ";
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("groupId", groupId);
		transactionHelper.add(query, params);
		countUsersInGroups(groupId, null, transactionHelper);
	}

	public static void count(String exportType, JsonArray profiles, TransactionHelper transactionHelper) {
		String query;
		JsonObject params = new JsonObject();
		if (profiles != null && profiles.size() > 0) {
			query = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"WHERE HEAD(u.profiles) IN {profiles} ";
			params.put("profiles", profiles);
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
			params.put("exportType", exportType);
		}
		query += "RETURN count(distinct u) as nb";
		transactionHelper.add(query, params);
	}

	public static void list(String exportType, JsonArray profiles, JsonArray attributes, Integer skip, Integer limit,
			TransactionHelper transactionHelper) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String filter = "";
		String unionManualGroups = "as functionalGroups ";
		if (isNotEmpty(exportType)) {
			filter = "AND HAS(s0.exports) AND {exportType} IN s0.exports ";
			params.put("exportType", exportType);
		}
		if (attributes != null && attributes.contains("manualAndFunctionalGroups")) {
			unionManualGroups = " + COLLECT(DISTINCT { structureExternalId : s.externalId, id: g.id, name: g.name }) as manualAndFunctionalGroups ";
		}
		if (profiles != null && profiles.size() > 0) {
			query.append("MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s0:Structure) " +
			"WHERE HEAD(u.profiles) IN {profiles} AND NOT(HAS(u.deleteDate)) ").append(filter).append(
			"OPTIONAL MATCH u-[:IN]->(g:ManualGroup)-[:DEPENDS]->(s:Structure) ").append(
			(attributes != null && (attributes.contains("functionalGroups") || attributes.contains("manualAndFunctionalGroups"))) ?
					"OPTIONAL MATCH u-[:IN]->(fg:FunctionalGroup)-[:DEPENDS]->(s1:Structure) " : "")
			.append("WITH u, COLLECT(DISTINCT s.externalId + '$' + g.id + '$' + g.name) as manualGroups ").append(
			(attributes != null && (attributes.contains("functionalGroups") || attributes.contains("manualAndFunctionalGroups"))) ?
					", COLLECT(DISTINCT { structureExternalId : s1.externalId, id: fg.id, externalId: fg.externalId, name: fg.name, idrgpmt : fg.idrgpmt, " +
					"idgpe : fg.idgpe, code_gep : fg.code_gep, code : fg.code, code_div : fg.code_div, usedInCourses : fg.usedInCourses }) " + unionManualGroups : "")
			;
			params.put("profiles", profiles);
		} else {
			query.append("MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s0:Structure) WHERE NOT(HAS(u.deleteDate)) ")
					.append(filter);
		}
		if (attributes != null && attributes.size() > 0) {
			query.append("RETURN DISTINCT");
			for (Object attribute : attributes) {
				if ("manualGroups".equals(attribute) || "manualAndFunctionalGroups".equals(attribute) || "functionalGroups".equals(attribute)) {
					query.append(" ").append(attribute);
				} else {
					query.append(" u.").append(attribute);
				}
				query.append(" as ").append(attribute).append(",");
			}
			query.deleteCharAt(query.length() - 1);
			query.append(" ");
		} else {
			query.append(" RETURN DISTINCT u ");
		}
		if (skip != null && limit != null) {
			query.append("ORDER BY externalId ASC " +
					"SKIP {skip} " +
					"LIMIT {limit} ");
			params.put("skip", skip);
			params.put("limit", limit);
		}
		transactionHelper.add(query.toString(), params);
	}

	public static void listByFunctions(String exportType, JsonArray functions, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("functions", functions);
		String filter;
		if(isNotEmpty(exportType)) {
			filter = "-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"WHERE f.externalId IN {functions} AND HAS(s.exports) AND {exportType} IN s.exports ";
			params.put("exportType", exportType);
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
				"MATCH (s:User {id : {studentId}})-[:IN]->(spg:ProfileGroup)-[:DEPENDS]->(st:Structure), " +
				"(spg)-[:HAS_PROFILE]->(:Profile { name : 'Student'}), " +
				"(st)<-[:DEPENDS]-(rspg:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"MERGE s-[:RELATED]->r " +
				"WITH s, r, st, rspg " +
				"MERGE r-[:IN]->rspg " +
				"WITH s, r, st " +
				"SET s.relative = CASE WHEN r.externalId IN s.relative THEN " +
				"s.relative ELSE coalesce(s.relative, []) + (r.externalId + '$10$1$1$0$0') END " +
				"RETURN COLLECT(st.id) as structures "; 
		String query2 =
				"MATCH (r:User {id : {relativeId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"WITH DISTINCT r " +
				"MATCH (s:User {id : {studentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(st:Structure), " +
				"(s)-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Student'}), " +
				"(c)<-[:DEPENDS]-(rcpg:ProfileGroup)-[:DEPENDS]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile { name : 'Relative'}) " +
				"MERGE r-[:IN]->rcpg";
		JsonObject params = new JsonObject()
				.put("relativeId", relativeId)
				.put("studentId", studentId);
		tx.add(query, params);
		tx.add(query2, params);
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
			.put("relativeId", relativeId)
			.put("studentId", studentId);
		tx.add(query, params);
		tx.add(query2, params);
	}

	public static void countUsersInGroups(String groupId, String groupType, TransactionHelper tx) {
		JsonObject params = new JsonObject();
		String filter = "";
		String type = "Group";
		if (isNotEmpty(groupId)) {
			filter = " {id: {id}}";
			params.put("id", groupId);
			final String query0 = "MATCH (g:Group {id: {id}}) SET g.nbUsers = 0;";
			tx.add(query0, params);
		} else if (isNotEmpty(groupType)) {
			type = groupType;
			final String query0 = "MATCH (g:" + type + ") SET g.nbUsers = 0;";
			tx.add(query0, params);
		}
		final String query = "MATCH (g:" + type + filter + ")<-[:IN]-(u:User) WITH g, count(u) as cu SET g.nbUsers = cu;";
		tx.add(query, params);
	}

}
