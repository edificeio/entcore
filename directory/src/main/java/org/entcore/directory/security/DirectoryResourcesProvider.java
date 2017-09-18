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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.controllers.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.entcore.common.user.DefaultFunctions.*;

public class DirectoryResourcesProvider implements ResourcesProvider {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {

		//Super-admin "hack"
		if(user.getFunctions().containsKey(SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}

		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(ClassController.class.getName())) {
			String method = serviceMethod
					.substring(ClassController.class.getName().length() + 1);
			switch (method) {
				case "get":
					isClassMember(request, user, handler);
					break;
				case "applyComRulesAndRegistryEvent" :
				case "addUser":
				case "csv" :
				case "findUsers" :
				case "createUser" :
				case "update" :
					isClassTeacher(request, user, handler);
					break;
				case "listAdmin" :
					isAdmin(user, true, handler);
					break;
				case "unlinkUser" :
				case "linkUser" :
					isAdminOfStructureOrClass4(request, user, handler);
					break;
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(UserController.class.getName())) {
			String method = serviceMethod
					.substring(UserController.class.getName().length() + 1);
			switch (method) {
				case "delete" :
				case "removeFunction" :
					adminOrTeacher(request, user, handler);
					break;
				case "updateAvatar" :
				case "get" :
				case "getUserBook" :
				case "updateUserBook" :
				case "update" :
					isUserOrTeacherOf(request, user, handler);
					break;
				case "listAdmin" :
				case "export" :
					isAdmin(user, true, handler);
					break;
				case "addGroup" :
				case "removeGroup" :
					isAdminOfUserAndGroup(request, user, handler);
					break;
				case "listGroup":
					isAdminOfGroup(request, user, handler);
					break;
				case "listIsolated" :
					isAdminOfStructure(request, user, handler);
					break;
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(StructureController.class.getName())) {
			String method = serviceMethod
					.substring(StructureController.class.getName().length() + 1);
			switch (method) {
				case "update" :
					isAdminOfStructureOrClass4(request, user, handler);
					break;
				case "listAdmin" :
				case "getLevels" :
				case "getMassmailUsers" :
				case "performMassmail" :
					isAdmin(user, false, handler);
					break;
				case "unlinkUser" :
				case "linkUser" :
					isAdminOfStructureOrClass4(request, user, handler);
					break;
				case "metrics":
					isAdminOfStructure(request, user, handler);
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(GroupController.class.getName())) {
			String method = serviceMethod
					.substring(GroupController.class.getName().length() + 1);
			switch (method) {
				case "listAdmin" :
					isAdmin(user, false, handler);
					break;
				case "create" :
					isAdminOfStructureOrClass(request, user, handler);
					break;
				case "update" :
				case "delete" :
					isAdminOfGroup(request, user, handler);
					break;
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(DirectoryController.class.getName())) {
			String method = serviceMethod
					.substring(DirectoryController.class.getName().length() + 1);
			switch (method) {
				case "getSchool" :
					isSchoolMember(request, user, handler);
					break;
				case "createUser" :
					isAdminOfStructureOrClass2(request, user, handler);
					break;
				case "export" :
					isAdminOfStructureOrClass3(request, user, handler);
					break;
				default: handler.handle(false);
			}
		}else {
			handler.handle(false);
		}
	}

	private void isAdminOfStructureOrClass3(HttpServerRequest request, UserInfos user, Handler<Boolean> handler) {
		Set<String> ids = prevalidateAndGetIds(user, handler);
		if (ids == null) return;
		handler.handle(ids.contains(request.params().get("id")));
	}

	private void isAdminOfStructure(HttpServerRequest request, UserInfos user, Handler<Boolean> handler) {
		final String structureId = request.params().get("structureId");
		if (structureId == null || structureId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal.getScope().contains(structureId)) {
			handler.handle(true);
			return;
		}
		handler.handle(false);
	}

	private void isAdminOfUserAndGroup(HttpServerRequest request, UserInfos user, Handler<Boolean> handler) {
		Set<String> ids = prevalidateAndGetIds(user, handler);
		if (ids == null) return;
		String query =
				"MATCH (g:Group {id : {id}})-[:DEPENDS]->()-[:BELONGS*0..1]->s, " +
				"(u:User {id : {userId}})-[:IN]->()-[:DEPENDS]->()-[:BELONGS*0..1]->s2 " +
				"WHERE s.id IN {ids} AND s2.id IN {ids} " +
				"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("id", request.params().get("groupId"))
				.putString("userId", request.params().get("userId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		validateQuery(request, handler, query, params);
	}

	private void isAdminOfGroup(final HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		Set<String> ids = prevalidateAndGetIds(user, handler);
		if (ids == null) return;
		String query =
				"MATCH (g:Group {id : {id}})-[:DEPENDS]->c-[:BELONGS*0..1]->s " +
				"WHERE s.id IN {ids} " +
				"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("id", request.params().get("groupId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		validateQuery(request, handler, query, params);
	}

	private void validateQuery(final HttpServerRequest request, final Handler<Boolean> handler, String query, JsonObject params) {
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
						res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)
				);
			}
		});
	}

	private Set<String> prevalidateAndGetIds(UserInfos user, Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return null;
		}
		UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		UserInfos.Function classAdmin = functions.get(DefaultFunctions.CLASS_ADMIN);
		if ((adminLocal == null || adminLocal.getScope() == null) &&
				(classAdmin == null || classAdmin.getScope() == null)) {
			handler.handle(false);
			return null;
		}
		Set<String> ids = new HashSet<>();
		if (adminLocal != null && adminLocal.getScope() != null) {
			ids.addAll(adminLocal.getScope());
		}
		if (classAdmin != null && classAdmin.getScope() != null) {
			ids.addAll(classAdmin.getScope());
		}
		return ids;
	}

	private void isAdminOfStructureOrClass(final HttpServerRequest request, UserInfos user,
			final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		final UserInfos.Function classAdmin = functions.get(DefaultFunctions.CLASS_ADMIN);
		if ((adminLocal == null || adminLocal.getScope() == null) &&
				(classAdmin == null || classAdmin.getScope() == null)) {
			handler.handle(false);
			return;
		}
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				String classId = event.getString("classId");
				String structureId = event.getString("structureId");
				if ((adminLocal != null && adminLocal.getScope() != null &&
						adminLocal.getScope().contains(structureId)) ||
						(classAdmin != null && classAdmin.getScope() != null &&
								classAdmin.getScope().contains(classId))) {
					handler.handle(true);
				} else if (adminLocal != null && classId != null && adminLocal.getScope() != null) {
					String query =
							"MATCH (s:Structure)<-[:BELONGS]-(c:Class {id : {classId}}) " +
							"WHERE s.id IN {ids} " +
							"RETURN count(*) > 0 as exists";
					JsonObject params = new JsonObject()
							.putString("classId", classId)
							.putArray("ids", new JsonArray(adminLocal.getScope().toArray()));
					validateQuery(request, handler, query, params);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void isAdminOfStructureOrClass2(final HttpServerRequest request, UserInfos user,
			final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		final UserInfos.Function classAdmin = functions.get(DefaultFunctions.CLASS_ADMIN);
		if ((adminLocal == null || adminLocal.getScope() == null) &&
				(classAdmin == null || classAdmin.getScope() == null)) {
			handler.handle(false);
			return;
		}
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String classId = request.formAttributes().get("classId");
				final String structureId = request.formAttributes().get("structureId");
				if ((adminLocal != null && adminLocal.getScope() != null &&
						adminLocal.getScope().contains(structureId)) ||
						(classAdmin != null && classAdmin.getScope() != null &&
								classAdmin.getScope().contains(classId))) {
					handler.handle(true);
				} else if (adminLocal != null && classId != null && adminLocal.getScope() != null) {
					String query =
							"MATCH (s:Structure)<-[:BELONGS]-(c:Class {id : {classId}}) " +
									"WHERE s.id IN {ids} " +
									"RETURN count(*) > 0 as exists";
					JsonObject params = new JsonObject()
							.putString("classId", classId)
							.putArray("ids", new JsonArray(adminLocal.getScope().toArray()));
					validateQuery(request, handler, query, params);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void isAdminOfStructureOrClass4(final HttpServerRequest request, UserInfos user,
			final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		final UserInfos.Function classAdmin = functions.get(DefaultFunctions.CLASS_ADMIN);
		if ((adminLocal == null || adminLocal.getScope() == null) &&
				(classAdmin == null || classAdmin.getScope() == null)) {
			handler.handle(false);
			return;
		}
		final String classId = request.params().get("classId");
		final String structureId = request.params().get("structureId");
		if (adminLocal != null && adminLocal.getScope() != null &&
				(adminLocal.getScope().contains(structureId) || adminLocal.getScope().contains(classId))) {
			handler.handle(true);
			return;
		}
		if (adminLocal != null && classId != null && adminLocal.getScope() != null) {
			String query =
					"MATCH (s:Structure)<-[:BELONGS]-(c:Class {id : {classId}}) " +
					"WHERE s.id IN {ids} " +
					"RETURN count(*) > 0 as exists";
			JsonObject params = new JsonObject()
					.putString("classId", classId)
					.putArray("ids", new JsonArray(adminLocal.getScope().toArray()));
			validateQuery(request, handler, query, params);
		} else {
			handler.handle(false);
		}
	}

	private void isAdmin(UserInfos user, boolean allowClass, Handler<Boolean> handler) {
		handler.handle(
				user.getFunctions().containsKey(SUPER_ADMIN) ||
				user.getFunctions().containsKey(ADMIN_LOCAL) ||
				(allowClass && user.getFunctions().containsKey(CLASS_ADMIN))
		);
	}

	private void isUserOrTeacherOf(final HttpServerRequest request, final UserInfos user,
			final Handler<Boolean> handler) {
		String userId = request.params().get("userId");
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		// me
		if (userId.equals(user.getUserId())) {
			handler.handle(true);
			return;
		}
		adminOrTeacher(request, user, handler);
	}

	private void adminOrTeacher(final HttpServerRequest request, final UserInfos user, final Handler<Boolean> handler) {
		Set<String> ids = getIds(user);
		if (ids == null) return;
		String query =
				"MATCH (u:User {id : {userId}})-[:IN]->()-[:DEPENDS]->()-[:BELONGS*0..1]->s2 " +
				"WHERE s2.id IN {ids} " +
				"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("id", request.params().get("groupId"))
				.putString("userId", request.params().get("userId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) &&
						res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)) {
					handler.handle(true);
				} else {
					isTeacherOf(request, user, handler);
				}
			}
		});
	}

	private void isTeacherOf(final HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		List<String> userIds = request.params().getAll("userId");
		if (userIds == null || userIds.isEmpty() || userIds.contains(user.getUserId()) ||
				(!"Teacher".equals(user.getType()) && !"Personnel".equals(user.getType()))) {
			handler.handle(false);
			return;
		}
		String query =
				"MATCH (t:User { id : {teacherId}})-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WITH c " +
				"MATCH c<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE u.id IN {userIds} " +
				"RETURN count(distinct u) = {size} as exists ";
		JsonObject params = new JsonObject()
				.putArray("userIds", new JsonArray(userIds.toArray()))
				.putString("teacherId", user.getUserId())
				.putNumber("size", userIds.size());
		validateQuery(request, handler, query, params);
	}

	private void isClassTeacher(final HttpServerRequest request, final UserInfos user,
								final Handler<Boolean> handler) {
		final String classId = request.params().get("classId");
		if (classId == null || classId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		Set<String> ids = getIds(user);
		String query =
				"MATCH (c:Class {id : {classId}})-[:BELONGS]->s2 " +
						"WHERE s2.id IN {ids} " +
						"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("userId", request.params().get("userId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) &&
						res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)) {
					handler.handle(true);
				} else if ("Teacher".equals(user.getType()) || "Personnel".equals(user.getType())) {
					String query =
							"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
									"<-[:IN]-(t:`User` { id : {teacherId}}) " +
									"RETURN count(*) > 0 as exists ";
					JsonObject params = new JsonObject()
							.putString("classId", classId)
							.putString("teacherId", user.getUserId());
					validateQuery(request, handler, query, params);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void isClassMember(final HttpServerRequest request, final UserInfos user,
			final Handler<Boolean> handler) {
		final String classId = request.params().get("classId");
		if (classId == null || classId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		Set<String> ids = getIds(user);
		String query =
				"MATCH (c:Class {id : {classId}})-[:BELONGS]->s2 " +
				"WHERE s2.id IN {ids} " +
				"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("userId", request.params().get("userId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) &&
						res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)) {
					handler.handle(true);
				} else {
					String query =
							"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
									"<-[:IN]-(t:`User` { id : {teacherId}}) " +
									"RETURN count(*) > 0 as exists ";
					JsonObject params = new JsonObject()
							.putString("classId", classId)
							.putString("teacherId", user.getUserId());
					validateQuery(request, handler, query, params);
				}
			}
		});
	}

	private void isSchoolMember(final HttpServerRequest request, final UserInfos user,
							   final Handler<Boolean> handler) {
		final String structureId = request.params().get("id");
		if (structureId == null || structureId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		Set<String> ids = getIds(user, true);
		if (ids.contains(structureId)) {
			handler.handle(true);
			return;
		}
		String query =
				"MATCH (c:`Structure` { id : {structureId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
						"<-[:IN]-(t:`User` { id : {teacherId}}) " +
						"RETURN count(*) > 0 as exists ";
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("teacherId", user.getUserId());
		validateQuery(request, handler, query, params);
	}

	static Set<String> getIds(UserInfos user) {
		return getIds(user, false);
	}

	private static Set<String> getIds(UserInfos user, boolean structuresOnly) {
		Set<String> ids = new HashSet<>();
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions != null && !functions.isEmpty()) {
			UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
			UserInfos.Function classAdmin = functions.get(DefaultFunctions.CLASS_ADMIN);
			if (adminLocal != null && adminLocal.getScope() != null) {
				ids.addAll(adminLocal.getScope());
			}
			if (!structuresOnly && classAdmin != null && classAdmin.getScope() != null) {
				ids.addAll(classAdmin.getScope());
			}
		}
		return ids;
	}

}
