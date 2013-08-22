package edu.one.core.auth.users;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Neo;
import edu.one.core.infra.security.BCrypt;

public class DefaultUserAuthAccount implements UserAuthAccount {

	private final Neo neo;

	public DefaultUserAuthAccount(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void activateAccount(String login, String activationCode, String password,
			final Handler<Boolean> handler) {
		String query =
				"START n=node:node_auto_index(ENTPersonLogin={login}) " +
				"WHERE n.activationCode = {activationCode} AND n.ENTPersonMotDePasse? IS NULL " +
				"SET n.ENTPersonMotDePasse = {password}, n.activationCode = null " +
				"RETURN n.ENTPersonMotDePasse";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				handler.handle("ok".equals(res.body().getString("status"))
						&& res.body().getObject("result").getObject("0") != null);
			}
		});
	}

}
