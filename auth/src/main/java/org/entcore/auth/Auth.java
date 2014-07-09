/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.auth;

import org.entcore.auth.security.AuthResourcesProvider;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import org.entcore.common.neo4j.Neo;
import org.vertx.java.core.eventbus.EventBus;

public class Auth extends Server {

	@Override
	public void start() {
		super.start();

		Controller auth = new AuthController(vertx, container, rm, trace, securedActions);

		auth.get("/login", "login");
		auth.get("/context", "context");
		auth.post("/login", "loginSubmit");

		auth.get("/logout", "logout");

		auth.get("/oauth2/auth", "authorize");

		auth.post("/oauth2/token", "token");

		auth.get("/oauth2/userinfo", "userInfo");

		auth.get("/activation", "activeAccount");

		auth.post("/activation", "activeAccountSubmit");

		auth.get("/forgot", "forgotPassword");

		auth.post("/forgot", "forgotPasswordSubmit");

		auth.get("/reset/:resetCode", "resetPassword");

		auth.post("/reset", "resetPasswordSubmit");

		auth.post("/sendResetPassword", "sendResetPassword");

		auth.put("/block/:userId", "blockUser");

		auth.get("/cgu", "cgu");

		try {
			auth.registerMethod(config.getString("address", "wse.oauth"), "oauthResourceServer");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		final EventBus eb = getEventBus(vertx);
		SecurityHandler.clearFilters();
		SecurityHandler.addFilter(
				new UserAuthFilter(new DefaultOAuthResourceProvider(eb)));
		SecurityHandler.addFilter(new ActionFilter(auth.securedUriBinding(),
				eb, new AuthResourcesProvider(new Neo(eb, container.logger()))));
	}

}
