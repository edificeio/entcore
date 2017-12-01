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
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;
import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.FederationService;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.entcore.auth.services.impl.FederationServiceImpl;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.MarshallingException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlController extends AbstractFederateController {

	private SamlServiceProviderFactory spFactory;
	private FederationService federationService;
	private JsonObject samlWayfParams;
	private JsonObject samlWayfMustacheFormat;
	private String ignoreCallBackPattern;
	private boolean softSlo;
	private boolean federatedAuthenticateError = false;

	// regex used to find namequalifier in session nameid (Mongo)
	private String NAME_QUALIFIER_REGEXP = ".*\\sNameQualifier=\"([^\"]*)\".*";
	private final Pattern NAME_QUALIFIER_PATTERN = Pattern.compile(NAME_QUALIFIER_REGEXP);

	private static final String SESSIONS_COLLECTION = "sessions";

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		// load soft-slo property : true = normal slo, false = redirect instead of slo
		softSlo = config.getBoolean("soft-slo", false);

		federatedAuthenticateError = config.getBoolean("federated-authenticate-error", false);

		// load nameQualifierRegex (in-case mongoDb NameId format change)
		String nameQualifierRegex = config.getString("nameQualifierRegex");
		if(nameQualifierRegex != null && !nameQualifierRegex.trim().isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Using nameQualifierRegex specified : " + nameQualifierRegex);
			}
			this.NAME_QUALIFIER_REGEXP = nameQualifierRegex;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Using default nameQualifierRegex : " + NAME_QUALIFIER_REGEXP);
			}
		}
	}

	public SamlController() throws ConfigurationException {
		DefaultBootstrap.bootstrap();
		this.federationService = new FederationServiceImpl();
	}

	@Get("/saml/wayf")
	public void wayf(HttpServerRequest request) {
		if (samlWayfParams != null) {
			if (samlWayfMustacheFormat == null) {
				final JsonArray wmf = new JsonArray();
				for (String attr : samlWayfParams.fieldNames()) {
					JsonObject i = samlWayfParams.getJsonObject(attr);
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
							.put("name", attr)
							.put("uri", uri.getScheme() + "://" + uri.getHost() +
									(attr.startsWith("login") ? "/auth/login" : "/auth/saml/authn/" + attr));
					wmf.add(o);
				}
				samlWayfMustacheFormat = new JsonObject().put("providers", wmf);
			}
			String callBack = request.params().get("callBack");
			final JsonObject swmf;
			if (isNotEmpty(callBack) && (ignoreCallBackPattern == null || !callBack.matches(ignoreCallBackPattern))) {
				try {
					callBack = URLEncoder.encode(callBack, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Error encode wayf callback.", e);
				}
				swmf = samlWayfMustacheFormat.copy();
				for (Object o: swmf.getJsonArray("providers")) {
					if (!(o instanceof JsonObject)) continue;
					final String uri = ((JsonObject) o).getString("uri");
					if (isNotEmpty(uri) && !uri.contains("callBack")) {
						((JsonObject) o).put("uri", uri + (uri.contains("?") ? "&" : "?") + "callBack=" + callBack);
					}
				}
			} else {
				swmf = samlWayfMustacheFormat;
			}
			renderView(request, swmf, "wayf.html", null);
		} else {
			request.response().setStatusCode(401).setStatusMessage("Unauthorized")
					.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
		}
	}

	@Get("/saml/authn/:providerId")
	public void auth(final HttpServerRequest request) {
		final JsonObject item = samlWayfParams.getJsonObject(request.params().get("providerId"));
		if (item == null) {
			forbidden(request, "invalid.provider");
			return;
		}
		final JsonObject event = item.copy().put("action", "generate-authn-request");
		vertx.eventBus().send("saml", event, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (log.isDebugEnabled()) {
					log.debug("authn request : " + event.body().encodePrettily());
				}
				final String authnRequest = event.body().getString("authn-request");
				if (isNotEmpty(authnRequest)) {
					CookieHelper.getInstance().setSigned("relaystate", event.body().getString("relay-state"), 900, request);
					final String callBack = request.params().get("callBack");
					if (isNotEmpty(callBack)) {
						CookieHelper.getInstance().setSigned("callback", callBack, 900, request);
					}
					redirect(request, authnRequest, "");
				} else {
					badRequest(request, "empty.authn.request");
				}
			}
		}));
	}

	/**
	 * Generate base64 SAMLResponse to the service provider specified.
	 * Then HTML auto-submit FORM is created with the samlResponse and render the page.
	 *
	 * Finally if user is recognnized and authenticated ine th SP side, he will access to the service.
	 *
	 * @param request
	 */
	@Get("/saml/sso/:providerId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public  void sso (final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				final String serviceProviderId = request.params().get("providerId");

				if (serviceProviderId == null || serviceProviderId.trim().isEmpty()) {
					forbidden(request, "invalid.provider");
					return;
				}

				// Get connected user sessionId
				final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);

				// Send to the bus to generate the SAMLResponse
				JsonObject event = new JsonObject()
						.put("action", "generate-saml-response")
						.put("SP", serviceProviderId)
						.put("userId", user.getUserId())
						.put("nameid", sessionId)
						.put("host", getScheme(request) + "://" + getHost(request));
				vertx.eventBus().send("saml", event, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {

						// Get base64 SAMLResponse generated
						String samlResponse = event.body().getString("SAMLResponse64");
						if (log.isDebugEnabled()) {
							log.debug("base64 samlResponse get from controller : " + samlResponse);
						}

						// Assertion Consumer Service location
						String destination = event.body().getString("destination");


						// If generation succeed, an HTML auto-submit FORM is created
						String error = event.body().getString("error");
						if (isNotEmpty(samlResponse) && isNotEmpty(destination) && (error == null || error.isEmpty())) {
							renderSamlResponse(user,samlResponse,serviceProviderId,destination,request);
						} else {
							// Else redirect to the login page
							redirect(request, "");
						}

					}
				}));
			}
		});
	}

	/**
	 * Generate HTML auto-submit FORM with samlResponse and render the page
	 * @param samlResponse64 base64 SAMLResponse
	 * @param destination the recipient (SP acs)
	 */
	private void renderSamlResponse(UserInfos user,String samlResponse64,String providerId, String destination,HttpServerRequest request) {
		JsonObject paramsFED = new JsonObject();
		paramsFED.put("SAMLResponse",samlResponse64);
		JsonObject relayStateMap = config.getJsonObject("relay-state");
		if(relayStateMap != null) {
			String relayState = relayStateMap.getString(providerId);
			if(relayState != null) {
				paramsFED.put("RelayState",relayState);
			} else {
				log.error("Error loading relay-state for providerId : " + providerId);
			}
		} else {
			log.error("Error loading relay-state properties.");
		}
		paramsFED.put("Destination",destination);
		renderView(request, paramsFED, "fed.html", null);
	}


	private void loginResult(final HttpServerRequest request, String error) {
		if(federatedAuthenticateError) {
			final JsonObject context = new JsonObject();
			if (error != null && !error.trim().isEmpty()) {
				context.put("error", new JsonObject()
						.put("message", I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
			}
			context.put("notLoggedIn", true);
			renderView(request, context, "login.html", null);
		}else{
			redirect(request, LOGIN_PAGE);
		}
	}

	@Post("/saml/acs")
	public void acs(final HttpServerRequest request) {
		validateResponseAndGetAssertion(request, new Handler<Assertion>() {
			@Override
			public void handle(final Assertion assertion) {
				SamlServiceProvider sp = spFactory.serviceProvider(assertion);
				sp.execute(assertion, new Handler<Either<String, Object>>() {
					@Override
					public void handle(final Either<String, Object> event) {
						if (event.isLeft()) {
							loginResult(request, "fed.auth.error.user.not.found");
						} else {
							final String nameIdFromAssertion = getNameId(assertion);
							final String sessionIndex = getSessionId(assertion);
							if (log.isDebugEnabled()) {
								log.debug("NameID : " + nameIdFromAssertion);
								log.debug("SessionIndex : " + sessionIndex);
							}
							if (nameIdFromAssertion == null || sessionIndex == null || nameIdFromAssertion.trim().isEmpty() || sessionIndex.trim().isEmpty()) {
								redirect(request, LOGIN_PAGE);
								return;
							}

							// if user is already authenticated in the ENT through the ENT login page, we do not authenticate him again
							// because this will store the "nameid"

							// ALGORITHM RULE :
							// if user has "nameId" : it means user connected first with a federated idp
							// else he connected to the ENT through the ENT login page
							// this way we know if we need to disonnect/redirect the user to the federated login/home page OR
							// if we only disconnect him to the ENT (no nameid)
							final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);

//							final JsonObject query = new JsonObject().put("_id", sessionId);
//							mongo.findOne(SESSIONS_COLLECTION, query, new io.vertx.core.Handler<Message<JsonObject>>() {
							federationService.getMongoDbSession(sessionId, new io.vertx.core.Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> eventMongo) {
									JsonObject res = eventMongo.body().getJsonObject("result");
									String userId;
									if ("ok".equals(eventMongo.body().getString("status")) && res != null &&
											(userId = res.getString("userId")) != null && !userId.trim().isEmpty()) {

										String nameID = res.getString("NameID");

										String userIdAssertion = null;
										if (event.right().getValue() != null && event.right().getValue() instanceof JsonObject) {
											userIdAssertion = ((JsonObject) event.right().getValue()).getString("id");
										}

										// no NameID and same userId : user already connected through IDP ENT
										if((nameID == null || nameID.trim().isEmpty())
												&& userIdAssertion != null && userIdAssertion.equals(userId)) {
											redirect(request, "/");
										} else {
											endAcs(request, event, sessionIndex, nameIdFromAssertion);
										}
									} else {
										endAcs(request, event, sessionIndex, nameIdFromAssertion);
									}
								}
							});
						}
					}
				});
			}
		});
	}

	/**
	 * End of acs method : authenticate user if not already connectd through IDP ENT
	 *
	 * @param request request from "/acs" method
	 * @param event event from SamlServiceProvider.execute(...)
	 * @param sessionIndex sessionIndex get from acs assertion
	 * @param nameId nameID get from acs assertion
	 */
	private void endAcs(final HttpServerRequest request, Either<String, Object> event,
						final String sessionIndex, final String nameId) {
		if (event.right().getValue() != null && event.right().getValue() instanceof JsonObject) {
			final JsonObject res = (JsonObject) event.right().getValue();
			if(res.size()== 0) {
				loginResult(request, "fed.auth.error.user.not.found");
			} else {
				authenticate(res, sessionIndex, nameId, request);
			}
		} else if (event.right().getValue() != null && event.right().getValue() instanceof JsonArray && isNotEmpty(signKey)) {
			try {
				JsonObject params = getUsersWithSignatures((JsonArray) event.right().getValue(), sessionIndex, nameId);
				renderView(request, params, "selectFederatedUser.html", null);
			} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
				log.error("Error signing federated users.", e);
				redirect(request, LOGIN_PAGE);
			}
		} else {
			redirect(request, LOGIN_PAGE);
		}
	}

	protected String getNameId(Assertion assertion) {
		try {
			return (assertion != null && assertion.getSubject() != null &&
					assertion.getSubject().getNameID() != null) ?
					SamlUtils.marshallNameId(assertion.getSubject().getNameID()) : null;
		} catch (MarshallingException e) {
			log.error("Error marshalling NameId.", e);
			return null;
		}
	}

	@Get("/saml/slo")
	public void slo(final HttpServerRequest request) {
		sloUser(request);
	}

	@Override
	protected void afterDropSession(JsonObject event, final HttpServerRequest request, UserInfos user, final String c) {
		request.headers().remove("Cookie");
		event.put("action", "generate-slo-request");
		event.put("IDP", (String) user.getOtherProperties().get("federatedIDP"));
		if (log.isDebugEnabled()) {
			log.debug("Session metadata : " + event.encodePrettily());
		}

		String nameID = event.getString("NameID");
		if(nameID != null && !nameID.isEmpty()) {
			if(softSlo) {
				Matcher academyMatcher = NAME_QUALIFIER_PATTERN.matcher(nameID);
				if (academyMatcher.find()) {
					String nameQualifier = academyMatcher.group(1);
					JsonObject confSoftSlo = config.getJsonObject("soft-slo-redirect");
					if(confSoftSlo != null) {
						String redirectIDP = confSoftSlo.getString(nameQualifier);
						if(redirectIDP != null) {
							redirect(request, redirectIDP, "");
						} else {
							log.error("Error loading soft-slo-redirect for IDP : " + nameQualifier);
							redirect(request, LOGIN_PAGE);
						}
					} else {
						log.error("Error loading soft-slo-redirect properties.");
						redirect(request, LOGIN_PAGE);
					}
				}
			} else {
				// normal slo
				vertx.eventBus().send("saml", event, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if (log.isDebugEnabled()) {
							log.debug("slo request : " + event.body().encodePrettily());
						}
						String slo = event.body().getString("slo");
						try {
							if (c != null && !c.isEmpty()) {
								slo = c + URLEncoder.encode(slo, "UTF-8");
							} else {
								slo = URLEncoder.encode(slo, "UTF-8");
							}
						} catch (UnsupportedEncodingException e) {
							log.error(e.getMessage(), e);
						}
						AuthController.logoutCallback(request, slo, config, eb);
					}
				}));
			}
		} else {
			AuthController.logoutCallback(request, null, config, eb);
		}
	}

	@Post("/saml/selectUser")
	public  void selectUser(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final JsonObject j = new JsonObject();
				for (String attr : request.formAttributes().names()) {
					if (isNotEmpty(request.formAttributes().get(attr))) {
						j.put(attr, request.formAttributes().get(attr));
					}
				}
				final String nameId = j.getString("nameId", "").replaceAll("\\r", "");
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
			j.put("key", HmacSha1.sign(sessionIndex + nameId + j.getString("login") + j.getString("id"), signKey));
			j.put("nameId", nameId);
			j.put("sessionIndex", sessionIndex);
		}
		return new JsonObject().put("users", array);
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
							.put("action", "validate-signature-decrypt").put("response", samlResponse);
					vertx.eventBus().send("saml", j, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
					}));
				} else if (samlResponse != null) {
					JsonObject j = new JsonObject()
							.put("action", "validate-signature").put("response", samlResponse);
					vertx.eventBus().send("saml", j, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
					}));
				} else {
					redirect(request, LOGIN_PAGE);
				}
			}
		});
	}

	private void getSamlResponse(final HttpServerRequest request, final Handler<String> handler) {
		if (log.isDebugEnabled()) {
			log.debug("getSamlResponse");
		}
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				if (samlWayfParams != null) {
					final String state = CookieHelper.getInstance().getSigned("relaystate", request);
					if (isEmpty(state) || (!state.equals(request.formAttributes().get("RelayState")) &&
							!state.equals(SamlUtils.SIMPLE_RS))) {
						forbidden(request, "invalid_state");
						return;
					}
				}
				String samlResponse = request.formAttributes().get("SAMLResponse");
				log.debug("samlResponse=" +samlResponse);
				if (samlResponse != null && !samlResponse.trim().isEmpty()) {
					handler.handle(new String(Base64.getDecoder().decode(samlResponse)));
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

	public void setIgnoreCallBackPattern(String ignoreCallBackPattern) {
		this.ignoreCallBackPattern = ignoreCallBackPattern;
	}
	public void setFederationService(FederationService federationService) {
		this.federationService = federationService;
	}
}
