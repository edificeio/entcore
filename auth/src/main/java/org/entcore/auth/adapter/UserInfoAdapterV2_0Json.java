package org.entcore.auth.adapter;

import org.vertx.java.core.json.JsonObject;

public class UserInfoAdapterV2_0Json implements UserInfoAdapter {

	@Override
	public JsonObject getInfo(JsonObject info, String clientId) {
		JsonObject s = info.copy();
		s.removeField("cache");
		return s;
	}

}
