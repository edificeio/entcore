package org.entcore.workspace.dao;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;

public class GenericDao {

	protected final MongoDb mongo;
	protected final String collection;

	public GenericDao(MongoDb mongo, String collection) {
		this.mongo = mongo;
		this.collection = collection;
	}

	public void findById(String id, final Handler<JsonObject> handler) {
		mongo.findOne(collection,  idMatcher(id), new Handler<Message<JsonObject>>() {
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

	public void findById(String id, String onwer, final Handler<JsonObject> handler) {
		mongo.findOne(collection,  idMatcher(id, onwer), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle(res.body());
			}
		});
	}

	protected JsonObject idMatcher(String id, String owner) {
		String query;
		if (owner != null && !owner.trim().isEmpty()) {
			query = "{ \"_id\": \"" + id + "\", \"owner\" : \"" + owner + "\"}";
		} else {
			query = "{ \"_id\": \"" + id + "\"}";
		}
		return new JsonObject(query);
	}


	
	public void delete(String id, final Handler<JsonObject> handler) {
		mongo.delete(collection, idMatcher(id), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle(res.body());
			}
		});
	}

	public void update(String id, JsonObject obj, final Handler<JsonObject> handler) {
		mongo.update(collection, idMatcher(id), obj, new Handler<Message<JsonObject>>() {
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

	public void update(String id, JsonObject obj, String owner, final Handler<JsonObject> handler) {
		mongo.update(collection, idMatcher(id, owner), obj, new Handler<Message<JsonObject>>() {
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
		mongo.save(collection, document, new Handler<Message<JsonObject>>() {
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
		mongo.find(collection, query, new Handler<Message<JsonObject>>() {
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
