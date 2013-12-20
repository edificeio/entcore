package edu.one.core.auth.adapter;

import org.vertx.java.core.json.JsonObject;

public interface UserInfoAdapter {

	JsonObject getInfo(JsonObject info, String clientId);

}
