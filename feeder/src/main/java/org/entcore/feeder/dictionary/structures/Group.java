/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.feeder.dictionary.structures;

import java.util.UUID;

import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
				object.put("id", id);
			}
			if (isNotEmpty(object.getString("name"))) {
				object.put("displayNameSearchField", Validator.sanitize(object.getString("name")));
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
							.put("groupId", id)
							.put("structureId", structureId);
					transactionHelper.add(qs, ps);
				}
				if (classId != null && !classId.trim().isEmpty()) {
					String qs =
							"MATCH (s:Class {id : {classId}}), (g:Group {id : {groupId}}) " +
							"CREATE UNIQUE s<-[:DEPENDS]-g";
					JsonObject ps = new JsonObject()
							.put("groupId", id)
							.put("classId", classId);
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
		transactionHelper.add(query, new JsonObject().put("id", id));
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
				.put("groupId", groupId)
				.put("userIds", userIds);
		
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
				.put("groupId", groupId)
				.put("userIds", userIds);
		
		transactionHelper.add(query, params);
	}

	public static void updateEmail(String groupId, String emailInternal, TransactionHelper transactionHelper)
			throws ValidationException{

		if(!StringValidation.isEmail(emailInternal)) {
			throw new ValidationException("invalid.email");
		} else {
			String query =  "MERGE (g:Group { id : {id}}) " +
					"SET g.emailInternal = {email}";

			JsonObject params = new JsonObject()
					.put("id", groupId)
					.put("email", emailInternal);

			transactionHelper.add(query, params);
		}
	}
}
