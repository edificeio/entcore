package edu.one.core.workspace.dao;

import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.MongoDb;

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
