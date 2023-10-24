/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.auth.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.response.DefaultResponseHandler;
import fr.wseduc.webutils.logging.Tracer;
import fr.wseduc.webutils.logging.TracerFactory;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import jp.eisbahn.oauth2.server.async.Handler;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;
import jp.eisbahn.oauth2.server.endpoint.ProtectedResource;
import jp.eisbahn.oauth2.server.endpoint.Token;
import jp.eisbahn.oauth2.server.endpoint.Token.Response;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.AccessDenied;
import jp.eisbahn.oauth2.server.exceptions.Try;
import jp.eisbahn.oauth2.server.fetcher.accesstoken.AccessTokenFetcherProvider;
import jp.eisbahn.oauth2.server.fetcher.accesstoken.impl.DefaultAccessTokenFetcherProvider;
import jp.eisbahn.oauth2.server.fetcher.clientcredential.ClientCredentialFetcher;
import jp.eisbahn.oauth2.server.fetcher.clientcredential.ClientCredentialFetcherImpl;
import jp.eisbahn.oauth2.server.granttype.GrantHandlerProvider;
import jp.eisbahn.oauth2.server.granttype.impl.DefaultGrantHandlerProvider;
import jp.eisbahn.oauth2.server.models.AuthInfo;
import jp.eisbahn.oauth2.server.models.ClientCredential;
import jp.eisbahn.oauth2.server.models.Request;
import jp.eisbahn.oauth2.server.models.UserData;
import org.entcore.auth.adapter.ResponseAdapterFactory;
import org.entcore.auth.adapter.UserInfoAdapter;
import org.entcore.auth.oauth.HttpServerRequestAdapter;
import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.pojo.SendPasswordDestination;
import org.entcore.auth.services.MfaService;
import org.entcore.auth.services.SafeRedirectionService;
import org.entcore.auth.services.impl.OpenIdSloServiceImpl;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.datavalidation.UserValidation;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.*;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.MapFactory;
import org.entcore.common.utils.Mfa;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.vertx.java.core.http.RouteMatcher;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.*;
import static fr.wseduc.webutils.request.RequestUtils.getTokenHeader;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.*;
import static org.entcore.common.aggregation.MongoConstants.TRACE_TYPE_CONNECTOR;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AuthController extends BaseController {

	private DataHandlerFactory oauthDataFactory;
	private Token token;
	private ProtectedResource protectedResource;
	private UserAuthAccount userAuthAccount;
	private static final Tracer trace = TracerFactory.getTracer("auth");
	private EventStore eventStore;
	private Map<Object, Object> invalidEmails;
	private JsonArray authorizedHostsLogin;
	private ClientCredentialFetcher clientCredentialFetcher;
	private long sessionsLimit;
	private HttpClient sessionLimitConfClient;
	private JsonArray ipAllowedByPassLimit;
	protected final SafeRedirectionService redirectionService = SafeRedirectionService.getInstance();
	private MfaService mfaSvc;
	private OpenIdSloServiceImpl sloServiceImpl;
	private Set<String> clientIdsAuthorized = new HashSet();

	public enum AuthEvent {
		ACTIVATION, LOGIN, SMS
	}
	public static final String CREATE_SESSION_ADRESS = "auth.createSession";

	private Pattern passwordPattern;
	private String smsProvider;
	private boolean slo;
	private List<String> internalAddress;
	private boolean checkFederatedLogin = false;
	private long jwtTtlSeconds;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		GrantHandlerProvider grantHandlerProvider = new DefaultGrantHandlerProvider();
		clientCredentialFetcher = new ClientCredentialFetcherImpl();
		token = new Token();
		token.setDataHandlerFactory(oauthDataFactory);
		token.setGrantHandlerProvider(grantHandlerProvider);
		token.setClientCredentialFetcher(clientCredentialFetcher);
		AccessTokenFetcherProvider accessTokenFetcherProvider = new DefaultAccessTokenFetcherProvider();
		protectedResource = new ProtectedResource();
		protectedResource.setDataHandlerFactory(oauthDataFactory);
		protectedResource.setAccessTokenFetcherProvider(accessTokenFetcherProvider);
		passwordPattern = Pattern.compile(config.getString("passwordRegex", ".{8}.*"));
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		JsonArray authorizedSessions = getOrElse(config.getJsonArray("authorize-mobile-session"), new JsonArray());
		authorizedSessions.forEach(session -> clientIdsAuthorized.add((String) session));
		if (server != null && server.get("smsProvider") != null)
			smsProvider = (String) server.get("smsProvider");
		slo = config.getBoolean("slo", false);
		sessionsLimit = config.getLong("sessions-limit", 0L);
		log.info("Initial Session limit : " + sessionsLimit);
		jwtTtlSeconds = config.getLong("jwt-ttl-seconds", 600L);
		final JsonObject sessionLimitConfig = config.getJsonObject("session-limit-config");
		if (sessionLimitConfig != null && isNotEmpty(sessionLimitConfig.getString("uri")) && isNotEmpty(sessionLimitConfig.getString("platform"))) {
			// TODO replace http conf with pg referencial
			try {
				final URI sessionLimitConfigUri = new URI(sessionLimitConfig.getString("uri"));
				final String sessionLimitConfigHost = sessionLimitConfigUri.getHost();
				final HttpClientOptions sessionLimitOptions = new HttpClientOptions()
					.setDefaultHost(sessionLimitConfigUri.getHost())
					.setDefaultPort(sessionLimitConfigUri.getPort())
					.setSsl("https".equals(sessionLimitConfigUri.getScheme()))
					.setMaxPoolSize(sessionLimitConfig.getInteger("poolSize", 2))
					.setKeepAlive(false)
					.setConnectTimeout(sessionLimitConfig.getInteger("timeout", 10000));
				if ("https".equals(sessionLimitConfigUri.getScheme())) {
					sessionLimitOptions.setForceSni(true);
				}
				sessionLimitConfClient = vertx.createHttpClient(sessionLimitOptions);
				vertx.setPeriodic(sessionLimitConfig.getLong("delay-refresh-session-limit", 300000L), sessionLimitHandler -> {
					sessionLimitConfClient.request(HttpMethod.GET, "/session/limit")
					.map(req -> req.putHeader("Host", sessionLimitConfigHost))
					.flatMap(HttpClientRequest::send)
					.onSuccess(clientResp -> {
						if (clientResp.statusCode() == 200) {
							clientResp.bodyHandler(sessionLimitBuffer -> {
								final JsonObject sessionLimitConfJson = new JsonObject(sessionLimitBuffer.toString("UTF-8"));
								if (sessionLimitConfJson.getLong(sessionLimitConfig.getString("platform")) != null) {
									final Long sessionsLimitTmp = sessionLimitConfJson.getLong(sessionLimitConfig.getString("platform"));
									if (sessionsLimitTmp != null && ((long) sessionsLimitTmp) != sessionsLimit) {
										sessionsLimit = sessionsLimitTmp;
										log.info("Update Session limit : " + sessionsLimit);
									}
								}
							});
						}
					})
					.onFailure(th -> log.error("Cannot update session limit", th));
				});
			} catch (URISyntaxException e) {
				log.error("Bad session limit confi URI", e);
			}
		}
		ipAllowedByPassLimit = getOrElse(config.getJsonArray("ip-allowed-by-pass-limit"), new JsonArray());

//		if (server != null) {
//			Boolean cluster = (Boolean) server.get("cluster");
//			if (Boolean.TRUE.equals(cluster)) {
//				ClusterManager cm = ((VertxInternal) vertx).clusterManager();
//				invalidEmails = cm.getSyncMap("invalidEmails");
//			} else {
//				invalidEmails = vertx.sharedData().getMap("invalidEmails");
//			}
//		} else {
		invalidEmails = MapFactory.getSyncClusterMap("invalidEmails", vertx);
		internalAddress = config.getJsonArray("internalAddress",
				new JsonArray().add("localhost").add("127.0.0.1")).getList();
		sloServiceImpl = new OpenIdSloServiceImpl(vertx, oauthDataFactory);
	}

	/**
	 * This method listens for
	 * messages on the"openid"
	 * address to
	 * process Single Log-Out (SLO) for OpenID Connect clients. It initializes a
	 * logout token and a list of client objects. For each message, it extracts the
	 * user ID, fetches authorizations for that user, and deletes associated tokens.
	 * It then constructs a client object for each authorization, ensuring
	 * uniqueness in the client list. For each unique client, it requests a logout
	 * token and, upon receiving it, triggers logout requests for each client. This
	 * process ensures the user is logged out from all registered clients.
	 **/
	@BusAddress("openid")
	public void sloOpenId(final Message<JsonObject> message) {
		if (message != null && message.body().getString("action").equals("oidc-slo")) {
			sloServiceImpl.sloOpenId(message);
		}
	}

	@Get("/oauth2/auth")
	public void authorize(final HttpServerRequest request) {
		final String responseType = request.params().get("response_type");
		final String clientId = request.params().get("client_id");
		final String redirectUri = request.params().get("redirect_uri");
		final String scope = request.params().get("scope");
		final String state = request.params().get("state");
		final String nonce = request.params().get("nonce");
		final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		if ("code".equals(responseType) && clientId != null && !clientId.trim().isEmpty()) {
			if (isNotEmpty(scope)) {
				final DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
				data.validateClientById(clientId, new Handler<Boolean>() {

					@Override
					public void handle(Boolean clientValid) {
						if (Boolean.TRUE.equals(clientValid)) {
							UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {

								@Override
								public void handle(UserInfos user) {
									if (user != null && user.getUserId() != null) {
										((OAuthDataHandler) data).createOrUpdateAuthInfo(clientId, user.getUserId(),
												scope, redirectUri, nonce, sessionId, new Handler<AuthInfo>() {

													@Override
													public void handle(AuthInfo auth) {
														if (auth != null) {
															code(request, redirectUri, auth.getCode(), state, nonce);
														} else {
															serverError(request, redirectUri, state);
														}
													}
												});
									} else {
										redirectToLogin(request, responseType, clientId, redirectUri, scope, state, nonce);
										//viewLogin(request, null, request.uri());
									}
								}
							});
						} else {
							unauthorizedClient(request, redirectUri, state);
						}
					}
				});
			} else {
				invalidScope(request, redirectUri, state);
			}
		} else {
			invalidRequest(request, redirectUri, state);
		}
	}

	private void redirectToLogin(HttpServerRequest request, String responseType, String clientId, String redirectUri, String scope, String state, String nonce) {
		String path = "/auth/oauth2/auth?state=%s&scope=%s&response_type=%s&redirect_uri=%s&client_id=%s";
		if (!StringUtils.isEmpty(nonce)) path += "&nonce=%s";

		String location = "";
		try {
			final String responseTypeEnc = URLEncoder.encode(responseType, StandardCharsets.UTF_8.toString());
			final String clientIdEnc = URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString());
			final String redirectUriEnc = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
			final String scopeEnc = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
			final String stateEnc = URLEncoder.encode(state, StandardCharsets.UTF_8.toString());
			final String pathFormated;
			if (!StringUtils.isEmpty(nonce)) {
				final String nonceEnc = URLEncoder.encode(nonce, StandardCharsets.UTF_8.toString());
				pathFormated = String.format(path, stateEnc, scopeEnc, responseTypeEnc, redirectUriEnc, clientIdEnc, nonceEnc);
			} else {
				pathFormated = String.format(path, stateEnc, scopeEnc, responseTypeEnc, redirectUriEnc, clientIdEnc);
			}
			final String callback = URLEncoder.encode(pathFormated, StandardCharsets.UTF_8.toString());
			final String cookieCallback = getScheme(request) + "://" + getHost(request) + pathFormated;
			if (config.containsKey("oauthLoginUri")) {
				final String authLocation = config.getString("oauthLoginUri");
				location = extractLocation(authLocation, request, callback, cookieCallback);
				location = String.format("%s?callback=%s", location, URLEncoder.encode(cookieCallback, StandardCharsets.UTF_8.toString()));
			} else if (config.containsKey("authLocations")) {
				final String host = Renders.getHost(request);
				final String authLocation = config.getJsonObject("authLocations", new JsonObject()).getJsonObject(host, new JsonObject()).getString("loginUri");
				location = extractLocation(authLocation, request, callback, cookieCallback);
			} else if (config.containsKey("loginUri")) {
				final String authLocation = config.getString("loginUri");
				location = extractLocation(authLocation, request, callback, cookieCallback);
			}

			if (location == null || location.isEmpty()) {
				location = String.format("%s?callback=%s", AbstractFederateController.LOGIN_PAGE, callback);
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Encoding exception in redirectToLogin method", e);
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
		if (authLocation != null && !AbstractFederateController.LOGIN_PAGE.equals(authLocation)) {
			if (AbstractFederateController.WAYF_PAGE.equals(authLocation)) {
				location = String.format("%s?callback=%s", AbstractFederateController.WAYF_PAGE, callback);
			} else {
				//external login page
				location = authLocation;
			}
			CookieHelper.getInstance().setSigned("callback", cookieCallback, 600, request);
		}
		return location;
	}

	/** 
	 * Endpoint to convert a valid OAuth2 token in another platform-recognized token representing a user session.
	 * @param type Set to "QueryParam" to produce a JWT reusable in HTTP query params.
	 * @return JSON {"token_type":"QueryParam", "access_token":stringified token, "expires_in":number of seconds}
	 */
	@Get("/oauth2/token")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void createTokenFromOAuth2(final HttpServerRequest request) {
		final String tokenType = request.params().get("type");
		if( !(request instanceof SecureHttpServerRequest) || StringUtils.isEmpty(tokenType) ) {
			badRequest(request);
			return;
		}
		final String oauth2 = AppOAuthResourceProvider.getTokenId((SecureHttpServerRequest) request).orElse(null);
		if( oauth2==null ){
			badRequest(request);
			return;
		}
		/* Do not check if oauth token is valid for the current user, just create a new token derived from it. */
		if( "queryparam".equalsIgnoreCase(tokenType) ) {
			// Create a token to use in HTTP query params => a JWT with a short validity period.
			final Request req = new HttpServerRequestAdapter(request);
			final DataHandler data = oauthDataFactory.create(req);
			data.getAccessToken(oauth2, access -> {
				if (access == null || access.getAuthId() == null) {
					unauthorized(request);
					return;
				}
				data.getAuthInfoById(access.getAuthId(), authInfo -> {
					if(authInfo==null) {
						unauthorized(request);
						return;
					}
					renderJson(request, createQueryParamToken(request, authInfo.getUserId(), authInfo.getClientId(), jwtTtlSeconds));
				});
			});
		} else {
			badRequest(request);
		}
	}

	private JsonObject createQueryParamToken(final HttpServerRequest request, String userId, String clientId, final long ttlInSeconds) {
		final JsonObject result = new JsonObject()
			.put("token_type", "QueryParam");
		try {
			return result
				.put("access_token", UserUtils.createJWTForQueryParam(vertx, userId, clientId, ttlInSeconds, request))
				.put("expires_in", ttlInSeconds);
		} catch(Exception e) {
			return result.put("expires_in", 0).putNull( "access_token" );
		}
	}

	@Post("/oauth2/token")
	public void token(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new io.vertx.core.Handler<Void>() {

			@Override
			public void handle(Void v) {
				final Request req = new HttpServerRequestAdapter(request);
				token.handleRequest(req, new Handler<Response>() {

					@Override
					public void handle(Response response) {
						if (sessionsLimit > 0L && !ipAllowedByPassLimit.contains(getIp(request))) {
							UserUtils.getSessionsNumber(eb, ar -> {
								if (ar.succeeded()) {
									if (ar.result() > sessionsLimit) {
										renderJson(request, new JsonObject().put("error","quota_overflow"), 509);
									} else {
										this.oauthTokenHandle(response);
									}
								} else {
									renderJson(request, new JsonObject().put("error","quota_overflow"), 509);
								}
							});
						} else {
							this.oauthTokenHandle(response);
						}
					}

					private void oauthTokenHandle(Response response) {
						final String grantType = req.getParameter("grant_type");
						final UserData userData = response.getUserData();
						final String clientId = req.getParameter("client_id");
						if (response.getCode() == 200 && ("password".equals(grantType) ||
								"refresh_token".equals(grantType) ||
								"saml2".equals(grantType) ||
								"custom_token".equals(grantType))) {
							final ClientCredential clientCredential = clientCredentialFetcher.fetch(req);
							final Promise<String> futureUserId = Promise.promise();
							if ("password".equals(grantType)) {
								final String login = req.getParameter("username");
								trace.info(getIp(request) + " - Connexion de l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
								futureUserId.complete(userData.getId());
								eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login, clientCredential.getClientId(), request);
								userAuthAccount.storeDomainByLogin(login, getHost(request), getScheme(request), new io.vertx.core.Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (Boolean.FALSE.equals(event)) {
											log.error("[OAUTH] Error while storing last known domain for user " + login);
										}
									}
								});
							} else if ("refresh_token".equals(grantType)) {
								final DataHandler data = oauthDataFactory.create(req);
								data.getAuthInfoByRefreshToken(req.getParameter("refresh_token"), authInfo -> {
									if (authInfo == null) {
										futureUserId.fail("auth.info.not.found");
									} else {
										final String id = authInfo.getUserId();
										trace.info(getIp(request) + " - Reconnexion de l'utilisateur " + id + " - Referer " + request.headers().get("Referer"));
										futureUserId.complete(id);
										storeLoginEventAndDomain(request, clientCredential, id);
									}
								});
							} else if ("saml2".equals(grantType) || "custom_token".equals(grantType)) {
								if(userData != null) {
									trace.info(getIp(request) + " - Connexion de l'utilisateur fédéré " + userData.getLogin() + " - Referer " + request.headers().get("Referer"));
								}
								storeLoginEventAndDomain(request, clientCredential, userData.getId());
								futureUserId.complete(userData.getId());
								if (isNotEmpty(userData.getActivationCode())) {
									activateUser(userData.getActivationCode(), userData.getLogin(),
											userData.getEmail(), userData.getMobile(), userData.getSource(), request);
								}
							}
							if (clientIdsAuthorized.contains(clientId)) {
								futureUserId.future().onSuccess(userId -> {
									createSessionForMobile(userId, response, request);
									trace.info(String.format(
											"%s - Session mobile crée pour l'utilisateur %s - ClientID %s - Referer %s",
											getIp(request), req.getParameter("username"), clientId,
											request.headers().get("Referer")));
								}).onFailure(th -> {
									log.warn("Could not create a session for the user", th);
									renderJson(request, new JsonObject(response.getBody()), response.getCode());
								});
							} else {
								renderJson(request, new JsonObject(response.getBody()), response.getCode());
							}

						} else {
							String errorString = new JsonObject(response.getBody()).getString("error_description");
							if (OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED.equals(errorString)) {
								trace.info(String.format(
										"%s - Erreur de connexion ( %s ) pour l'utilisateur %s - Referer %s",
										getIp(request), OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED,
										req.getParameter("username"), request.headers().get("Referer")));
							} else if (OAuthDataHandler.AUTH_ERROR_BLOCKED_USER.equals(errorString)) {
								trace.info(String.format(
										"%s - Erreur de connexion ( %s ) pour l'utilisateur %s - Referer %s",
										getIp(request), OAuthDataHandler.AUTH_ERROR_BLOCKED_USER,
										req.getParameter("username"), request.headers().get("Referer")));
							} else if ("auth.error.ban".equals(errorString)) {
								trace.info(String.format(
										"%s - Erreur de connexion ( %s ) pour l'utilisateur %s - Referer %s",
										getIp(request), "auth.error.ban", req.getParameter("username"),
										request.headers().get("Referer")));
							} else if (OAuthDataHandler.AUTH_ERROR_BLOCKED_PROFILETYPE.equals(errorString)) {
								trace.info(String.format(
										"%s - Erreur de connexion ( %s ) pour l'utilisateur %s - Referer %s",
										getIp(request), OAuthDataHandler.AUTH_ERROR_BLOCKED_PROFILETYPE,
										req.getParameter("username"), request.headers().get("Referer")));
							} else {
								trace.info(String.format(
										"%s - Erreur de connexion pour l'utilisateur - Referer %s",
										getIp(request), request.headers().get("Referer")));
							}

							renderJson(request, new JsonObject(response.getBody()), response.getCode());
						}
					}

					private void storeLoginEventAndDomain(final HttpServerRequest request, final ClientCredential clientCredential,
							String id) {
						eventStore.createAndStoreEventByUserId(AuthEvent.LOGIN.name(), id, clientCredential.getClientId(), request);
						userAuthAccount.storeDomain(id, getHost(request), getScheme(request), new io.vertx.core.Handler<Boolean>() {
							@Override
							public void handle(Boolean event) {
								if (Boolean.FALSE.equals(event)) {
									log.error("[OAUTH] Error while storing last known domain for user " + id);
								}
							}
						});
					}


					private void activateUser(final String activationCode, final String login, String email, String mobile, String source,
							final HttpServerRequest request) {
						final String theme = config.getJsonObject("activation-themes", new JsonObject())
								.getJsonObject(Renders.getHost(request), new JsonObject()).getString(source);
						userAuthAccount.activateAccountWithRevalidateTerms(login, activationCode, UUID.randomUUID().toString(),
								email, mobile, theme, request, activated -> {
								if (activated.isRight() && activated.right().getValue() != null) {
									trace.info(getIp(request) + " - Activation fédérée mobile du compte utilisateur " + login + " - Referer " + request.headers().get("Referer"));
									eventStore.createAndStoreEvent(AuthController.AuthEvent.ACTIVATION.name(), login, request);
								} else {
									trace.info(getIp(request) + " - Echec de l'activation fédérée mobile : compte utilisateur " + login + " introuvable ou déjà activé" + " - Referer " + request.headers().get("Referer"));
								}
						});
					}

				});
			}
		});
	}

	@Post("/oauth2/token-as-cookie")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	@ApiDoc("Gives back a cookie to the user corresponding to its jwtToken")
	public void tokenAsCookie(final HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
			final Optional<String> jwtToken = getTokenHeader(request);
			if(jwtToken.isPresent()) {
				final String oneSessionId = UUID.randomUUID().toString();
				UserUtils.createSessionWithId(eb, user.getUserId(), oneSessionId, false)
				.onSuccess(e -> {
					log.debug("[AuthController@tokenAsCookie] Session created for user");
					final long timeout = config.getLong("cookie_timeout", Long.MIN_VALUE);
					CookieHelper.getInstance().setSigned("oneSessionId", oneSessionId, timeout, request);
					CookieHelper.set("authenticated", "true", timeout, request);
					Renders.render(request, new JsonObject().put("succces", true));
				})
				.onFailure(th -> {
					log.warn("[AuthController@tokenAsCookie] Error while creating session", th);
					Renders.renderError(request);
				});
			} else {
				log.warn("[AuthController@tokenAsCookie] Called without a jwt token");
				Renders.badRequest(request);
			}
		});
	}

	private void loginResult(final HttpServerRequest request, String error, String callBack) {
		final JsonObject context = new JsonObject();
		if (callBack != null && !callBack.trim().isEmpty()) {
			try {
				context.put("callBack", URLEncoder.encode(callBack, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (error != null && !error.trim().isEmpty()) {
			context.put("error", new JsonObject().put("message",
					I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
		}
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.put("notLoggedIn", user == null);
				renderView(request, context, "login.html", null);
			}
		});
	}

	@Get("/context")
	public void context(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		context.put("callBack", config.getJsonObject("authenticationServer").getString("loginCallback"));
		context.put("cgu", config.getBoolean("cgu", true));
		context.put("passwordRegex", passwordPattern.toString());
		context.put("mandatory", config.getJsonObject("mandatory", new JsonObject()));
		// Human-readable password format :
		final I18n i18n = I18n.getInstance();
		final JsonObject pwdResetFormatByLang = new JsonObject();
		final JsonObject pwdActivationFormatByLang = new JsonObject();
		i18n.getLanguages(Renders.getHost(request))
		.stream()
		.map(String.class::cast)
		.forEach( (String lang) -> {
			if( lang != null ) {
				try {
					pwdResetFormatByLang.put(lang, i18n.translate("password.rules.reset", Renders.getHost(request), lang));
				} catch (Exception e) {
					pwdResetFormatByLang.put(lang, "");
					log.error("error when translating password.rules.reset in {0} : {1}", lang, e);
				}

				try {
					pwdActivationFormatByLang.put(lang, i18n.translate("password.rules.activation", Renders.getHost(request), lang));
				} catch (Exception e) {
					pwdActivationFormatByLang.put(lang, "");
					log.error("error when translating password.rules.activation in {0} : {1}", lang, e);
				}
			}
		});
		context.put("passwordRegexI18n", pwdResetFormatByLang);
		context.put("passwordRegexI18nActivation", pwdActivationFormatByLang);

		final JsonArray mfaConfig = new JsonArray();
		if( Mfa.withSms() ) mfaConfig.add(Mfa.TYPE_SMS);
		if( Mfa.withEmail() ) mfaConfig.add(Mfa.TYPE_EMAIL);
		context.put("mfaConfig", mfaConfig);

		renderJson(request, context);
	}

	@Get("/user/requirements")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void userRequirements(final HttpServerRequest request) {
		UserUtils.getSession(eb, request, session -> {
			final JsonObject requirements = new JsonObject();
			if( session != null ) {
				UserValidation.getMandatoryUserValidation(eb, session, true)
				.onSuccess( validations -> {
					if( validations != null ) {
						requirements.mergeIn(validations);
					}
					final UserInfos userInfos = UserUtils.sessionToUserInfos(session);
					if( userInfos!=null && (userInfos.isADML() || userInfos.isADMC()) ) {
						requirements.put("mfaProtectedUrls", Mfa.getMfaProtectedUrls());
					}
				})
				.onComplete( ar -> {
					if( ar.failed() ) {
						notFound(request);
					} else {
						renderJson(request, requirements);
					}
				});
			} else {
				notFound(request, "user.not.found");
			}
		});
	}

	@Get("/admin-welcome-message")
	public void adminWelcomeMessage(final HttpServerRequest request) {
		renderView(request);
	}

	private void viewLogin(final HttpServerRequest request, String error, String callBack) {
		final JsonObject context = new JsonObject();
		if (callBack != null && !callBack.trim().isEmpty()) {
			try {
				context.put("callBack", URLEncoder.encode(callBack, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (error != null && !error.trim().isEmpty()) {
			context.put("error", new JsonObject().put("message",
					I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
		}
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.put("notLoggedIn", user == null);
				renderView(request, context, "login.html", null);
			}
		});
	}

	@Get("/login")
	public void login(final HttpServerRequest request) {
		final String host = getHost(request);
		if (authorizedHostsLogin != null && isNotEmpty(host) && !authorizedHostsLogin.contains(host)) {
			redirectionService.redirect(request, pathPrefix + "/openid/login");
		} else {
			UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
				@Override
				public void handle(UserInfos user) {
					if (user == null || !config.getBoolean("auto-redirect", true)) {
						if (sessionsLimit > 0L && !ipAllowedByPassLimit.contains(getIp(request))) {
							UserUtils.getSessionsNumber(eb, ar -> {
								if (ar.succeeded()) {
									if (ar.result() > sessionsLimit) {
										renderView(request, new JsonObject(), "tooload.html", null);
									} else {
										viewLogin(request, null, "");
									}
								} else {
									renderView(request, new JsonObject(), "tooload.html", null);
								}
							});
						} else {
							viewLogin(request, null, "");
						}
					} else {
						String callBack = request.params().get("callBack");
						if (isEmpty(callBack)) {
							callBack = getScheme(request) + "://" + host;
						}
						redirectionService.redirect(request, callBack, "");
					}
				}
			});
		}
	}

	@Post("/login")
	public void loginSubmit(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new io.vertx.core.Handler<Void>() {
			@Override
			public void handle(Void v) {
				String c = request.formAttributes().get("callBack");
				final StringBuilder callBackBuilder = new StringBuilder();
				if (c != null && !c.trim().isEmpty()) {
					try {
						if (request.formAttributes().get("details") != null && !request.formAttributes().get("details").isEmpty()) {
							c += "#" + request.formAttributes().get("details");
						}
						callBackBuilder.append(URLDecoder.decode(c, "UTF-8"));
					} catch (UnsupportedEncodingException ex) {
						log.error(ex.getMessage(), ex);
						callBackBuilder.append(config.getJsonObject("authenticationServer").getString("loginCallback"));
					}
				} else {
					checkAndAppendCookieCallback(callBackBuilder, request);
				}
				final String login = request.formAttributes().get("email");
				final String password = request.formAttributes().get("password");
				final String callBack = callBackBuilder.toString();
				logInWithPasswordThenRedirect(login, password, request, callBack)
				.onFailure(e -> {
					// try activation code
					logInWithActivationCode(login, password)
					.onSuccess(infos -> handleMatchActivationCode(login, password, infos, request))
					// try reset code
					.recover(throwable -> logInWithResetCode(login, password)
						.onSuccess(infos -> handleMatchResetCode(login, password, request))
					)
					.onFailure(throwable -> {
						final OAuthError oauthError = OAuthError.class.isInstance(e)
						? OAuthError.class.cast(e)
						: null;
						final String description = oauthError!=null ? oauthError.getDescription() : e.getMessage();
						final String message = (oauthError!=null && !StringUtils.isEmpty(description) && oauthError.getCode() == 401)
						? getIp(request) + " - Erreur de connexion (" +  description + ") pour l'utilisateur "
							+ login + " - Referer " + request.headers().get("Referer")
						: getIp(request) + " - Erreur de connexion pour l'utilisateur "
							+ login + " - Referer " + request.headers().get("Referer");

						trace.info(message);
						loginResult(request, description, callBack);
					});
				});
			}
		});
	}

	/**
	 * This method occurs when there is no "callBack" param in request {@link io.vertx.core.MultiMap}
	 * We check if we have a "callback" in a getSignedCookie {@link CookieHelper} to assign to our callBack {@link StringBuilder}
	 *
	 * @param callBack  String builder containing callback for redirect option... {@link StringBuilder}
	 * @param request 	Request where we attempt to fetch our "callback" {@link CookieHelper} {@link HttpServerRequest}
	 */
	private void checkAndAppendCookieCallback(StringBuilder callBack, HttpServerRequest request) {
		final String callbackCookie = CookieHelper.getInstance().getSigned("callback", request);
		if (isNotEmpty(callbackCookie)) {
			CookieHelper.getInstance().setSigned("callback", "", 0, request);
			callBack.append(callbackCookie);
			return;
		}
		callBack.append(config.getJsonObject("authenticationServer").getString("loginCallback"));
	}

	/**
	 * Try to log a user in, checking his password.
	 * On success, the `request` will be responded with an HTTP 302 Redirect Location:`callBack`
	 * @param login the user to log in
	 * @param password to be checked
	 * @param request to respond to
	 * @param callBack where to redirect on success
	 * @return a future, useful for composing/recovering.
	 */
	private Future<String> logInWithPasswordThenRedirect(
		final String login, final String password, final HttpServerRequest request, final String callBack
		) {
		Promise<String> promise = Promise.promise();
		DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
		data.getUserId(login, password, tryUserId -> {
			String userId;
			try {
				userId = tryUserId.get();
				if (!StringUtils.isEmpty(userId)) {
					// Log the user in and redirect him as needed
					handleGetUserId(login, userId, request, callBack);
					promise.complete(userId);
				} else {
					throw new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED);
				}
			} catch (AccessDenied e) {
				promise.fail(e);
			}
		});
		return promise.future();
	}

	/**
	 * Try to log a user in, checking his activation code.
	 * @param login the user to log in. Can be a user alias.
	 * @param activationCode to be checked
	 * @return a future of user's details : login, displayName, email, mobile.
	 */
	private Future<JsonObject> logInWithActivationCode(final String login, final String activationCode) {
		Promise<JsonObject> promise = Promise.promise();
		// try activation with login
		userAuthAccount.matchActivationCode(login, activationCode, event1 -> {
			if( event1.isRight() ) {
				promise.complete(event1.right().getValue());
			} else {
				// try activation with loginAlias
				userAuthAccount.matchActivationCodeByLoginAlias(login, activationCode, event2 -> {
					if( event2.isRight() ) {
						promise.complete(event2.right().getValue());
					} else {
						promise.fail(event2.left().getValue());
					}
				});
			}
		});
		return promise.future();
	}

	/**
	 * Try to log a user in, checking his reset code.
	 * @param login the user to log in. Can be a user alias.
	 * @param resetCode to be checked
	 * @return a future of user's details : login, displayName, email, mobile.
	 */
	private Future<JsonObject> logInWithResetCode(final String login, final String resetCode) {
		Promise<JsonObject> promise = Promise.promise();
		// try reset with login
		userAuthAccount.matchResetCode(login, resetCode, event1 -> {
			if( event1.isRight() ) {
				promise.complete(event1.right().getValue());
			} else {
				// try reset with loginAlias
				userAuthAccount.matchResetCodeByLoginAlias(login, resetCode, event2 -> {
					if( event2.isRight() ) {
						promise.complete(event2.right().getValue());
					} else {
						promise.fail(event2.left().getValue());
					}
				});
			}
		});
		return promise.future();
	}
	
	private void handleGetUserId(String login, String userId, HttpServerRequest request, String callback) {
		trace.info(getIp(request) + " - Connexion de l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
		userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
				new io.vertx.core.Handler<Boolean>() {
					public void handle(Boolean ok) {
						if (!ok) {
							trace.error("[Auth](loginSubmit) Error while storing last known domain for user " + userId);
						}
					}
				});
		eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login, request);
		createSession(userId, request, callback);
	}

	private void handleMatchActivationCode(String login, String password, final JsonObject infos, HttpServerRequest request) {
		trace.info(getIp(request) + " - Code d'activation entré pour l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
		final JsonObject json = new JsonObject();
		json.put("activationCode", password);
		json.put("login", login);
		if (config.getBoolean("cgu", true)) {
			json.put("cgu", true);
		}
		json.put("displayName", StringUtils.trimToBlank(infos.getString("displayName")));
		json.put("email", StringUtils.trimToBlank(infos.getString("email")));
		json.put("phone", StringUtils.trimToBlank(infos.getString("mobile")));
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				json.put("notLoggedIn", user == null);
				renderView(request, json, "activation.html", null);
			}
		});
	}

	private void handleMatchResetCode(String login, String password, HttpServerRequest request) {
		redirectionService.redirect(request, "/auth/reset/" + password + "?login=" + login);
	}

	/**
	 * Creates a session for requests that seem to be coming from the mobile app (i.e. OAuth2 authorization).
	 * The created request will have the access token as a  session id #WB-1578.
	 * @param userId User id of the logged user
	 * @param response OAuth uthentication response
	 * @param request User's authentication request
	 */
	private void createSessionForMobile(final String userId, final Response response, final HttpServerRequest request) {
		final String token = (String) Json.decodeValue(response.getBody(), Map.class).get("access_token");
		UserUtils.createSessionWithId(eb, userId, token, "true".equals(request.formAttributes().get("secureLocation")))
		.onComplete(asyncResult -> {
			renderJson(request, new JsonObject(response.getBody()), response.getCode());
		});
	}

	private void createSession(String userId, final HttpServerRequest request, final String callBack) {
		UserUtils.createSession(eb, userId, "true".equals(request.formAttributes().get("secureLocation")),
				sessionId -> {
					if (sessionId != null && !sessionId.trim().isEmpty()) {
						boolean rememberMe = "true".equals(request.formAttributes().get("rememberMe"));
						long timeout = rememberMe ? 3600l * 24 * 365 : config.getLong("cookie_timeout", Long.MIN_VALUE);
						CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
						CookieHelper.set("authenticated", "true", timeout, request);
						//create xsrf token on create session to avoid cache issue
						if(config.getBoolean("xsrfOnAuth", true)){
							CookieHelper.set("XSRF-TOKEN", UUID.randomUUID().toString(), timeout, request);
						}
						redirectionService.redirect(request,
								callBack.matches("https?://[0-9a-zA-Z\\.\\-_]+/auth/login/?(\\?.*)?")
										? callBack.replaceFirst("/auth/login", "")
										: callBack,
								"");
					} else {
						loginResult(request, "auth.error.authenticationFailed", callBack);
					}
				});
	}

	@Get("/logout")
	public void logout(final HttpServerRequest request) {
		final String c = request.params().get("callback");
		if (slo) {
			UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
				@Override
				public void handle(UserInfos event) {
					if (event != null && Boolean.TRUE.equals(event.getFederated())
							&& !request.params().contains("SAMLRequest")) {
						if (config.getJsonObject("openid-federate", new JsonObject()).getJsonObject("domains",
								new JsonObject()).containsKey(getHost(request))) {
							redirectionService.redirect(request, "/auth/openid/slo?callback=" + c);
						} else {
							redirectionService.redirect(request, "/auth/saml/slo?callback=" + c);
						}
					} else {
						String c1 = c;
						if (c1 != null && c1.endsWith("service=")) { // OMT hack
							try {
								c1 += URLEncoder.encode(getScheme(request) + "://" + getHost(request), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								log.error(e.getMessage(), e);
							}
						}
						logoutCallback(request, c1, config, eb);
					}
				}
			});
		} else {
			logoutCallback(request, c, config, eb);
		}
	}

	public static void logoutCallback(final HttpServerRequest request, String c, JsonObject config, EventBus eb) {
		final SafeRedirectionService redirectionService = SafeRedirectionService.getInstance();
		final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		final StringBuilder callback = new StringBuilder();
		if (c != null && !c.trim().isEmpty()) {
			if (c.contains("_current-domain_")) {
				c = c.replaceAll("_current\\-domain_", request.headers().get("Host"));
			}
			try {
				callback.append(URLDecoder.decode(c, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
				callback.append(config.getJsonObject("authenticationServer").getString("logoutCallback", "/"));
			}
		} else {
			callback.append(config.getJsonObject("authenticationServer").getString("logoutCallback", "/"));
		}

		if (sessionId != null && !sessionId.trim().isEmpty()) {
			UserUtils.deleteSession(eb, sessionId, new io.vertx.core.Handler<Boolean>() {

				@Override
				public void handle(Boolean deleted) {
					if (Boolean.TRUE.equals(deleted)) {
						CookieHelper.set("oneSessionId", "", 0l, request);
						CookieHelper.set("authenticated", "", 0l, request);
					}
					redirectionService.redirect(request, callback.toString(), "");
				}
			});
		} else {
			redirectionService.redirect(request, callback.toString(), "");
		}
	}

	@Get("/oauth2/userinfo")
	@SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
	public void userInfo(final HttpServerRequest request) {
		UserUtils.getSession(eb, request, new io.vertx.core.Handler<JsonObject>() {

			@Override
			public void handle(JsonObject infos) {
				if (infos != null) {
					JsonObject info;
					UserInfoAdapter adapter = ResponseAdapterFactory.getUserInfoAdapter(request);
					if (request instanceof SecureHttpServerRequest) {
						SecureHttpServerRequest sr = (SecureHttpServerRequest) request;
						String clientId = sr.getAttribute("client_id");
						info = adapter.getInfo(infos, clientId);
						if (isNotEmpty(clientId)) {
							createStatsEvent(infos, clientId);
						}
					} else {
						info = adapter.getInfo(infos, null);
					}
					renderJson(request, info);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/oauth2/userinfo")
	@SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
	public void userInfoPost(final HttpServerRequest request)
	{
		this.userInfo(request);
	}

	private void createStatsEvent(JsonObject infos, String clientId) {
		JsonObject custom = new JsonObject().put("override-module", clientId)
				.put("connector-type", "OAuth2");
		UserInfos user = new UserInfos();
		user.setUserId(infos.getString("userId"));
		final JsonArray structures = infos.getJsonArray("structures");
		if (structures != null) {
			user.setStructures(structures.getList());
		}
		user.setType(infos.getString("type"));
		eventStore.createAndStoreEvent(TRACE_TYPE_CONNECTOR, user, custom);
	}

	@Get("/internal/userinfo")
	@SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
	public void internalUserInfo(final HttpServerRequest request) {
		if (!(request instanceof SecureHttpServerRequest) || !internalAddress.contains(getIp(request))) {
			forbidden(request);
			return;
		}
		UserUtils.getSession(eb, ((SecureHttpServerRequest) request),
				new io.vertx.core.Handler<JsonObject>() {

					@Override
					public void handle(JsonObject infos) {
						if (infos != null) {
							JsonObject info;
							UserInfoAdapter adapter = ResponseAdapterFactory.getUserInfoAdapter(request);
							SecureHttpServerRequest sr = (SecureHttpServerRequest) request;
							String clientId = sr.getAttribute("client_id");
							info = adapter.getInfo(infos, clientId);
							renderJson(request, info);
						} else {
							unauthorized(request);
						}
					}
				});
	}

	@BusAddress("wse.oauth")
	public void oauthResourceServer(final Message<JsonObject> message) {
		if (message.body() == null) {
			message.reply(new JsonObject());
			return;
		}
		validToken(message);
	}

	@BusAddress("auth.store.lock.event")
	public void storeLockEvent(final Message<JsonObject> message) {
		if (message.body() != null) {
			userAuthAccount.storeLockEvent(message.body().getJsonArray("ids"), message.body().getBoolean("block"));
		}
		message.reply(new JsonObject());
	}

	public void loginUser(String userId, String login, HttpServerRequest requestToAnswer, String callback)
	{
		this.handleGetUserId(login, userId, requestToAnswer, callback);
	}

	private void validToken(final Message<JsonObject> message) {
		protectedResource.handleRequest(new JsonRequestAdapter(message.body()),
				new jp.eisbahn.oauth2.server.async.Handler<Try<OAuthError, ProtectedResource.Response>>() {

					@Override
					public void handle(Try<OAuthError, ProtectedResource.Response> resp) {
						ProtectedResource.Response response;
						try {
							response = resp.get();
							JsonObject r = new JsonObject().put("status", "ok").put("client_id", response.getClientId())
									.put("remote_user", response.getRemoteUser()).put("scope", response.getScope());
							message.reply(r);
						} catch (OAuthError e) {
							message.reply(new JsonObject().put("error", e.getType()));
						}
					}
				});
	}

	@Get("/activation")
	public void activeAccount(final HttpServerRequest request) {
		final JsonObject json = new JsonObject();
		if (request.params().contains("activationCode")) {
			json.put("activationCode", request.params().get("activationCode"));
		}
		if (request.params().contains("login")) {
			json.put("login", request.params().get("login"));
		}
		if (config.getBoolean("cgu", true)) {
			json.put("cgu", true);
		}
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				json.put("notLoggedIn", user == null);
				renderView(request, json);
			}
		});
	}

	@Post("/activation/match")
	public void activeAccountMatch(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, data -> {
			if (data == null) {
				badRequest(request);
				return;
			}
			final String login = data.getString("login");
			final String password = data.getString("password");
			// try activation with login or loginAlias
			logInWithActivationCode(login, password)
			.onComplete(ar -> {
				renderJson(request, new JsonObject().put("match", ar.succeeded()));
			});
		});
	}

	/**
	 * API endpoint to verify that a reset code matches with a login or an alias login
	 * @param request the request with the reset code (password) and the login (or alias login)
	 */
	@Post("/reset/match")
	public void resetPasswordMatch(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, data -> {
			if (data == null) {
				badRequest(request);
				log.warn("Request body with password and login is expected");
				return;
			}
			final String login = data.getString("login");
			final String password = data.getString("password");
			logInWithResetCode(login, password)
			.onComplete(ar -> {
				renderJson(request, new JsonObject().put("match", ar.succeeded()));
			});
		});
	}

	@Post("/activation")
	public void activeAccountSubmit(final HttpServerRequest request) {
		activeAccountSubmit(request, true);
	}

	@Post("/activation/no-login")
	public void activeAccountSubmitNoLogin(final HttpServerRequest request) {
		activeAccountSubmit(request, false);
	}

	private void activeAccountSubmit(final HttpServerRequest request, final boolean autoLogin) {
		request.setExpectMultipart(true);
		request.endHandler(new io.vertx.core.Handler<Void>() {

			@Override
			public void handle(Void v) {
				final String login = request.formAttributes().get("login");
				final String activationCode = request.formAttributes().get("activationCode");
				final String email = request.formAttributes().get("mail");
				final String phone = request.formAttributes().get("phone");
				final String theme = request.formAttributes().get("theme");
				String password = request.formAttributes().get("password");
				String confirmPassword = request.formAttributes().get("confirmPassword");
				if (config.getBoolean("cgu", true) && !"true".equals(request.formAttributes().get("acceptCGU"))) {
					trace.info(getIp(request) + " - Invalid cgu " + login + " - Referer " + request.headers().get("Referer"));
					JsonObject error = new JsonObject().put("error", new JsonObject().put("message", "invalid.cgu"))
							.put("cgu", true);
					if (activationCode != null) {
						error.put("activationCode", activationCode);
					}
					if (login != null) {
						error.put("login", login);
					}
					renderJson(request, error);
				} else if (login == null || activationCode == null || password == null || login.trim().isEmpty()
						|| activationCode.trim().isEmpty() || password.trim().isEmpty()
						|| !password.equals(confirmPassword) || !passwordPattern.matcher(password).matches()
						|| (config.getJsonObject("mandatory", new JsonObject()).getBoolean("mail", false)
								&& (email == null || email.trim().isEmpty() || invalidEmails.containsKey(email)))
						|| (config.getJsonObject("mandatory", new JsonObject()).getBoolean("phone", false)
								&& (phone == null || phone.trim().isEmpty()))
						|| (email != null && !email.trim().isEmpty() && !StringValidation.isEmail(email))
						|| (phone != null && !phone.trim().isEmpty() && !StringValidation.isPhone(phone))) {
					trace.info(getIp(request) + " - Echec de l'activation du compte utilisateur " + login + " - Referer " + request.headers().get("Referer"));
					JsonObject error = new JsonObject().put("error",
							new JsonObject().put("message",
									I18n.getInstance().translate("auth.activation.invalid.argument", getHost(request),
											I18n.acceptLanguage(request))));
					if (activationCode != null) {
						error.put("activationCode", activationCode);
					}
					if (login != null) {
						error.put("login", login);
					}
					if (config.getBoolean("cgu", true)) {
						error.put("cgu", true);
					}
					renderJson(request, error);
				} else {
					userAuthAccount.activateAccount(login, activationCode, password, email, phone, theme, request,
							new io.vertx.core.Handler<Either<String, String>>() {

								@Override
								public void handle(Either<String, String> activated) {
									if (activated.isRight() && activated.right().getValue() != null) {
										handleActivation(login, request, activated, autoLogin);
									} else {
										// if failed because duplicated user
										if (activated.isLeft()
												&& "activation.error.duplicated".equals(activated.left().getValue())) {
											trace.info(getIp(request) + " - Echec de l'activation : utilisateur " + login + " en doublon" + " - Referer " + request.headers().get("Referer"));
											JsonObject error = new JsonObject().put("error",
													new JsonObject().put("message",
															I18n.getInstance().translate(activated.left().getValue(),
																	getHost(request), I18n.acceptLanguage(request))));
											error.put("activationCode", activationCode);
											renderJson(request, error);
										} else {
											// else try activation with loginAlias
											userAuthAccount.activateAccountByLoginAlias(login, activationCode, password,
													email, phone, theme, request,
													new io.vertx.core.Handler<Either<String, String>>() {
														@Override
														public void handle(Either<String, String> activated) {
															if (activated.isRight()
																	&& activated.right().getValue() != null) {
																handleActivation(login, request, activated, autoLogin);
															} else {
																trace.info(getIp(request) + " - Echec de l'activation : compte utilisateur "
																		+ login + " introuvable ou déjà activé" + " - Referer " + request.headers().get("Referer"));
																JsonObject error = new JsonObject().put("error",
																		new JsonObject().put("message",
																				I18n.getInstance().translate(
																						activated.left().getValue(),
																						getHost(request),
																						I18n.acceptLanguage(request))));
																error.put("activationCode", activationCode);
																renderJson(request, error);
															}
														}
													});
										}
									}
								}
							});
				}
			}
		});
	}

	private void handleActivation(String login, HttpServerRequest request, Either<String, String> activated,
								  boolean autoLogin) {
		final String userId = activated.right().getValue();
		trace.info(getIp(request) + " - Activation du compte utilisateur " + login + " - Referer " + request.headers().get("Referer"));
		eventStore.createAndStoreEvent(AuthEvent.ACTIVATION.name(), login, request);
		if (config.getBoolean("activationAutoLogin", false) && autoLogin) {
			trace.info(getIp(request) + " - Connexion de l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
			userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
					new io.vertx.core.Handler<Boolean>() {
						public void handle(Boolean ok) {
							if (!ok) {
								trace.error(
										"[Auth](loginSubmit) Error while storing last known domain for user " + userId);
							}
						}
					});
			eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login, request);
			createSession(userId, request, getScheme(request) + "://" + getHost(request));
		} else {
			redirectionService.redirect(request, "/auth/login");
		}
	}

	@Get("/forgot")
	public void forgotPassword(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.put("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	@Get("/upgrade")
	public void upgrade(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.put("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	/**
	 * Send a mail or a sms to reset forgot id
	 * @param request the request with the mail
	 * @response 200 if the reset code has been sent or even if the mail does not exist to avoid telling if the mail exists in database or not
	 * @response 400 if the mail is empty or if the service is invalid
	 */
	@Post("/forgot-id")
	public void forgetId(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new io.vertx.core.Handler<JsonObject>() {
			public void handle(JsonObject data) {
				final String mail = data.getString("mail");
				final String service = data.getString("service");
				final String firstName = data.getString("firstName");
				final String structure = data.getString("structureId");
				if (mail == null || mail.trim().isEmpty()) {
					badRequest(request);
					return;
				}
				userAuthAccount.findByMailAndFirstNameAndStructure(mail, firstName, structure,
						new io.vertx.core.Handler<Either<String, JsonArray>>() {
							@Override
							public void handle(Either<String, JsonArray> event) {
								// No user with that email, or more than one found.
								if (event.isLeft()) {
									renderJson(request, new JsonObject());
									return;
								}
								JsonArray results = event.right().getValue();
								if (results.size() == 0) {
									// Return status 200 to avoid telling if email exists in database or not
									renderJson(request, new JsonObject());
									return;
								}
								JsonArray structures = new JsonArray();
								if (results.size() > 1) {
									for (Object ob : results) {
										JsonObject j = (JsonObject) ob;
										j.remove("login");
										j.remove("mobile");
										if (!structures.toString().contains(j.getString("structureId")))
											structures.add(j);
									}
									if (firstName != null && structures.size() == 1)
										renderJson(request, new JsonObject());
									else
										renderJson(request, new JsonObject().put("structures", structures));
									return;
								}

								JsonObject match = results.getJsonObject(0);

								if(match.getString("activationCode") != null)
								{
									renderJson(request, new JsonObject());
									return;
								}

								final String id = match.getString("login", "");
								final String mobile = match.getString("mobile", "");

								// Force mail
								if ("mail".equals(service)) {
									userAuthAccount.sendForgottenIdMail(request, id, mail,
											new io.vertx.core.Handler<Either<String, JsonObject>>() {
												public void handle(Either<String, JsonObject> event) {
													if (event.isLeft()) {
														renderJson(request, new JsonObject());
														return;
													}
													if (smsProvider != null && !smsProvider.isEmpty()) {
														final String obfuscatedMobile = fr.wseduc.webutils.StringValidation
																.obfuscateMobile(mobile);
														renderJson(request,
																new JsonObject().put("mobile", obfuscatedMobile));
													} else {
														renderJson(request, new JsonObject());
													}
												}
											});
								} else if ("mobile".equals(service) && !mobile.isEmpty() && smsProvider != null
										&& !smsProvider.isEmpty()) {
									eventStore.createAndStoreEvent(AuthEvent.SMS.name(), id, request);
									userAuthAccount.sendForgottenIdSms(request, id, mobile,
											DefaultResponseHandler.defaultResponseHandler(request));
								} else {
									badRequest(request);
								}
							}
						});
			}
		});
	}

	/**
	 * API endpoint to verify if sms provider is configured
	 */
	@Get("/password-channels")
	public void getForgotPasswordService(final HttpServerRequest request) {
		renderJson(request, new JsonObject().put("mobile", smsProvider != null && !smsProvider.isEmpty()));
	}

	/**
	 * Send a mail or a sms with a reset code to the user
	 * @param request the request with the login and the service (mail or mobile)
	 * @response 200 if the reset code has been sent or even if the login does not exist to avoid telling if the login exists in database or not
	 * @response 400 if the login is empty or if the service is invalid
	 */
	@Post("/forgot-password")
	public void forgotPasswordSubmit(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new io.vertx.core.Handler<JsonObject>() {
			public void handle(JsonObject data) {
				final String login = data.getString("login");
				final String service = data.getString("service");
				final String resetCode = StringValidation.generateRandomCode(8);
				if (login == null || login.trim().isEmpty() || service == null || service.trim().isEmpty()) {
					badRequest(request, "invalid.login");
					return;
				}

				userAuthAccount.findByLogin(login, resetCode, checkFederatedLogin,
						new io.vertx.core.Handler<Either<String, JsonObject>>() {
							public void handle(Either<String, JsonObject> result) {
								if (result.isLeft()) {
									renderJson(request, new JsonObject());
									return;
								}
								if (result.right().getValue().size() == 0) {
									renderJson(request, new JsonObject());
									return;
								}
								if(result.right().getValue().getString("activationCode") != null)
								{
									renderJson(request, new JsonObject());
									return;
								}

								final String mail = result.right().getValue().getString("email", "");
								final String mobile = result.right().getValue().getString("mobile", "");
								final String displayName = result.right().getValue().getString("displayName", "");

								if ("mail".equals(service)) {
									userAuthAccount.sendResetPasswordMail(request, mail, resetCode, displayName, login,
											DefaultResponseHandler.defaultResponseHandler(request));
								} else if ("mobile".equals(service) && smsProvider != null && !smsProvider.isEmpty()) {
									eventStore.createAndStoreEvent(AuthEvent.SMS.name(), login, request);
									userAuthAccount.sendResetPasswordSms(request, mobile, resetCode, displayName, login,
											DefaultResponseHandler.defaultResponseHandler(request));
								} else {
									badRequest(request, "invalid.service");
								}
							}
						});

			}
		});
	}

	@Post("/sendResetPassword")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void sendResetPassword(final HttpServerRequest request) {
		String login = request.formAttributes().get("login");
		String email = request.formAttributes().get("email");
		String mobile = request.formAttributes().get("mobile");
		SendPasswordDestination dest = null;

		if (login == null || login.trim().isEmpty()) {
			badRequest(request, "login required");
			return;
		}
		if (StringValidation.isEmail(email)) {
			dest = new SendPasswordDestination();
			dest.setType("email");
			dest.setValue(email);
		} else if (StringValidation.isPhone(mobile)) {
			dest = new SendPasswordDestination();
			dest.setType("mobile");
			dest.setValue(mobile);
		} else {
			badRequest(request, "valid email or valid mobile required");
			return;
		}

		userAuthAccount.sendResetCode(request, login, dest, checkFederatedLogin, new io.vertx.core.Handler<Boolean>() {
			@Override
			public void handle(Boolean sent) {
				if (Boolean.TRUE.equals(sent)) {
					renderJson(request, new JsonObject());
				} else {
					badRequest(request);
				}
			}
		});
	}

	@Post("/generatePasswordRenewalCode")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void generatePasswordRenewalCode(final HttpServerRequest request) {
		String login = request.formAttributes().get("login");

		if (login == null || login.trim().isEmpty()) {
			badRequest(request, "login required");
			return;
		}

		userAuthAccount.generateResetCode(login, checkFederatedLogin, (Either<String, JsonObject> either) -> {
			if (either.isRight()) {
				renderJson(request, new JsonObject().put("renewalCode", either.right().getValue().getString("code")));
			} else {
				renderError(request);
			}
		});
	}

	@Post("/massGeneratePasswordRenewalCode")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void massGeneratePasswordRenewalCode(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, json -> {
			JsonArray userIds = json.getJsonArray("users");
			userAuthAccount.massGenerateResetCode(userIds, checkFederatedLogin, either -> {
				if (either.isRight()) {
					renderJson(request, either.right().getValue());
				} else {
					renderError(request);
				}
			});
		});
	}

	@Put("/block/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void blockUser(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new io.vertx.core.Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				final String userId = request.params().get("userId");
				boolean block = json.getBoolean("block", true);
				userAuthAccount.blockUser(userId, block, new io.vertx.core.Handler<Boolean>() {
					@Override
					public void handle(Boolean r) {
						if (Boolean.TRUE.equals(r)) {
							request.response().end();
							UserUtils.deletePermanentSession(eb, userId, null, null, new io.vertx.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (!event) {
										log.error("Error delete permanent session with userId : " + userId);
									}
								}
							});
							UserUtils.deleteCacheSession(eb, userId, null, new io.vertx.core.Handler<JsonArray>() {
								@Override
								public void handle(JsonArray event) {
									if (event == null) {
										log.error("Error delete cache session with userId : " + userId);
									}
								}
							});
						} else {
							badRequest(request);
						}
					}
				});
			}
		});
	}

	@Put("/users/block")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void blockUsers(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new io.vertx.core.Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				JsonArray userIds = json.getJsonArray("users");
				boolean block = json.getBoolean("block", true);
				userAuthAccount.blockUsers(userIds, block, new io.vertx.core.Handler<Boolean>() {
					@Override
					public void handle(Boolean r) {
						if (Boolean.TRUE.equals(r)) {
							request.response().end();
							for (int i = 0; i < userIds.size(); i++) {
								String userId = userIds.getString(i);
								UserUtils.deletePermanentSession(eb, userId, null, null, new io.vertx.core.Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (!event) {
											log.error("Error delete permanent session with userId : " + userId);
										}
									}
								});
								UserUtils.deleteCacheSession(eb, userId, null, new io.vertx.core.Handler<JsonArray>() {
									@Override
									public void handle(JsonArray event) {
										if (event == null) {
											log.error("Error delete cache session with userId : " + userId);
										}
									}
								});
							}
						} else {
							badRequest(request);
						}
					}
				});
			}
		});
	}

	@Get("/reset/:resetCode")
	public void resetPassword(final HttpServerRequest request) {
		resetPasswordView(request, null);
	}

	private void resetPasswordView(final HttpServerRequest request, final JsonObject p) {
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				JsonObject params = p;
				if (params == null) {
					params = new JsonObject();
				}
				if (user != null && "password".equals(request.params().get("resetCode"))) {
					renderView(request, params.put("login", user.getLogin()).put("callback", "/userbook/mon-compte"),
							"changePassword.html", null);
				} else if (user != null && "forceChangePassword".equals(request.params().get("resetCode"))) {
					renderView(request, params.put("login", user.getLogin())
									.put("callback", getOrElse(request.params().get("callback"), "/")),
							"forceChangePassword.html", null);
				} else {
					renderView(request,
							params.put("notLoggedIn", user == null).put("login", request.params().get("login"))
									.put("resetCode", request.params().get("resetCode")),
							"reset.html", null);
				}
			}
		});
	}

	@Post("/changePassword")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void changePasswordSubmit(final HttpServerRequest request) {
		changePassword(request);
	}

	@Post("/reset")
	public void resetPasswordSubmit(final HttpServerRequest request) {
		changePassword(request);
	}

	private void changePassword(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new io.vertx.core.Handler<Void>() {

			@Override
			public void handle(Void v) {
				final String login = request.formAttributes().get("login");
				final String resetCode = request.formAttributes().get("resetCode");
				final String oldPassword = request.formAttributes().get("oldPassword");
				final String password = request.formAttributes().get("password");
				String confirmPassword = request.formAttributes().get("confirmPassword");
				final String callback = Utils.getOrElse(request.formAttributes().get("callback"), "/auth/login", false);
				final String forceChange = Utils.getOrElse(request.formAttributes().get("forceChange"), "");
				if (login == null
						|| ((resetCode == null || resetCode.trim().isEmpty())
						&& (oldPassword == null || oldPassword.trim().isEmpty() || oldPassword.equals(password)))
						|| password == null || login.trim().isEmpty() || password.trim().isEmpty()
						|| !password.equals(confirmPassword) || !passwordPattern.matcher(password).matches()) {
					trace.info(getIp(request) + " - Erreur lors de la réinitialisation " + "du mot de passe de l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
					JsonObject error = new JsonObject().put("error", new JsonObject().put("message", I18n.getInstance()
							.translate("auth.reset.invalid.argument", getHost(request), I18n.acceptLanguage(request))));
					if (resetCode != null) {
						error.put("resetCode", resetCode);
					}
					renderJson(request, error);
				} else {
					DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
					data.getUserId(login, oldPassword, new Handler<Try<AccessDenied, String>>() {

						@Override
						public void handle(Try<AccessDenied, String> tryUserId) {

							String userId = null;
							try {
								userId = tryUserId.get();
							} catch (AccessDenied e) {
								// Will be handled by resetCode check
							}

							// Keep current session and app token alive
							Optional<String> sessionId = UserUtils.getSessionId(request);
							Optional<String> appToken = AppOAuthResourceProvider.getTokenId(new SecureHttpServerRequest(request));

							final String sessionIdStr = sessionId.isPresent() ? sessionId.get() : null;
							final String appTokenStr = appToken.isPresent() ? appToken.get() : null;
							final io.vertx.core.Handler<String> resultHandler = new io.vertx.core.Handler<String>() {

								@Override
								public void handle(String resetedUserId) {
									if (resetedUserId != null) {
											trace.info(getIp(request) + " - Réinitialisation réussie du mot de passe de l'utilisateur " + login + " - Referer " + request.headers().get("Referer"));
										final boolean forcedChangePw = "force".equals(forceChange);
										UserUtils.deleteCacheSession(eb, resetedUserId,  forcedChangePw ? null : sessionIdStr, deleted -> {
											if (sessionIdStr == null || forcedChangePw) {
												CookieHelper.set("oneSessionId", "", 0l, request);
												CookieHelper.set("authenticated", "", 0l, request);
											}
											if (forcedChangePw) {
												redirectionService.redirect(request, config.getJsonObject("authenticationServer",
														new JsonObject()).getString("loginURL", "/auth/login"));
											} else {
												redirectionService.redirect(request, callback);
											}
										});
										UserUtils.deletePermanentSession(eb, resetedUserId, sessionIdStr, appTokenStr, null);
									} else {
											trace.info(getIp(request) + " - Erreur lors de la réinitialisation du mot de passe de l'utilisateur "+ login + " - Referer " + request.headers().get("Referer"));
										error(request, resetCode);
									}
								}
							};

							if (resetCode != null && !resetCode.trim().isEmpty()) {
								userAuthAccount.resetPassword(login, resetCode, password, request, resultHandler);
							} else if(userId != null && !userId.trim().isEmpty()) {
								userAuthAccount.changePassword(login, password, request, resultHandler);
							} else {
								error(request, null);
							}

						}
					});
				}
			}

			private void error(final HttpServerRequest request, final String resetCode) {
				JsonObject error = new JsonObject().put("error", new JsonObject().put("message",
						I18n.getInstance().translate("reset.error", getHost(request), I18n.acceptLanguage(request))));
				if (resetCode != null) {
					error.put("resetCode", resetCode);
				}
				renderJson(request, error);
			}
		});

	}

	@Get("/cgu")
	public void cgu(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.put("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	@Post("/cgu")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void needToValidateCgu(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, data -> {
			if (data == null) {
				badRequest(request);
				return;
			}
				final String userId = data.getString("userId");
				this.userAuthAccount.needToValidateCgu(userId, ok->{
					if(ok) {
						noContent(request);
					}else {
						badRequest(request,"cgu.accept.failed");
					}
				});
		});
	}

	@Put("/cgu/revalidate")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void revalidateCgu(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if(user==null) {
				unauthorized(request,"cgu.accept.unauthorized");
			}else {
				String userId = user.getUserId();
				this.userAuthAccount.revalidateCgu(userId, ok->{
					if(ok) {
						noContent(request);	
					}else {
						badRequest(request,"cgu.accept.failed");
					}
				});
			}
		});
	}

	@Get("/user/mfa/code")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getUserMfaCode(final HttpServerRequest request) {
		// Get current code status (outdated|pending|valid)
		// May send a new code by sms or email, depending on the conf.
		UserUtils.getSession(eb, request, session -> {
			if (session == null) {
				renderError(request, new JsonObject().put("error", "session.not.found"));
				return;
			}
			final UserInfos userInfos = UserUtils.sessionToUserInfos(session);
			if (userInfos == null) {
				unauthorized(request);
				return;
			}

			mfaSvc.startMfaWorkflow(request, session, userInfos)
			.onSuccess( mfaState -> {
				/*{
					"type": "sms | email",
					"waitInSeconds": "estimated number of seconds before code reaches the mobile phone or mailbox",
					"state"?: {
						"state": "outdated | pending | valid", 
						"tries": number of remaining retries before code becomes outdated,
						"ttl": number of seconds remaining before expiration of the code
					}
				}*/
				mfaState.remove("valid");
				renderJson(request, new JsonObject()
					.put("type", Mfa.withSms() ? Mfa.TYPE_SMS : Mfa.TYPE_EMAIL)
					.put("waitInSeconds", UserValidation.getDefaultWaitInSeconds())
					.put("state", mfaState)
				);
			})
			.onFailure( e -> {
				renderError( request, new JsonObject().put("error", e.getMessage()) );
			});
		});
	}

	@Post("/user/mfa/code")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void postUserMfaCode(final HttpServerRequest request) {
		UserUtils.getSession(eb, request, session -> {
			if (session == null) {
				renderError(request, new JsonObject().put("error", "session.not.found"));
				return;
			}
			final UserInfos userInfos = UserUtils.sessionToUserInfos(session);
			if (userInfos == null) {
				unauthorized(request);
				return;
			}

			// Try validating a MFA workflow with a code
			RequestUtils.bodyToJson(request, pathPrefix + "postUserMfaCode", payload -> {
				final String key = payload.getString("key");

				mfaSvc.tryCode(request, userInfos, key)
				.onSuccess( mfaState -> {
					/*{
						"state": "outdated | pending | valid", 
						"tries": number of remaining retries before code becomes outdated,
						"ttl": number of seconds remaining before expiration of the code
					}*/
					mfaState.remove("valid");
					renderJson(request, mfaState);
				})
				.onFailure( exception -> {
					renderError(request, new JsonObject().put("error", exception.getMessage()));
				});
			});
		});
	}

	@Delete("/sessions")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void deletePermanentSessions(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
					UserUtils.deletePermanentSession(eb, user.getUserId(), sessionId, null,
							new io.vertx.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (event) {
										ok(request);
									} else {
										renderError(request);
									}
								}
							});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/generate/otp")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void generateOTP(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				userAuthAccount.generateOTP(user.getUserId(), defaultResponseHandler(request));
			} else {
				unauthorized(request, "invalid.user");
			}
		});
	}

	@Get("/revalidate-terms")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void revalidateTerms(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/validate-mail")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void validateMail(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/validate-mfa")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void validateMfa(HttpServerRequest request) {
		renderView(request);
	}

	public void setUserAuthAccount(UserAuthAccount userAuthAccount) {
		this.userAuthAccount = userAuthAccount;
	}

	public void setEventStore(EventStore eventStore) {
		this.eventStore = eventStore;
	}

	public void setAuthorizedHostsLogin(JsonArray authorizedHostsLogin) {
		this.authorizedHostsLogin = authorizedHostsLogin;
	}

	public void setOauthDataFactory(DataHandlerFactory oauthDataFactory) {
		this.oauthDataFactory = oauthDataFactory;
	}

	public void setCheckFederatedLogin(boolean checkFederatedLogin) {
		this.checkFederatedLogin = checkFederatedLogin;
	}

	public void setMfaService( final MfaService mfaSvc ) {
		this.mfaSvc = mfaSvc;
	}


	/**
	 *
	 * This method is used to force the password change for a user
	 * @param request
	 * 				- the request with the userId, the userId to force the password change
	 *
	 * @response 200 if the force change password has been sent
	 * @response 400 if the userId is empty
	 *
	 *
	 */
	@Post("/forceChangePassword")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void forceChangePassword(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new io.vertx.core.Handler<JsonObject>() {
			@Override
			public void handle(JsonObject data) {

				final String userId = data.getString("userId");

				if (userId == null || userId.trim().isEmpty()) {
					badRequest(request);
					return;
				}

				userAuthAccount.forceChangePassword(userId, either -> {
					if (either.isRight()) {
						renderJson(request, either.right().getValue());
					} else {
						renderError(request);
					}
				});
			}
		});
	}
}
