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

package org.entcore.workspace.security;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

import java.util.HashSet;
import java.util.Map;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.Workspace;
import org.entcore.workspace.controllers.QuotaController;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WorkspaceResourcesProvider implements ResourcesProvider {
	private FolderManager folderManager;

	private MongoDb mongo = MongoDb.getInstance();

	public void setFolderManager(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	@Override
	public void authorize(final HttpServerRequest request, final Binding binding, final UserInfos user,
			final Handler<Boolean> handler) {
		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(WorkspaceController.class.getName())) {
			String method = serviceMethod.substring(WorkspaceController.class.getName().length() + 1);
			switch (method) {
			case "getDocumentProperties":
			case "getDocument":
				authorizeGetDocument(request, user, binding.getServiceMethod(), handler);
				break;
			case "commentDocument":
			case "commentFolder":
			case "updateDocument":
			case "moveDocument":
			case "moveFolder":
			case "copyDocument":
			case "copyFolder":
			case "deleteDocument":
			case "deleteFolder":
			case "shareJson":
			case "shareResource":
			case "getRevision":
			case "listRevisions":
				authorizeDocument(request, user, binding.getServiceMethod(), handler);
				break;
			case "deleteRevision":
				authorizeRevisionOwner(request, user, binding.getServiceMethod(), new Handler<Boolean>() {
					public void handle(Boolean check) {
						if (check) {
							authorizeDocument(request, user,
									serviceMethod.substring(0, WorkspaceController.class.getName().length() + 1)
											+ "updateDocument",
									handler);
						} else {
							authorizeDocument(request, user, binding.getServiceMethod(), handler);
						}
					}
				});
				break;
			case "deleteComment":
				authorizeCommentOwner(request, user, binding.getServiceMethod(), new Handler<Boolean>() {
					public void handle(Boolean check) {
						if (check) {
							authorizeDocument(request, user,
									serviceMethod.substring(0, WorkspaceController.class.getName().length() + 1)
											+ "commentDocument",
									handler);
						} else {
							authorizeDocument(request, user, binding.getServiceMethod(), handler);
						}
					}
				});
				break;
			case "moveDocuments":
			case "moveTrashFolder":
			case "copyDocuments":
			case "moveTrash":
			case "restoreFolder":
			case "restoreTrash":
			case "bulkDelete":
				authorizeDocuments(request, user, binding.getServiceMethod(), handler);
				break;
			case "renameDocument":
			case "renameFolder":
				authorizeDocument(request, user, binding.getServiceMethod(), handler);
				break;
			default:
				handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(QuotaController.class.getName())) {
			String method = serviceMethod.substring(QuotaController.class.getName().length() + 1);
			switch (method) {
			case "getQuota":
				isUserOrAdmin(request, user, handler);
				break;
			case "update":
				isAdminFromUsers(request, user, handler);
				break;
			case "getQuotaStructure":
				isAdmin(request, user, handler);
				break;
			case "updateDefault":
			case "getQuotaGlobal":
				isSuperAdmin(user, handler);
				break;
			case "getDefault":
				UserInfos.Function adminLocal = getFunction(user, handler);
				if (adminLocal != null)
					handler.handle(true);
				break;
			default:
				handler.handle(false);
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
		if (adminLocal == null)
			return;
		String query = "MATCH (s:Structure)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {id : {userId}}) "
				+ "WHERE s.id IN {structures} " + "RETURN count(*) > 0 as exists ";
		JsonObject params = new JsonObject()
				.put("structures", new fr.wseduc.webutils.collections.JsonArray(adminLocal.getScope()))
				.put("userId", userId);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray res = message.body().getJsonArray("result");
				handler.handle("ok".equals(message.body().getString("status")) && res != null && res.size() == 1
						&& res.getJsonObject(0).getBoolean("exists", false));
			}
		});
	}

	private void isAdminFromUsers(HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
		final UserInfos.Function adminLocal = getFunction(user, handler);
		if (adminLocal == null)
			return;
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				String query = "MATCH (s:Structure)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) "
						+ "WHERE s.id IN {structures} AND u.id IN {users} " + "RETURN count(distinct u) as nb ";
				final JsonArray users = object.getJsonArray("users", new fr.wseduc.webutils.collections.JsonArray());
				JsonObject params = new JsonObject()
						.put("structures", new fr.wseduc.webutils.collections.JsonArray(adminLocal.getScope()))
						.put("users", users);
				Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						JsonArray res = message.body().getJsonArray("result");
						handler.handle("ok".equals(message.body().getString("status")) && res != null && res.size() == 1
								&& res.getJsonObject(0).getInteger("nb", -1).equals(users.size()));
					}
				});
			}
		});
	}

	private void isAdmin(HttpServerRequest request, UserInfos user, Handler<Boolean> handler) {
		UserInfos.Function adminLocal = getFunction(user, handler);
		if (adminLocal == null)
			return;
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

	@SuppressWarnings("unchecked")
	private void authorizeDocuments(HttpServerRequest request, UserInfos user, String serviceMethod,
			Handler<Boolean> handler) {
		bodyToJson(request, body -> {
			JsonArray idsArray = body.getJsonArray("ids");
			if (idsArray != null && !idsArray.isEmpty()) {
				ElementQuery query = new ElementQuery(true);
				query.setIds(idsArray.getList());
				executeCountQuery(request, user, query, idsArray.size(), handler);
			} else {
				handler.handle(false);
			}
		});
	}

	private void authorizeGetDocument(HttpServerRequest request, UserInfos user, String serviceMethod,
			Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			ElementQuery query = new ElementQuery(true);
			query.setVisibilitiesOr(new HashSet<>());
			query.getVisibilitiesOr().add("public");
			query.getVisibilitiesOr().add("protected");
			query.setId(id);
			executeCountQuery(request, user, query, 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeDocument(HttpServerRequest request, UserInfos user, String serviceMethod,
			Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			ElementQuery query = new ElementQuery(true);
			query.setId(id);
			executeCountQuery(request, user, query, 1, handler);

		} else {
			handler.handle(false);
		}
	}

	private void authorizeRevisionOwner(HttpServerRequest request, UserInfos user, String serviceMethod,
			final Handler<Boolean> handler) {
		final String revisionId = request.params().get("revisionId");
		JsonObject query = new JsonObject(
				"{ \"_id\": \"" + revisionId + "\", \"userId\": \"" + user.getUserId() + "\" }");

		if (revisionId != null && !revisionId.trim().isEmpty()) {
			executeCountQuery(request, Workspace.REVISIONS_COLLECTION, query, 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeCommentOwner(HttpServerRequest request, UserInfos user, String serviceMethod,
			final Handler<Boolean> handler) {
		final String id = request.params().get("id");
		final String commentId = request.params().get("commentId");
		JsonObject query = new JsonObject("{ \"_id\": \"" + id + "\", \"comments.id\": \"" + commentId
				+ "\", \"comments.author\": \"" + user.getUserId() + "\"  }");

		if (commentId != null && !commentId.trim().isEmpty()) {
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION, query, 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void executeCountQuery(final HttpServerRequest request, UserInfos user, ElementQuery query,
			final int expectedCountResult, Handler<Boolean> h) {
		request.pause();
		folderManager.countByQuery(query, user, res -> {
			request.resume();
			if (res.succeeded()) {
				h.handle(res.result().intValue() == expectedCountResult);
			} else {
				h.handle(false);
			}
		});
	}

	private void executeCountQuery(final HttpServerRequest request, String collection, JsonObject query,
			final int expectedCountResult, final Handler<Boolean> handler) {
		request.pause();
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonObject res = event.body();
				handler.handle(res != null && "ok".equals(res.getString("status"))
						&& expectedCountResult == res.getInteger("count"));
			}
		});
	}

}
