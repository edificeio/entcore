package edu.one.core.workspace.security;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.RackDao;

public class WorkspaceResourcesProvider implements ResourcesProvider {

	private MongoDb mongo;

	public WorkspaceResourcesProvider(MongoDb mongo) {
		this.mongo = mongo;
	}

	@Override
	public void authorize(HttpServerRequest resourceRequest, UserInfos user,
			final Handler<Boolean> handler) {
		if (resourceRequest != null && resourceRequest.path() != null) {
			String path = resourceRequest.path().trim();
			String collection = null;
			if (path.contains("rack")) {
				collection = RackDao.RACKS_COLLECTION;
			} else if (path.contains("document")) {
				collection = DocumentDao.DOCUMENTS_COLLECTION;
			}
			final JsonObject q = generateQuery(resourceRequest, user);
			if (q != null && q.getObject("query") != null && q.getInteger("expectedCountResult") > 0) {
				mongo.count(collection, q.getObject("query"), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonObject res = event.body();
						handler.handle(
							res != null &&
							"ok".equals(res.getString("status")) &&
							q.getInteger("expectedCountResult").equals(res.getInteger("count"))
						);
					}
				});
			} else {
				handler.handle(false);
			}
		} else {
			handler.handle(false);
		}
	}

	private JsonObject generateQuery(HttpServerRequest resourceRequest, UserInfos user) {
		String query = null;
		int expectedCountResult = 0;
		if (resourceRequest.params().contains("ids")) {
			String ids = resourceRequest.params().get("ids");
			if (ids != null && ids.trim().isEmpty()) {
				JsonArray idsArray = new JsonArray(ids.split(","));
				query = "{ \"_id\": { \"$in\" : " + idsArray.encode() +
						"}, \"$or\" : [{ \"owner\": \"" + user.getId() +
						"\"}, \"shared\" : { \"$elemMatch\" : { \"userId\": \""
						+ user.getId() + "\"}}]}";
				expectedCountResult = idsArray.size();
			}
		} else if (resourceRequest.params().contains("id")) {
			String id = resourceRequest.params().get("id");
			if (id != null && id.trim().isEmpty()) {
				query = "{ \"_id\": \"" + id + "\", \"$or\" : [{ \"owner\": \"" + user.getId() +
						"\"}, \"shared\" : { \"$elemMatch\" : { \"userId\": \""
						+ user.getId() + "\"}}]}";
				expectedCountResult = 1;
			}
		} else {
			return null;
		}
		return new JsonObject()
			.putObject("query", new JsonObject(query))
			.putNumber("expectedCountResult", expectedCountResult);
	}

}
