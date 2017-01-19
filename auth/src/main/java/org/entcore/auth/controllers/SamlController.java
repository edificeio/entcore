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
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;
import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlController extends AbstractFederateController {

	private SamlServiceProviderFactory spFactory;
	private JsonObject samlWayfParams;
	private JsonObject samlWayfMustacheFormat;

	public SamlController() throws ConfigurationException {
		DefaultBootstrap.bootstrap();
	}

	@Get("/saml/wayf")
	public void wayf(HttpServerRequest request) {
		if (samlWayfParams != null) {
			if (samlWayfMustacheFormat == null) {
				final JsonArray wmf = new JsonArray();
				for (String attr : samlWayfParams.getFieldNames()) {
					JsonObject i = samlWayfParams.getObject(attr);
					if (i == null) continue;
					final String acs = i.getString("acs");
					if (isEmpty(acs)) continue;
					URI uri;
					try {
						uri = new URI(acs);
					} catch (URISyntaxException e) {
						log.error("Invalid acs URI", e);
						continue;
					}
					JsonObject o = new JsonObject()
							.putString("name", attr)
							.putString("uri", uri.getScheme() + "://" + uri.getHost() +
									(attr.startsWith("login") ? "/auth/login" : "/auth/saml/authn/" + attr));
					wmf.addObject(o);
				}
				samlWayfMustacheFormat = new JsonObject().putArray("providers", wmf);
			}
			renderView(request, samlWayfMustacheFormat, "wayf.html", null);
		} else {
			request.response().setStatusCode(401).setStatusMessage("Unauthorized")
					.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
		}
	}

	@Get("/saml/authn/:providerId")
	public void auth(final HttpServerRequest request) {
		final JsonObject item = samlWayfParams.getObject(request.params().get("providerId"));
		if (item == null) {
			forbidden(request, "invalid.provider");
			return;
		}
		final JsonObject event = item.copy().putString("action", "generate-authn-request");
		vertx.eventBus().send("saml", event, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (log.isDebugEnabled()) {
					log.debug("authn request : " + event.body().encodePrettily());
				}
				final String authnRequest = event.body().getString("authn-request");
				if (isNotEmpty(authnRequest)) {
					CookieHelper.getInstance().setSigned("relaystate", event.body().getString("relay-state"), 900, request);
					redirect(request, authnRequest, "");
				} else {
					badRequest(request, "empty.authn.request");
				}
			}
		});
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
		sloUser(request);
	}

	@Override
	protected void afterDropSession(JsonObject event, final HttpServerRequest request, UserInfos user, final String c) {
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
				if (samlWayfParams != null) {
					final String state = CookieHelper.getInstance().getSigned("relaystate", request);
					if (isEmpty(state) || (!state.equals(request.formAttributes().get("RelayState")) &&
							!state.equals(SamlUtils.SIMPLE_RS))) {
						forbidden(request, "invalid_state");
						return;
					}
				}
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

	public void setSamlWayfParams(JsonObject samlWayfParams) {
		this.samlWayfParams = samlWayfParams;
	}

}
