/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.auth.adapter;

import org.vertx.java.core.json.JsonObject;

public interface UserInfoAdapter {

	JsonObject getInfo(JsonObject info, String clientId);

}
