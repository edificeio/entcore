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

package org.entcore.auth.security;

import fr.wseduc.webutils.http.Binding;

import org.entcore.auth.controllers.AuthController;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
		if (user.getFunctions() != null && user.getFunctions().containsKey("SUPER_ADMIN")) {
			request.resume();
			handler.handle(true);
			return;
		}
		String id = request.params().get("userId");
		if (id == null || id.trim().isEmpty()) {
			handler.handle(false);
			return;
		}

		String query = "";
		if (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.ADMIN_LOCAL)) {
			query =
					"MATCH (t:User { id : {teacherId}})-[:IN]->(fg:FunctionGroup)-[:DEPENDS]->(s:Structure)" +
					"<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User {id : {id}}) " +
					"WHERE fg.name =~ \".*AdminLocal.*\"" +
					"RETURN count(*) >= 1 as exists ";
		} else {
			query =
					"MATCH (t:User { id : {teacherId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class)" +
					"<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User {id : {id}}) " +
					"RETURN count(*) >= 1 as exists ";
		}
		JsonObject params = new JsonObject()
				.put("id", id)
				.put("teacherId", user.getUserId());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getJsonArray("result");
				request.resume();
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 1 && (res.getJsonObject(0)).getBoolean("exists", false)
				);
			}
		});
	}

	private void isClassTeacherByUserLogin(final HttpServerRequest request,
			final UserInfos user, final Handler<Boolean> handler) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				if (user.getFunctions() != null && user.getFunctions().containsKey("SUPER_ADMIN")) {
					handler.handle(true);
					return;
				}
				String login = request.formAttributes().get("login");
				if (login == null || login.trim().isEmpty()) {
					handler.handle(false);
					return;
				}
				String query;
				if (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.ADMIN_LOCAL)) {
					query =
						"MATCH (t:User { id : {teacherId}})-[:IN]->(fg:FunctionGroup)-[:DEPENDS]->" +
						"(:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure)" +
						"<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User {login : {login}}) " +
						"WHERE fg.name =~ \".*AdminLocal.*\"" +
						"RETURN count(*) >= 1 as exists ";
				} else {
					query =
						"MATCH (t:User { id : {teacherId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class)" +
						"<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User {login : {login}}) " +
						"RETURN count(*) >= 1 as exists ";
				}
				JsonObject params = new JsonObject()
						.put("login", login)
						.put("teacherId", user.getUserId());
				neo.execute(query, params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						JsonArray res = r.body().getJsonArray("result");
						handler.handle(
								"ok".equals(r.body().getString("status")) &&
										res.size() == 1 && (res.getJsonObject(0)).getBoolean("exists", false)
						);
					}
				});
			}
		});
	}

}
