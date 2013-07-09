package edu.one.core.workspace.dao;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
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

	public void delete(String id, final Handler<JsonObject> handler) {
		mongo.delete(DOCUMENTS_COLLECTION, idMatcher(id), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle(res.body());
			}
		});
	}

	public void update(String id, JsonObject obj, final Handler<JsonObject> handler) {
		mongo.update(DOCUMENTS_COLLECTION, idMatcher(id), obj, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonObject r = res.body();
				if (r == null) {
					r = new JsonObject();
				}
				handler.handle(r);
			}
		});
	}

	public void save(JsonObject document, final Handler<JsonObject> handler) {
		mongo.save(DOCUMENTS_COLLECTION, document, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonObject r = res.body();
				if (r == null) {
					r = new JsonObject();
				}
				handler.handle(r);
			}
		});
	}

	public void find(JsonObject query, final Handler<JsonObject> handler) {
		mongo.find(DOCUMENTS_COLLECTION, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonObject r = res.body();
				if (r == null) {
					r = new JsonObject();
				}
				handler.handle(r);
			}
		});
	}
}
