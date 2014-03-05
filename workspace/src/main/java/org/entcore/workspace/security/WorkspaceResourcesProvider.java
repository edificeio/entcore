package org.entcore.workspace.security;

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

public class WorkspaceResourcesProvider implements ResourcesProvider {

	private MongoDb mongo;

	public WorkspaceResourcesProvider(MongoDb mongo) {
		this.mongo = mongo;
	}

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		String method = binding.getServiceMethod()
				.substring(WorkspaceService.class.getName().length() + 1);
		switch (method) {
		case "getDocument":
			authorizeGetDocument(request, user, binding.getServiceMethod(), handler);
			break;
		case "commentDocument":
		case "updateDocument":
		case "moveDocument":
		case "moveTrash":
		case "copyDocument":
		case "deleteDocument":
		case "restoreTrash":
			authorizeDocument(request, user, binding.getServiceMethod(), handler);
			break;
		case "moveDocuments":
		case "copyDocuments":
			authorizeDocuments(request, user, binding.getServiceMethod(), handler);
			break;
		default:
			handler.handle(false);
		}
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
		if (user.getProfilGroupsIds() != null) {
			for (String groupId: user.getProfilGroupsIds()) {
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
					"\"}, { \"protected\" : true}, {\"shared\" : { \"$elemMatch\" : " +
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
