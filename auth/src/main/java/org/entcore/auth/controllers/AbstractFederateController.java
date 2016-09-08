/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.auth.controllers;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.CookieHelper;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.events.EventStore;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

public abstract class AbstractFederateController extends BaseController {

	public static final String LOGIN_PAGE = "/auth/login";
	private UserAuthAccount userAuthAccount;
	private EventStore eventStore;

	protected void authenticate(JsonObject res, String sessionIndex, String nameId, HttpServerRequest request) {
		final String userId = res.getString("id");
		final String activationCode = res.getString("activationCode");
		final String login = res.getString("login");
		final String email = res.getString("email");
		final String mobile = res.getString("mobile");
		if (activationCode != null && login != null) {
			activateUser(activationCode, login, email, mobile, sessionIndex, nameId, request);
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
				new org.vertx.java.core.Handler<String>() {

			@Override
			public void handle(String sessionId) {
				if (sessionId != null && !sessionId.trim().isEmpty()) {
					long timeout = container.config().getLong("cookie_timeout", Long.MIN_VALUE);
					CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
					redirect(request, "/");
				} else {
					redirect(request, LOGIN_PAGE);
				}
			}
		});
	}

	private void activateUser(final String activationCode, final String login, String email, String mobile,
							  final String sessionIndex, final String nameId, final HttpServerRequest request) {
		userAuthAccount.activateAccount(login, activationCode, UUID.randomUUID().toString(),
				email, mobile, request, new Handler<Either<String, String>>() {
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
							.putObject("error", new JsonObject()
									.putString("message", I18n.getInstance()
											.translate("activation.error", getHost(request), I18n.acceptLanguage(request))));
					error.putString("activationCode", activationCode);
					renderJson(request, error);
				}
			}
		});
	}


	public void setUserAuthAccount(UserAuthAccount userAuthAccount) {
		this.userAuthAccount = userAuthAccount;
	}

	public void setEventStore(EventStore eventStore) {
		this.eventStore = eventStore;
	}

}
