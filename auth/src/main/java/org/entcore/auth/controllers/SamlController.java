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

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;
import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.events.EventStore;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.ConfigurationException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlController extends BaseController {

	public static final String LOGIN_PAGE = "/auth/login";
	private SamlServiceProviderFactory spFactory;
	private UserAuthAccount userAuthAccount;
	private EventStore eventStore;
	private String signKey;

	public SamlController() throws ConfigurationException {
		DefaultBootstrap.bootstrap();
	}

	@Post("/saml/acs")
	public void acs(final HttpServerRequest request) {
		validateResponseAndGetAssertion(request, new Handler<Assertion>() {
			@Override
			public void handle(final Assertion assertion) {
				SamlServiceProvider sp = spFactory.serviceProvider(assertion);
				sp.execute(assertion, new Handler<Either<String, JsonElement>>() {
					@Override
					public void handle(Either<String, JsonElement> event) {
						if (event.isLeft()) {
							redirect(request, LOGIN_PAGE);
						} else {
							final String nameId = (assertion != null && assertion.getSubject() != null &&
									assertion.getSubject().getNameID() != null) ? assertion.getSubject().getNameID().getValue() : null;
							final String sessionIndex = getSessionId(assertion);
							if (log.isDebugEnabled()) {
								log.debug("NameID : " + nameId);
								log.debug("SessionIndex : " + sessionIndex);
							}
							if (nameId == null || sessionIndex == null || nameId.trim().isEmpty() || sessionIndex.trim().isEmpty()) {
								redirect(request, LOGIN_PAGE);
								return;
							}
							if (event.right().getValue() != null && event.right().getValue().isObject()) {
								final JsonObject res = event.right().getValue().asObject();
								authenticate(res, sessionIndex, nameId, request);
							} else if (event.right().getValue() != null && event.right().getValue().isArray() && isNotEmpty(signKey)) {
								try {
									JsonObject params = getUsersWithSignatures(event.right().getValue().asArray(), sessionIndex, nameId);
									renderView(request, params, "selectFederatedUser.html", null);
								} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
									log.error("Error signing federated users.", e);
									redirect(request, LOGIN_PAGE);
								}
							} else {
								redirect(request, LOGIN_PAGE);
							}
						}
					}
				});
			}
		});
	}

	@Get("/saml/slo")
	public void slo(final HttpServerRequest request) {
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
								request.headers().remove("Cookie");
								event.putString("action", "generate-slo-request");
								event.putString("IDP", (String) user.getOtherProperties().get("federatedIDP"));
								if (log.isDebugEnabled()) {
									log.debug("Session metadata : " + event.encodePrettily());
								}
								vertx.eventBus().send("saml", event, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (log.isDebugEnabled()) {
											log.debug("slo request : " + event.body().encodePrettily());
										}
										String slo = event.body().getString("slo");
										if (c != null && !c.isEmpty()) {
											try {
												slo = c + URLEncoder.encode(slo, "UTF-8");
											} catch (UnsupportedEncodingException e) {
												log.error(e.getMessage(), e);
											}
										}
										AuthController.logoutCallback(request, slo, container, eb);
									}
								});
							} else {
								AuthController.logoutCallback(request, c, container, eb);
							}
						}
					});
				} else {
					AuthController.logoutCallback(request, c, container, eb);
				}
			}
		});
	}

	@Post("/saml/selectUser")
	public  void selectUser(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final JsonObject j = new JsonObject();
				for (String attr : request.formAttributes().names()) {
					if (isNotEmpty(request.formAttributes().get(attr))) {
						j.putString(attr, request.formAttributes().get(attr));
					}
				}
				final String nameId = j.getString("nameId");
				final String sessionIndex = j.getString("sessionIndex");
				try {
					if (j.getString("key", "").equals(HmacSha1.sign(sessionIndex + nameId + j.getString("login") + j.getString("id"), signKey))) {
						authenticate(j, sessionIndex, nameId, request);
					} else {
						log.error("Invalid signature for federated user.");
						redirect(request, LOGIN_PAGE);
					}
				} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
					log.error("Error validating signature of federated user.", e);
					redirect(request, LOGIN_PAGE);
				}
			}
		});
	}

	private void authenticate(JsonObject res, String sessionIndex, String nameId, HttpServerRequest request) {
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

	private JsonObject getUsersWithSignatures(JsonArray array, String sessionIndex, String nameId)
			throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
		for (Object o : array) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject j = (JsonObject) o;
			j.putString("key", HmacSha1.sign(sessionIndex + nameId + j.getString("login") + j.getString("id"), signKey));
			j.putString("nameId", nameId);
			j.putString("sessionIndex", sessionIndex);
		}
		return new JsonObject().putArray("users", array);
	}

	private void createSession(String userId, String sessionIndex, String nameId, final HttpServerRequest request) {
		UserUtils.createSession(eb, userId, sessionIndex, nameId,
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

	private String getSessionId(Assertion assertion) {
		if (assertion != null && assertion.getAuthnStatements() != null) {
			for (AuthnStatement ans: assertion.getAuthnStatements()) {
				if (ans.getSessionIndex() != null && !ans.getSessionIndex().trim().isEmpty()) {
					return ans.getSessionIndex();
				}
			}
		}
		return null;
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

	public void setSignKey(String signKey) {
		this.signKey = signKey;
	}
}
