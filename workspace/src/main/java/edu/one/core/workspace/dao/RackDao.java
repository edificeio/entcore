package edu.one.core.workspace.dao;

import edu.one.core.infra.MongoDb;

public class RackDao extends GenericDao {

	public static final String RACKS_COLLECTION = "racks";

	public RackDao(MongoDb mongo) {
		super(mongo, RACKS_COLLECTION);
	}

}
