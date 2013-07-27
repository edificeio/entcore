package edu.one.core.workspace.security;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.RackDao;
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
		case "getRackDocument":
			authorizeGetRackDocument(request, user, handler);
			break;
		case "deleteDocument":
			authorizeDeleteDocument(request, user, handler);
			break;
		case "deleteRackDocument":
			authorizeDeleteRackDocument(request, user, handler);
			break;
		default:
			handler.handle(false);
		}
	}

	private void authorizeDeleteRackDocument(HttpServerRequest request,
			UserInfos user, Handler<Boolean> handler) {
		authorizeGetRackDocument(request, user, handler);
	}

	private void authorizeDeleteDocument(HttpServerRequest request,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"owner\": \"" + user.getUserId() + "\"}";
			executeCountQuery(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeGetRackDocument(HttpServerRequest request,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"to\" : \"" + user.getUserId() + "\"}";
			executeCountQuery(RackDao.RACKS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeGetDocument(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query = "{ \"_id\": \"" + id + "\", \"$or\" : [{ \"owner\": \"" + user.getUserId() +
					"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
					+ user.getUserId()+ "\", \"" + serviceMethod + "\": true }}}]}";
			executeCountQuery(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private void executeCountQuery(String collection, JsonObject query, final int expectedCountResult,
			final Handler<Boolean> handler) {
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
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
