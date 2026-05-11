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

import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.common.schema.Source;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.position.UserPositionService;
import org.entcore.common.utils.Id;
import org.entcore.common.utils.StringUtils;
import org.entcore.feeder.dictionary.structures.*;
import org.entcore.feeder.dictionary.structures.User.DeleteTask;
import org.entcore.feeder.dto.AddUserDTO;
import org.entcore.feeder.dto.AddUsersDTO;
import org.entcore.feeder.dto.CreateClassDTO;
import org.entcore.feeder.mapper.ClassMapper;
import org.entcore.feeder.dto.CreateStructureDTO;
import org.entcore.feeder.dto.CreateUserDTO;
import org.entcore.feeder.dto.CreateFunctionDTO;
import org.entcore.feeder.dto.CreateGroupDTO;
import org.entcore.feeder.dto.DeleteFunctionDTO;
import org.entcore.feeder.dto.DeleteFunctionGroupDTO;
import org.entcore.feeder.dto.AddGroupUsersDTO;
import org.entcore.feeder.dto.DeleteGroupDTO;
import org.entcore.feeder.dto.RemoveGroupUsersDTO;
import org.entcore.feeder.dto.RelativeStudentDTO;
import org.entcore.feeder.dto.UnlinkRelativeStudentDTO;
import org.entcore.feeder.dto.DeleteUserDTO;
import org.entcore.feeder.mapper.FunctionMapper;
import org.entcore.feeder.dto.RemoveClassDTO;
import org.entcore.feeder.dto.RemoveUserDTO;
import org.entcore.feeder.dto.RemoveUsersDTO;
import org.entcore.feeder.dto.RestoreUserDTO;
import org.entcore.feeder.mapper.StructureMapper;
import org.entcore.feeder.dto.UpdateClassDTO;
import org.entcore.feeder.dto.UpdateStructureDTO;
import org.entcore.feeder.dto.UpdateUserDTO;
import org.entcore.feeder.dto.UpdateUserLoginDTO;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.StatementsBuilder;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.feeder.utils.VoidFunction;
import org.vertx.java.busmods.BusModBase;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;


public class ManualFeeder extends BusModBase {

	public static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private static final Validator structureValidator = new Validator("dictionary/schema/Structure.json");
	private static final Validator classValidator = new Validator("dictionary/schema/Class.json");
	public static final Map<String, Validator> profiles;
	private final Neo4j neo4j;
	private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
	public static final String SOURCE = "MANUAL";
	private final UserPositionService userPositionService;
	private Boolean loginAliasValidatorForAD;

	static {
		Map<String, Validator> p = new HashMap<>();
		p.put("Personnel", new Validator("dictionary/schema/Personnel.json"));
		p.put("Teacher", new Validator("dictionary/schema/Personnel.json"));
		p.put("Student", new Validator("dictionary/schema/Student.json"));
		p.put("Relative", new Validator("dictionary/schema/User.json"));
		p.put("Guest", new Validator("dictionary/schema/User.json"));
		profiles = Collections.unmodifiableMap(p);
	}

	public ManualFeeder(Neo4j neo4j, EventBus eb, final UserPositionService userPositionService) {
		this.neo4j = neo4j;
		this.eb = eb;
		this.userPositionService = userPositionService;
	}

	public void createStructure(final CreateStructureDTO dto, final Handler<JsonObject> replyHandler) {
		JsonObject struct = StructureMapper.toStructureProps(dto);
		final Integer transactionId = dto.getTransactionId();
		final boolean commit = Boolean.TRUE.equals(dto.getCommit());
		if (struct.getString("externalId") == null) {
			struct.put("externalId", UUID.randomUUID().toString());
		}
		if (struct.getString("timetable") == null) {
			struct.put("timetable", "");
		}
		final String error = structureValidator.validate(struct);
		if (error != null) {
			logger.error(error);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
			return;
		}
		StatementsBuilder statementsBuilder = new StatementsBuilder();
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
		statementsBuilder.add(query, params);
		neo4j.executeTransaction(statementsBuilder.build(), transactionId, commit, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
					replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
				} else {
					replyHandler.handle(event.body());
				}
			}
		});
	}

	public void createClass(final CreateClassDTO dto, final Handler<JsonObject> replyHandler) {
		final String structureId = dto.getStructureId();
		if (structureId == null || structureId.trim().isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId must be specified"));
			return;
		}
		JsonObject c = ClassMapper.toClassProps(dto);
		c.put("externalId", structureId + "$" + dto.getName());
		final Integer transactionId = dto.getTransactionId();
		final boolean commit = Boolean.TRUE.equals(dto.getCommit());
		final String error = classValidator.validate(c);
		if (error != null) {
			logger.error(error);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
			return;
		}
		StatementsBuilder statementsBuilder = new StatementsBuilder();
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
		statementsBuilder.add(query, params);
		neo4j.executeTransaction(statementsBuilder.build(), transactionId, commit, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
					replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
				} else {
					replyHandler.handle(event.body());
				}
			}
		});
	}

	public void updateClass(final UpdateClassDTO dto, final Handler<JsonObject> replyHandler) {
		final String classId = dto.getClassId();
		if (classId == null || classId.trim().isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "classId must be specified"));
			return;
		}
		JsonObject c = ClassMapper.toClassProps(dto);
		final String error = classValidator.modifiableValidate(c);
		if (error != null) {
			logger.error(error);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
			return;
		}
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
				replyHandler.handle(m.body());
			}
		});
	}

	public void removeClass(final RemoveClassDTO dto, final Handler<JsonObject> replyHandler) {
		final String classId = dto.getClassId();
		if (StringUtils.isEmpty(classId)) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "classId must be specified"));
			return;
		}
		String queryUserIds =
			"MATCH (c:`Class` {id: {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
			"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), cpg<-[:IN]-(m:User)-[:IN]->spg " +
			"RETURN COLLECT(m.id) as ids ";
		neo4j.execute(queryUserIds, new JsonObject().put("classId", classId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				try {
					TransactionHelper tx = TransactionManager.getTransaction();
					JsonArray res = r.body().getJsonArray("result");
					if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
						final JsonArray userIds = res.getJsonObject(0).getJsonArray("ids");
						if (userIds != null && !userIds.isEmpty()) {
							final JsonArray classIds = new JsonArray();
							userIds.forEach(u -> classIds.add(classId));
							prepareRemovingUsersFromClasses(tx, userIds, classIds);
						}
					}
					prepareRemovingProfileGroupsOfClass(tx, classId);
					prepareRemovingClass(tx, classId);
					tx.commit(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							final JsonArray results = event.body().getJsonArray("results");
							if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
								JsonArray r = getOrElse(results.getJsonArray(0), new JsonArray());
								if (r != null && !r.isEmpty()) {
									Transition.publishDeleteGroups(eb, logger, r);
								}
							}
							replyHandler.handle(event.body());
						}
					});
				} catch (TransactionException e) {
					logger.error("Error in transaction while removing class", e);
					replyHandler.handle(new JsonObject().put("status", "error").put("message", "transaction.error"));
				}
			}
		});
	}

	private void prepareRemovingProfileGroupsOfClass(TransactionHelper tx, String classId) {
		String query =
			"MATCH (c:Class {id: {classId}})<-[:DEPENDS]-(cpg:Group) " +
			"OPTIONAL MATCH cpg<-[:IN]-(u:User) " +
			"WITH cpg, cpg.id as groupId, cpg.name as groupName, collect(u.id) as userIds " +
			"DETACH DELETE cpg " +
			"RETURN groupId as group, groupName as groupName, userIds as users ";
		tx.add(query, new JsonObject().put("classId", classId));
	}

	private void prepareRemovingClass(TransactionHelper tx, String classId) {
		String query =
			"MATCH (c:Class {id: {classId}}) " +
			"DETACH DELETE c ";
		tx.add(query, new JsonObject().put("classId", classId));
	}

	public void createUser(final CreateUserDTO dto, final Handler<JsonObject> replyHandler) {
		final JsonObject user = dto.getData().toJson();
		if (user.getString("externalId") == null) {
			user.put("externalId", UUID.randomUUID().toString());
		}
		final String profile = dto.getProfile() != null ? dto.getProfile() : "";
		if (!profiles.containsKey(profile)) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid profile : " + profile));
			return;
		}
		JsonArray childrenIds = null;
		if ("Relative".equals(profile)) {
			childrenIds = user.getJsonArray("childrenIds");
		}
		JsonArray userPositionIds = user.getJsonArray("userPositionIds");
		if (userPositionIds != null && !userPositionIds.isEmpty() && !"Personnel".equals(profile)) {
			logger.warn("Cannot create a user with profile {0} and positions", profile);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "user.profiles.not.allowed.for.profile.at.creation"));
			return;
		}
		final String userSource = "SSO".equals(user.getString("source")) ? "SSO" : SOURCE;
		final String error = profiles.get(profile).validate(user);
		if (error != null) {
			logger.error(error);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
			return;
		}
		user.put("source", userSource);
		final String structureId = dto.getStructureId();
		if (structureId != null && !structureId.trim().isEmpty()) {
			final List<String> classesNamesList = dto.getClassesNames();
			final JsonArray classesNames = classesNamesList != null ? new JsonArray(classesNamesList) : null;
			createUserInStructure(dto, replyHandler, user, profile, structureId, childrenIds, classesNames, userPositionIds);
			return;
		}
		final String classId = dto.getClassId();
		if (classId != null && !classId.trim().isEmpty()) {
			createUserInClass(dto, replyHandler, user, profile, classId, childrenIds);
			return;
		}
		replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId or classId must be specified"));
	}

	private void createUserInStructure(final CreateUserDTO dto, final Handler<JsonObject> replyHandler,
			final JsonObject user, String profile, String structureId, JsonArray childrenIds,
			JsonArray classesNames, JsonArray userPositionIds) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
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
				"RETURN DISTINCT u.id as id, u.login AS login";
		statementsBuilder.add(query, params);
		if (classesNames != null && !classesNames.isEmpty()) {
			final String classesQuery =
					"MATCH (s:Structure {id:{structureId}})<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(cpg:ProfileGroup {filter: {profile}}), " +
					"s<-[:DEPENDS]-(:ProfileGroup {filter: {profile}})<-[:IN]-(u:User {id: {userId}})" +
					"WHERE c.name IN {classesNames} " +
					"MERGE cpg<-[:IN {source: {source}}]-u ";
			final JsonObject classesParams = new JsonObject()
					.put("structureId", structureId)
					.put("profile", profile)
					.put("userId", user.getString("id"))
					.put("classesNames", classesNames)
					.put("source", user.getString("source"));
			statementsBuilder.add(classesQuery, classesParams);
		}
		final Promise<Void> promise = Promise.promise();
		if (userPositionIds == null) {
			promise.complete();
		} else {
			userPositionService.getUserPositionSettingQueryAndParam(
				userPositionIds.stream().map(id -> (String) id).collect(Collectors.toSet()),
				user.getString("id"),
				dto.getCallerId())
			.onSuccess(queryAndParams -> {
				statementsBuilder.add(queryAndParams.getQuery(), queryAndParams.getParams());
				promise.complete();
			})
			.onFailure(promise::fail);
		}
		promise.future().onSuccess(e -> {
			neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
						eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().put("new-user", user.getString("id")));
					} else {
						replyHandler.handle(event.body());
					}
				}
			});
		}).onFailure(th -> {
			logger.warn("An error occurred when trying to create user positions update query", th);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Unknown error"));
		});
	}

	public void addUser(final AddUserDTO dto, final Handler<JsonObject> replyHandler) {
		final String userId = dto.getUserId();
		if (userId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userId must be specified"));
			return;
		}
		final String structureId = dto.getStructureId();
		final String classId = dto.getClassId();
		if (structureId != null && !structureId.trim().isEmpty()) {
			TransactionHelper tx = TransactionHelper.DEFAULT;
			new org.entcore.common.schema.users.User(userId)
				.attach(tx, new Id<org.entcore.common.schema.structures.Structure, String>(structureId))
				.onSuccess(rawRes -> {
					final JsonArray res = (JsonArray) rawRes;
					JsonArray allResults;
					JsonArray singleResult;
					if (res.size() == 0) {
						allResults = new JsonArray().add(res);
						singleResult = res;
					} else {
						Object first = res.getValue(0);
						if (first instanceof JsonArray) {
							allResults = res;
							singleResult = res.size() == 1 ? (JsonArray) first : null;
						} else {
							allResults = new JsonArray().add(res);
							singleResult = res;
						}
					}
					replyHandler.handle(new JsonObject().put("status", "ok").put("results", allResults).put("result", singleResult));
				})
				.onFailure(rawT -> {
					final Throwable t = (Throwable) rawT;
					replyHandler.handle(new JsonObject().put("status", "error").put("message", t.getMessage()));
				});
			tx.commit();
		} else if (classId != null && !classId.trim().isEmpty()) {
			addUserInClass(replyHandler, userId, classId);
		} else {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId or classId must be specified"));
		}
	}


	public void addUsers(final AddUsersDTO dto, final Handler<JsonObject> replyHandler) {
		final List<String> userIdsList = dto.getUserIds();
		if (userIdsList == null || userIdsList.isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userIds must be specified"));
			return;
		}
		final String structureId = dto.getStructureId();
		final String classId = dto.getClassId();
		if (structureId != null && !structureId.trim().isEmpty()) {
			TransactionHelper tx = new TransactionHelper(Neo4j.getInstance(), Source.MANUAL);
			for (String userId : userIdsList) {
				new org.entcore.common.schema.users.User(userId).attach(tx, new Id<org.entcore.common.schema.structures.Structure, String>(structureId));
			}
			tx.commit(res -> replyHandler.handle(res.body()));
		} else if (classId != null && !classId.trim().isEmpty()) {
			addUsersInClass(replyHandler, new JsonArray(userIdsList), classId);
		} else {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId or classId must be specified"));
		}
	}

	public void removeUser(final RemoveUserDTO dto, final Handler<JsonObject> replyHandler) {
		final String userId = dto.getUserId();
		if (userId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userId must be specified"));
			return;
		}
		final String structureId = dto.getStructureId();
		final String classId = dto.getClassId();
		if (structureId != null && !structureId.trim().isEmpty()) {
			new org.entcore.common.schema.users.User(userId)
				.dettach(TransactionHelper.DEFAULT, new Id<org.entcore.common.schema.structures.Structure, String>(structureId))
				.onSuccess(rawRes -> {
					final JsonArray res = (JsonArray) rawRes;
					JsonArray allResults;
					JsonArray singleResult;
					if (res.size() == 0) {
						allResults = new JsonArray().add(res);
						singleResult = res;
					} else {
						Object first = res.getValue(0);
						if (first instanceof JsonArray) {
							allResults = res;
							singleResult = res.size() == 1 ? (JsonArray) first : null;
						} else {
							allResults = new JsonArray().add(res);
							singleResult = res;
						}
					}
					replyHandler.handle(new JsonObject().put("status", "ok").put("results", allResults).put("result", singleResult));
				})
				.onFailure(rawT -> {
					final Throwable t = (Throwable) rawT;
					replyHandler.handle(new JsonObject().put("status", "error").put("message", t.getMessage()).put("error", t.getMessage()));
				});
		} else if (classId != null && !classId.trim().isEmpty()) {
			removeUserFromClass(replyHandler, userId, classId);
		} else {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId or classId must be specified"));
		}
	}

	public void removeUsers(final RemoveUsersDTO dto, final Handler<JsonObject> replyHandler) {
		final List<String> userIdsList = dto.getUserIds();
		if (userIdsList == null || userIdsList.isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userIds must be specified"));
			return;
		}
		final JsonArray userIds = new JsonArray(userIdsList);
		final String structureId = dto.getStructureId();
		if (structureId != null && !structureId.trim().isEmpty()) {
			removeUsersFromStructure(replyHandler, userIds, structureId);
			return;
		}
		final List<String> classIdsList = dto.getClassIds();
		if (classIdsList != null && !classIdsList.isEmpty()) {
			final JsonArray classIds = new JsonArray(classIdsList);
			if (classIds.size() != userIds.size()) {
				replyHandler.handle(new JsonObject().put("status", "error").put("message", "userIds and classIds Array must have same number of elements"));
				return;
			}
			removeUsersFromClass(replyHandler, userIds, classIds);
			return;
		}
		replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId or classIds must be specified"));
	}

	public static void applyRemoveUserFromStructure(String userId, String userExternalId,
		String newStructureId, String newStructureExternalId, TransactionHelper tx)
	{
		boolean addNewStruct = newStructureId != null || newStructureExternalId != null;
		JsonObject params = new JsonObject()
				.put("structureId", newStructureId != null ? newStructureId : newStructureExternalId)
				.put("userId", userId != null ? userId : userExternalId);
		final String matchUser = "MATCH (u:User { " + (userId != null ? "id" : "externalId") + " : {userId}}) " +
								(addNewStruct == false ? "WHERE HAS(u.removedFromStructures) AND u.removedFromStructures <> [] WITH u " : "");
		final String matchStructure = addNewStruct == true
		  ? "MATCH (s:Structure { " + (newStructureId != null ? "id" : "externalId") + ": {structureId} }) "
			: "MATCH (s:Structure) " +
				"WHERE s.externalId IN u.removedFromStructures ";
		final String addToRemoved = addNewStruct == true
			? "WITH u, r, MAX(CASE WHEN type(r) = 'IN' AND coalesce(r.source, '') <> 'MANUAL' THEN s.externalId ELSE null END) AS sID " +
			"SET u.removedFromStructures = [rsId IN coalesce(u.removedFromStructures, []) WHERE rsId <> coalesce(sID, '')] + coalesce(sID, []) "
			: "";
		final String query =
						matchUser +
						matchStructure +
						"MATCH u-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS*0..1]->" +
						"(pg:ProfileGroup)-[:DEPENDS]->s, " +
						"pg-[:HAS_PROFILE]->(p:Profile), p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
						"MERGE dpg<-[:IN]-u " +
						"SET u.structures = FILTER(sId IN u.structures WHERE sId <> s.externalId), " +
						"u.classes = FILTER(cId IN u.classes WHERE NOT(cId =~ (s.externalId + '.*'))) " +
						addToRemoved +
						"DELETE r " +
						"RETURN DISTINCT u.id as id";
		final String removeFunctions =
				matchUser +
				matchStructure +
				"MATCH u-[r:HAS_FUNCTION]->() " +
				"WHERE s.id IN r.scope AND (NOT(HAS(r.source)) OR r.source <> 'MANUAL') " +
				"OPTIONAL MATCH (s)-[:HAS_ATTACHMENT*1..]->(ss:Structure) " +
						"WITH s, r, count(CASE WHEN ss.id IN r.scope THEN 1 ELSE NULL END) as parentADML " +
						"WHERE parentADML = 0 " +
						"SET r.scope = FILTER(sId IN r.scope WHERE sId <> s.id) " +
						"WITH r " +
						"WHERE LENGTH(r.scope) = 0 " +
						"DELETE r";
		final String removeFunctionGroups =
				matchUser +
				matchStructure +
				"MATCH u-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->s " +
						"WHERE NOT(HAS(r.source)) OR r.source <> 'MANUAL' " +
						"DELETE r";
		final String removeDefaultGroup =
				matchUser +
				"MATCH (u)-[indpg:IN]-(:DefaultProfileGroup) " +
				"MATCH (u)-[:IN]-(pg:ProfileGroup) " +
				"WHERE NOT (pg:DefaultProfileGroup)" +
				"DELETE indpg";
		tx.add(query, params);
		tx.add(removeFunctions, params);
		tx.add(removeFunctionGroups, params);
		tx.add(removeDefaultGroup, params);
	}

	private void removeUserFromStructure(final Message<JsonObject> message,
			String userId, String structureId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			ManualFeeder.applyRemoveUserFromStructure(userId, null, structureId, null, tx);
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

	private void removeUsersFromStructure(final Handler<JsonObject> replyHandler,
										 JsonArray userIds, String structureId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			for (Object userIdObj : userIds) {
				ManualFeeder.applyRemoveUserFromStructure(userIdObj.toString(), null, structureId, null, tx);
			}
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
					} else {
						replyHandler.handle(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from structure", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "transaction.error"));
		}
	}

	private void createUserInClass(final CreateUserDTO dto, final Handler<JsonObject> replyHandler,
			final JsonObject user, String profile, String classId, JsonArray childrenIds) {
		// userPositionIds must not be stored as a node property in the class path
		final JsonArray userPositionIds = (JsonArray) user.remove("userPositionIds");
		StatementsBuilder statementsBuilder = new StatementsBuilder();
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
				"RETURN DISTINCT u.id as id, u.login AS login";
		statementsBuilder.add(query, params);
		final Promise<Void> promise = Promise.promise();
		if (userPositionIds == null) {
			promise.complete();
		} else {
			userPositionService.getUserPositionSettingQueryAndParam(
				userPositionIds.stream().map(id -> (String) id).collect(Collectors.toSet()),
				user.getString("id"),
				dto.getCallerId())
			.onSuccess(queryAndParams -> {
				statementsBuilder.add(queryAndParams.getQuery(), queryAndParams.getParams());
				promise.complete();
			}).onFailure(promise::fail);
		}
		promise.future().onSuccess(e -> {
			neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
						eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().put("new-user", user.getString("id")));
					} else {
						replyHandler.handle(event.body());
					}
				}
			});
		}).onFailure(th -> {
			logger.warn("An error occurred while creating user position update query", th);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Unknown error"));
		});
	}

	private void addUserInClass(final Handler<JsonObject> replyHandler, String userId, String classId) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
		JsonObject params = new JsonObject()
				.put("classId", classId)
				.put("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WITH u, p " +
						"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
						"(pg:ProfileGroup)-[:HAS_PROFILE]->p, s-[:BELONGS]->(struct:Structure) " +
						"MERGE (pg)<-[inProfileGroup:IN]-(u) " +
						"CREATE UNIQUE (cpg)<-[:IN {source:'MANUAL'}]-(u) " +
						"SET u.classes = CASE WHEN s.externalId IN u.classes THEN " +
						"u.classes ELSE coalesce(u.classes, []) + s.externalId END, " +
						"u.structures = CASE WHEN struct.externalId IN u.structures THEN " +
						"u.structures ELSE coalesce(u.structures, []) + struct.externalId END, " +
						"inProfileGroup.source = CASE WHEN inProfileGroup.source = 'MANUAL' THEN 'MANUAL' ELSE null END " +
						"RETURN DISTINCT u.id as id";
		statementsBuilder.add(query, params);
		neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
					replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
				} else {
					replyHandler.handle(event.body());
				}
			}
		});
	}

	private void addUsersInClass(final Handler<JsonObject> replyHandler, JsonArray userIds, String classId) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
		for (Object userId : userIds.getList()) {
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
		neo4j.executeTransaction(statementsBuilder.build(), null, true, res -> replyHandler.handle(res.body()));
	}

	private void removeUserFromClass(final Handler<JsonObject> replyHandler,
									 String userId, String classId) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();

			JsonObject params = new JsonObject()
					.put("classId", classId)
					.put("userId", userId);
			String query =
					"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS]->(c:Class  {id : {classId}}) " +
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
						replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
					} else {
						replyHandler.handle(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove user from class", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "transaction.error"));
		}
	}

	private void removeUsersFromClass(final Handler<JsonObject> replyHandler,
									 JsonArray userIds, JsonArray classIds) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			prepareRemovingUsersFromClasses(tx, userIds, classIds);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
						replyHandler.handle(event.body().put("result", results.getJsonArray(0)));
					} else {
						replyHandler.handle(event.body());
					}
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when remove users from class", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "transaction.error"));
		}
	}

	protected void prepareRemovingUsersFromClasses(TransactionHelper tx, JsonArray userIds, JsonArray classIds) {
		for(int i=0; i < userIds.size(); i++) {
			final String userId = userIds.getString(i);
			final Object classIdsForUserId = classIds.getValue(i);
			JsonArray currentClassIds = classIdsForUserId instanceof JsonArray? (JsonArray) classIdsForUserId: new JsonArray().add(classIdsForUserId.toString());
			for(Object classIdObj : currentClassIds) {
				JsonObject params = new JsonObject()
						.put("classId", classIdObj.toString())
						.put("userId", userId);
				String query =
						"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS]->(c:Class  {id : {classId}}) " +
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
	}

	public void updateUser(final UpdateUserDTO dto, final Handler<JsonObject> replyHandler) {
		final String userId = dto.getUserId();
		if (userId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userId must be specified"));
			return;
		}
		final String callerId = dto.getCallerId();
		final JsonObject user = dto.getData().toJson();
		final Boolean useLoginAliasValidatorForAD = this.loginAliasValidatorForAD;
		String q =
				"MATCH (u:User { id : {userId}})-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN DISTINCT p.name as profile, u.login as login, u.loginAlias as loginAlias ";
		neo4j.execute(q, new JsonObject().put("userId", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getJsonArray("result");
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() > 0) {
					StatementsBuilder statementsBuilder = new StatementsBuilder();
					final JsonArray userPositionIds = (JsonArray) user.remove("positionIds");
					Set<String> oldLogins = new HashSet<String>();
					String updatedLoginAlias = user.getString("loginAlias");
					final JsonArray deletedAlias = new JsonArray();
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String profile = ((JsonObject) o).getString("profile");
						Validator v = profiles.get(profile);
						if (v == null) {
							replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid profile : " + profile));
							return;
						}
						String error = null;
						if (updatedLoginAlias != null) {
							if (updatedLoginAlias.equals(((JsonObject) o).getString("login"))) {
								user.putNull("loginAlias");
							} else if (useLoginAliasValidatorForAD) {
								error = Validator.validAdLoginAlias("loginAlias", updatedLoginAlias, "AdLoginAlias", "fr", I18n.getInstance(), false);
							}
						}
						error = (error == null) ? v.modifiableValidate(user) : error;
						if (error != null) {
							logger.error(error);
							replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
							return;
						}
						if (updatedLoginAlias != null) {
							String oldAlias = ((JsonObject) o).getString("loginAlias");
							String login = ((JsonObject) o).getString("login");
							if (oldAlias != null && !oldAlias.isEmpty()) {
								oldLogins.add(oldAlias);
								deletedAlias.add(new JsonObject().put("event-type", "DELETED_ALIAS").put("type", profile)
										.put("login", login).put("loginAlias", oldAlias).put("id", userId));
							}
							if (!updatedLoginAlias.equals(login) && !updatedLoginAlias.isEmpty()) {
								deletedAlias.add(new JsonObject().put("event-type", "ADDED_ALIAS").put("type", profile)
										.put("login", login).put("loginAlias", updatedLoginAlias).put("id", userId));
							}
						}
					}
					user.put("checksum", "manual");
					String query =
							"MATCH (u:User { id : {userId}}) " +
							"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", user) +
							"RETURN DISTINCT u.id as id ";
					JsonObject params = user.put("userId", userId);
					statementsBuilder.add(query, params);
					final Promise<Void> promise = Promise.promise();
					if (userPositionIds == null) {
						promise.complete();
					} else {
						userPositionService.getUserPositionSettingQueryAndParam(
							userPositionIds.stream().map(id -> (String) id).collect(Collectors.toSet()),
							userId,
							callerId)
						.onSuccess(queryAndParams -> {
							statementsBuilder.add(queryAndParams.getQuery(), queryAndParams.getParams());
							promise.complete();
						})
						.onFailure(promise::fail);
					}
					promise.future().onSuccess(e -> {
						neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> m) {
								Validator.removeLogins(oldLogins);
								DeleteTask.storeDeleteUserEvent(eventStore, deletedAlias);
								final JsonObject body = m.body();
								final JsonArray results = (JsonArray) body.remove("results");
								if (results != null && !results.isEmpty()) {
									body.put("result", results.getValue(0));
								}
								replyHandler.handle(body);
							}
						});
					}).onFailure(th -> {
						logger.warn("An error occurred while creating user position update query", th);
						replyHandler.handle(new JsonObject().put("status", "error").put("message", "Unknown error"));
					});
				} else {
					replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid profile."));
				}
			}
		});
	}

	public void updateUserLogin(final UpdateUserLoginDTO dto, final Handler<JsonObject> replyHandler) {
		final String userId = dto.getUserId();
		final String newLogin = dto.getLogin();
		if (userId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userId must be specified"));
			return;
		}
		if (newLogin == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "login must be specified"));
			return;
		}
		String q =
				"MATCH (u:User { id : {userId}})-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN DISTINCT p.name as profile, u.login as login ";
		neo4j.execute(q, new JsonObject().put("userId", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getJsonArray("result");
				JsonArray loginChangeEvents = new JsonArray();
				Set<String> oldLogins = new HashSet<String>();
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() > 0) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String profile = ((JsonObject) o).getString("profile");
						String oldLogin = ((JsonObject) o).getString("login");
						if (oldLogin == null) {
							logger.error("Error reading old user login for user " + userId);
							replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid user"));
							return;
						}
						if (!newLogin.equals(oldLogin) && !newLogin.isEmpty()) {
							oldLogins.add(oldLogin);
							loginChangeEvents.add(new JsonObject().put("event-type", "CHANGE_LOGIN").put("type", profile)
									.put("login", oldLogin).put("loginAlias", newLogin).put("id", userId));
						}
					}
					String loginError = Validator.validLogin(newLogin);
					if (loginError != null) {
						logger.error(loginError);
						replyHandler.handle(new JsonObject().put("status", "error").put("message", loginError));
						return;
					}
					String query = "MATCH (u:User {id: {userId}}) SET u.login = {login} RETURN u.id AS id";
					JsonObject params = new JsonObject().put("userId", userId).put("login", newLogin);
					neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> m) {
							Validator.removeLogins(oldLogins);
							DeleteTask.storeDeleteUserEvent(eventStore, loginChangeEvents);
							replyHandler.handle(m.body());
						}
					});
				} else {
					replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid user."));
				}
			}
		});
	}

	public void deleteUser(final DeleteUserDTO dto, final Handler<JsonObject> replyHandler) {
		final List<String> usersList = dto.getUsers();
		if (usersList == null || usersList.isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Missing users."));
			return;
		}
		final JsonArray users = new JsonArray(usersList);
		String query =
				"MATCH (u:User)" +
				"WHERE u.id IN {users} AND (u.source IN ['MANUAL', 'CSV', 'CLASS_PARAM', 'BE1D', 'SSO'] OR HAS(u.disappearanceDate)) " +
				"return u.id as id, u.externalId AS externalId, u.login as login, u.loginAlias as loginAlias, has(u.activationCode) as inactive ";
		neo4j.execute(query, new JsonObject().put("users", users), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null) {
					if (users.size() == res.size()) {
						final Set<String> oldLogins = new HashSet<>();
						final JsonArray deleteUsers = new JsonArray();
						try {
							TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
							tx.setAutoSend(true);
							for (Object o : res) {
								if (!(o instanceof JsonObject)) continue;
								final JsonObject j = (JsonObject) o;
								final String id = j.getString("id");
								if (isNotEmpty(id)) {
									User.backupRelationship(id, false, tx);
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
							tx.commit(m -> {
								final JsonArray results = m.body().getJsonArray("results");
								if ("ok".equals(m.body().getString("status")) && deleteUsers.size() > 0 && results != null &&
										(results.size() - 2) > 0) {
									final JsonArray r = results.getJsonArray(results.size() - 2);
									User.DeleteTask.publishDeleteUsers(eb, eventStore, r);
									Validator.removeLogins(oldLogins);
								}
								replyHandler.handle(m.body());
							});
						} catch (TransactionException e) {
							logger.error("Error in transaction when deleting users", e);
							replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
						}
					} else {
						replyHandler.handle(new JsonObject().put("status", "error").put("message", "unauthorized.user"));
					}
				} else {
					replyHandler.handle(event.body());
				}
			}
		});
	}

	public void restoreUser(final RestoreUserDTO dto, final Handler<JsonObject> replyHandler) {
		final List<String> usersList = dto.getUsers();
		if (usersList == null || usersList.isEmpty()) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Missing users."));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			for (String userId : usersList) {
				User.restorePreDeleted(userId, tx);
			}
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when restoring users", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void createFunction(final CreateFunctionDTO dto, final Handler<JsonObject> replyHandler) {
		final String profile = dto.getProfile() != null ? dto.getProfile() : "";
		if (!profiles.containsKey(profile)) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "Invalid profile : " + profile));
			return;
		}
		final JsonObject function = FunctionMapper.toFunctionData(dto);
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Profile.createFunction(profile, null, function, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when creating function", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f) {
		executeTransaction(message, f, event -> message.reply(event.body()));
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f,
									Integer transactionId, Boolean commit, Boolean autoSend) {
		executeTransaction(message, f, transactionId, commit, autoSend, event -> message.reply(event.body()));
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f,
									Handler<Message<JsonObject>> h) {
		executeTransaction(message, f, null, true, true, h);
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f,
									Integer transactionId, Boolean commit, Boolean autoSend,
									Handler<Message<JsonObject>> h) {
		TransactionHelper tx;
		try {
			tx = TransactionManager.getInstance().begin(transactionId);
			tx.setAutoSend(autoSend.booleanValue());
			f.apply(tx);
			if (commit.booleanValue()) {
				tx.commit(h);
			} else {
				tx.flush(h);
			}
		} catch (TransactionException | ValidationException e) {
			sendError(message, e.getMessage(), e);
		}
	}

	public void deleteFunction(final DeleteFunctionDTO dto, final Handler<JsonObject> replyHandler) {
		final String functionCode = dto.getFunctionCode();
		if (functionCode == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "functionCode must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Profile.deleteFunction(functionCode, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when deleting function", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void deleteFunctionGroup(final DeleteFunctionGroupDTO dto, final Handler<JsonObject> replyHandler) {
		final String groupId = dto.getGroupId();
		if (groupId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "groupId must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Profile.deleteFunctionGroup(groupId, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when deleting function group", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void addUserFunction(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String function = message.body().getString("function");
		if (userId == null || function == null) return;
		final Integer transactionId = message.body().getInteger("transactionId");
		final Boolean commit = message.body().getBoolean("commit", true);
		final Boolean autoSend = message.body().getBoolean("autoSend", true);
		final JsonArray scope = message.body().getJsonArray("scope");
		String inherit =  message.body().getString("inherit", "");
		StatementsBuilder statementsBuilder = new StatementsBuilder();
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
			JsonObject params = new JsonObject()
					.put("scope", scope);
			statementsBuilder.add(query, params);
			neo4j.executeTransaction(statementsBuilder.build(), transactionId, commit.booleanValue(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray result = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && result != null && result.size() == 1) {
						final JsonArray s = result.getJsonArray(0).getJsonObject(0).getJsonArray("ids");
						executeTransaction(message, new VoidFunction<TransactionHelper>() {
							@Override
							public void apply(TransactionHelper tx) {
								User.addFunction(userId, function, s, tx);
							}
						}, transactionId, commit, autoSend);
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
			}, transactionId, commit, autoSend);
		}
	}

	public void addUserHeadTeacherManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String classExternalId = message.body().getString("classExternalId");
		final String structureExternalId = message.body().getString("structureExternalId");
		if (userId == null || classExternalId == null || structureExternalId == null) return;

		try
		{
			TransactionHelper tx = TransactionManager.getTransaction();
			Structure.load(structureExternalId, tx, new Handler<Structure>()
			{
				@Override
				public void handle(Structure struct)
				{
						struct.createHeadTeacherGroupIfAbsent(classExternalId);
						User.addHeadTeacherManual(userId, structureExternalId,classExternalId, tx);

						tx.commit(new Handler<Message<JsonObject>>()
						{
							@Override
							public void handle(Message<JsonObject> event)
							{
								message.reply(event.body());
							}
						});
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when adding user to head teacher group", e);
			sendError(message, "transaction.error");
		}
	}

	public void updateUserHeadTeacherManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String classExternalId = message.body().getString("classExternalId");
		final String structureExternalId = message.body().getString("structureExternalId");
		if (userId == null || classExternalId == null || structureExternalId == null) return;

		try
		{
			TransactionHelper tx = TransactionManager.getTransaction();
			Structure.load(structureExternalId, tx, new Handler<Structure>()
			{
				@Override
				public void handle(Structure struct)
				{
						struct.createHeadTeacherGroupIfAbsent(classExternalId);
						User.updateHeadTeacherManual(userId, structureExternalId,classExternalId, tx);

						tx.commit(new Handler<Message<JsonObject>>()
						{
							@Override
							public void handle(Message<JsonObject> event)
							{
								message.reply(event.body());
							}
						});
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when updating user to head teacher group", e);
			sendError(message, "transaction.error");
		}
	}

	public void createManualSubject(final Message<JsonObject> message) {
		JsonObject subject = message.body().getJsonObject("subject");
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Subject.createManualSubject(subject, tx);
			}
		});

	}

	public void updateManualSubject(final Message<JsonObject> message) {
		JsonObject subject = message.body().getJsonObject("subject");
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Subject.updateManualSubject(subject, tx);
			}
		});

	}

	public void deleteManualSubject(final Message<JsonObject> message) {
		String subjectId = message.body().getString("subjectId");
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply (TransactionHelper tx) {
				Subject.deleteManualSubject(subjectId,tx);
			}
		});
	}

	public void addUserDirectionManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String structureExternalId = message.body().getString("structureExternalId");
		if (userId == null || structureExternalId == null) return;

		try
		{
			TransactionHelper tx = TransactionManager.getTransaction();
			Structure.load(structureExternalId, tx, new Handler<Structure>()
			{
				@Override
				public void handle(Structure struct)
				{
						struct.createDirectionGroupIfAbsent();
						User.addDirectionManual(userId,structureExternalId, tx);

						tx.commit(new Handler<Message<JsonObject>>()
						{
							@Override
							public void handle(Message<JsonObject> event)
							{
								message.reply(event.body());
							}
						});
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in transaction when adding user to direction group", e);
			sendError(message, "transaction.error");
		}
	}

	public void removeUserDirectionManual(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String structureExternalId = message.body().getString("structureExternalId");
		if (userId == null || structureExternalId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.removeDirectionManual(userId, structureExternalId, tx);
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

	public void createGroup(final CreateGroupDTO dto, final Handler<JsonObject> replyHandler) {
		final JsonObject group = dto.getGroup() != null ? dto.getGroup().toJson() : new JsonObject();
		if (group.size() == 0) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "missing.group"));
			return;
		}
		final String structureId = dto.getStructureId();
		final String classId = dto.getClassId();
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Group.manualCreateOrUpdate(group, structureId, classId, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException | ValidationException e) {
			logger.error("Error in transaction when creating/updating group", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void deleteGroup(final DeleteGroupDTO dto, final Handler<JsonObject> replyHandler) {
		final String groupId = dto.getGroupId();
		if (groupId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "groupId must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Group.manualDelete(groupId, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when deleting group", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}
	
	public void addGroupUsers(final AddGroupUsersDTO dto, final Handler<JsonObject> replyHandler) {
		final String groupId = dto.getGroupId();
		final List<String> userIds = dto.getUserIds();
		if (groupId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "groupId must be specified"));
			return;
		}
		if (userIds == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userIds must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Group.addUsers(groupId, new JsonArray(userIds), tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when adding users to group", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void removeGroupUsers(final RemoveGroupUsersDTO dto, final Handler<JsonObject> replyHandler) {
		final String groupId = dto.getGroupId();
		final List<String> userIds = dto.getUserIds();
		if (groupId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "groupId must be specified"));
			return;
		}
		if (userIds == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "userIds must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			Group.removeUsers(groupId, new JsonArray(userIds), tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when removing users from group", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
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
		final Integer transactionId = message.body().getInteger("transactionId");
		final Boolean commit = message.body().getBoolean("commit", true);
		final Boolean autoSend = message.body().getBoolean("autoSend", true);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Structure.addAttachment(structureId, parentStructureId, tx);
			}
		}, transactionId, commit, autoSend);
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

	public void updateStructure(final UpdateStructureDTO dto, final Handler<JsonObject> replyHandler) {
		if (dto.getStructureId() == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "structureId must be specified"));
			return;
		}
		JsonObject s = StructureMapper.toStructureProps(dto);
		final String structureId = dto.getStructureId();
		final String error = structureValidator.modifiableValidate(s);
		if (error != null) {
			logger.error(error);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", error));
			return;
		}
		String query;
		JsonObject params = s.copy().put("structureId", structureId);
		if (s.getString("name") != null) {
			query = "MATCH (s:`Structure` { id : {structureId}}) " +
					"SET s.manualName = ({name} <> s.name), s.name = {name} " +
					"WITH s " +
					"MATCH (s)<-[:DEPENDS]-(g:Group) " +
					"WHERE last(split(g.name, '-')) IN ['Student','Teacher','Personnel','Relative','Guest','AdminLocal','HeadTeacher', 'Direction', 'SCOLARITE'] " +
					"SET g.name = {name} + '-' + last(split(g.name, '-')), g.displayNameSearchField = {sanitizeName}, ";
			params.put("sanitizeName", Validator.sanitize(s.getString("name")));
		} else {
			query = "MATCH (s:`Structure` { id : {structureId}}) SET";
		}
		query = query + Neo4jUtils.nodeSetPropertiesFromJson("s", s) +
				"RETURN DISTINCT s.id as id ";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if (logger.isInfoEnabled()) {
					try {
						if (dto.getIgnoreMFA() != null) {
							logger.info(
								"ignoreMFA set to " + dto.getIgnoreMFA() +
								" at " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()) +
								" by user \"" + dto.getUserLogin() + "\" (id=" + dto.getUserId() + ") " +
								" on structure \"" + s.getString("name") + "\" (id=" + structureId + ")"
							);
						}
					} catch (Exception e) {
						logger.error("Unexpected error while logging ignoreMFA update: " + e.getMessage());
					}
				}
				replyHandler.handle(m.body());
			}
		});
	}

	public void relativeStudent(final RelativeStudentDTO dto, final Handler<JsonObject> replyHandler) {
		final String relativeId = dto.getRelativeId();
		final String studentId = dto.getStudentId();
		if (relativeId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "relativeId must be specified"));
			return;
		}
		if (studentId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "studentId must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			User.relativeStudent(relativeId, studentId, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when linking relative to student", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void unlinkRelativeStudent(final UnlinkRelativeStudentDTO dto, final Handler<JsonObject> replyHandler) {
		final String relativeId = dto.getRelativeId();
		final String studentId = dto.getStudentId();
		if (relativeId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "relativeId must be specified"));
			return;
		}
		if (studentId == null) {
			replyHandler.handle(new JsonObject().put("status", "error").put("message", "studentId must be specified"));
			return;
		}
		try {
			TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
			tx.setAutoSend(true);
			User.unlinkRelativeStudent(relativeId, studentId, tx);
			tx.commit(m -> replyHandler.handle(m.body()));
		} catch (TransactionException e) {
			logger.error("Error in transaction when unlinking relative from student", e);
			replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
		}
	}

	public void setLoginAliasValidatorForAD(Boolean loginAliasValidatorForAD) {
		this.loginAliasValidatorForAD = loginAliasValidatorForAD;
	}

    public void setManualGroupAutolinkUsersPositions(Message<JsonObject> message) {
        final String groupId = getMandatoryString("groupId", message);
        final JsonArray userPositions = message.body().getJsonArray("manualGroupAutolinkUsersPositions");

        if (userPositions == null || groupId == null) return;

        executeTransaction(message, tx -> Group.setManualGroupAutolinkUsersPositions(groupId, userPositions, tx));
    }

    public void updateManualGroupsByUserPositions(Message<JsonObject> message) {
        final String userPosition = getMandatoryString("userPosition", message);
        if (userPosition == null || userPosition.isEmpty()) return;

        executeTransaction(message, tx -> Group.updateManualGroupsByUserPositions(userPosition, tx));
    }
}
