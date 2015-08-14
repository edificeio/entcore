/*
 * Copyright © WebServices pour l'Éducation, 2015
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

import fr.wseduc.rs.Post;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.CookieHelper;
import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.events.EventStore;
import org.entcore.common.user.UserUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.ConfigurationException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.util.UUID;

public class SamlController extends BaseController {

	public static final String LOGIN_PAGE = "/auth/login";
	private SamlServiceProviderFactory spFactory;
	private UserAuthAccount userAuthAccount;
	private EventStore eventStore;

	public SamlController() throws ConfigurationException {
		DefaultBootstrap.bootstrap();
	}

	@Post("/saml/acs")
	public void acs(final HttpServerRequest request) {
		validateResponseAndGetAssertion(request, new Handler<Assertion>() {
			@Override
			public void handle(Assertion assertion) {
				SamlServiceProvider sp = spFactory.serviceProvider(assertion);
				sp.execute(assertion, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isLeft()) {
							redirect(request, LOGIN_PAGE);
						} else {
							final JsonObject res = event.right().getValue();
							final String userId = res.getString("id");
							final String activationCode = res.getString("activationCode");
							final String login = res.getString("login");
							final String email = res.getString("email");
							final String mobile = res.getString("mobile");
							if (activationCode != null && login != null) {
								activateUser(activationCode, login, email, mobile, request);
							} else if (activationCode == null && userId != null && !userId.trim().isEmpty()) {
								createSession(userId, request);
							} else {
								redirect(request, LOGIN_PAGE);
							}
						}
					}
				});
			}
		});
	}

	private void createSession(String userId, final HttpServerRequest request) {
		UserUtils.createSession(eb, userId,
				new org.vertx.java.core.Handler<String>() {

					@Override
					public void handle(String sessionId) {
						if (sessionId != null && !sessionId.trim().isEmpty()) {
							boolean rememberMe = "true".equals(request.formAttributes().get("rememberMe"));
							long timeout = rememberMe ? 3600l * 24 * 365 : container.config()
									.getLong("cookie_timeout", Long.MIN_VALUE);
							CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
							redirect(request, "/");
						} else {
							redirect(request, LOGIN_PAGE);
						}
					}
				});
	}

	private void activateUser(final String activationCode, final String login, String email, String mobile,
			final HttpServerRequest request) {
		userAuthAccount.activateAccount(login, activationCode, UUID.randomUUID().toString(),
				email, mobile, request, new Handler<Either<String, String>>() {
			@Override
			public void handle(Either<String, String> activated) {
				if (activated.isRight() && activated.right().getValue() != null) {
					log.info("Activation du compte utilisateur " + login);
					eventStore.createAndStoreEvent(AuthController.AuthEvent.ACTIVATION.name(), login);
						createSession(activated.right().getValue(), request);
				} else {
					log.info("Echec de l'activation : compte utilisateur " + login +
							" introuvable ou déjà activé.");
					JsonObject error = new JsonObject()
							.putObject("error", new JsonObject()
							.putString("message", I18n.getInstance()
									.translate("activation.error", request.headers().get("Accept-Language"))));
					error.putString("activationCode", activationCode);
					renderJson(request, error);
				}
			}
		});
	}

	private void validateResponseAndGetAssertion(final HttpServerRequest request, final Handler<Assertion> handler) {
		getSamlResponse(request, new Handler<String>() {
			@Override
			public void handle(final String samlResponse) {
				if (samlResponse != null && samlResponse.contains("EncryptedAssertion")) {
					JsonObject j = new JsonObject()
							.putString("action", "validate-signature-decrypt").putString("response", samlResponse);
					vertx.eventBus().send("saml", j, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							String assertion = event.body().getString("assertion");
							if ("ok".equals(event.body().getString("status")) && event.body().getBoolean("valid", false) &&
									assertion != null) {
								try {
									handler.handle(SamlUtils.unmarshallAssertion(assertion));
								} catch (Exception e) {
									log.error(e.getMessage(), e);
									redirect(request, LOGIN_PAGE);
								}
							} else {
								redirect(request, LOGIN_PAGE);
							}
						}
					});
				} else if (samlResponse != null) {
					JsonObject j = new JsonObject()
							.putString("action", "validate-signature").putString("response", samlResponse);
					vertx.eventBus().send("saml", j, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status")) && event.body().getBoolean("valid", false)) {
								try {
									Response response = SamlUtils.unmarshallResponse(samlResponse);
									if (response.getAssertions() == null || response.getAssertions().size() != 1) {
										redirect(request, LOGIN_PAGE);
										return;
									}
									handler.handle(response.getAssertions().get(0));
								} catch (Exception e) {
									log.error(e.getMessage(), e);
									redirect(request, LOGIN_PAGE);
								}
							} else {
								redirect(request, LOGIN_PAGE);
							}
						}
					});
				} else {
					redirect(request, LOGIN_PAGE);
				}
			}
		});
	}

	private void getSamlResponse(final HttpServerRequest request, final Handler<String> handler) {
		log.debug("getSamlResponse");
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				log.debug("handle");
				String samlResponse = request.formAttributes().get("SAMLResponse");
				log.debug(samlResponse);
				if (samlResponse != null && !samlResponse.trim().isEmpty()) {
					handler.handle(new String(Base64.decode(samlResponse)));
				} else {
					badRequest(request); // TODO replace by error page
				}
			}
		});
	}

	public void setServiceProviderFactory(SamlServiceProviderFactory spFactory) {
		this.spFactory = spFactory;
	}

	public void setUserAuthAccount(UserAuthAccount userAuthAccount) {
		this.userAuthAccount = userAuthAccount;
	}

	public void setEventStore(EventStore eventStore) {
		this.eventStore = eventStore;
	}
}
