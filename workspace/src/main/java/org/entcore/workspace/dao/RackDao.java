package org.entcore.workspace.dao;

import org.vertx.java.core.json.JsonObject;

import fr.wseduc.mongodb.MongoDb;

public class RackDao extends GenericDao {

	public static final String RACKS_COLLECTION = "racks";

	public RackDao(MongoDb mongo) {
		super(mongo, RACKS_COLLECTION);
	}

	@Override
	protected JsonObject idMatcher(String id, String owner) {
		String query = "{ \"_id\": \"" + id + "\", \"to\" : \"" + owner + "\"}";
		return new JsonObject(query);
	}

}
