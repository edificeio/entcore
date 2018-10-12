/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.oauth.OpenIdConnectClient;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;
import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.auth.services.OpenIdServiceProviderFactory;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class OpenIdConnectController extends AbstractFederateController {

	private static final String SCOPE_OPENID = "openid profile";
	private OpenIdServiceProviderFactory openIdConnectServiceProviderFactory;
	private JsonObject certificates = new JsonObject();
	private boolean subMapping;

	@Get("/openid/certs")
	public void certs(HttpServerRequest request) {
		renderJson(request, certificates);
	}

	@Get("/openid/login")
	public void login(HttpServerRequest request) {
		OpenIdConnectClient oic = openIdConnectServiceProviderFactory.openIdClient(request);
		if (oic == null) return;
		final String state = UUID.randomUUID().toString();
		CookieHelper.getInstance().setSigned("csrfstate", state, 900, request);
		final String nonce = UUID.randomUUID().toString();
		CookieHelper.getInstance().setSigned("nonce", nonce, 900, request);
		oic.authorizeRedirect(request, state, nonce, SCOPE_OPENID);
	}

	@Get("/openid/authenticate")
	public void authenticate(final HttpServerRequest request) {
		final OpenIdConnectServiceProvider openIdConnectServiceProvider = openIdConnectServiceProviderFactory.serviceProvider(request);
		if (openIdConnectServiceProvider == null) return;
		OpenIdConnectClient oic = openIdConnectServiceProviderFactory.openIdClient(request);
		if (oic == null) return;
		final String state = CookieHelper.getInstance().getSigned("csrfstate", request);
		if (state == null) {
			forbidden(request, "invalid_state");
			return;
		}
		final String nonce = CookieHelper.getInstance().getSigned("nonce", request);
		if (nonce == null) {
			forbidden(request, "invalid_replay");
			return;
		}
		oic.authorizationCodeToken(request, state, nonce, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject payload) {
				if (payload != null) {
					log.info("payload : " + payload.encode());
					openIdConnectServiceProvider.executeFederate(payload, new Handler<Either<String, Object>>() {
						@Override
						public void handle(Either<String, Object> res) {
							if (res.isRight() && res.right().getValue() instanceof JsonObject) {
								authenticate((JsonObject) res.right().getValue(), "_", payload.getString("id_token_hint"), request);
							} else if (subMapping && res.isLeft() && OpenIdConnectServiceProvider.UNRECOGNIZED_USER_IDENTITY
									.equals(res.left().getValue())) {
								final String p = payload.encode();
								try {
									JsonObject params = new JsonObject().put("payload", p)
											.put("key", HmacSha1.sign(p, signKey));
									renderView(request, params, "mappingFederatedUser.html", null);
								} catch (Exception e) {
									log.error("Error loading mapping openid connect identity.", e);
									renderError(request);
								}
							} else {
								forbidden(request, "invalid.payload");
							}
						}
					});
				} else {
					forbidden(request, "invalid_token");
				}
			}
		});
	}

	@Post("/openid/mappingUser")
	public void mappingUser(final HttpServerRequest request) {
		final OpenIdConnectServiceProvider openIdConnectServiceProvider = openIdConnectServiceProviderFactory.serviceProvider(request);
		if (openIdConnectServiceProvider == null) return;
		if (!subMapping) {
			forbidden(request, "unauthorized.sub.mapping");
			return;
		}
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String login = request.formAttributes().get("login");
				final String password = request.formAttributes().get("password");
				final String payload = request.formAttributes().get("payload");
				final String key = request.formAttributes().get("key");
				try {
					if (isEmpty(login) || isEmpty(password) || isEmpty(payload) || isEmpty(key) ||
							!key.equals(HmacSha1.sign(payload, signKey))) {
						badRequest(request, "invalid.attribute");
						return;
					}
					final JsonObject p = new JsonObject(payload);
					openIdConnectServiceProvider.mappingUser(login, password, p,
							new Handler<Either<String, Object>>() {
						@Override
						public void handle(Either<String, Object> event) {
							if (event.isRight()) {
								authenticate((JsonObject) event.right().getValue(), "_", p.getString("id_token_hint"), request);
							} else {
								forbidden(request, "invalid.sub.mapping");
							}
						}
					});
				} catch (Exception e) {
					log.error("Error mapping OpenId Connect user.", e);
					badRequest(request, "invalid.attribute");
				}
			}
		});
	}

	@Get("/openid/slo")
	public void slo(final HttpServerRequest request) {
		sloUser(request);
	}

	@Override
	protected void afterDropSession(JsonObject meta, HttpServerRequest request, UserInfos user, String c) {
		OpenIdConnectClient oic = openIdConnectServiceProviderFactory.openIdClient(request);
		if (oic != null && meta != null && isNotEmpty(meta.getString("NameID"))) {
			String callback = oic.logoutUri(UUID.randomUUID().toString(), meta.getString("NameID"), c);
			AuthController.logoutCallback(request, callback, config, eb);
		} else {
			AuthController.logoutCallback(request, c, config, eb);
		}
	}

	public void setOpenIdConnectServiceProviderFactory(OpenIdServiceProviderFactory openIdConnectServiceProviderFactory) {
		this.openIdConnectServiceProviderFactory = openIdConnectServiceProviderFactory;
	}

	public void setCertificates(JsonObject certificates) {
		this.certificates = certificates;
	}

	public void setSubMapping(boolean subMapping) {
		this.subMapping = subMapping;
	}

}
