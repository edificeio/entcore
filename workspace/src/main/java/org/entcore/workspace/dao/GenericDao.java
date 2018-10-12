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

package org.entcore.workspace.dao;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

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
