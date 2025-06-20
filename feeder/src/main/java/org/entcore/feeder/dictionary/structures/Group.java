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

import io.vertx.core.Promise;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Group {

	private static final Logger log = LoggerFactory.getLogger(Group.class);
	private static final String TX_RUN_LINK_RULES = "TX_RUN_LINK_RULES";

	public static void manualCreateOrUpdate(JsonObject object, String structureId, String classId,
			TransactionHelper transactionHelper) throws ValidationException {
		if (object == null) {
			throw new ValidationException("invalid.group");
		} else {
			final boolean create = (object.getString("id") == null || object.getString("id").trim().isEmpty());
			final String id = create ? UUID.randomUUID().toString() : object.getString("id");
			object.put("create", create);
			if (create) {
				object.put("id", id);
			}
			if (isNotEmpty(object.getString("name"))) {
				object.put("displayNameSearchField", Validator.sanitize(object.getString("name")));
			}
			String query =
					"MERGE (t:Group:ManualGroup:Visible { id : {id}}) " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("t", object, "id", "name", "displayNameSearchField", "create") +
					", t.name = CASE WHEN EXISTS(t.lockDelete) AND t.lockDelete = true AND {create} = false THEN t.name ELSE {name} END " +
					", t.displayNameSearchField = CASE WHEN EXISTS(t.lockDelete) AND t.lockDelete = true AND {create} = false THEN t.displayNameSearchField ELSE {displayNameSearchField} END " +
					"RETURN t.id as id, t.createdAt as createdAt, t.createdByName as createdByName, t.modifiedAt as modifiedAt, t.modifiedByName as modifiedByName ";
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
				"WHERE NOT(EXISTS(g.lockDelete)) OR g.lockDelete <> true " +
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
		User.countUsersInGroups(groupId, null, transactionHelper);
	}
	
	public static void removeUsers(String groupId, JsonArray userIds, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User)-[r:IN|COMMUNIQUE]-(g:Group) " +
				"WHERE g.id = {groupId} AND u.id IN {userIds} " +
				"AND ('ManualGroup' IN labels(g) OR 'FunctionalGroup' IN labels(g)) " +
				"SET u.groups = FILTER(gId IN coalesce(u.groups, []) WHERE gId <> g.externalId) " +
				"DELETE r ";
				
		JsonObject params = new JsonObject()
				.put("groupId", groupId)
				.put("userIds", userIds);
		
		transactionHelper.add(query, params);
		User.countUsersInGroups(groupId, null, transactionHelper);
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

	public static Future<Void> runLinkRules() {
		Promise p = Promise.promise();
		try {
			final TransactionHelper tx = TransactionManager.getInstance().begin(TX_RUN_LINK_RULES);
			final String listAutolinkGroups =
				"MATCH (g:ManualGroup) " +
				"WHERE EXISTS(g.autolinkUsersFromGroups) " +
				  " OR EXISTS(g.autolinkUsersFromPositions) " +
				"RETURN g.id as id";
			tx.add(listAutolinkGroups, new JsonObject());

			tx.commit().compose(groups -> {
				if (groups != null && groups.size() == 1 && groups.getJsonArray(0) != null) {
					return groups.getJsonArray(0).stream().reduce(
						Future.succeededFuture(new JsonArray()),
						(previousFuture, group) -> previousFuture.compose(r ->
								groupLinkRules(((JsonObject) group).getString("id"), tx).commit()),
						(f1, f2) -> f2 // return last future
					);
				} else {
					return Future.succeededFuture(new JsonArray());
				}
			}).onComplete(ar -> {
				TransactionManager.getInstance().rollback(TX_RUN_LINK_RULES);
				if (ar.succeeded()) {
					log.info("PostImport | SUCCEED to manualGroupLinkUsersAuto");
					p.complete();
				} else {
					log.error("PostImport | Failed to manualGroupLinkUsersAuto", ar.cause());
					p.fail(ar.cause());
				}

			});
		}
		catch(TransactionException e) {
			log.error("Error opening or running transaction in group link rules", e);
			p.fail(e);
		}
		catch(Exception e) {
			log.error("Unknown error in group link rules", e);
			p.fail(e);
		}
		return p.future();
	}

	protected static TransactionHelper groupLinkRules(String groupId, TransactionHelper tx) {
		log.info("tx groupLinkRules with groupId : " + groupId);
		final String linkQuery =
			"MATCH (g:ManualGroup {id: {groupId}})-[:DEPENDS]->(:Structure)<-[:HAS_ATTACHMENT*0..]-(struct:Structure) " +
			"WHERE EXISTS(g.autolinkUsersFromGroups) OR EXISTS(g.autolinkUsersFromPositions) "+
			"WITH g, struct " +
			"MATCH (position:UserPosition)<-[:HAS_POSITION*0..]-(u:User)-[:IN]->(target:Group)-[:DEPENDS]->(struct) " +
			"WHERE " +
			// filter by type of auto link
			"((g.autolinkTargetAllStructs = true OR struct.id IN g.autolinkTargetStructs ) " +
					"AND target.filter IN g.autolinkUsersFromGroups " +
					"OR position.name IN COALESCE(g.autolinkUsersFromPositions, [])) " +
			// update the timestamp to remove old user
			"WITH g, u " +
			"MERGE (u)-[new:IN]->(g) " +
			"ON CREATE SET new.source = 'AUTO' " +
			"SET new.updated = {now} ";

		final String removeQuery =
			"MATCH (g:ManualGroup {id: {groupId}})<-[old:IN]-(:User) " +
			"WHERE EXISTS(g.autolinkUsersFromGroups) AND old.source = 'AUTO' AND (NOT EXISTS(old.updated) OR old.updated <> {now}) " +
			"DELETE old ";

		final JsonObject params = new JsonObject()
				.put("groupId", groupId)
				.put("now", System.currentTimeMillis());

		tx.add(linkQuery, params);
		tx.add(removeQuery, params);
		User.countUsersInGroups(groupId, "ManualGroup", tx);
		return tx;
	}

}
