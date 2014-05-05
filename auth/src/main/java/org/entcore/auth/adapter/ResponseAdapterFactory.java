/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.auth.adapter;

import fr.wseduc.webutils.Utils;
import org.vertx.java.core.http.HttpServerRequest;

public class ResponseAdapterFactory {

	public static UserInfoAdapter getUserInfoAdapter(HttpServerRequest request) {
		String version = Utils.getOrElse(request.params().get("version"), "");
		switch (version) {
			case "v1.0":
				return new UserInfoAdapterV1_0Json();
			default:
				return new UserInfoAdapterV1_0Json();
		}
	}

}
