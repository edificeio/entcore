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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;

import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.Directory;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.CLASS_ADMIN;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultUserService implements UserService {

	private final Neo4j neo = Neo4j.getInstance();
	private final EmailSender notification;
	private final EventBus eb;
	private Logger logger = LoggerFactory.getLogger(DefaultUserService.class);

	public DefaultUserService(EmailSender notification, EventBus eb) {
		this.notification = notification;
		this.eb = eb;
	}

	@Override
	public void createInStructure(String structureId, JsonObject user, Handler<Either<String, JsonObject>> result) {
		user.putArray("profiles", new JsonArray().add(user.getString("type")));
		JsonObject action = new JsonObject()
				.putString("action", "manual-create-user")
				.putString("structureId", structureId)
				.putString("profile", user.getString("type"))
				.putObject("data", user);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result) {
		user.putArray("profiles", new JsonArray().add(user.getString("type")));
		JsonObject action = new JsonObject()
				.putString("action", "manual-create-user")
				.putString("classId", classId)
				.putString("profile", user.getString("type"))
				.putObject("data", user);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void update(final String id, final JsonObject user, final Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-update-user")
				.putString("userId", id)
				.putObject("data", user);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void sendUserCreatedEmail(final HttpServerRequest request, String userId,
			final Handler<Either<String, Boolean>> result) {
		String query =
				"MATCH (u:`User` { id : {id}}) WHERE NOT(u.email IS NULL) AND NOT(u.activationCode IS NULL) " +
				"RETURN u.login as login, u.email as email, u.activationCode as activationCode ";
		JsonObject params = new JsonObject().putString("id", userId);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				Either<String, JsonObject> r = validUniqueResult(m);
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					String email = j.getString("email");
					String login = j.getString("login");
					String activationCode = j.getString("activationCode");
					if (email == null || login == null || activationCode == null ||
							email.trim().isEmpty() || login.trim().isEmpty() || activationCode.trim().isEmpty()) {
						result.handle(new Either.Left<String, Boolean>("user.invalid.values"));
						return;
					}
					JsonObject json = new JsonObject()
							.putString("activationUri", notification.getHost(request) +
									"/auth/activation?login=" + login +
									"&activationCode=" + activationCode)
							.putString("host", notification.getHost(request))
							.putString("login", login);
					logger.debug(json.encode());
					notification.sendEmail(request, email, null, null,
							"email.user.created.info", "email/userCreated.html", json, true,
							new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> message) {
									if ("ok".equals(message.body().getString("status"))) {
										result.handle(new Either.Right<String, Boolean>(true));
									} else {
										result.handle(new Either.Left<String, Boolean>(
												message.body().getString("message")));
									}
								}
							});
				} else {
					result.handle(new Either.Left<String, Boolean>(r.left().getValue()));
				}
			}
		});
	}

	@Override
	public void get(String id, boolean getManualGroups, Handler<Either<String, JsonObject>> result) {
		
		String getMgroups = "";
		String resultMgroups = "";
		if (getManualGroups) {
			getMgroups = "OPTIONAL MATCH u-[:IN]->(mgroup: ManualGroup) WITH COLLECT(distinct {id: mgroup.id, name: mgroup.name}) as manualGroups, admStruct, admGroups, parents, children, functions, u, structureNodes ";
			resultMgroups = "CASE WHEN manualGroups IS NULL THEN [] ELSE manualGroups END as manualGroups, ";
		}
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"OPTIONAL MATCH u-[:IN]->()-[:DEPENDS]->(s:Structure) WITH COLLECT(distinct s) as structureNodes, u " +
				"OPTIONAL MATCH u-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) WITH COLLECT(distinct [f.externalId, rf.scope]) as functions, u, structureNodes " +
				"OPTIONAL MATCH u<-[:RELATED]-(child: User) WITH COLLECT(distinct {id: child.id, displayName: child.displayName, externalId: child.externalId}) as children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:RELATED]->(parent: User) WITH COLLECT(distinct {id: parent.id, displayName: parent.displayName, externalId: parent.externalId}) as parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:IN]->(fgroup: FunctionalGroup) WITH COLLECT(distinct {id: fgroup.id, name: fgroup.name}) as admGroups, parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:ADMINISTRATIVE_ATTACHMENT]->(admStruct: Structure) WITH COLLECT(distinct {id: admStruct.id}) as admStruct, admGroups, parents, children, functions, u, structureNodes " +
				getMgroups +
				"RETURN DISTINCT u.profiles as type, structureNodes, functions, " +
				"CASE WHEN children IS NULL THEN [] ELSE children END as children, " +
				"CASE WHEN parents IS NULL THEN [] ELSE parents END as parents, " +
				"CASE WHEN admGroups IS NULL THEN [] ELSE admGroups END as functionalGroups, " +
				"CASE WHEN admStruct IS NULL THEN [] ELSE admStruct END as administrativeStructures, " +
				resultMgroups +
				"u";
		neo.execute(query, new JsonObject().putString("id", id), fullNodeMergeHandler("u", result, "structureNodes"));
	}

	@Override
	public void list(String structureId, String classId, JsonArray expectedProfiles,
			Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		String filterProfile = "";
		String filterStructure = "";
		String filterClass = "";
		if (expectedProfiles != null && expectedProfiles.size() > 0) {
			filterProfile = "WHERE p.name IN {expectedProfiles} ";
			params.putArray("expectedProfiles", expectedProfiles);
		}
		if (classId != null && !classId.trim().isEmpty()) {
			filterClass = "(g:ProfileGroup)-[:DEPENDS]->(n:Class {id : {classId}}), ";
			params.putString("classId", classId);
		} else if (structureId != null && !structureId.trim().isEmpty()) {
			filterStructure = "(pg:ProfileGroup)-[:DEPENDS]->(n:Structure {id : {structureId}}), ";
			params.putString("structureId", structureId);
		}
		String query =
				"MATCH " +filterClass + filterStructure +
				"(u:User)-[:IN]->g-[:DEPENDS*0..1]->pg-[:HAS_PROFILE]->(p:Profile) " +
				filterProfile +
				"RETURN DISTINCT u.id as id, p.name as type, u.externalId as externalId, u.IDPN as IDPN, " +
				"u.activationCode as code, u.login as login, u.firstName as firstName, " +
				"u.lastName as lastName, u.displayName as displayName " +
				"ORDER BY type DESC, displayName ASC ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void listIsolated(String structureId, List<String> profile, Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		String query;
		// users without class
		if (structureId != null && !structureId.trim().isEmpty()) {
			query = "MATCH  (s:Structure { id : {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), " +
					"g-[:HAS_PROFILE]->(p:Profile) " +
					"WHERE  NOT(u-[:IN]->()-[:DEPENDS]->(:Class)-[:BELONGS]->s) ";
			params.putString("structureId", structureId);
			if (profile != null && !profile.isEmpty()) {
				query += "AND p.name IN {profile} ";
				params.putArray("profile", new JsonArray(profile.toArray()));
			}
		} else { // users without structure
			query = "MATCH (u:User)" +
					"WHERE NOT(u-[:IN]->()-[:DEPENDS]->(:Structure)) " +
					"OPTIONAL MATCH u-[:IN]->(dpg:DefaultProfileGroup)-[:HAS_PROFILE]->(p:Profile) ";
		}
		query += "RETURN DISTINCT u.id as id, p.name as type, " +
				"u.activationCode as code, u.firstName as firstName," +
				"u.lastName as lastName, u.displayName as displayName " +
				"ORDER BY type DESC, displayName ASC ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void listAdmin(String structureId, String classId, String groupId,
						  JsonArray expectedProfiles, UserInfos userInfos, org.vertx.java.core.Handler<fr.wseduc.webutils.Either<String,JsonArray>> results) {
		listAdmin(structureId, classId, groupId, expectedProfiles, null, null, userInfos, results);
	};

	@Override
	public void listAdmin(String structureId, String classId, String groupId,
						  JsonArray expectedProfiles, String filterActivated, String nameFilter,
						  UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		String filter = "";
		String filterProfile = "WHERE 1=1 ";
		String optionalMatch =
			"OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class:Class)-[:BELONGS]->(s) " +
			"OPTIONAL MATCH u-[:RELATED]->(parent: User) " +
			"OPTIONAL MATCH (child: User)-[:RELATED]->u " +
			"OPTIONAL MATCH u-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) ";
		if (expectedProfiles != null && expectedProfiles.size() > 0) {
			filterProfile += "AND p.name IN {expectedProfiles} ";
			params.putArray("expectedProfiles", expectedProfiles);
		}
		if (classId != null && !classId.trim().isEmpty()) {
			filter = "(n:Class {id : {classId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
			params.putString("classId", classId);
		} else if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "(n:Structure {id : {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
			params.putString("structureId", structureId);
		} else if (groupId != null && !groupId.trim().isEmpty()) {
			filter = "(n:Group {id : {groupId}})<-[:IN]-";
			params.putString("groupId", groupId);
		}
		String condition = "";
		String functionMatch = "WITH u MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), u-[:IN]->pg ";
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL) &&
				!userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND s.id IN {scope} ";
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				functionMatch = "WITH u MATCH (c:Class)<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), u-[:IN]->pg ";
				condition = "AND c.id IN {scope} ";
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		}
		if(nameFilter != null && !nameFilter.trim().isEmpty()){
			condition += "AND u.displayName =~ {regex}  ";
			params.putString("regex", "(?i)^.*?" + Pattern.quote(nameFilter.trim()) + ".*?$");
		}
		if(filterActivated != null){
			if("inactive".equals(filterActivated)){
				condition += "AND NOT(u.activationCode IS NULL)  ";
			} else if("active".equals(filterActivated)){
				condition += "AND u.activationCode IS NULL ";
			}
		}

		String query =
				"MATCH " + filter + "(u:User) " +
				functionMatch + filterProfile + condition + optionalMatch +
				"RETURN DISTINCT u.id as id, p.name as type, u.externalId as externalId, " +
				"u.activationCode as code, u.login as login, u.firstName as firstName, " +
				"u.lastName as lastName, u.displayName as displayName, u.source as source, u.attachmentId as attachmentId, " +
				"u.birthDate as birthDate, " +
				"extract(function IN u.functions | last(split(function, \"$\"))) as aafFunctions, " +
				"collect(distinct {id: s.id, name: s.name}) as structures, " +
				"collect(distinct {id: class.id, name: class.name}) as allClasses, " +
				"collect(distinct [f.externalId, rf.scope]) as functions, " +
				"CASE WHEN parent IS NULL THEN [] ELSE collect(distinct {id: parent.id, firstName: parent.firstName, lastName: parent.lastName}) END as parents, " +
				"CASE WHEN child IS NULL THEN [] ELSE collect(distinct {id: child.id, firstName: child.firstName, lastName: child.lastName, attachmentId : child.attachmentId }) END as children, " +
				"HEAD(COLLECT(distinct parent.externalId)) as parent1ExternalId, " + // Hack for GEPI export
				"HEAD(TAIL(COLLECT(distinct parent.externalId))) as parent2ExternalId, " + // Hack for GEPI export
				"COUNT(distinct class.id) > 0 as hasClass " + // Hack for Esidoc export
				"ORDER BY type DESC, displayName ASC ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void delete(List<String> users, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-delete-user")
				.putArray("users", new JsonArray(users.toArray()));
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void restore(List<String> users, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-restore-user")
				.putArray("users", new JsonArray(users.toArray()));
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void addFunction(String id, String functionCode, JsonArray scope, String inherit,
			Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-add-user-function")
				.putString("userId", id)
				.putString("function", functionCode)
				.putString("inherit", inherit)
				.putArray("scope", scope);
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void removeFunction(String id, String functionCode, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-remove-user-function")
				.putString("userId", id)
				.putString("function", functionCode);
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	public void listFunctions(String userId, Handler<Either<String, JsonArray>> result) {
		String query =
				"MATCH (u:User{id: {userId}})-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) " + 
				"RETURN COLLECT(distinct [f.externalId, rf.scope]) as functions";
		JsonObject params = new JsonObject();
		params.putString("userId", userId);
		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void addGroup(String id, String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-add-user-group")
				.putString("userId", id)
				.putString("groupId", groupId);
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void removeGroup(String id, String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-remove-user-group")
				.putString("userId", id)
				.putString("groupId", groupId);
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void listAdml(String scopeId, Handler<Either<String, JsonArray>> result) {
		String query =
				"MATCH (n)<-[:DEPENDS]-(g:FunctionGroup)<-[:IN]-(u:User) " +
				"WHERE (n:Structure OR n:Class) AND n.id = {scopeId} AND g.name =~ '^.*-AdminLocal$' " +
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.putString("scopeId", scopeId);
		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void getInfos(String userId, Handler<Either<String, JsonObject>> result) {
		String query =
				"MATCH (n:User {id : {id}}) " +
				"OPTIONAL MATCH n-[:IN]->(gp:Group) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) " +
				"OPTIONAL MATCH n-[:IN]->()-[:HAS_PROFILE]->(p:Profile) " +
				"OPTIONAL MATCH n-[:ADMINISTRATIVE_ATTACHMENT]->(sa:Structure) " +
				"RETURN distinct " +
				"n, COLLECT(distinct c) as classes, HEAD(COLLECT(distinct p.name)) as type, " +
				"COLLECT(distinct s) as structures, COLLECT(distinct [f.externalId, rf.scope]) as functions, " +
				"COLLECT(distinct gp) as groups, COLLECT(distinct sa) as administratives";
		neo.execute(query, new JsonObject().putString("id", userId),
				fullNodeMergeHandler("n", result, "structures", "classes","groups", "administratives"));
	}

	@Override
	public void relativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-relative-student")
				.putString("relativeId", relativeId)
				.putString("studentId", studentId);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(0, eitherHandler));
	}

	@Override
	public void unlinkRelativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-unlink-relative-student")
				.putString("relativeId", relativeId)
				.putString("studentId", studentId);
		eb.send(Directory.FEEDER, action, validEmptyHandler(eitherHandler));
	}

	@Override
	public void ignoreDuplicate(String userId1, String userId2, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "ignore-duplicate")
				.putString("userId1", userId1)
				.putString("userId2", userId2);
		eb.send(Directory.FEEDER, action, validEmptyHandler(result));
	}

	@Override
	public void listDuplicates(JsonArray structures, boolean inherit, Handler<Either<String, JsonArray>> results) {
		JsonObject action = new JsonObject()
				.putString("action", "list-duplicate")
				.putArray("structures", structures)
				.putBoolean("inherit", inherit);
		eb.send(Directory.FEEDER, action, validResultHandler(results));
	}

	@Override
	public void mergeDuplicate(String userId1, String userId2, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.putString("action", "merge-duplicate")
				.putString("userId1", userId1)
				.putString("userId2", userId2);
		eb.send(Directory.FEEDER, action, validEmptyHandler(handler));
	}

	@Override
	public void listByUAI(List<String> UAI, JsonArray expectedTypes, boolean isExportFull, JsonArray fields, Handler<Either<String, JsonArray>> results) {
		if (UAI == null || UAI.isEmpty()) {
			results.handle(new Either.Left<String, JsonArray>("missing.uai"));
			return;
		} else {
			for (String uaiCode: UAI) {
				if (!StringValidation.isUAI(uaiCode)) {
					results.handle(new Either.Left<String, JsonArray>("invalid.uai"));
					return;
				}
			}
		}

		if (fields == null || fields.size() == 0) {
			fields = new JsonArray().add("id").add("externalId").add("lastName").add("firstName").add("login");
		}

		//user's fields for Full Export
		if(isExportFull){
			fields.add("email");
			fields.add("emailAcademy");
			fields.add("mobile");
			fields.add("deleteDate");
			fields.add("functions");
			fields.add("displayName");
		}

		// Init params and filter for all type of queries
		String  filter =  "WHERE s.UAI IN {uai} ";

		JsonObject params = new JsonObject().putArray("uai", new JsonArray(UAI.toArray()));

		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure)<-[:DEPENDS]-(cpg:ProfileGroup)");

		// filter by types if needed OR full export
		if( isExportFull || (expectedTypes != null && expectedTypes.size() > 0)) {
			query.append("-[:HAS_PROFILE]->(p:Profile)");
		}
		// filter by types if needed
		if (expectedTypes != null && expectedTypes.size() > 0) {

			filter += "AND p.name IN {expectedTypes} ";
			params.putArray("expectedTypes", expectedTypes);
		}

		query.append(", cpg<-[:IN]-(u:User) ")
				.append(filter);

		if (fields.contains("administrativeStructure")) {
			query.append("OPTIONAL MATCH u-[:ADMINISTRATIVE_ATTACHMENT]->sa ");
		}

		query.append("RETURN DISTINCT ");

		for (Object field : fields) {
			if ("type".equals(field) || "profile".equals(field)) {
				query.append(" HEAD(u.profiles)");
			} else if ("administrativeStructure".equals(field)) {
				query.append(" sa.externalId ");
			} else {
				query.append(" u.").append(field);
			}
			query.append(" as ").append(field).append(",");
		}
		query.deleteCharAt(query.length() - 1);

		//Full Export : profiles and Structure
		if(isExportFull){
			query.append(", p.name as profiles");
			query.append(", s.externalId as structures")
					.append(" , CASE WHEN size(u.classes) > 0  THEN  last(collect(u.classes)) END as classes");
		}

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public void generateMergeKey(String userId, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsError(handler, userId)) return;
		final String query = "MATCH (u:User {id: {id}}) SET u.mergeKey = {mergeKey} return u.mergeKey as mergeKey";
		final JsonObject params = new JsonObject().putString("id", userId).putString("mergeKey", UUID.randomUUID().toString());
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void mergeByKey(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsNull(handler, userId, body)) return;
		JsonObject action = new JsonObject()
				.putString("action", "merge-by-keys")
				.putString("originalUserId", userId)
				.putArray("mergeKeys", body.getArray("mergeKeys"));
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(5, handler));
	}

	@Override
	public void list(String groupId, boolean itSelf, String userId,
			final Handler<Either<String, JsonArray>> handler) {
		String condition = (itSelf || userId == null) ? "" : "AND u.id <> {userId} ";
		String query =
				"MATCH (n:Group)<-[:IN]-(u:User) " +
				"WHERE n.id = {groupId} " + condition +
				"OPTIONAL MATCH (n)-[:DEPENDS*0..1]->(:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"OPTIONAL MATCH (u)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH (pg)-[:HAS_PROFILE]->(pro:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				"u.displayName as username, u.firstName as firstName, u.lastName as lastName, profile.name as type," +
				"CASE WHEN s IS NULL THEN [] ELSE COLLECT(DISTINCT {id: s.id, name: s.name}) END as structures," +
				"CASE WHEN pro IS NULL THEN NULL ELSE HEAD(COLLECT(DISTINCT pro.name)) END as profile " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.putString("groupId", groupId);
		if (!itSelf && userId != null) {
			params.putString("userId", userId);
		}
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void list(JsonArray groupIds, JsonArray userIds, boolean itSelf, String userId,
			final Handler<Either<String, JsonArray>> handler) {
		String condition = (itSelf || userId == null) ? "" : "AND u.id <> {userId} ";
		String query =
				"MATCH (n:Group)<-[:IN]-(u:User) " +
				"WHERE n.id IN {groupIds} " + condition +
				"OPTIONAL MATCH n-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username " +
				"UNION " +
				"MATCH (u:User) " +
				"WHERE u.id IN {userIds} " + condition +
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.putArray("groupIds", groupIds);
		params.putArray("userIds", userIds);
		if (!itSelf && userId != null) {
			params.putString("userId", userId);
		}
		neo.execute(query, params, validResultHandler(handler));
	}

}
