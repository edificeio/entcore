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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.events.EventStore;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public abstract class AbstractFederateController extends BaseController {

	public static final String LOGIN_PAGE = "/auth/login";
	private UserAuthAccount userAuthAccount;
	private EventStore eventStore;
	protected String signKey;

	protected void authenticate(JsonObject res, String sessionIndex, String nameId, JsonObject activationThemes, HttpServerRequest request) {
		final String userId = res.getString("id");
		final String activationCode = res.getString("activationCode");
		final String login = res.getString("login");
		final String email = res.getString("email");
		final String mobile = res.getString("mobile");
		final String theme = activationThemes.getJsonObject(Renders.getHost(request), new JsonObject()).getString(res.getString("source"));
		if (userId != null) {
			userAuthAccount.storeDomain(userId, getHost(request), getScheme(request), new Handler<Boolean>() {
				@Override
				public void handle(Boolean event) {
					if (Boolean.FALSE.equals(event)) {
						log.error("[Federate] Error while storing last known domain for user " + userId);
					}
				}
			});
		}
		if (activationCode != null && login != null) {
			activateUser(activationCode, login, email, mobile, theme, sessionIndex, nameId, request);
		} else if (activationCode == null && userId != null && !userId.trim().isEmpty()) {
			log.info("Connexion de l'utilisateur fédéré " + login);
			eventStore.createAndStoreEvent(AuthController.AuthEvent.LOGIN.name(), login);
			createSession(userId, sessionIndex, nameId, request);
		} else {
			redirect(request, LOGIN_PAGE);
		}
	}

	protected void createSession(String userId, String sessionIndex, String nameId, final HttpServerRequest request) {
		UserUtils.createSession(eb, userId, sessionIndex, nameId,
				new io.vertx.core.Handler<String>() {

			@Override
			public void handle(String sessionId) {
				if (sessionId != null && !sessionId.trim().isEmpty()) {
					long timeout = config.getLong("cookie_timeout", Long.MIN_VALUE);
					CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
					CookieHelper.set("authenticated", "true", timeout, request);
					final String callback = CookieHelper.getInstance().getSigned("callback", request);
					if (isNotEmpty(callback)) {
						redirect(request, callback, "");
					} else {
						redirect(request, "/");
					}
				} else {
					redirect(request, LOGIN_PAGE);
				}
			}
		});
	}

	private void activateUser(final String activationCode, final String login, String email, String mobile, String theme,
							  final String sessionIndex, final String nameId, final HttpServerRequest request) {
		userAuthAccount.activateAccountWithRevalidateTerms(login, activationCode, UUID.randomUUID().toString(),
				email, mobile, theme, request, new Handler<Either<String, String>>() {
			@Override
			public void handle(Either<String, String> activated) {
				if (activated.isRight() && activated.right().getValue() != null) {
					log.info("Activation du compte utilisateur " + login);
					eventStore.createAndStoreEvent(AuthController.AuthEvent.ACTIVATION.name(), login);
					createSession(activated.right().getValue(), sessionIndex, nameId, request);
				} else {
					log.info("Echec de l'activation : compte utilisateur " + login +
							" introuvable ou déjà activé.");
					JsonObject error = new JsonObject()
							.put("error", new JsonObject()
									.put("message", I18n.getInstance()
											.translate(activated.left().getValue(), getHost(request), I18n.acceptLanguage(request))));
					error.put("activationCode", activationCode);
					renderJson(request, error);
				}
			}
		});
	}

	protected void sloUser(final HttpServerRequest request) {
		final String c = request.params().get("callback");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null && user.getFederated()) {
					final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
					UserUtils.deleteSessionWithMetadata(eb, sessionId, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if (event != null) {
								CookieHelper.set("oneSessionId", "", 0l, request);
								CookieHelper.set("authenticated", "", 0l, request);
								afterDropSession(event, request, user, c);
							} else {
								AuthController.logoutCallback(request, c, config, eb);
							}
						}
					});
				} else {
					AuthController.logoutCallback(request, c, config, eb);
				}
			}
		});
	}

	protected abstract void afterDropSession(JsonObject event, HttpServerRequest request, UserInfos user, String c);

	public void setUserAuthAccount(UserAuthAccount userAuthAccount) {
		this.userAuthAccount = userAuthAccount;
	}

	public void setEventStore(EventStore eventStore) {
		this.eventStore = eventStore;
	}

	public void setSignKey(String signKey) {
		this.signKey = signKey;
	}

}
