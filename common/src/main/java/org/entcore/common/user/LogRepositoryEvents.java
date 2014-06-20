/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.user;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class LogRepositoryEvents implements RepositoryEvents {
	private static final Logger log = LoggerFactory.getLogger(LogRepositoryEvents.class);

	@Override
	public void exportResources(String exportId, String userId, JsonArray groups, String exportPath,
			String locale) {
		log.info("Export " + userId + " resources on path " + exportPath);
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		log.info(groups.encode());
	}

	@Override
	public void deleteUsers(JsonArray users) {
		log.info(users.encode());
	}

}
