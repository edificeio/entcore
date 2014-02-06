package org.entcore.auth.users;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public interface UserAuthAccount {

	void activateAccount(String login, String activationCode, String password,
			Handler<Boolean> handler);

	void forgotPassword(HttpServerRequest request, String login, Handler<Boolean> handler);

	void resetPassword(String login, String resetCode, String password, Handler<Boolean> handler);

	void changePassword(String login, String password, Handler<Boolean> handler);

	void sendResetCode(HttpServerRequest request, String login, String email, Handler<Boolean> handler);

	void blockUser(String id, boolean block, Handler<Boolean> handler);

}
