package edu.one.core.auth;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Server;

public class Auth extends Server {

	@Override
	public void start() {
		super.start();

		Controller auth = new AuthController(vertx, container, rm, securedActions);

		auth.get("/login", "login");

		auth.post("/login", "loginSubmit");

		auth.get("/logout", "logout");

		auth.get("/oauth2/auth", "authorize");

		auth.post("/oauth2/token", "token");

	}

}
