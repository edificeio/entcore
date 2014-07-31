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


public class User {

	public static class DeleteTask implements Handler<Long> {

		private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);
		private final long delay;
		private final EventBus eb;

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

		public PreDeleteTask(long delay) {
			this.delay = delay;
		}

		@Override
		public void handle(Long event) {
			log.info("Execute task pre-delete user.");
			JsonObject params = new JsonObject().putNumber("date", System.currentTimeMillis() - delay);
			String query =
					"MATCH (u:User) " +
					"WHERE HAS(u.disappearanceDate) AND NOT(HAS(u.deleteDate)) AND u.disappearanceDate < {date} " +
					"RETURN u.id as id ";
			TransactionManager.getInstance().getNeo4j().execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					final JsonArray res = message.body().getArray("result");
					if ("ok".equals(message.body().getString("status")) && res != null && res.size() > 0) {
						try {
							TransactionHelper tx = TransactionManager.getInstance().begin();
							for (Object o : res) {
								if (!(o instanceof JsonObject)) continue;
								String userId = ((JsonObject) o).getString("id");
								backupRelationship(userId, tx);
								preDelete(userId, tx);
							}
							tx.commit(new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> m) {
									if ("ok".equals(m.body().getString("status"))) {
										log.info("PreDelete users : " + res.encode());
									} else {
										log.error(m.body().encode());
									}
								}
							});
						} catch (Exception e) {
							log.error("PreDelete task error");
							log.error(e.getMessage(), e);
						}
					}
				}
			});
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
				"OPTIONAL MATCH u-[r:IN|COMMUNIQUE|COMMUNIQUE_DIRECT|RELATED]-() " +
				"SET u.deleteDate = timestamp() " +
				"CREATE UNIQUE dg<-[:IN]-u " +
				"DELETE r ";
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
				"RETURN u.id as id, u.externalId as externalId, u.displayName as displayName ";
		transactionHelper.add(query, params);
		query =
				"MATCH (:DeleteGroup)<-[:IN]-(u:User) " +
				"WHERE HAS(u.deleteDate) AND u.deleteDate < {date} " +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE u,b,r,rb ";
		transactionHelper.add(query, params);
	}

}
