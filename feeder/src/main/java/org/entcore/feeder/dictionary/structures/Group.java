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

package org.entcore.feeder.dictionary.structures;

import java.util.UUID;

import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Group {

	public static void manualCreateOrUpdate(JsonObject object, String structureId, String classId,
			TransactionHelper transactionHelper) throws ValidationException {
		if (object == null) {
			throw new ValidationException("invalid.group");
		} else {
			final boolean create = (object.getString("id") == null || object.getString("id").trim().isEmpty());
			final String id = create ? UUID.randomUUID().toString() : object.getString("id");
			if (create) {
				object.putString("id", id);
			}
			if (isNotEmpty(object.getString("name"))) {
				object.putString("displayNameSearchField", Validator.sanitize(object.getString("name")));
			}
			String query =
					"MERGE (t:Group:ManualGroup:Visible { id : {id}}) " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("t", object, "id") +
					"RETURN t.id as id ";
			transactionHelper.add(query, object);
			if (create) {
				if (structureId != null && !structureId.trim().isEmpty()) {
					String qs =
							"MATCH (s:Structure {id : {structureId}}), (g:Group {id : {groupId}}) " +
							"CREATE UNIQUE s<-[:DEPENDS]-g";
					JsonObject ps = new JsonObject()
							.putString("groupId", id)
							.putString("structureId", structureId);
					transactionHelper.add(qs, ps);
				}
				if (classId != null && !classId.trim().isEmpty()) {
					String qs =
							"MATCH (s:Class {id : {classId}}), (g:Group {id : {groupId}}) " +
							"CREATE UNIQUE s<-[:DEPENDS]-g";
					JsonObject ps = new JsonObject()
							.putString("groupId", id)
							.putString("classId", classId);
					transactionHelper.add(qs, ps);
				}
			}
		}
	}

	public static void manualDelete(String id, TransactionHelper transactionHelper) {
		String query =
				"MATCH (g:ManualGroup {id : {id}}) " +
				"OPTIONAL MATCH g-[r]-() " +
				"DELETE g, r";
		transactionHelper.add(query, new JsonObject().putString("id", id));
	}
	
	public static void addUsers(String groupId, JsonArray userIds, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User), (g:Group) " +
				"WHERE g.id = {groupId} AND u.id IN {userIds} " +
				"AND ('ManualGroup' IN labels(g) OR 'FunctionalGroup' IN labels(g)) " +
				"CREATE UNIQUE (u)-[:IN {source:'MANUAL'}]->(g) " +
				"WITH g, u " +
				"WHERE 'FunctionalGroup' IN labels(g) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> g.externalId) + g.externalId ";
		
		JsonObject params = new JsonObject()
				.putString("groupId", groupId)
				.putArray("userIds", userIds);
		
		transactionHelper.add(query, params);
	}
	
	public static void removeUsers(String groupId, JsonArray userIds, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User)-[r:IN|COMMUNIQUE]->(g:Group) " +
				"WHERE g.id = {groupId} AND u.id IN {userIds} " +
				"AND ('ManualGroup' IN labels(g) OR 'FunctionalGroup' IN labels(g)) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> g.externalId) " +
				"DELETE r ";
				
		JsonObject params = new JsonObject()
				.putString("groupId", groupId)
				.putArray("userIds", userIds);
		
		transactionHelper.add(query, params);
	}
}
