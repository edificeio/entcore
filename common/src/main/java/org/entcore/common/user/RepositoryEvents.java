/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.user;

import org.vertx.java.core.json.JsonArray;

public interface RepositoryEvents {

	void deleteGroups(JsonArray groups);

	void deleteUsers(JsonArray users);

}
