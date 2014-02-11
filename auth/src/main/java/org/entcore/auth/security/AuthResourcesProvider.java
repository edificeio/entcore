package org.entcore.auth.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.auth.AuthController;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class AuthResourcesProvider implements ResourcesProvider {

	private final Neo neo;

	public AuthResourcesProvider(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(AuthController.class.getName())) {
			String method = serviceMethod
					.substring(AuthController.class.getName().length() + 1);
			switch (method) {
				case "blockUser" :
					isClassTeacher(request, user, handler);
					break;
				case "sendResetPassword" :
					isClassTeacherByUserLogin(request, user, handler);
					break;
				default: handler.handle(false);
			}
		} else {
			handler.handle(false);
		}
	}

	private void isClassTeacher(final HttpServerRequest request, UserInfos user,
			final Handler<Boolean> handler) {
		request.pause();
		if ("SuperAdmin".equals(user.getType())) {
			handler.handle(true);
			return;
		}
		String id = request.params().get("userId");
		if (id == null || id.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		String query =
				"MATCH (t:`Teacher` { id : {teacherId}})-[:APPARTIENT]->(c:Class)" +
				"<-[:APPARTIENT]-(s:User)-[:EN_RELATION_AVEC*0..1]->(u:User {id : {id}}) " +
				"RETURN count(*) >= 1 as exists ";
		JsonObject params = new JsonObject()
				.putString("id", id)
				.putString("teacherId", user.getUserId());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getArray("result");
				request.resume();
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)
				);
			}
		});
	}

	private void isClassTeacherByUserLogin(final HttpServerRequest request,
			final UserInfos user, final Handler<Boolean> handler) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				if ("SuperAdmin".equals(user.getType())) {
					handler.handle(true);
					return;
				}
				String login = request.formAttributes().get("login");
				if (login == null || login.trim().isEmpty()) {
					handler.handle(false);
					return;
				}
				String query =
						"MATCH (t:`Teacher` { id : {teacherId}})-[:APPARTIENT]->(c:Class)" +
						"<-[:APPARTIENT]-(s:User)-[:EN_RELATION_AVEC*0..1]->(u:User {login : {login}}) " +
						"RETURN count(*) >= 1 as exists ";
				JsonObject params = new JsonObject()
						.putString("login", login)
						.putString("teacherId", user.getUserId());
				neo.execute(query, params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						JsonArray res = r.body().getArray("result");
						handler.handle(
								"ok".equals(r.body().getString("status")) &&
										res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)
						);
					}
				});
			}
		});
	}

}
