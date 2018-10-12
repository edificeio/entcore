/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.cas.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.cas.endpoint.Credential;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.cas.http.OAuthWrappedRequest;
import org.entcore.cas.http.WrappedRequest;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class CredentialController extends BaseController {

	private Credential credential;

	@Get("/login")
	public void login(HttpServerRequest request) {
		credential.loginRequestor(new WrappedRequest(request));
	}

	@Get("/oauth/login")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void loginOauth(HttpServerRequest request) {
		credential.loginRequestor(new OAuthWrappedRequest(request));
	}

	@BusAddress("cas")
	public void busEvents(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "logout" :
				credential.logout(message.body().getString("userId"));
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
	}

}
