package edu.one.core.workspace.dao;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;

public class DocumentDao {

	private static final String DOCUMENTS_COLLECTION = "documents";
	private MongoDb mongo;

	public DocumentDao(MongoDb mongo) {
		this.mongo = mongo;
	}

	public void findById(String id, final Handler<JsonObject> handler) {
		mongo.findOne(DOCUMENTS_COLLECTION,  idMatcher(id), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle(res.body());
			}
		});
	}

	private JsonObject idMatcher(String id) {
		String query = "{ \"_id\": \"" + id + "\"}";
		return new JsonObject(query);
	}

	public void delete(String id, final HttpServerResponse response) {
		mongo.delete(DOCUMENTS_COLLECTION, idMatcher(id), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					response.setStatusCode(204).end(res.body().toString());
				} else {
					response.setStatusCode(500).end(res.body().toString());
				}
			}
		});
	}

}
