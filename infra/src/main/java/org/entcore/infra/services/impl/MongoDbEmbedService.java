package org.entcore.infra.services.impl;

import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.infra.services.EmbedService;

public class MongoDbEmbedService extends MongoDbCrudService implements EmbedService {

	public MongoDbEmbedService(String collection) {
		super(collection);
	}

}
