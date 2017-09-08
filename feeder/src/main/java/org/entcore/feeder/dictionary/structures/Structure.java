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

import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.*;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Structure {

	private static final Logger log = LoggerFactory.getLogger(Structure.class);
	protected final String id;
	protected final String externalId;
	protected final Importer importer = Importer.getInstance();
	protected JsonObject struct;
	protected final Set<String> classes = Collections.synchronizedSet(new HashSet<String>());
	protected final Set<String> functionalGroups = Collections.synchronizedSet(new HashSet<String>());

	public Structure(JsonObject struct) {
		this(struct.getString("externalId"), struct);
	}

	protected Structure(JsonObject struct, JsonArray groups, JsonArray classes) {
		this(struct);
		if (groups != null) {
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				functionalGroups.add((String) o);
			}
		}
		if (classes != null) {
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				this.classes.add((String) o);
			}
		}
	}

	protected Structure(String externalId, JsonObject struct) {
		if (struct != null && externalId != null && externalId.equals(struct.getString("externalId"))) {
			this.id = struct.getString("id");
		} else {
			throw new IllegalArgumentException("Invalid structure with externalId : " + externalId);
		}
		this.externalId = externalId;
		this.struct = struct;
	}

	private TransactionHelper getTransaction() {
		return importer.getTransaction();
	}

	public void update(JsonObject struct) {
		if (this.struct.equals(struct)) {
			return;
		}
		String query =
				"MATCH (s:Structure { externalId : {externalId}}) " +
				"WITH s " +
				"WHERE s.checksum IS NULL OR s.checksum <> {checksum} " +
				"SET " + Neo4jUtils.nodeSetPropertiesFromJson("s", struct, "id", "externalId", "created");
		getTransaction().add(query, struct);
		this.struct = struct;
	}

	public void create() {
		String query =
				"CREATE (s:Structure {props}) " +
				"WITH s " +
				"MATCH (p:Profile) " +
				"CREATE p<-[:HAS_PROFILE]-(g:Group:ProfileGroup {name : s.name+'-'+p.name, displayNameSearchField: {groupSearchField}})-[:DEPENDS]->s " +
				"SET g.id = id(g)+'-'+timestamp() ";
		JsonObject params = new JsonObject()
				.putString("id", id)
				.putString("externalId", externalId)
				.putString("groupSearchField", Validator.sanitize(struct.getString("name")))
				.putObject("props", struct);
		getTransaction().add(query, params);
	}


	public synchronized Object[] addJointure(String externalId) {
		if (struct != null) {
			JsonArray joinKey = struct.getArray("joinKey");
			if (joinKey == null) {
				joinKey = new JsonArray();
				struct.putArray("joinKey", joinKey);
			}
			joinKey.add(externalId);
			String query =
					"MATCH (s:Structure {externalId: {externalId}}) " +
					"SET s.joinKey = {joinKey} ";
			JsonObject params = new JsonObject().putArray("joinKey", joinKey).putString("externalId", getExternalId());
			getTransaction().add(query, params);
			return joinKey.toArray();
		}
		return null;
	}

	public void addAttachment() {
		JsonArray functionalAttachment = struct.getArray("functionalAttachment");
		if (functionalAttachment != null && functionalAttachment.size() > 0 &&
				!externalId.equals(functionalAttachment.get(0))) {
			JsonObject params = new JsonObject().putString("externalId", externalId);
			String query;
			if (functionalAttachment.size() == 1) {
				query =
						"MATCH (s:Structure { externalId : {externalId}}), " +
						"(ps:Structure { externalId : {functionalAttachment}}) " +
						"CREATE UNIQUE s-[:HAS_ATTACHMENT]->ps";
				params.putString("functionalAttachment", (String) functionalAttachment.get(0));
			} else {
				query =
						"MATCH (s:Structure { externalId : {externalId}}), (ps:Structure) " +
						"WHERE ps.externalId IN {functionalAttachment} " +
						"CREATE UNIQUE s-[:HAS_ATTACHMENT]->ps";
				params.putArray("functionalAttachment", functionalAttachment);
			}
			getTransaction().add(query, params);
		}
	}

	public void createClassIfAbsent(String classExternalId, String name) {
		if (classes.add(classExternalId)) {
			String query =
					"MATCH (s:Structure { externalId : {structureExternalId}}) " +
					"CREATE s<-[:BELONGS]-(c:Class {props})" +
					"WITH s, c " +
					"MATCH s<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-'+p.name, displayNameSearchField: {groupSearchField}})-[:DEPENDS]->g " +
					"SET pg.id = id(pg)+'-'+timestamp() ";
			JsonObject params = new JsonObject()
					.putString("structureExternalId", externalId)
					.putString("groupSearchField", Validator.sanitize(name))
					.putObject("props", new JsonObject()
									.putString("externalId", classExternalId)
									.putString("id", UUID.randomUUID().toString())
									.putString("name", name)
					);
			getTransaction().add(query, params);
		}
	}

	public void createFunctionalGroupIfAbsent(String groupExternalId, String name) {
		if (functionalGroups.add(groupExternalId)) {
			String query =
					"MATCH (s:Structure { externalId : {structureExternalId}}) " +
					"WHERE (NOT(HAS(s.timetable)) OR s.timetable = '') " +
					"CREATE s<-[:DEPENDS]-(c:Group:FunctionalGroup {props}) ";
			JsonObject params = new JsonObject()
					.putString("structureExternalId", externalId)
					.putObject("props", new JsonObject()
							.putString("externalId", groupExternalId)
							.putString("id", UUID.randomUUID().toString())
							.putString("displayNameSearchField", Validator.sanitize(name))
							.putString("name", name)
					);
			getTransaction().add(query, params);
		}
	}

	public void linkModules(String moduleExternalId) {
		String query =
				"MATCH (s:Structure { externalId : {externalId}}), " +
				"(m:Module { externalId : {moduleExternalId}}) " +
				"CREATE UNIQUE s-[:OFFERS]->m";
		JsonObject params = new JsonObject()
				.putString("externalId", externalId)
				.putString("moduleExternalId", moduleExternalId);
		getTransaction().add(query, params);
	}

	public String getExternalId() {
		return externalId;
	}

	public void transition(final Handler<Message<JsonObject>> handler) {
		final TransactionHelper tx = TransactionManager.getInstance().getTransaction("GraphDataUpdate");
		String query =
				"MATCH (s:Structure {id : {id}})<-[:BELONGS]-(c:Class)" +
				"<-[:DEPENDS]-(cpg:Group)<-[:IN]-(u:User) " +
				"OPTIONAL MATCH s<-[:DEPENDS]-(fg:FunctionalGroup) " +
				"RETURN collect(distinct u.id) as users, collect(distinct cpg.id) as profileGroups, " +
				"collect(distinct fg.id) as functionalGroups";
		JsonObject params = new JsonObject().putString("id", id);
		tx.getNeo4j().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray r = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && r != null && r.size() == 1) {
					final JsonObject res = r.get(0);
					usersInGroups(new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> event) {
							for (Object u : res.getArray("users")) {
								User.backupRelationship(u.toString(), tx);
								User.transition(u.toString(), tx);
							}
							transitionClassGroup();
							handler.handle(event);
						}
					});
				} else {
					log.error("Structure " + id + " transition error.");
					log.error(event.body().encode());
					handler.handle(event);
				}
			}
		});
	}

	private void usersInGroups(Handler<Message<JsonObject>> handler) {
		final Neo4j neo4j = TransactionManager.getInstance().getNeo4j();
		final JsonObject params = new JsonObject().putString("id", id);
		String query =
				"MATCH (s:Structure {id : {id}})<-[:BELONGS]-(c:Class)" +
				"<-[:DEPENDS]-(cpg:Group) " +
				"OPTIONAL MATCH cpg<-[:IN]-(u:User) " +
				"RETURN cpg.id as group, cpg.name as groupName, collect(u.id) as users " +
				"UNION " +
				"MATCH (s:Structure {id : {id}})<-[r:DEPENDS]-(fg:FunctionalGroup) " +
				"OPTIONAL MATCH fg<-[:IN]-(u:User) " +
				"RETURN fg.id as group, fg.name as groupName, collect(u.id) as users ";
		neo4j.execute(query, params, handler);
	}

	private void transitionClassGroup() {
		TransactionHelper tx = TransactionManager.getInstance().getTransaction("GraphDataUpdate");
		JsonObject params = new JsonObject().putString("id", id);
		String query =
				"MATCH (s:Structure {id : {id}})<-[r:BELONGS]-(c:Class)" +
				"<-[r1:DEPENDS]-(cpg:Group)-[r2]-() " +
				"OPTIONAL MATCH c-[r3]-() " +
				"DELETE r, r1, r2, r3, c, cpg ";
		tx.add(query, params);
		query = "MATCH (s:Structure {id : {id}})<-[r:DEPENDS]-(fg:FunctionalGroup) " +
				"OPTIONAL MATCH fg-[r1]-() " +
				"DELETE r, r1, fg";
		tx.add(query, params);
	}

	public static void count(String exportType, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject();
		String query = "MATCH (s:Structure) ";
		if (isNotEmpty(exportType)) {
			query += "WHERE HAS(s.exports) AND {exportType} IN s.exports ";
			params.putString("exportType", exportType);
		}
		query += "RETURN count(distinct s) as nb";
		transactionHelper.add(query, params);
	}

	public static void list(String exportType, JsonArray attributes, Integer skip, Integer limit, TransactionHelper transactionHelper) {
		StringBuilder query = new StringBuilder("MATCH (s:Structure) ");
		JsonObject params = new JsonObject();
		if (isNotEmpty(exportType)) {
			query.append("WHERE HAS(s.exports) AND {exportType} IN s.exports ");
			params.putString("exportType", exportType);
		}
		if (attributes != null && attributes.size() > 0) {
			query.append("RETURN DISTINCT");
			for (Object attribute : attributes) {
				query.append(" s.").append(attribute).append(" as ").append(attribute).append(",");
			}
			query.deleteCharAt(query.length() - 1);
			query.append(" ");
		} else {
			query.append("RETURN DISTINCT s ");
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

	public static void addAttachment(String structureId, String parentStructureId,
			TransactionHelper transactionHelper) {
		String query =
				"MATCH (s:Structure { id : {structureId}}), " +
				"(ps:Structure { id : {parentStructureId}}) " +
				"CREATE UNIQUE s-[r:HAS_ATTACHMENT]->ps " +
				"RETURN id(r) as id";
		String query2 =
				"MATCH (s:Structure { id : {structureId}})-[:HAS_ATTACHMENT*1..]->(ps:Structure)" +
				"<-[:DEPENDS]-(fg:FunctionGroup {name : ps.name + '-AdminLocal'})<-[:IN]-(u:User)" +
				"-[rf:HAS_FUNCTION]-(f:Function {name : 'AdminLocal'}) " +
				"SET rf.scope = coalesce(rf.scope, []) + s.id " +
				"WITH DISTINCT s as n, f, u " +
				"MERGE (fg:Group:FunctionGroup { externalId : n.id + '-ADMIN_LOCAL'}) " +
				"ON CREATE SET fg.id = id(fg) + '-' + timestamp(), fg.name = n.name + '-' + f.name, fg.displayNameSearchField = lower(n.name) " +
				"CREATE UNIQUE n<-[:DEPENDS]-fg " +
				"MERGE fg<-[:IN { source : 'MANUAL'}]-u";
		JsonObject params =  new JsonObject()
				.putString("structureId", structureId)
				.putString("parentStructureId", parentStructureId);
		transactionHelper.add(query, params);
		transactionHelper.add(query2, params);
	}

	public static void removeAttachment(String structureId, String parentStructureId,
			TransactionHelper transactionHelper) {
		String query =
				"MATCH (s:Structure { id : {structureId}})-[r:HAS_ATTACHMENT]->(ps:Structure { id : {parentStructureId}}) " +
				"DELETE r";
		String query2 =
				"MATCH (s:Structure { id : {structureId}})-[r:HAS_ATTACHMENT*0..]->(s2:Structure)" +
				"<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
				"WITH COLLECT(distinct u.id) as ids " +
				"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-(:FunctionGroup {externalId: s.id + '-ADMIN_LOCAL'})" +
				"<-[r:IN]-(u:User)-[rf:HAS_FUNCTION]-(:Function {name : 'AdminLocal'}) " +
				"WHERE NOT(u.id IN ids) " +
				"SET rf.scope = FILTER(sId IN rf.scope WHERE sId <> s.id) " +
				"DELETE r";
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("parentStructureId", parentStructureId);
		transactionHelper.add(query, params);
		transactionHelper.add(query2, params);
	}

}
