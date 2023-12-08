/*
 * Copyright © "Open Digital Education", 2015
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
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.data.ZLib;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;

import org.entcore.auth.security.SamlHelper;
import org.entcore.auth.security.SamlUtils;
import org.entcore.auth.services.FederationService;
import org.entcore.auth.services.SafeRedirectionService;
import org.entcore.auth.services.impl.FederationServiceImpl;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.MarshallingException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import static fr.wseduc.webutils.Utils.*;

public class SamlController extends AbstractFederateController {

	private FederationService federationService;
	private JsonObject samlWayfParams;
	private JsonObject samlWayfMustacheFormat = new JsonObject();
	private String ignoreCallBackPattern;
	private boolean softSlo;
	private boolean federatedAuthenticateError = false;
	private JsonObject activationThemes;
	private long sessionsLimit;
	private SamlHelper samlHelper;
	final SafeRedirectionService redirectionService = SafeRedirectionService.getInstance();

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

		activationThemes = config.getJsonObject("activation-themes", new JsonObject());

		sessionsLimit = config.getLong("sessions-limit", 0L);

		// load nameQualifierRegex (in-case mongoDb NameId format change)
		String nameQualifierRegex = config.getString("nameQualifierRegex");
		if (nameQualifierRegex != null && !nameQualifierRegex.trim().isEmpty()) {
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
			final String host = Renders.getHost(request);
			if (samlWayfMustacheFormat.getJsonObject(host) == null) {
				JsonObject wayfParams = samlWayfParams.getJsonObject(host);
				wayfParams = (wayfParams == null) ? samlWayfParams : wayfParams;
				final JsonArray wmf = new fr.wseduc.webutils.collections.JsonArray();
				for (String attr : wayfParams.fieldNames()) {
					JsonObject i = wayfParams.getJsonObject(attr);
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

					String uriStr;
					if(attr.startsWith("login"))
						uriStr = uri.getScheme() + "://" + uri.getHost() + "/auth/login";
					else if(attr.startsWith("other"))
						uriStr = uri.toString();
					else
						uriStr = uri.getScheme() + "://" + uri.getHost() + "/auth/saml/authn/" + attr;

					JsonObject o = new JsonObject()
							.put("name", attr)
							.put("uri", uriStr);

					wmf.add(o);
				}
				samlWayfMustacheFormat.put(host, new JsonObject().put("providers", wmf));
			}
			String callBack = request.params().get("callBack");
			final JsonObject swmf;
			if (isNotEmpty(callBack) && (ignoreCallBackPattern == null || !callBack.matches(ignoreCallBackPattern))) {
				try {
					callBack = URLEncoder.encode(callBack, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Error encode wayf callback.", e);
				}
				swmf = samlWayfMustacheFormat.copy().getJsonObject(host);

				for (Object o: swmf.getJsonArray("providers")) {
					if (!(o instanceof JsonObject)) continue;
					final String uri = ((JsonObject) o).getString("uri");
					if (isNotEmpty(uri) && !uri.contains("callBack")) {
						((JsonObject) o).put("uri", uri + (uri.contains("?") ? "&" : "?") + "callBack=" + callBack);
					}
				}
			} else {
				swmf = samlWayfMustacheFormat.getJsonObject(host);
			}

			// get theme for logo image src
			JsonObject skins = new JsonObject(vertx.sharedData().<String, Object>getLocalMap("skins"));
			String skin = skins.getString(getHost(request));
			if (swmf != null) {
				swmf.put("childTheme", (skin != null && !skin.trim().isEmpty()) ? skin : "raw");
			}

			final String userAgent = request.getHeader("User-Agent:");
			final String xRequestedWith = request.getHeader("X-Requested-With");
			if ((userAgent != null && (userAgent.contains("iPhone") || userAgent.contains("Android"))) ||
					(xRequestedWith != null && xRequestedWith.startsWith("com.ode")) ||
					("true".equals(request.params().get("mobile")))) {
				renderView(request, swmf, "wayf-mobile.html", null);
			} else {
				renderView(request, swmf, "wayf.html", null);
			}
		} else {
			request.response().setStatusCode(401).setStatusMessage("Unauthorized")
					.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
		}
	}

	@Get("/saml/authn/:providerId")
	public void auth(final HttpServerRequest request) {
		final String host = Renders.getHost(request);
		JsonObject wayfParams = samlWayfParams.getJsonObject(host);
		wayfParams = (wayfParams == null) ? samlWayfParams : wayfParams;
		final JsonObject item = wayfParams.getJsonObject(request.params().get("providerId"));
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
					redirectionService.redirect(request, authnRequest, "");
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
                String relayState = null;
                JsonObject relayStateMap = config.getJsonObject("relay-state");
                if(relayStateMap != null) {
                    relayState = relayStateMap.getString(serviceProviderId);
                    if(relayState == null) {
                        log.error("Error loading relay-state for providerId : " + serviceProviderId);
                    }
                } else {
                    log.error("Error loading relay-state properties.");
                }

				ssoGenerateSAML(user, "", serviceProviderId, relayState,  request);
			}
		});
	}

	private void ssoGenerateSAML(UserInfos user, String authNRequestId, String serviceProviderId, String relayState, HttpServerRequest request) {
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
                .put("host", getHost(request))
                .put("authNRequestId", authNRequestId)
				.put("scheme", getScheme(request));
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
                    renderSamlResponse(user,samlResponse,serviceProviderId,destination,relayState, request);
                } else {
                    // Else redirect to the login page
					log.error("SAMLResponse generation failed to access to service " + serviceProviderId + " by user " + user.getUserId() );
					log.error("SAMLResponse (required) : " + samlResponse);
					log.error("destination (required)  : " + destination);
					log.error("error (must be null)    : " + error);
                    redirectionService.redirect(request, "");
                }

            }
        }));
	}

	@Get("/saml/redirect/sso/")
	public void ssoredirectGet ( final HttpServerRequest request){
		log.info("ssoredirect GET called");
		try{
            String SAMLrequestEnflatedEncoded = request.params().get("SAMLRequest");
            String SAMLAuthnRequest = base64decodedInflated(SAMLrequestEnflatedEncoded);
            String relayState = request.params().get("RelayState");
            ssoRedirect(SAMLAuthnRequest, relayState, request);
        } catch (Exception e) {
            log.error("ssoRedirect decode base64 and inflate xml FAILED ", e);
            redirectionService.redirect(request, "");

        }

	}


	/**
	 * Returns String Base64 decoded and inflated
	 *
	 * @param input	String input
	 *
	 * @return the base64 decoded and inflated string
	 */
	private static String base64decodedInflated(String input) throws Exception {
		if (input == null || input.isEmpty()) {
			return "";
		}
		// Base64 decoder
		byte[] decoded = Base64.getDecoder().decode(input);

		// Inflater
        Inflater decompresser = new Inflater(true);
        decompresser.setInput(decoded);
        byte[] result = new byte[1024];
        String inflated = "";
        long limit = 0;
        while(!decompresser.finished() && limit < 150) {
            int resultLength = decompresser.inflate(result);
            limit += 1;
            inflated += new String(result, 0, resultLength, "UTF-8");
        }
        decompresser.end();
        return inflated;
	}

	@Post("/saml/post/sso/")
	public void ssoredirectPost(final HttpServerRequest request) {
		log.info("ssoredirect POST called");
		request.setExpectMultipart(true);
		request.pause();
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				try {
                    String SAMLRequestEncoded = request.formAttributes().get("SAMLRequest");
                    String xmlStr = new String(Base64.getDecoder().decode(SAMLRequestEncoded));
                    String relayState = request.formAttributes().get("RelayState");
                    ssoRedirect(xmlStr, relayState, request);
				} catch (Exception e) {
					log.error("ssoRedirect decode base64 xml FAILED ", e);
					redirectionService.redirect(request, "");
				}

		}});
		request.resume();

	}

	@Get("/saml/metadata/:idp")
	public void idpGar(HttpServerRequest request) {
		JsonObject idpConfig = config.getJsonObject("idp-metadata-mapping", new JsonObject());
		String idpParam = request.getParam("idp");
		if( !idpConfig.isEmpty() && idpConfig.containsKey(idpParam)) {
			request.response().sendFile(idpConfig.getString(idpParam));
		}
		else {
			request.response().setStatusCode(404).setStatusMessage("idp not found").end();
		}
	}


	/**
	 * Generate SAML response from saml Issuer
	 * @param SAMLAuthnRequest (issuer name inside)
	 * @param request
	 */
	private void ssoRedirect(String SAMLAuthnRequest, String relayState, HttpServerRequest request) {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(SAMLAuthnRequest)));
			XPathFactory xpf = XPathFactory.newInstance();
			XPath path = xpf.newXPath();
			String expression = "/AuthnRequest/Issuer";
			final String serviceProviderId = (String) path.evaluate(expression, doc.getDocumentElement());
			final String authNRequestId = doc.getDocumentElement().getAttribute("ID");
			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				@Override
				public void handle(final UserInfos user) {
					if (user == null) redirectToLogin(request, SAMLAuthnRequest, relayState);
					else ssoGenerateSAML(user, authNRequestId, serviceProviderId, relayState,  request);
				}
			});
		} catch (Exception e) {
			log.info("Provider not available");
			log.info("SAMLAuthnRequest ------------------------------");
			log.info(SAMLAuthnRequest);
			log.info("END SAMLAuthnRequest ------------------------------");
			log.info("relayState:" + relayState);
			redirectionService.redirect(request,"");
			log.error("Can't generate SAML response from provider ", e);

		}
	}

	private void redirectToLogin(HttpServerRequest request, String SAMLAuthnRequest, String relayState) {
		String path = "/auth/saml/redirect/sso/?SAMLRequest=%s&RelayState=%s";
		String location = "";
		try {
			String authnRequestB64 = URLEncoder.encode(ZLib.deflateAndEncode(SAMLAuthnRequest), StandardCharsets.UTF_8.toString());
			String rs = URLEncoder.encode(relayState, StandardCharsets.UTF_8.toString());
			String callback = URLEncoder.encode(String.format(path, authnRequestB64, rs), StandardCharsets.UTF_8.toString());
			String cookieCallback = getScheme(request) + "://" + getHost(request) + String.format(path, authnRequestB64, rs);
			if (config.containsKey("authLocations")) {
				final String host = Renders.getHost(request);
				final String authLocation = config.getJsonObject("authLocations", new JsonObject()).getJsonObject(host, new JsonObject()).getString("loginUri");
				location = extractLocation(authLocation, request, callback, cookieCallback);
			} else if (config.containsKey("loginUri")) {
				final String authLocation = config.getString("loginUri");
				location = extractLocation(authLocation, request, callback, cookieCallback);
			}

			if (location == null || location.isEmpty()) {
				location = String.format("%s?callback=%s", LOGIN_PAGE, callback);
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Encoding exception in redirectToLogin method", e);
		} catch (IOException e) {
			log.error("IOException during deflating and encoding SAMLAuthnRequest: " + SAMLAuthnRequest, e);
		}

		if (location.startsWith("http")) {
			redirect(request, location, "");
		} else {
			redirect(request, location);
		}
	}

	/**
	 * Extract location and set cookie for external login page.
	 * @param authLocation authLocation
	 * @return location
	 */
	private String extractLocation(final String authLocation, HttpServerRequest request, String callback, String cookieCallback) {
		String location = null;
		if (authLocation != null && !LOGIN_PAGE.equals(authLocation)) {
			if (WAYF_PAGE.equals(authLocation)) {
				location = String.format("%s?callback=%s", WAYF_PAGE, callback);
			} else {
				//external login page
				location = authLocation;
			}
			CookieHelper.getInstance().setSigned("callback", cookieCallback, 600, request);
		}
		return location;
	}

	/**
	 * Generate HTML auto-submit FORM with samlResponse and render the page
	 * @param samlResponse64 base64 SAMLResponse
	 * @param destination the recipient (SP acs)
	 */
	private void renderSamlResponse(UserInfos user,String samlResponse64,String providerId, String destination,String relayState, HttpServerRequest request) {
		JsonObject paramsFED = new JsonObject();
		paramsFED.put("SAMLResponse",samlResponse64);
		if (relayState != null) {
			paramsFED.put("RelayState", relayState);
		}
        paramsFED.put("Destination", destination);
		renderView(request, paramsFED, "fed.html", null);
	}


	private void loginResult(final HttpServerRequest request, String error, Assertion assertion) {
		if (Utils.getOrElse(config.getBoolean("log-failed-assertion"), false)) {
			try {
				log.warn(SamlUtils.marshallAssertion(assertion));
			} catch (MarshallingException e) {
				log.error("Error marshalling failed assertion", e);
			}
		}
		if(federatedAuthenticateError) {
			final JsonObject context = new JsonObject();
			if (error != null && !error.trim().isEmpty()) {
				context.put("error", new JsonObject()
						.put("message", I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
			}
			context.put("notLoggedIn", true);
			renderView(request, context, "login.html", null);
		}else{
			redirectionService.redirect(request, LOGIN_PAGE);
		}
	}

	@Post("/saml/acs")
	public void acs(final HttpServerRequest request) {
		if (sessionsLimit > 0L) {
			request.pause();
			UserUtils.getSessionsNumber(eb, ar -> {
				if (ar.succeeded()) {
					if (ar.result() > sessionsLimit) {
						renderView(request, new JsonObject(), "tooload.html", null);
					} else {
						request.resume();
						acsProcess(request);
					}
				} else {
					renderView(request, new JsonObject(), "tooload.html", null);
				}
			});
		} else {
			acsProcess(request);
		}
	}

	private void acsProcess(final HttpServerRequest request) {
		validateResponseAndGetAssertion(request, new Handler<Assertion>() {
			@Override
			public void handle(final Assertion assertion) {
				samlHelper.getUserFromAssertion(assertion, new Handler<Either<String, Object>>() {
					@Override
					public void handle(final Either<String, Object> event) {
						if (event.isLeft()) {
							String error = event.left().getValue();
							if(error.equals("blocked.profile"))
								loginResult(request, "auth.error.blockedProfileType", assertion);
							else if(error.equals("blocked.user"))
								loginResult(request, "auth.error.blockedUser", assertion);
							else
								loginResult(request, "fed.auth.error.user.not.found", assertion);
						} else {
							final String nameIdFromAssertion = getNameId(assertion);
							final String sessionIndex = getSessionId(assertion);
							if (log.isDebugEnabled()) {
								log.debug("NameID : " + nameIdFromAssertion);
								log.debug("SessionIndex : " + sessionIndex);
							}
							if (nameIdFromAssertion == null || sessionIndex == null || nameIdFromAssertion.trim().isEmpty() || sessionIndex.trim().isEmpty()) {
								redirectionService.redirect(request, LOGIN_PAGE);
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
											redirectionService.redirect(request, "/");
										} else {
											endAcs(request, event, sessionIndex, nameIdFromAssertion, assertion);
										}
									} else {
										endAcs(request, event, sessionIndex, nameIdFromAssertion, assertion);
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
	 * @param request      request from "/acs" method
	 * @param event        event from SamlServiceProvider.execute(...)
	 * @param sessionIndex sessionIndex get from acs assertion
	 * @param nameId       nameID get from acs assertion
	 * @param assertion    saml assertion
	 */
	private void endAcs(final HttpServerRequest request, Either<String, Object> event, final String sessionIndex,
			final String nameId, Assertion assertion) {
		if (event.right().getValue() != null && event.right().getValue() instanceof JsonObject) {
			final JsonObject res = (JsonObject) event.right().getValue();
			if(res.size()== 0) {
				loginResult(request, "fed.auth.error.user.not.found", assertion);
			} else {
				authenticate(res, sessionIndex, nameId, activationThemes, request);
			}
		} else if (event.right().getValue() != null && event.right().getValue() instanceof JsonArray && isNotEmpty(signKey)) {
			try {
				JsonObject params = samlHelper.getUsersWithSignatures((JsonArray) event.right().getValue(), sessionIndex, nameId);
				renderView(request, params, "selectFederatedUser.html", null);
			} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
				log.error("Error signing federated users.", e);
				redirectionService.redirect(request, LOGIN_PAGE);
			}
		} else {
			redirectionService.redirect(request, LOGIN_PAGE);
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
							redirectionService.redirect(request, redirectIDP, "");
						} else {
							log.error("Error loading soft-slo-redirect for IDP : " + nameQualifier);
							redirectionService.redirect(request, LOGIN_PAGE);
						}
					} else {
						log.error("Error loading soft-slo-redirect properties.");
						redirectionService.redirect(request, LOGIN_PAGE);
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
						authenticate(j, sessionIndex, nameId, activationThemes, request);
					} else {
						log.error("Invalid signature for federated user.");
						redirectionService.redirect(request, LOGIN_PAGE);
					}
				} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
					log.error("Error validating signature of federated user.", e);
					redirectionService.redirect(request, LOGIN_PAGE);
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

	private void validateResponseAndGetAssertion(final HttpServerRequest request, final Handler<Assertion> handler) {
		getSamlResponse(request, new Handler<String>() {
			@Override
			public void handle(final String samlResponse) {
				samlHelper.validateSamlResponseAndGetAssertion(samlResponse, ar -> {
					if (ar.succeeded()) {
						handler.handle(ar.result());
					} else {
						redirectionService.redirect(request, LOGIN_PAGE);
					}
				});
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
				if (samlWayfParams != null && !config.getBoolean("idp-initiated", false)) {
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

	public void setSamlWayfParams(JsonObject samlWayfParams) {
		this.samlWayfParams = samlWayfParams;
	}

	public void setIgnoreCallBackPattern(String ignoreCallBackPattern) {
		this.ignoreCallBackPattern = ignoreCallBackPattern;
	}
	public void setFederationService(FederationService federationService) {
		this.federationService = federationService;
	}

	public void setSamlHelper(SamlHelper samlHelper) {
		this.samlHelper = samlHelper;
	}

}
