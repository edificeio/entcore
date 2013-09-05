package edu.one.core.auth;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Server;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.infra.request.filter.UserAuthFilter;
import edu.one.core.infra.security.oauth.DefaultOAuthResourceProvider;

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

		auth.get("/oauth2/userinfo", "userInfo"); // TODO support version (api required version in headers)

		auth.get("/activation", "activeAccount");

		auth.post("/activation", "activeAccountSubmit");

		auth.get("/forgot", "forgotPassword");

		auth.post("/forgot", "forgotPasswordSubmit");

		auth.get("/reset/:resetCode", "resetPassword");

		auth.post("/reset", "resetPasswordSubmit");

		try {
			auth.registerMethod(config.getString("address", "wse.oauth"), "oauthResourceServer");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.clearFilters();
		SecurityHandler.addFilter(
				new UserAuthFilter(new DefaultOAuthResourceProvider(Server.getEventBus(vertx))));
	}

}
