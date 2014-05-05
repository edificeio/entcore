/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.controllers.ClassController;
import org.entcore.directory.controllers.UserController;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class DirectoryResourcesProvider implements ResourcesProvider {

	private final Neo neo;

	public DirectoryResourcesProvider(Neo neo) {
		this.neo = neo;
	}

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
				default: handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(UserController.class.getName())) {
			String method = serviceMethod
					.substring(UserController.class.getName().length() + 1);
			switch (method) {
				case "updateAvatar" :
				case "get" :
				case "getUserBook" :
				case "updateUserBook" :
				case "update" :
					isUserOrTeacherOf(request, user, handler);
					break;
				default: handler.handle(false);
			}
		} else {
			handler.handle(false);
		}
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
