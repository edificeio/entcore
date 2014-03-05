package org.entcore.workspace.dao;

import fr.wseduc.mongodb.MongoDb;

public class DocumentDao extends GenericDao {

	public static final String DOCUMENTS_COLLECTION = "documents";

	public DocumentDao(MongoDb mongo) {
		super(mongo, DOCUMENTS_COLLECTION);
	}

}
