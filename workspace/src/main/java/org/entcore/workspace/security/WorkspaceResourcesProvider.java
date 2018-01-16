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

package org.entcore.workspace.security;

import fr.wseduc.webutils.request.RequestUtils;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.workspace.Workspace;
import org.entcore.workspace.controllers.QuotaController;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Binding;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.service.WorkspaceService;

import java.util.Map;

public class WorkspaceResourcesProvider implements ResourcesProvider {

	private MongoDb mongo = MongoDb.getInstance();

	@Override
	public void authorize(final HttpServerRequest request, final Binding binding,
			final UserInfos user, final Handler<Boolean> handler) {
		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(WorkspaceService.class.getName())) {
			String method = serviceMethod
					.substring(WorkspaceService.class.getName().length() + 1);
			switch (method) {
				case "getDocumentProperties":
			case "getDocument":
				authorizeGetDocument(request, user, binding.getServiceMethod(), handler);
				break;
			case "commentDocument":
			case "commentFolder":
			case "updateDocument":
			case "moveDocument":
			case "moveTrash":
			case "copyDocument":
			case "deleteDocument":
			case "restoreTrash":
			case "shareJson":
			case "shareJsonSubmit":
			case "removeShare":
			case "getRevision":
			case "listRevisions":
				authorizeDocument(request, user, binding.getServiceMethod(), handler);
				break;
			case "deleteRevision":
				authorizeRevisionOwner(request, user, binding.getServiceMethod(), new Handler<Boolean>() {
					public void handle(Boolean check) {
						if(check){
							authorizeDocument(request, user, serviceMethod.substring(0, WorkspaceService.class.getName().length() + 1) + "updateDocument", handler);
						}
						else {
							authorizeDocument(request, user, binding.getServiceMethod(), handler);
						}
					}
				});
				break;
			case "deleteComment":
				authorizeCommentOwner(request, user, binding.getServiceMethod(), new Handler<Boolean>() {
					public void handle(Boolean check) {
						if(check){
							authorizeDocument(request, user, serviceMethod.substring(0, WorkspaceService.class.getName().length() + 1) + "commentDocument", handler);
						}
						else {
							authorizeDocument(request, user, binding.getServiceMethod(), handler);
						}
					}
				});
				break;
			case "moveDocuments":
			case "copyDocuments":
				authorizeDocuments(request, user, binding.getServiceMethod(), handler);
				break;
			case "renameDocument":
			case "renameFolder":
				authorizeOwner(request, user, binding.getServiceMethod(), handler);
				break;
			default:
				handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(QuotaController.class.getName())) {
			String method = serviceMethod
					.substring(QuotaController.class.getName().length() + 1);
			switch (method) {
				case "getQuota" :
					isUserOrAdmin(request, user, handler);
					break;
				case "update" :
					isAdminFromUsers(request, user, handler);
					break;
				case "getQuotaStructure" :
					isAdmin(request, user, handler);
					break;
				case "updateDefault" :
				case "getQuotaGlobal" :
					isSuperAdmin(user, handler);
					break;
				case "getDefault":
					UserInfos.Function adminLocal = getFunction(user, handler);
					if(adminLocal != null)
						handler.handle(true);
					break;
				default: handler.handle(false);
			}
		}
	}

	private void isUserOrAdmin(HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		final String userId = request.params().get("userId");
		if (user.getUserId().equals(userId)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = getFunction(user, handler);
		if (adminLocal == null) return;
		String query =
				"MATCH (s:Structure)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {id : {userId}}) " +
				"WHERE s.id IN {structures} " +
				"RETURN count(*) > 0 as exists ";
		JsonObject params = new JsonObject()
				.putArray("structures", new JsonArray(adminLocal.getScope().toArray()))
				.putString("userId", userId);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray res = message.body().getArray("result");
				handler.handle(
						"ok".equals(message.body().getString("status")) && res != null && res.size() == 1 &&
								res.<JsonObject>get(0).getBoolean("exists", false)
				);
			}
		});
	}

	private void isAdminFromUsers(HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		final UserInfos.Function adminLocal = getFunction(user, handler);
		if (adminLocal == null) return;
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				String query =
						"MATCH (s:Structure)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
						"WHERE s.id IN {structures} AND u.id IN {users} " +
						"RETURN count(distinct u) as nb ";
				final JsonArray users = object.getArray("users", new JsonArray());
				JsonObject params = new JsonObject()
						.putArray("structures", new JsonArray(adminLocal.getScope().toArray()))
						.putArray("users", users);
				Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						JsonArray res = message.body().getArray("result");
						handler.handle(
								"ok".equals(message.body().getString("status")) && res != null && res.size() == 1 &&
								res.<JsonObject>get(0).getInteger("nb", -1).equals(users.size())
						);
					}
				});
			}
		});
	}

	private void isAdmin(HttpServerRequest request, UserInfos user, Handler<Boolean> handler) {
		UserInfos.Function adminLocal = getFunction(user, handler);
		if (adminLocal == null) return;
		String structureId = request.params().get("structureId");
		handler.handle(adminLocal.getScope().contains(structureId));
	}

	private UserInfos.Function getFunction(UserInfos user, Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return null;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return null;
		}
		UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return null;
		}
		return adminLocal;
	}

	private void isSuperAdmin(UserInfos user, Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		handler.handle(functions.containsKey(DefaultFunctions.SUPER_ADMIN));
	}

	private void authorizeDocuments(HttpServerRequest request, UserInfos user,
			String serviceMethod, Handler<Boolean> handler) {
		String ids = request.params().get("ids");
		if (ids != null && !ids.trim().isEmpty()) {
			JsonArray idsArray = new JsonArray(ids.split(","));
			String query = "{ \"_id\": { \"$in\" : " + idsArray.encode() + "}, "
					+ "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
					"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user, serviceMethod) + "}}]}";
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION,
					new JsonObject(query), idsArray.size(), handler);
		} else {
			handler.handle(false);
		}
	}

	private String orSharedElementMatch(UserInfos user, String serviceMethod) {
		String s =  serviceMethod.replaceAll("\\.", "-");
		StringBuilder sb = new StringBuilder();
		if (user.getGroupsIds() != null) {
			for (String groupId: user.getGroupsIds()) {
				sb.append(", { \"groupId\": \"" + groupId + "\", \"" + s + "\": true }");
			}
		}
		return "{ \"$or\" : [" +
				"{ \"userId\": \"" + user.getUserId() + "\", \"" + s + "\": true }" +
				sb.toString() +
				"]}";
	}

	private void authorizeGetDocument(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"$or\" : [{ \"owner\": \"" + user.getUserId() +
					"\"}, { \"protected\" : true}, { \"public\" : true}, {\"shared\" : { \"$elemMatch\" : " +
					orSharedElementMatch(user, serviceMethod) + "}}]}";
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeDocument(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"$or\" : [{ \"owner\": \"" + user.getUserId() +
					"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user, serviceMethod) + "}}]}";
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeOwner(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"owner\": \"" + user.getUserId() + "\" }";
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeRevisionOwner(HttpServerRequest request, UserInfos user, String serviceMethod, final Handler<Boolean> handler) {
		final String revisionId = request.params().get("revisionId");
		JsonObject query = new JsonObject("{ \"_id\": \"" + revisionId + "\", \"userId\": \"" + user.getUserId() + "\" }");

		if(revisionId != null && !revisionId.trim().isEmpty()){
			executeCountQuery(request, Workspace.REVISIONS_COLLECTION, query, 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeCommentOwner(HttpServerRequest request, UserInfos user, String serviceMethod, final Handler<Boolean> handler) {
		final String id = request.params().get("id");
		final String commentId = request.params().get("commentId");
		JsonObject query = new JsonObject("{ \"_id\": \"" + id + "\", \"comments.id\": \"" + commentId + "\", \"comments.author\": \"" + user.getUserId() + "\"  }");

		if(commentId != null && !commentId.trim().isEmpty()){
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION, query, 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void executeCountQuery(final HttpServerRequest request, String collection,
			JsonObject query, final int expectedCountResult,
			final Handler<Boolean> handler) {
		request.pause();
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonObject res = event.body();
				handler.handle(
					res != null &&
					"ok".equals(res.getString("status")) &&
					expectedCountResult == res.getInteger("count")
				);
			}
		});
	}

}
