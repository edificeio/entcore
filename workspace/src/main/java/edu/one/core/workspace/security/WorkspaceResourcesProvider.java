package edu.one.core.workspace.security;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.service.WorkspaceService;

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
					"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
					+ user.getUserId()+ "\", \"" + serviceMethod.replaceAll("\\.", "-") + "\": true }}}]}";
			executeCountQuery(request, DocumentDao.DOCUMENTS_COLLECTION,
					new JsonObject(query), idsArray.size(), handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeGetDocument(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"$or\" : [{ \"owner\": \"" + user.getUserId() +
					"\"}, { \"protected\" : true}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
					+ user.getUserId()+ "\", \"" + serviceMethod.replaceAll("\\.", "-") + "\": true }}}]}";
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
					"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
					+ user.getUserId()+ "\", \"" + serviceMethod.replaceAll("\\.", "-") + "\": true }}}]}";
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
