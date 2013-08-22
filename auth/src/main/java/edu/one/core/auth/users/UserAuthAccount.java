package edu.one.core.auth.users;

import org.vertx.java.core.Handler;

public interface UserAuthAccount {

	void activateAccount(String login, String activationCode, String password,
			Handler<Boolean> handler);

}
