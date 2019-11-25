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

package org.entcore.feeder;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.dictionary.structures.*;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.*;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.busmods.BusModBase;

import java.util.*;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.isNotEmpty;


public class ManualFeeder extends BusModBase {

	public static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private static final Validator structureValidator = new Validator("dictionary/schema/Structure.json");
	private static final Validator classValidator = new Validator("dictionary/schema/Class.json");
	public static final Map<String, Validator> profiles;
	private final Neo4j neo4j;
	private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
	public static final String SOURCE = "MANUAL";

	static {
		Map<String, Validator> p = new HashMap<>();
		p.put("Personnel", new Validator("dictionary/schema/Personnel.json"));
		p.put("Teacher", new Validator("dictionary/schema/Personnel.json"));
		p.put("Student", new Validator("dictionary/schema/Student.json"));
		p.put("Relative", new Validator("dictionary/schema/User.json"));
		p.put("Guest", new Validator("dictionary/schema/User.json"));
		profiles = Collections.unmodifiableMap(p);
	}

	public ManualFeeder(Neo4j neo4j, EventBus eb) {
		this.neo4j = neo4j;
		this.eb = eb;
	}

	public void createStructure(final Message<JsonObject> message) {
		JsonObject struct = getMandatoryObject("data", message);
		if (struct == null) return;
		if (struct.getString("externalId") == null) {
			struct.put("externalId", UUID.randomUUID().toString());
		}
		final String error = structureValidator.validate(struct);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query =
					"CREATE (s:Structure {props}) " +
							"WITH s " +
							"MATCH (p:Profile) " +
							"CREATE p<-[:HAS_PROFILE]-(g:Group:ProfileGroup {name : s.name+'-'+p.name, " +
							"displayNameSearchField: {groupSearchField}, filter: p.name})-[:DEPENDS]->s " +
							"SET g.id = id(g)+'-'+timestamp() " +
							"RETURN DISTINCT s.id as id ";
			JsonObject params = new JsonObject()
					.put("groupSearchField", Validator.sanitize(struct.getString("name")))
					.put("props", struct);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void createClass(final Message<JsonObject> message) {
		JsonObject c = getMandatoryObject("data", message);
		if (c == null) return;
		String structureId = getMandatoryString("structureId", message);
		if (structureId == null) return;
		if (c.getString("externalId") == null || c.getString("externalId").isEmpty()) {
			c.put("externalId", structureId + "$" + c.getString("name"));
		}
		final String error = classValidator.validate(c);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query =
					"MATCH (s:Structure { id : {structureId}}) " +
							"CREATE s<-[:BELONGS]-(c:Class {props}) " +
							"SET c.externalId = s.externalId + '$' + c.name " +
							"WITH s, c " +
							"MATCH s<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
							"CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-'+p.name, " +
							"displayNameSearchField: {groupSearchField}, filter: p.name})-[:DEPENDS]->g " +
							"SET pg.id = id(pg)+'-'+timestamp() " +
							"RETURN DISTINCT c.id as id ";
			JsonObject params = new JsonObject()
					.put("structureId", structureId)
					.put("groupSearchField", Validator.sanitize(c.getString("name")))
					.put("props", c);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void updateClass(final Message<JsonObject> message) {
		JsonObject c = getMandatoryObject("data", message);
		if (c == null) return;
		String classId = getMandatoryString("classId", message);
		if (classId == null) return;
		final String error = classValidator.modifiableValidate(c);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String rename = "";
			if (c.getString("name") != null) {
				rename = "WITH c " +
						"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
						"(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"SET cpg.name = c.name+'-'+p.name ";
			}
			String query =
					"MATCH (c:`Class` { id : {classId}}) " +
							"SET " + Neo4jUtils.nodeSetPropertiesFromJson("c", c) +
							rename +
							"RETURN DISTINCT c.id as id ";
			JsonObject params = c.put("classId", classId);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void createUser(final Message<JsonObject> message) {
		final JsonObject user = getMandatoryObject("data", message);
		if (user == null) return;
		if (user.getString("externalId") == null) {
			user.put("externalId", UUID.randomUUID().toString());
		}
		final String profile = message.body().getString("profile", "");
		if (!profiles.containsKey(profile)) {
			sendError(message, "Invalid profile : " + profile);
			return;
		}
		JsonArray childrenIds = null;
		if ("Relative".equals(profile)) {
			childrenIds = user.getJsonArray("childrenIds");
		}
		final String error = profiles.get(profile).validate(user);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
			return;
		}
		user.put("source", SOURCE);
		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			createUserInStructure(message, user, profile, structureId, childrenIds);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			createUserInClass(message, user, profile, classId, childrenIds);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	private void createUserInStructure(final Message<JsonObject> message,
									   final JsonObject user, String profile, String structureId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.put("structureId", structureId)
				.put("profile", profile)
				.put("props", user);
		if (childrenIds != null && childrenIds.size() > 0) {
			related =
					"WITH u " +
							"MATCH (student:User) " +
							"WHERE student.id IN {childrenIds} " +
							"CREATE student-[:RELATED {source: 'MANUAL'}]->u " +
							"SET student.relative = coalesce(student.relative, []) + (u.externalId + '$10$1$1$0$0') ";
			params.put("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-" +
						"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}) " +
						"CREATE UNIQUE pg<-[:IN]-(u:User {props}) " +
						"SET u.structures = [s.externalId] " +
						related +
						"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
				if ("ok".equals(m.body().getString("status"))) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().put("new-user", user.getString("id")));
				}
			}
		});
	}

	public void addUser(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			addUserInStructure(message, userId, structureId);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			addUserInClass(message, userId, classId);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}


	public void addUsers(final Message<JsonObject> message) {
		final JsonArray userIds = message.body().getJsonArray("userIds", new JsonArray());
		if (userIds.isEmpty()) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			addUsersInStructure(message, userIds, structureId);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			addUsersInClass(message, userIds, classId);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	private void addUserInStructure(final Message<JsonObject> message,
									String userId, String structureId) {
		JsonObject params = new JsonObject()
				.put("structureId", structureId)
				.put("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WITH u, p " +
						"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->p " +
						"CREATE UNIQUE pg<-[:IN {source:'MANUAL'}]-u " +
						"SET u.structures = CASE WHEN s.externalId IN u.structures THEN " +
						"u.structures ELSE coalesce(u.structures, []) + s.externalId END " +
						"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	private void addUsersInStructure(final Message<JsonObject> message,
									 JsonArray userIds, String structureId) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
		for(Object userId : userIds.getList()){
			JsonObject params = new JsonObject()
					.put("structureId", structureId)
					.put("userId", userId.toString());
			String query =
					"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
							"WITH u, p " +
							"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p) " +
							"MERGE (pg)<-[:IN {source:'MANUAL'}]-(u) " +
							"SET u.structures = CASE WHEN s.externalId IN u.structures THEN " +
							"u.structures ELSE coalesce(u.structures, []) + s.externalId END " +
							"RETURN DISTINCT u.id as id";
			statementsBuilder.add(query,params);
		}
		neo4j.executeTransaction(statementsBuilder.build(), null, true, res-> {
			message.reply(res.body());
		});
	}

	public void removeUser(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			removeUserFromStructure(message, userId, structureId);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			removeUserFromClass(message, userId, classId);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	public void removeUsers(final Message<JsonObject> message) {
		final JsonArray userIds = message.body().getJsonArray("userIds", new JsonArray());
		if (userIds.isEmpty()) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			removeUsersFromStructure(message, userIds, structureId);
			return;
		}
		final JsonArray classIds = message.body().getJsonArray("classIds", new JsonArray());
		if (!classIds.isEmpty()) {
			if(classIds.size()!=userIds.size()){
				sendError(message, "userIds and classIds Array must have same number of elements");
				return;
			}
			removeUsersFromClass(message, userIds, classIds);
			return;
		}
		sendError(message, "structureId or classIds must be specified");
	}

	private void removeUserFromStructure(final Message<JsonObject> message,
										 String userId, String structureId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			JsonObject params = new JsonObject()
					.put("structureId", structureId)
					.put("userId", userId);
			final String query =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS*0..1]->" +
							"(pg:ProfileGroup)-[:DEPENDS]->(s:Structure { id : {structureId}}), " +
							"pg-[:HAS_PROFILE]->(p:Profile), p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
							"CREATE UNIQUE dpg<-[:IN]-u " +
							"SET u.structures = FILTER(sId IN u.structures WHERE sId <> s.externalId), " +
							"u.classes = FILTER(cId IN u.classes WHERE NOT(cId =~ (s.externalId + '.*'))) " +
							"DELETE r " +
							"RETURN DISTINCT u.id as id";
			final String removeFunctions =
					"MATCH (u:User { id : {userId}})-[r:HAS_FUNCTION]->() " +
							"WHERE {structureId} IN r.scope " +
							"SET r.scope = FILTER(sId IN r.scope WHERE sId <> {structureId}) " +
							"WITH r " +
							"WHERE LENGTH(r.scope) = 0 " +
							"DELETE r";
			final String removeFunctionGroups =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure { id : {structureId}})" +
							"DELETE r";
			tx.add(query, params);
			tx.add(removeFunctions, params);
			tx.add(removeFunctionGroups, params);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						message.reply(event.body().put("result", results.getJsonArray(0)));
					} else {
						message.reply(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from structure", e);
			sendError(message, "transaction.error");
		}
	}

	private void removeUsersFromStructure(final Message<JsonObject> message,
										  JsonArray userIds, String structureId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			for(Object userIdObj: userIds) {
				JsonObject params = new JsonObject()
						.put("structureId", structureId)
						.put("userId", userIdObj.toString());
				final String query =
						"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS*0..1]->" +
								"(pg:ProfileGroup)-[:DEPENDS]->(s:Structure { id : {structureId}}), " +
								"pg-[:HAS_PROFILE]->(p:Profile), p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
								"CREATE UNIQUE dpg<-[:IN]-u " +
								"SET u.structures = FILTER(sId IN u.structures WHERE sId <> s.externalId), " +
								"u.classes = FILTER(cId IN u.classes WHERE NOT(cId =~ (s.externalId + '.*'))) " +
								"DELETE r " +
								"RETURN DISTINCT u.id as id";
				final String removeFunctions =
						"MATCH (u:User { id : {userId}})-[r:HAS_FUNCTION]->() " +
								"WHERE {structureId} IN r.scope " +
								"SET r.scope = FILTER(sId IN r.scope WHERE sId <> {structureId}) " +
								"WITH r " +
								"WHERE LENGTH(r.scope) = 0 " +
								"DELETE r";
				final String removeFunctionGroups =
						"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure { id : {structureId}})" +
								"DELETE r";
				tx.add(query, params);
				tx.add(removeFunctions, params);
				tx.add(removeFunctionGroups, params);
			}
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						message.reply(event.body().put("result", results.getJsonArray(0)));
					} else {
						message.reply(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from structure", e);
			sendError(message, "transaction.error");
		}
	}

	private void createUserInClass(final Message<JsonObject> message,
								   final JsonObject user, String profile, String classId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.put("classId", classId)
				.put("profile", profile)
				.put("props", user);
		if (childrenIds != null && childrenIds.size() > 0) {
			related =
					"WITH u " +
							"MATCH (student:User) " +
							"WHERE student.id IN {childrenIds} " +
							"CREATE student-[:RELATED {source: 'MANUAL'}]->u " +
							"SET student.relative = coalesce(student.relative, []) + (u.externalId + '$10$1$1$0$0') ";
			params.put("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
						"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}), s-[:BELONGS]->(struct:Structure) " +
						"CREATE UNIQUE pg<-[:IN]-(u:User {props}), cpg<-[:IN]-u " +
						"SET u.classes = [s.externalId], u.structures = [struct.externalId] " +
						related +
						"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
				if ("ok".equals(m.body().getString("status"))) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().put("new-user", user.getString("id")));
				}
			}
		});

	}

	private void addUserInClass(final Message<JsonObject> message,
								String userId, String classId) {
		JsonObject params = new JsonObject()
				.put("classId", classId)
				.put("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WITH u, p " +
						"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
						"(pg:ProfileGroup)-[:HAS_PROFILE]->p, s-[:BELONGS]->(struct:Structure) " +
						"CREATE UNIQUE pg<-[:IN {source:'MANUAL'}]-u, cpg<-[:IN {source:'MANUAL'}]-u " +
						"SET u.classes = CASE WHEN s.externalId IN u.classes THEN " +
						"u.classes ELSE coalesce(u.classes, []) + s.externalId END, " +
						"u.structures = CASE WHEN struct.externalId IN u.structures THEN " +
						"u.structures ELSE coalesce(u.structures, []) + struct.externalId END " +
						"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	private void addUsersInClass(final Message<JsonObject> message,
								 JsonArray userIds, String classId) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
		for(Object userId : userIds.getList()) {
			JsonObject params = new JsonObject()
					.put("classId", classId)
					.put("userId", userId);
			String query =
					"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
							"WITH u, p " +
							"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
							"(pg:ProfileGroup)-[:HAS_PROFILE]->(p), (s)-[:BELONGS]->(struct:Structure) " +
							"MERGE pg<-[:IN {source:'MANUAL'}]-u " +
							"MERGE cpg<-[:IN {source:'MANUAL'}]-u " +
							"SET u.classes = CASE WHEN s.externalId IN u.classes THEN " +
							"u.classes ELSE coalesce(u.classes, []) + s.externalId END, " +
							"u.structures = CASE WHEN struct.externalId IN u.structures THEN " +
							"u.structures ELSE coalesce(u.structures, []) + struct.externalId END " +
							"RETURN DISTINCT u.id as id";
			statementsBuilder.add(query, params);
		}
		neo4j.executeTransaction(statementsBuilder.build(), null,true, res-> {
			message.reply(res.body());
		});
	}

	private void removeUserFromClass(final Message<JsonObject> message,
									 String userId, String classId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();

			JsonObject params = new JsonObject()
					.put("classId", classId)
					.put("userId", userId);
			String query =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS]->" +
							"(c:Class  {id : {classId}}), cpg-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
							"p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
							"CREATE UNIQUE dpg<-[:IN]-u " +
							"SET u.classes = FILTER(cId IN u.classes WHERE cId <> c.externalId) , u.headTeacherManual = FILTER(x IN u.headTeacherManual WHERE x <> c.externalId) " +
							"DELETE r " +
							"RETURN DISTINCT u.id as id";

			tx.add(query, params);

			String query2 =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(c:Class {id : {classId}}) " +
							"DELETE r ";

			tx.add(query2, params);

			String query3 =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(s:Structure)<-[b:BELONGS]-(c:Class {id : {classId}}) " +
							"WHERE length(u.headTeacherManual) = 0 AND (u.headTeacher IS NULL OR length(u.headTeacher) = 0) " +
							"DELETE r " +
							"RETURN DISTINCT u.id as id";

			tx.add(query3, params);

			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						message.reply(event.body().put("result", results.getJsonArray(0)));
					} else {
						message.reply(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from structure", e);
			sendError(message, "transaction.error");
		}
	}

	private void removeUsersFromClass(final Message<JsonObject> message,
									  JsonArray userIds, JsonArray classIds) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			for(int i=0; i < userIds.size(); i++) {
				String userId = userIds.getString(i);
				Object classIdsForUserId = classIds.getValue(i);
				JsonArray currentClassIds = classIdsForUserId instanceof JsonArray? (JsonArray) classIdsForUserId: new JsonArray().add(classIdsForUserId.toString());
				for(Object classIdObj : currentClassIds) {
					JsonObject params = new JsonObject()
							.put("classId", classIdObj.toString())
							.put("userId", userId);
					String query =
							"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS]->" +
									"(c:Class  {id : {classId}}), cpg-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
									"p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
									"CREATE UNIQUE dpg<-[:IN]-u " +
									"SET u.classes = FILTER(cId IN u.classes WHERE cId <> c.externalId) , u.headTeacherManual = FILTER(x IN u.headTeacherManual WHERE x <> c.externalId) " +
									"DELETE r " +
									"RETURN DISTINCT u.id as id";

					tx.add(query, params);

					String query2 =
							"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(c:Class {id : {classId}}) " +
									"DELETE r ";

					tx.add(query2, params);

					String query3 =
							"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]->(g:Group:HTGroup)-[:DEPENDS]->(s:Structure)<-[b:BELONGS]-(c:Class {id : {classId}}) " +
									"WHERE length(u.headTeacherManual) = 0 AND (u.headTeacher IS NULL OR length(u.headTeacher) = 0) " +
									"DELETE r " +
									"RETURN DISTINCT u.id as id";

					tx.add(query3, params);
				}
			}
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						message.reply(event.body().put("result", results.getJsonArray(0)));
					} else {
						message.reply(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from structure", e);
			sendError(message, "transaction.error");
		}
	}

	public void updateUser(final Message<JsonObject> message) {
		final JsonObject user = getMandatoryObject("data", message);
		if (user == null) return;
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;
		String q =
				"MATCH (u:User { id : {userId}})-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"RETURN DISTINCT p.name as profile ";
		neo4j.execute(q, new JsonObject().put("userId", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getJsonArray("result");
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() > 0) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String profile = ((JsonObject) o).getString("profile");
						Validator v = profiles.get(profile);
						if (v == null) {
							sendError(message, "Invalid profile : " + profile);
							return;
						}
						final String error = v.modifiableValidate(user);
						if (error != null) {
							logger.error(error);
							sendError(message, error);
							return;
						}
					}
					user.put("checksum", "manual");
					String query =
							"MATCH (u:User { id : {userId}}) " +
									"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", user) +
									"RETURN DISTINCT u.id as id ";
					JsonObject params = user.put("userId", userId);
					neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> m) {
							message.reply(m.body());
						}
					});
				} else {
					sendError(message, "Invalid profile.");
				}
			}
		});
	}

	public void deleteUser(final Message<JsonObject> message) {
		final JsonArray users = message.body().getJsonArray("users");
		if (users == null || users.size() == 0) {
			sendError(message, "Missing users.");
			return;
		}
		String query =
				"MATCH (u:User)" +
						"WHERE u.id IN {users} AND (u.source IN ['MANUAL', 'CSV', 'CLASS_PARAM', 'BE1D'] OR HAS(u.disappearanceDate)) " +
						"return u.id as id, u.login as login, u.loginAlias as loginAlias, has(u.activationCode) as inactive ";
		neo4j.execute(query, new JsonObject().put("users", users), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null) {
					if (users.size() == res.size()) {
						final Set<String> oldLogins = new HashSet<>();
						final JsonArray deleteUsers = new JsonArray();
						executeTransaction(message, tx -> {
									for (Object o : res) {
										if (!(o instanceof JsonObject)) continue;
										final JsonObject j = (JsonObject) o;
										final String id = j.getString("id");
										if (isNotEmpty(id)) {
											User.backupRelationship(id, tx);
											User.preDelete(id, tx);
											if (j.getBoolean("inactive", false)) {
												oldLogins.add(j.getString("login"));
												if (isNotEmpty(j.getString("loginAlias"))) {
													oldLogins.add(j.getString("loginAlias"));
												}
												deleteUsers.add(id);
											}
										}
									}
									if (deleteUsers.size() > 0) {
										User.getDelete(deleteUsers, tx);
										User.delete(deleteUsers, tx);
									}
								},
								m -> {
									final JsonArray results = m.body().getJsonArray("results");
									if ("ok".equals(m.body().getString("status")) && deleteUsers.size() > 0 && results != null &&
											(results.size() - 2) > 0) {
										final JsonArray r = results.getJsonArray(results.size() - 2);
										User.DeleteTask.publishDeleteUsers(eb, eventStore, r);
										Validator.removeLogins(oldLogins);
									}
									message.reply(m.body());
								});
					} else {
						sendError(message, "unauthorized.user");
					}
				} else {
					message.reply(event.body());
				}
			}
		});
	}

	public void restoreUser(final Message<JsonObject> message) {
		final JsonArray users = message.body().getJsonArray("users");
		if (users == null || users.size() == 0) {
			sendError(message, "Missing users.");
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				for (Object o : users) {
					User.restorePreDeleted(o.toString(), tx);
				}
			}
		});
	}

	public void createFunction(final Message<JsonObject> message) {
		final JsonObject function = getMandatoryObject("data", message);
		if (function == null) return;
		final String profile = message.body().getString("profile", "");
		if (!profiles.containsKey(profile)) {
			sendError(message, "Invalid profile : " + profile);
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.createFunction(profile, null, function, tx);
			}
		});
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f) {
		executeTransaction(message, f, event -> message.reply(event.body()));
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f, Handler<Message<JsonObject>> h) {
		TransactionHelper tx;
		try {
			tx = TransactionManager.getInstance().begin();
			f.apply(tx);
			tx.commit(h);
		} catch (TransactionException | ValidationException e) {
			sendError(message, e.getMessage(), e);
		}
	}

	public void deleteFunction(final Message<JsonObject> message) {
		final String functionCode = getMandatoryString("functionCode", message);
		if (functionCode == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.deleteFunction(functionCode, tx);
			}
		});
	}

	public void createFunctionGroup(Message<JsonObject> message) {
		final JsonArray functions = message.body().getJsonArray("functions");
		if (functions == null || functions.size() == 0) {
			sendError(message, "missing.functions");
			return;
		}
		final String name = message.body().getString("name");
		if (name == null || name.trim().isEmpty()) {
			sendError(message, "missing.name");
			return;
		}
		final String externalId = message.body().getString("externalId");
		if (externalId == null || externalId.trim().isEmpty()) {
			sendError(message, "missing.externalId");
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.createFunctionGroup(functions, name, externalId, tx);
			}
		});
	}

	public void deleteFunctionGroup(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		if (groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.deleteFunctionGroup(groupId, tx);
			}
		});
	}

	public void addUserFunction(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String function = message.body().getString("function");
		if (userId == null || function == null) return;
		final JsonArray scope = message.body().getJsonArray("scope");
		String inherit =  message.body().getString("inherit", "");
		if (scope != null && ("s".equals(inherit) || "sc".equals(inherit))) {
			String query;
			if ("sc".equals(inherit)) {
				query = "MATCH (s:Structure)<-[:HAS_ATTACHMENT*0..]-(:Structure)<-[:BELONGS*0..1]-(scope) " +
						"WHERE s.id IN {scope} " +
						"RETURN COLLECT(scope.id) as ids ";
			} else {
				query = "MATCH (s:Structure)<-[:HAS_ATTACHMENT*0..]-(scope:Structure) " +
						"WHERE s.id IN {scope} " +
						"RETURN COLLECT(scope.id) as ids ";
			}
			neo4j.execute(query, new JsonObject().put("scope", scope), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray result = event.body().getJsonArray("result");
					if ("ok".equals(event.body().getString("status")) && result != null && result.size() == 1) {
						final JsonArray s = result.getJsonObject(0).getJsonArray("ids");
						executeTransaction(message, new VoidFunction<TransactionHelper>() {
							@Override
							public void apply(TransactionHelper tx) {
								User.addFunction(userId, function, s, tx);
							}
						});
					} else {
						sendError(message, "invalid.scope");
					}
				}
			});
		} else {
			executeTransaction(message, new VoidFunction<TransactionHelper>() {
				@Override
				public void apply(TransactionHelper tx) {
					User.addFunction(userId, function, scope, tx);
				}
			});
		}
	}

	public void addUserHeadTeacherManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String classExternalId = message.body().getString("classExternalId");
		final String structureExternalId = message.body().getString("structureExternalId");
		final String structureName = message.body().getString("structureName");
		if (userId == null || classExternalId == null || structureExternalId == null) return;
		final String structureGroupHTExternalId = structureExternalId + "-ht";
		final String classGroupHTExternalId = classExternalId + "-ht";
		final String classGroupHTName = classExternalId.substring(structureExternalId.length()+1) + "-ht";
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.addHeadTeacherManual(userId,structureExternalId,classExternalId,structureGroupHTExternalId,
						classGroupHTExternalId,	classGroupHTName,structureName,tx);
			}
		});
	}

	public void updateUserHeadTeacherManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String classExternalId = message.body().getString("classExternalId");
		final String structureExternalId = message.body().getString("structureExternalId");
		if (userId == null || classExternalId == null || structureExternalId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.updateHeadTeacherManual(userId, structureExternalId,classExternalId, tx);
			}
		});
	}

	public void removeUserFunction(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String function = message.body().getString("function");
		if (userId == null || function == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.removeFunction(userId, function, tx);
			}
		});
	}

	public void addUserGroup(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String groupId = message.body().getString("groupId");
		if (userId == null || groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.addGroup(userId, groupId, tx);
			}
		});
	}

	public void removeUserGroup(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String groupId = message.body().getString("groupId");
		if (userId == null || groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.removeGroup(userId, groupId, tx);
			}
		});
	}

	public void createOrUpdateTenant(Message<JsonObject> message) {
		final JsonObject tenant = getMandatoryObject("data", message);
		if (tenant == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Tenant.createOrUpdate(tenant, tx);
			}
		});
	}

	public void createGroup(Message<JsonObject> message) {
		final JsonObject group = message.body().getJsonObject("group");
		if (group == null || group.size() == 0) {
			sendError(message, "missing.group");
			return;
		}
		final String structureId = message.body().getString("structureId");
		final String classId = message.body().getString("classId");
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Group.manualCreateOrUpdate(group, structureId, classId, tx);
			}
		});
	}

	public void deleteGroup(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		if (groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Group.manualDelete(groupId, tx);
			}
		});
	}

	public void addGroupUsers(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		final JsonArray userIds = message.body().getJsonArray("userIds");

		if (userIds == null || groupId == null) return;

		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Group.addUsers(groupId, userIds, tx);
			}
		});
	}

	public void removeGroupUsers(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		final JsonArray userIds = message.body().getJsonArray("userIds");

		if (userIds == null || groupId == null) return;

		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Group.removeUsers(groupId, userIds, tx);
			}
		});
	}

	public void updateEmailGroup(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		final String email = getMandatoryString("email", message);

		if (email == null || groupId == null) return;

		executeTransaction(message, tx -> {
			Group.updateEmail(groupId, email, tx);
		});
	}

	public void structureAttachment(Message<JsonObject> message) {
		final String structureId = getMandatoryString("structureId", message);
		final String parentStructureId = getMandatoryString("parentStructureId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Structure.addAttachment(structureId, parentStructureId, tx);
			}
		});
	}

	public void structureDetachment(Message<JsonObject> message) {
		final String structureId = getMandatoryString("structureId", message);
		final String parentStructureId = getMandatoryString("parentStructureId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Structure.removeAttachment(structureId, parentStructureId, tx);
			}
		});
	}

	public void updateStructure(final Message<JsonObject> message) {
		JsonObject s = getMandatoryObject("data", message);
		if (s == null) return;
		String structureId = getMandatoryString("structureId", message);
		if (structureId == null) return;
		final String error = structureValidator.modifiableValidate(s);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query;
			JsonObject params = s.copy().put("structureId", structureId);
			if (s.getString("name") != null) {
				query = "MATCH (s:`Structure` { id : {structureId}})<-[:DEPENDS]-(g:Group) " +
						"WHERE last(split(g.name, '-')) IN ['Student','Teacher','Personnel','Relative','Guest','AdminLocal','HeadTeacher', 'SCOLARITE'] " +
						"SET g.name = {name} + '-' + last(split(g.name, '-')), g.displayNameSearchField = {sanitizeName}, ";
				params.put("sanitizeName", Validator.sanitize(s.getString("name")));
			}else{
				query = "MATCH (s:`Structure` { id : {structureId}}) SET";

			}
			query = query + Neo4jUtils.nodeSetPropertiesFromJson("s", s) +
					"RETURN DISTINCT s.id as id ";


			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void relativeStudent(Message<JsonObject> message) {
		final String relativeId = getMandatoryString("relativeId", message);
		final String studentId = getMandatoryString("studentId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				User.relativeStudent(relativeId, studentId, tx);
			}
		});
	}

	public void unlinkRelativeStudent(Message<JsonObject> message) {
		final String relativeId = getMandatoryString("relativeId", message);
		final String studentId = getMandatoryString("studentId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				User.unlinkRelativeStudent(relativeId, studentId, tx);
			}
		});
	}
}
