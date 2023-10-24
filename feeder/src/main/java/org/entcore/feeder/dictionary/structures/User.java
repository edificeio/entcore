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

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.email.EmailSender;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserDataSync;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class User {

	private static final Logger log = LoggerFactory.getLogger(User.class);
	private static final String GET_DELETE_OPTIONS =
			"OPTIONAL MATCH (fgroup:FunctionalGroup) " +
			"WHERE fgroup.externalId IN u.groups " +
			"OPTIONAL MATCH (mgroup:ManualGroup) " +
			"WHERE mgroup.id IN b.IN_OUTGOING " +
			"OPTIONAL MATCH (c:Class) " +
			"WHERE c.externalId IN u.classes " +
			"OPTIONAL MATCH (s:Structure) " +
			"WHERE s.externalId IN u.structures " +
			"RETURN DISTINCT u.id as id, u.firstName as firstName, u.lastName as lastName, " +
			"u.deleteDate as deleteDate, u.birthDate as birthDate, u.login as login, u.loginAlias as loginAlias, " +
			"u.externalId as externalId, u.displayName as displayName, " +
			"HEAD(u.profiles) as type, " +
			"CASE WHEN c IS NULL THEN [] ELSE collect(distinct c.id) END as classIds, " +
			"CASE WHEN fgroup IS NULL THEN [] ELSE collect(distinct fgroup.id) END as functionalGroupsIds, " +
			"CASE WHEN mgroup IS NULL THEN [] ELSE collect(distinct mgroup.id) END as manualGroupsIds, " +
			"CASE WHEN s IS NULL THEN [] ELSE collect(distinct s.id) END as structureIds ";

	private static final String OLD_PLATFORM_USERS = "oldplatformusers";

	public static class DeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final EventBus eb;
		private final Vertx vertx;
		private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
		private static final int LIMIT = 1000;
		private int page;
		private final long deleteDelay;

		public DeleteTask(long delay, EventBus eb, Vertx vertx, long deleteDelay) {
			this.delay = delay;
			this.eb = eb;
			this.vertx = vertx;
			this.deleteDelay = deleteDelay;
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
				User.deleteNullId(delay, tx);
				User.getDelete(delay, LIMIT, tx);
				tx.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						JsonArray results = m.body().getJsonArray("results");
						if ("ok".equals(m.body().getString("status")) && results != null) {
							final JsonArray r = results.getJsonArray(1);
							if (r != null && r.size() > 0) {
								final JsonArray deleteUsers = new JsonArray();
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
												publishDeleteUsers(eb, eventStore, r);
												if (r.size() == LIMIT) {
													vertx.setTimer(LIMIT * deleteDelay, new Handler<Long>() {
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

		public static void publishDeleteUsers(EventBus eb, EventStore eventStore, JsonArray r) {
			log.info("Delete users : " + r.encode());
			eb.publish(Feeder.USER_REPOSITORY, new JsonObject()
					.put("action", "delete-users")
					.put("old-users", r));
			eventStore.createAndStoreEvent(Feeder.FeederEvent.DELETE_USER.name(),
					(UserInfos) null, new JsonObject().put("old-users", cleanDeleteEvent(r)));
			storeDeleteUserEvent(eventStore, r);
		}

		public static void storeDeleteUserEvent(EventStore eventStore, JsonArray r) {
			if (Vertx.currentContext().config().getBoolean("store-delete-user-event", false)) {
				for (Object o : r) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					final JsonObject event = new JsonObject()
						.put("event_type", j.getString("event-type", "DELETED"))
						.put("user_id", j.getString("id"))
						.put("profile", j.getString("type"));
					if (isNotEmpty(j.getString("login"))) {
						event.put("login", j.getString("login"));
					}
					if (isNotEmpty(j.getString("loginAlias"))) {
						event.put("login_alias", j.getString("loginAlias"));
					}
					eventStore.storeCustomEvent("auth", event);
				}
			}
		}

		private static JsonArray cleanDeleteEvent(JsonArray r) {
			final JsonArray result = r.copy();
			for (Object o : result) {
				if (o instanceof JsonObject) {
					((JsonObject) o).remove("birthDate");
				}
			}
			return result;
		}

	}

	public static class PreDeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final String profile;
		private static final int LIMIT = 5000;
		private final TimelineHelper timeline;

		public PreDeleteTask(long delay) {
			this(delay, null, null);
		}

		public PreDeleteTask(long delay, TimelineHelper timeline) {
			this(delay, null, timeline);
		}

		public PreDeleteTask(Long delay, String profile, TimelineHelper timeline) {
			this.delay = delay;
			this.profile = profile;
			this.timeline = timeline;
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
					"RETURN u.id as id, TOSTRING(ID(u)) AS nodeId " +
					"LIMIT {limit} ";
			TransactionManager.getInstance().getNeo4j().execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					final JsonArray res = message.body().getJsonArray("result");
					if ("ok".equals(message.body().getString("status")) && res != null && res.size() > 0) {
						preDeleteUsers(res, null);
					} else if (timeline != null && "ok".equals(message.body().getString("status"))) {
						notifyRemainingDays();
					}
				}
			});
		}

		private void notifyRemainingDays() {
			JsonObject params = new JsonObject().put("date", System.currentTimeMillis() - (24 * 3600 * 1000L));
			String filter = "";
			if (profile != null) {
				params.put("profile", profile);
				filter = "AND head(u.profiles) = {profile} ";
			}
			String query =
					"MATCH (u:User) " +
					"WHERE HAS(u.disappearanceDate) AND NOT(HAS(u.deleteDate)) AND u.disappearanceDate < {date} " +
					filter +
					"RETURN u.id as id, u.disappearanceDate as disappearanceDate ";
			TransactionManager.getInstance().getNeo4j().execute(query, params, message -> {
				final JsonArray res = message.body().getJsonArray("result");
				if ("ok".equals(message.body().getString("status")) && res != null && res.size() > 0) {
					final long now = System.currentTimeMillis();
					for (Object o: res) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						JsonObject p = new JsonObject().put("remainingDays",
								Math.round((j.getLong("disappearanceDate") + delay - now) / 3600000.0 / 24));
						final List<String> recipients = new ArrayList<>();
						recipients.add(j.getString("id"));
						timeline.notifyTimeline(null, "userbook.predelete-delay", null, recipients, null, p);
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
							"u.firstName as firstName, HEAD(u.profiles) as profile, TOSTRING(ID(u)) AS nodeId";
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
				JsonArray nullIdUserNodes = new JsonArray();
				TransactionHelper tx = TransactionManager.getInstance().begin();
				for (Object o : users) {
					if (!(o instanceof JsonObject)) continue;
					String userId = ((JsonObject) o).getString("id");
					String nodeId = ((JsonObject) o).getString("nodeId");
					if(userId == null && nodeId != null)
						nullIdUserNodes.add(nodeId);
					else
					{
						backupRelationship(userId, false, tx);
						preDelete(userId, tx);
					}
				}
				if(nullIdUserNodes.size() > 0)
				{
					String deleteNullQuery = "MATCH (u:User) " +
												"WHERE ID(u) IN {nodeIds} AND u.id IS NULL " +
												"DETACH DELETE u";
					tx.add(deleteNullQuery, new JsonObject().put("nodeIds", nullIdUserNodes));
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
	public static void backupRelationship(final String userId,
																				final TransactionHelper transaction) {
		backupRelationship(userId, true, transaction);
	}

	/**
	 * Adds to the current transaction the queries to create a backup node which will save the id of :
	 * - the groups the user was in
	 * - the groups the user could communicate with (incoming and outoing)
	 * - the direct communication links
	 * - the relationships to other users
	 * - the structures in which they were
	 * @param userId id of the user to backup
	 * @param backupAdmlGroups {@code true} if we want to backup ADML related links. If set to {@code false} (for densely
	 *                         connected users) it won't backup ADML relationships to prevent the fields IN_OUTGOING and
	 *                         COMMUNIQUE_* from reaching the maximum size of an indexed array of strings.
	 * @param transaction Transaction in which these actions should be executed
	 */
	public static void backupRelationship(final String userId,
																				final boolean backupAdmlGroups,
																				final TransactionHelper transaction) {
		JsonObject params = new JsonObject().put("userId", userId);
		final String filterGroupsToBeBackedUp;
		if(backupAdmlGroups) {
			filterGroupsToBeBackedUp = "";
		} else {
			filterGroupsToBeBackedUp = " AND NOT n.filter ='AdminLocal' ";
		}
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN]->(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " + filterGroupsToBeBackedUp +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.IN_OUTGOING = coalesce(b.IN_OUTGOING, []) + ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})-[r:COMMUNIQUE]->(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " + filterGroupsToBeBackedUp +
				"WITH u, COLLECT(n.id) as ids " +
				"MERGE u-[:HAS_RELATIONSHIPS]->(b:Backup {userId: {userId}}) " +
				"SET b.COMMUNIQUE_OUTGOING = ids ";
		transaction.add(query, params);
		query =
				"MATCH (u:User { id : {userId}})<-[r:COMMUNIQUE]-(n) " +
				"WHERE HAS(n.id) AND NOT(n:DeleteGroup) " + filterGroupsToBeBackedUp +
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

	public static void restoreRelationship(String mergedUserLogin, TransactionHelper transaction) {
		JsonObject params = new JsonObject().put("mergedUserLogin", mergedUserLogin);

		String query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.IN_OUTGOING MERGE (u)-[:IN]->(g)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.COMMUNIQUE_OUTGOING MERGE (u)-[:COMMUNIQUE]->(g)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (g:Group) WHERE g.id IN b.COMMUNIQUE_INCOMING MERGE (u)<-[:COMMUNIQUE]-(g)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (n:User) WHERE n.id IN b.COMMUNIQUE_DIRECT_OUTGOING MERGE (u)-[:COMMUNIQUE_DIRECT]->(n)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (n:User) WHERE n.id IN b.COMMUNIQUE_DIRECT_INCOMING MERGE (u)<-[:COMMUNIQUE_DIRECT]-(n)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (n:User) WHERE n.id IN b.RELATED_OUTGOING MERGE (u)-[:RELATED]->(n)";
		transaction.add(query, params);

		query =
			"MATCH (u:User {login: {mergedUserLogin}})-[:HAS_RELATIONSHIPS]->(b:Backup) " +
			"MATCH (n:User) WHERE n.id IN b.RELATED_INCOMING MERGE (u)<-[:RELATED]-(n)";
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
				"OPTIONAL MATCH u-[r:IN|COMMUNIQUE|COMMUNIQUE_DIRECT|RELATED|DUPLICATE|TEACHES_FOS|TEACHES|HAS_FUNCTION]-() " +
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
		transaction.add(query, params, new Handler<Either<String, JsonArray>>()
		{
			@Override
			public void handle(Either<String, JsonArray> res)
			{
				DuplicateUsers.checkDuplicatesIntegrity(userId, new Handler<Message<JsonObject>>()
				{
					@Override
					public void handle(Message<JsonObject> msg)
					{
						if("ok".equals(msg.body().getString("status")) == false)
							log.error("Failed to check duplicates for user " + userId);
					}
				});
			}
		});
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
		query =
				"MATCH (u:User { id : {userId}}) WHERE HAS(u.directionManual) " +
				"REMOVE u.directionManual ";
		transaction.add(query, params);
	}

	public static void getDelete(long delay, int limit, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("date", System.currentTimeMillis() - delay)
				.put("limit", limit);
		String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} WITH DISTINCT u LIMIT {limit} " +
				"OPTIONAL MATCH (u)-[:HAS_RELATIONSHIPS]->(b:Backup) " +
				 GET_DELETE_OPTIONS;
		transactionHelper.add(query, params);
	}

	public static void getDelete(JsonArray deleteUsers, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject().put("deleteUsers", deleteUsers);
		String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.id IN {deleteUsers} " +
				"OPTIONAL MATCH (u)-[:HAS_RELATIONSHIPS]->(b:Backup) " +
				GET_DELETE_OPTIONS;
		transactionHelper.add(query, params);
	}

	public static void deleteNullId(long delay, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("date", System.currentTimeMillis() - delay);
		String query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} AND NOT(HAS(u.id))" +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE u,b,r,rb ";
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
				"MERGE u-[r:IN]->g " +
				"SET r.source = 'MANUAL'";
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

		String query4 =
				"MATCH (u:User { id : {userId}})-[r:IN |COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(s:Structure {externalId : {structureExternalId}}) " +
				"WHERE length(u.headTeacherManual) = 0 AND length(u.headTeacher) > 0 " +
				"REMOVE r.source ";;

		transactionHelper.add(query4, params);
	}

	public static void addDirectionManual(String userId,String structureExternalId, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("structureExternalId", structureExternalId);

		String query =
				"MERGE (u:User { id: {userId} })" +
				"FOREACH(x in CASE WHEN {structureExternalId} in u.directionManual THEN [] ELSE [1] END | " +
				"SET u.directionManual = coalesce(u.directionManual,[]) + {structureExternalId} " +
				") " +
				"RETURN u.directionManual";

		transactionHelper.add(query, params);

		String query3 =
				"MATCH (u:User { id : {userId}}), (s:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(g:Group:DirectionGroup) " +
				"MERGE u-[r:IN]->g " +
				"SET r.source = 'MANUAL'";
		;
		transactionHelper.add(query3, params);
	}


	public static void removeDirectionManual(String userId,String structureExternalId, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("structureExternalId", structureExternalId);

		String query =
				"MATCH (u:User) " +
				"WHERE HAS(u.directionManual) AND u.id = {userId} " +
				"SET u.directionManual = FILTER(x IN u.directionManual WHERE x <> {structureExternalId}) " +
				"RETURN u.directionManual";

		transactionHelper.add(query, params);

		String query3 =
				"MATCH (u:User { id : {userId}})-[r:IN |COMMUNIQUE]->(g:Group:DirectionGroup)-[:DEPENDS]->(s:Structure {externalId : {structureExternalId}}) " +
				"DELETE r ";;

		transactionHelper.add(query3, params);
	}

	public static void addFunction(String userId, String functionCode, JsonArray s,
			TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User { id : {userId}}) " +
				"OPTIONAL MATCH (f:Function { externalId : {functionCode}}) " +
				"MERGE u-[rf:HAS_FUNCTION]->f ";

		JsonArray scope = null;
		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("functionCode", functionCode);
		if (s != null) {
			query += "SET rf.scope = {scope} ";
			scope = new JsonArray(new ArrayList<>(new HashSet<String>(s.getList())));
			params.put("scope", scope);
		}
		transactionHelper.add(query, params);
		if(scope != null){
			String query2 =
					"MATCH (u:User { id : {userId}}) " +
					"OPTIONAL MATCH (s:Structure) WHERE s.id IN {scope} " +
					"OPTIONAL MATCH (f:Function { externalId : {functionCode}}) " +
					"MERGE (fg:Group:FunctionGroup { externalId : s.id + '-' + {functionCode}}) " +
					"ON CREATE SET fg.id = id(fg) + '-' + timestamp(), fg.name = s.name + '-' + f.name, fg.displayNameSearchField = lower(s.name) + lower(f.name), fg.filter = f.name\n" +
					"CREATE UNIQUE s<-[:DEPENDS]-fg<-[:IN {source:'MANUAL'}]-u " +
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
				"MATCH (u:User { id : {userId}})-[hasFunction:HAS_FUNCTION]->(f:Function {externalId : {functionCode}}), (u)-[inOrComm:IN|COMMUNIQUE]-(fg:FunctionGroup) " +
				"WHERE fg.externalId ENDS WITH {functionCode} " +
				"DELETE hasFunction, inOrComm";
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
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(f:Group {id : {groupId}}) " +
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
				"MATCH (f:Function)<-[rf:HAS_FUNCTION]-u" +
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
				"MERGE s-[rrc:RELATED]->r " +
				"ON CREATE SET rrc.source = 'MANUAL' " +
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
		} else {
			final String query0 = "MATCH (g:" + type + ") WHERE (NOT(HAS(g.nbUsers)) OR g.nbUsers > 0) AND NOT(g<-[:IN]-(:User)) SET g.nbUsers = 0;";
			tx.add(query0, params);
		}
		final String query = "MATCH (g:" + type + filter + ")<-[:IN]-(u:User) WITH g, count(u) as cu SET g.nbUsers = cu;";
		tx.add(query, params);
	}

	public static void searchUserFromOldPlatform(Vertx vertx) {
		final JsonObject keys = new JsonObject().put("created", 0).put("modified", 0).put("_id", 0).put(UserDataSync.STATUS_FIELD, 0);
		MongoDb.getInstance().find(OLD_PLATFORM_USERS, new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.UNPROCESSED), null, keys, m -> {
			if ("ok".equals(m.body().getString("status"))) {
				final JsonArray res = m.body().getJsonArray("results");
				EmailSender emailSender = new EmailFactory(vertx).getSender();
				HttpServerRequest forged = new JsonHttpServerRequest(new JsonObject());
				if (res != null) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						tryActivateUser(vertx, emailSender, forged, (JsonObject) o);
					}
				}
				log.info("PostImport | SUCCEED to activeUserFromOldPlatform");
			} else {
				log.error("Error find user old platform : " + m.body().getString("message"));
			}
		});
	}

	private static void tryActivateUser(Vertx vertx, EmailSender emailSender, HttpServerRequest forged, JsonObject j)
	{
		JsonObject oldLogin = new JsonObject();
		j.copy().fieldNames().forEach(s -> {
			Object value = j.getValue(s);
			if (value == null || isEmpty(value.toString())) {
				j.remove(s);
			} else if (("login".equals(s) || "loginAlias".equals(s)) &&
					Validator.validLoginAlias(s, j.getString(s), "loginAlias", "fr", I18n.getInstance()) != null)
			{
				oldLogin.put(s, j.getString(s));
				j.remove(s);
			}
		});
		String query;
		if ("Relative".equals(j.getString("profile"))) {
			query =
					"MATCH (s:User {ine:{ine}})-[:RELATED]->(u:User) " +
					"WHERE u.firstNameSearchField = {firstName} AND u.lastNameSearchField = {lastName} AND ";
			j.put("firstName", Validator.sanitize((String) j.remove("firstName")));
			j.put("lastName", Validator.sanitize((String) j.remove("lastName")));
		} else if ("Student".equals(j.getString("profile"))) {
			query =
					"MATCH (u:User {ine:{ine}}) " +
					"WHERE ";
		}
		else
		{
			query = "MATCH (u:User) " +
					"WHERE u.firstNameSearchField = {firstName} AND u.lastNameSearchField = {lastName} AND u.birthDate = {birthDate} AND ";
			j.put("firstName", Validator.sanitize((String) j.remove("firstName")));
			j.put("lastName", Validator.sanitize((String) j.remove("lastName")));
		}
		query +=
				"HAS(u.activationCode) AND head(u.profiles) = {profile} " +
				"AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
				"WITH COLLECT(DISTINCT u) as users " +
				"WHERE LENGTH(users) = 1 " +
				"UNWIND users as u " +
				"SET u.activationCode = null, " + Neo4jUtils.nodeSetPropertiesFromJson(
						"u", j, "ine", "profile", "lastName", "firstName", UserDataSync.OLD_ID_FIELD,
						UserDataSync.EXPORT_ATTEMPTS_FIELD, UserDataSync.IMPORT_ATTEMPTS_FIELD) +
				"RETURN u.id as userId,  head(u.profiles) as profile, u.login AS login";
		Neo4j.getInstance().execute(query, j, r -> {
			if ("ok".equals(r.body().getString("status"))) {
				final JsonArray res = r.body().getJsonArray("result");
				if (res.size() == 1) {
					final JsonObject u = res.getJsonObject(0);
					final String userId = u.getString("userId");
					log.info("Activate user " + u.encode() + " : " + j.encode());
					Server.getEventBus(vertx).publish("activation.ack", u);

					DuplicateUsers.checkDuplicatesIntegrity(userId, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> msg)
						{
							if("ok".equals(msg.body().getString("status")) == false)
								log.error("Failed to check duplicates for activated oldplatform user " + userId);
						}
					});

					JsonObject update = new JsonObject().put("$set",
						new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.ACTIVATED).put(UserDataSync.NEW_ID_FIELD, u.getString("userId"))
					);

					JsonObject criteria = new JsonObject()
											.put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.UNPROCESSED)
											.put(UserDataSync.OLD_ID_FIELD, j.getString(UserDataSync.OLD_ID_FIELD));
					MongoDb.getInstance().update(OLD_PLATFORM_USERS, criteria, update, false, false, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> message)
						{
							if ("ok".equals(message.body().getString("status")))
							{
								JsonObject updateDoublons = new JsonObject().put("$set", new JsonObject().put(UserDataSync.STATUS_FIELD, UserDataSync.SyncState.DOUBLON));
								MongoDb.getInstance().update(OLD_PLATFORM_USERS, criteria, updateDoublons, false, true);
							}
							else
							{
								log.error("Failed to set activated status for " + j.getString(UserDataSync.OLD_ID_FIELD));
							}
						}
					});

					String login = u.getString("login");
					boolean updatedLogin = login.equals(oldLogin.getString("login")) == false && login.equals(oldLogin.getString("loginAlias")) == false;

					if(updatedLogin == true)
					{
						String email = j.getString("email");
						if(isNotEmpty(email))
						{
							log.info("Update user " + u.encode() + " login");

							JsonObject params = new JsonObject().put("name", j.getString("displayName", "")).put("login", login);
							emailSender.sendEmail(forged, email, null, null,
									"remote.user.update.login.mail", "email/update-user-login.html", params, true,
									new Handler<AsyncResult<Message<JsonObject>>>()
									{
										@Override
										public void handle(AsyncResult<Message<JsonObject>> ar) {
											if (ar.succeeded() == false) {
												log.error("Failed to send email to updated user login: " + u.encode());
											}
										}
									});
						}
						else
							log.error("No email address to contact updated user login: " + u.encode());
					}
				}
			} else {
				log.error("Error setting user attributes from old platform : " + r.body().getString("message"));
			}
		});

	}

	public static void findAndModifyUserFromOldPlatform(final Message<JsonObject> message) {
		final JsonObject matcher = message.body().getJsonObject("matcher");
		final JsonObject update = message.body().getJsonObject("update");
		final JsonObject sort = message.body().getJsonObject("sort");
		final JsonObject keys = message.body().getJsonObject("keys");
		// TO DO: ajouter une méthode dans le persisteur mongo qui prend la limit sans le skip ni le batchSize
		MongoDb.getInstance().findAndModify(OLD_PLATFORM_USERS, matcher, update, sort, keys,  m -> {
			if ("ok".equals(m.body().getString("status"))) {
				final JsonObject res = m.body().getJsonObject("result");
				message.reply(new JsonObject().put("result", res).put("status", "ok"));
			} else {
				final String errorMmessage = m.body().getString("message");
				log.error("Error find user old platform : " + errorMmessage);
				message.reply(new JsonObject().put("status", "error").put("message", errorMmessage));
			}
		});
	}

	public static void updateUsersFromOldPlatform(final Message<JsonObject> message) {
		final JsonObject criteria = message.body().getJsonObject("criteria");
		final JsonObject update = message.body().getJsonObject("update");
		MongoDb.getInstance().update(OLD_PLATFORM_USERS, criteria, update, false, true, m -> {
			if ("ok".equals(m.body().getString("status"))) {
				message.reply(new JsonObject().put("status", "ok"));
			} else {
				final String errorMmessage = m.body().getString("message");
				log.error("Error find user old platform : " + errorMmessage);
				message.reply(new JsonObject().put("status", "error").put("message", errorMmessage));
			}
		});
	}
}
