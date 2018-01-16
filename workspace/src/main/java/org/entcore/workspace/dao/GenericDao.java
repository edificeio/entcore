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

package org.entcore.workspace.dao;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.mongodb.MongoDb;

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

	public void findById(String id, JsonObject keys, final Handler<JsonObject> handler) {
		mongo.findOne(collection,  idMatcher(id), keys, new Handler<Message<JsonObject>>() {
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
		findById(id, onwer, false, handler);
	}

	public void findById(String id, String onwer, boolean publicOnly, final Handler<JsonObject> handler) {
		mongo.findOne(collection,  idMatcher(id, onwer, publicOnly), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle(res.body());
			}
		});
	}

	protected JsonObject idMatcher(String id, String owner) {
		return idMatcher(id, owner, false);
	}

	protected JsonObject idMatcher(String id, String owner, boolean publicOnly) {
		String query;
		if (owner != null && !owner.trim().isEmpty()) {
			query = "{ \"_id\": \"" + id + "\", \"owner\" : \"" + owner + "\"}";
		} else if (publicOnly) {
			query = "{ \"_id\": \"" + id + "\", \"public\" : true}";
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
