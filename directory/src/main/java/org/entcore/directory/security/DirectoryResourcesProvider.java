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
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.controllers.ClassController;
import org.entcore.directory.controllers.GroupController;
import org.entcore.directory.controllers.StructureController;
import org.entcore.directory.controllers.UserController;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.user.DefaultFunctions.*;

public class DirectoryResourcesProvider implements ResourcesProvider {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(ClassController.class.getName())) {
			String method = serviceMethod
					.substring(ClassController.class.getName().length() + 1);
			switch (method) {
				case "get":
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
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(UserController.class.getName())) {
			String method = serviceMethod
					.substring(UserController.class.getName().length() + 1);
			switch (method) {
				case "delete" :
					isTeacherOf(request, user, handler);
					break;
				case "updateAvatar" :
				case "get" :
				case "getUserBook" :
				case "updateUserBook" :
				case "update" :
					isUserOrTeacherOf(request, user, handler);
					break;
				case "listAdmin" :
					isAdmin(user, true, handler);
					break;
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(StructureController.class.getName())) {
			String method = serviceMethod
					.substring(StructureController.class.getName().length() + 1);
			switch (method) {
				case "listAdmin" :
					isAdmin(user, false, handler);
					break;
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(GroupController.class.getName())) {
			String method = serviceMethod
					.substring(GroupController.class.getName().length() + 1);
			switch (method) {
				case "listAdmin" :
					isAdmin(user, false, handler);
					break;
				default: handler.handle(false);
			}
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

	private void isUserOrTeacherOf(final HttpServerRequest request, UserInfos user,
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
		isTeacherOf(request, user, handler);
	}

	private void isTeacherOf(final HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		String userId = request.params().get("userId");
		if (userId == null || userId.trim().isEmpty() || userId.equals(user.getUserId())) {
			handler.handle(false);
			return;
		}
		String query =
				"MATCH (t:User { id : {teacherId}})-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(c:Class)" +
				"<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User {id: {userId}}) " +
				"RETURN count(*) >= 1 as exists ";
		JsonObject params = new JsonObject()
				.putString("userId", userId)
				.putString("teacherId", user.getUserId());
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

	private void isClassTeacher(final HttpServerRequest request, UserInfos user,
			final Handler<Boolean> handler) {
		String classId = request.params().get("classId");
		if (classId == null || classId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
				"<-[:IN]-(t:`User` { id : {teacherId}}) " +
				"RETURN count(*) = 1 as exists ";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("teacherId", user.getUserId());
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

}
