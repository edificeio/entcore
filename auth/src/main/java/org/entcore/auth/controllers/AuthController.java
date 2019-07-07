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

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.isEmpty;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.code;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidRequest;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidScope;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.serverError;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.unauthorizedClient;
import static org.entcore.common.aggregation.MongoConstants.TRACE_TYPE_CONNECTOR;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.response.DefaultResponseHandler;
import fr.wseduc.webutils.logging.Tracer;
import fr.wseduc.webutils.logging.TracerFactory;
import fr.wseduc.webutils.request.RequestUtils;

import io.vertx.core.shareddata.LocalMap;
import jp.eisbahn.oauth2.server.async.Handler;
import org.entcore.auth.adapter.ResponseAdapterFactory;
import org.entcore.auth.adapter.UserInfoAdapter;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.auth.services.impl.DefaultOpendIdConnectService;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.MapFactory;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.security.ActionType;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;
import jp.eisbahn.oauth2.server.endpoint.ProtectedResource;
import jp.eisbahn.oauth2.server.endpoint.Token;
import jp.eisbahn.oauth2.server.endpoint.Token.Response;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.exceptions.Try;
import jp.eisbahn.oauth2.server.fetcher.accesstoken.AccessTokenFetcherProvider;
import jp.eisbahn.oauth2.server.fetcher.accesstoken.impl.DefaultAccessTokenFetcherProvider;
import jp.eisbahn.oauth2.server.fetcher.clientcredential.ClientCredentialFetcher;
import jp.eisbahn.oauth2.server.fetcher.clientcredential.ClientCredentialFetcherImpl;
import jp.eisbahn.oauth2.server.granttype.GrantHandlerProvider;
import jp.eisbahn.oauth2.server.granttype.impl.DefaultGrantHandlerProvider;
import jp.eisbahn.oauth2.server.models.AuthInfo;
import jp.eisbahn.oauth2.server.models.Request;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.auth.oauth.HttpServerRequestAdapter;
import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.oauth.OAuthDataHandlerFactory;
import org.entcore.auth.pojo.SendPasswordDestination;
import org.entcore.auth.users.UserAuthAccount;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;

import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;

import fr.wseduc.security.SecuredAction;
import org.vertx.java.core.http.RouteMatcher;

public class AuthController extends BaseController {

	private DataHandlerFactory oauthDataFactory;
	private Token token;
	private ProtectedResource protectedResource;
	private UserAuthAccount userAuthAccount;
	private static final Tracer trace = TracerFactory.getTracer("auth");
	private EventStore eventStore;
	private Map<Object, Object> invalidEmails;
	private JsonArray authorizedHostsLogin;

	public enum AuthEvent {
		ACTIVATION, LOGIN, SMS
	}

	private Pattern passwordPattern;
	private String smsProvider;
	private boolean slo;
	private List<String> internalAddress;
	private boolean checkFederatedLogin = false;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		JsonObject oic = config.getJsonObject("openid-connect");
		OpenIdConnectService openIdConnectService = (oic != null)
				? new DefaultOpendIdConnectService(oic.getString("iss"), vertx, oic.getString("keys"))
				: null;
		checkFederatedLogin = config.getBoolean("check-federated-login", false);
		oauthDataFactory = new OAuthDataHandlerFactory(Neo4j.getInstance(), MongoDb.getInstance(), openIdConnectService,
				checkFederatedLogin);
		GrantHandlerProvider grantHandlerProvider = new DefaultGrantHandlerProvider();
		ClientCredentialFetcher clientCredentialFetcher = new ClientCredentialFetcherImpl();
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
		if (server != null && server.get("smsProvider") != null)
			smsProvider = (String) server.get("smsProvider");
		slo = config.getBoolean("slo", false);
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
				new fr.wseduc.webutils.collections.JsonArray().add("localhost").add("127.0.0.1")).getList();
	}

	@Get("/oauth2/auth")
	public void authorize(final HttpServerRequest request) {
		final String responseType = request.params().get("response_type");
		final String clientId = request.params().get("client_id");
		final String redirectUri = request.params().get("redirect_uri");
		final String scope = request.params().get("scope");
		final String state = request.params().get("state");
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
												scope, redirectUri, new Handler<AuthInfo>() {

													@Override
													public void handle(AuthInfo auth) {
														if (auth != null) {
															code(request, redirectUri, auth.getCode(), state);
														} else {
															serverError(request, redirectUri, state);
														}
													}
												});
									} else {
										viewLogin(request, null, request.uri());
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

	@Post("/oauth2/token")
	public void token(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new io.vertx.core.Handler<Void>() {

			@Override
			public void handle(Void v) {
				Request req = new HttpServerRequestAdapter(request);
				token.handleRequest(req, new Handler<Response>() {

					@Override
					public void handle(Response response) {
						renderJson(request, new JsonObject(response.getBody()), response.getCode());
					}
				});
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
		renderJson(request, context);
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
			redirect(request, pathPrefix + "/openid/login");
		} else {
			UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
				@Override
				public void handle(UserInfos user) {
					if (user == null || !config.getBoolean("auto-redirect", true)) {
						viewLogin(request, null, request.params().get("callBack"));
					} else {
						String callBack = request.params().get("callBack");
						if (isEmpty(callBack)) {
							callBack = getScheme(request) + "://" + host;
						}
						redirect(request, callBack, "");
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
				final StringBuilder callBack = new StringBuilder();
				if (c != null && !c.trim().isEmpty()) {
					try {
						if (request.formAttributes().get("details") != null && !request.formAttributes().get("details").isEmpty()) {
							c += "#" + request.formAttributes().get("details");
						}
						callBack.append(URLDecoder.decode(c, "UTF-8"));
					} catch (UnsupportedEncodingException ex) {
						log.error(ex.getMessage(), ex);
						callBack.append(config.getJsonObject("authenticationServer").getString("loginCallback"));
					}
				} else {
					callBack.append(config.getJsonObject("authenticationServer").getString("loginCallback"));
				}
				DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
				final String login = request.formAttributes().get("email");
				final String password = request.formAttributes().get("password");
				data.getUserId(login, password, new Handler<String>() {

					@Override
					public void handle(final String userId) {
						final String c = callBack.toString();
						if (userId != null && !userId.trim().isEmpty()) {
							handleGetUserId(login, userId, request, c);
						} else {
							// try activation with login
							userAuthAccount.matchActivationCode(login, password, new io.vertx.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean passIsActivationCode) {
									if (passIsActivationCode) {
										handleMatchActivationCode(login, password, request);
									} else {
										// try activation with loginAlias
										userAuthAccount.matchActivationCodeByLoginAlias(login, password,
												new io.vertx.core.Handler<Boolean>() {
													@Override
													public void handle(Boolean passIsActivationCode) {
														if (passIsActivationCode) {
															handleMatchActivationCode(login, password, request);
														} else {
															// try reset with login
															userAuthAccount.matchResetCode(login, password,
																	new io.vertx.core.Handler<Boolean>() {
																		@Override
																		public void handle(Boolean passIsResetCode) {
																			if (passIsResetCode) {
																				handleMatchResetCode(login, password,
																						request);
																			} else {
																				// try reset with loginAlias
																				userAuthAccount
																						.matchResetCodeByLoginAlias(
																								login, password,
																								new io.vertx.core.Handler<Boolean>() {
																									@Override
																									public void handle(
																											Boolean passIsResetCode) {
																										if (passIsResetCode) {
																											handleMatchResetCode(
																													login,
																													password,
																													request);
																										} else {
																											trace.info(
																													"Erreur de connexion pour l'utilisateur "
																															+ login);
																											loginResult(
																													request,
																													"auth.error.authenticationFailed",
																													c);
																										}
																									}
																								});
																			}
																		}
																	});
														}
													}
												});
									}
								}
							});

						}
					}
				});

			}
		});
	}

	private void handleGetUserId(String login, String userId, HttpServerRequest request, String callback) {
		trace.info("Connexion de l'utilisateur " + login);
		userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
				new io.vertx.core.Handler<Boolean>() {
					public void handle(Boolean ok) {
						if (!ok) {
							trace.error("[Auth](loginSubmit) Error while storing last known domain for user " + userId);
						}
					}
				});
		eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login);
		createSession(userId, request, callback);
	}

	private void handleMatchActivationCode(String login, String password, HttpServerRequest request) {
		trace.info("Code d'activation entré pour l'utilisateur " + login);
		final JsonObject json = new JsonObject();
		json.put("activationCode", password);
		json.put("login", login);
		if (config.getBoolean("cgu", true)) {
			json.put("cgu", true);
		}
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				json.put("notLoggedIn", user == null);
				renderView(request, json, "activation.html", null);
			}
		});
	}

	private void handleMatchResetCode(String login, String password, HttpServerRequest request) {
		redirect(request, "/auth/reset/" + password + "?login=" + login);
	}

	private void createSession(String userId, final HttpServerRequest request, final String callBack) {
		UserUtils.createSession(eb, userId, "true".equals(request.formAttributes().get("secureLocation")),
				sessionId -> {
					if (sessionId != null && !sessionId.trim().isEmpty()) {
						boolean rememberMe = "true".equals(request.formAttributes().get("rememberMe"));
						long timeout = rememberMe ? 3600l * 24 * 365 : config.getLong("cookie_timeout", Long.MIN_VALUE);
						CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
						CookieHelper.set("authenticated", "true", timeout, request);
						redirect(request,
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
						if (config.containsKey("openid-federate")) {
							redirect(request, "/auth/openid/slo?callback=" + c);
						} else {
							redirect(request, "/auth/saml/slo?callback=" + c);
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
					redirect(request, callback.toString(), "");
				}
			});
		} else {
			redirect(request, callback.toString(), "");
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
		UserUtils.getSessionByUserId(eb, ((SecureHttpServerRequest) request).getAttribute("remote_user"),
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
			// try activation with login
			userAuthAccount.matchActivationCode(login, password, match -> {
				if (match) {
					renderJson(request, new JsonObject().put("match", match));
				} else {
					// try activation with loginAlias
					userAuthAccount.matchActivationCodeByLoginAlias(login, password, matchAlias -> {
						renderJson(request, new JsonObject().put("match", matchAlias));
					});
				}
			});
		});
	}

	@Post("/activation")
	public void activeAccountSubmit(final HttpServerRequest request) {
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
					trace.info("Invalid cgu " + login);
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
					trace.info("Echec de l'activation du compte utilisateur " + login);
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
										handleActivation(login, request, activated);
									} else {
										// if failed because duplicated user
										if (activated.isLeft()
												&& "activation.error.duplicated".equals(activated.left().getValue())) {
											trace.info("Echec de l'activation : utilisateur " + login + " en doublon.");
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
																handleActivation(login, request, activated);
															} else {
																trace.info("Echec de l'activation : compte utilisateur "
																		+ login + " introuvable ou déjà activé.");
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

	private void handleActivation(String login, HttpServerRequest request, Either<String, String> activated) {
		final String userId = activated.right().getValue();
		trace.info("Activation du compte utilisateur " + login);
		eventStore.createAndStoreEvent(AuthEvent.ACTIVATION.name(), login);
		if (config.getBoolean("activationAutoLogin", false)) {
			trace.info("Connexion de l'utilisateur " + login);
			userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
					new io.vertx.core.Handler<Boolean>() {
						public void handle(Boolean ok) {
							if (!ok) {
								trace.error(
										"[Auth](loginSubmit) Error while storing last known domain for user " + userId);
							}
						}
					});
			eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login);
			createSession(userId, request, getScheme(request) + "://" + getHost(request));
		} else {
			redirect(request, "/auth/login");
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
									badRequest(request, event.left().getValue());
									return;
								}
								JsonArray results = event.right().getValue();
								if (results.size() == 0) {
									badRequest(request, "no.match");
									return;
								}
								JsonArray structures = new fr.wseduc.webutils.collections.JsonArray();
								if (results.size() > 1) {
									for (Object ob : results) {
										JsonObject j = (JsonObject) ob;
										j.remove("login");
										j.remove("mobile");
										if (!structures.toString().contains(j.getString("structureId")))
											structures.add(j);
									}
									if (firstName != null && structures.size() == 1)
										badRequest(request, "non.unique.result");
									else
										renderJson(request, new JsonObject().put("structures", structures));
									return;
								}
								JsonObject match = results.getJsonObject(0);
								final String id = match.getString("login", "");
								final String mobile = match.getString("mobile", "");

								// Force mail
								if ("mail".equals(service)) {
									userAuthAccount.sendForgottenIdMail(request, id, mail,
											new io.vertx.core.Handler<Either<String, JsonObject>>() {
												public void handle(Either<String, JsonObject> event) {
													if (event.isLeft()) {
														badRequest(request, event.left().getValue());
														return;
													}
													if (smsProvider != null && !smsProvider.isEmpty()) {
														final String obfuscatedMobile = StringValidation
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
									eventStore.createAndStoreEvent(AuthEvent.SMS.name(), id);
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

	@Get("/password-channels")
	public void getForgotPasswordService(final HttpServerRequest request) {
		userAuthAccount.findByLogin(request.params().get("login"), null, checkFederatedLogin,
				new io.vertx.core.Handler<Either<String, JsonObject>>() {
					public void handle(Either<String, JsonObject> result) {
						if (result.isLeft()) {
							badRequest(request, result.left().getValue());
							return;
						}
						if (result.right().getValue().size() == 0) {
							badRequest(request, "no.match");
							return;
						}

						final String mail = result.right().getValue().getString("email", "");
						final String mobile = result.right().getValue().getString("mobile", "");

						boolean mailCheck = mail != null && !mail.trim().isEmpty();
						boolean mobileCheck = mobile != null && !mobile.trim().isEmpty();

						if (!mailCheck && !mobileCheck) {
							badRequest(request, "no.match");
							return;
						}

						final String obfuscatedMail = StringValidation.obfuscateMail(mail);
						final String obfuscatedMobile = StringValidation.obfuscateMobile(mobile);

						if (smsProvider != null && !smsProvider.isEmpty())
							renderJson(request,
									new JsonObject().put("mobile", obfuscatedMobile).put("mail", obfuscatedMail));
						else
							renderJson(request, new JsonObject().put("mail", obfuscatedMail));
					}
				});
	}

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
									badRequest(request, result.left().getValue());
									return;
								}
								if (result.right().getValue().size() == 0) {
									badRequest(request, "no.match");
									return;
								}

								final String mail = result.right().getValue().getString("email", "");
								final String mobile = result.right().getValue().getString("mobile", "");

								if ("mail".equals(service)) {
									userAuthAccount.sendResetPasswordMail(request, mail, resetCode,
											DefaultResponseHandler.defaultResponseHandler(request));
								} else if ("mobile".equals(service) && smsProvider != null && !smsProvider.isEmpty()) {
									eventStore.createAndStoreEvent(AuthEvent.SMS.name(), login);
									userAuthAccount.sendResetPasswordSms(request, mobile, resetCode,
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
	@IgnoreCsrf
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

		userAuthAccount.generateResetCode(login, checkFederatedLogin, (Either<String, String> either) -> {
			if (either.isRight()) {
				renderJson(request, new JsonObject().put("renewalCode", either.right().getValue()));
			} else {
				renderError(request);
			}
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
							UserUtils.deletePermanentSession(eb, userId, null, new io.vertx.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (!event) {
										log.error("Error delete permanent session with userId : " + userId);
									}
								}
							});
							UserUtils.deleteCacheSession(eb, userId, new io.vertx.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (!event) {
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
								UserUtils.deletePermanentSession(eb, userId, null, new io.vertx.core.Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (!event) {
											log.error("Error delete permanent session with userId : " + userId);
										}
									}
								});
								UserUtils.deleteCacheSession(eb, userId, new io.vertx.core.Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (!event) {
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

	@Post("/reset")
	public void resetPasswordSubmit(final HttpServerRequest request) {
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
				final String forceChange = request.formAttributes().get("forceChange");
				if (login == null
						|| ((resetCode == null || resetCode.trim().isEmpty())
								&& (oldPassword == null || oldPassword.trim().isEmpty() || oldPassword.equals(password)))
						|| password == null || login.trim().isEmpty() || password.trim().isEmpty()
						|| !password.equals(confirmPassword) || !passwordPattern.matcher(password).matches()) {
					trace.info("Erreur lors de la réinitialisation " + "du mot de passe de l'utilisateur " + login);
					JsonObject error = new JsonObject().put("error", new JsonObject().put("message", I18n.getInstance()
							.translate("auth.reset.invalid.argument", getHost(request), I18n.acceptLanguage(request))));
					if (resetCode != null) {
						error.put("resetCode", resetCode);
					}
					renderJson(request, error);
				} else {
					final io.vertx.core.Handler<Boolean> resultHandler = new io.vertx.core.Handler<Boolean>() {

						@Override
						public void handle(Boolean reseted) {
							if (Boolean.TRUE.equals(reseted)) {
								trace.info("Réinitialisation réussie du mot de passe de l'utilisateur " + login);
								redirect(request, callback);
							} else {
								trace.info("Erreur lors de la réinitialisation " + "du mot de passe de l'utilisateur "
										+ login);
								error(request, resetCode);
							}
						}
					};
					if (resetCode != null && !resetCode.trim().isEmpty()) {
						userAuthAccount.resetPassword(login, resetCode, password, resultHandler);
					} else {
						DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
						data.getUserId(login, oldPassword, new Handler<String>() {

							@Override
							public void handle(String userId) {
								if (userId != null && !userId.trim().isEmpty()) {
									if ("force".equals(forceChange)) {
										userAuthAccount.changePassword(login, password, reseted -> {
											if (Boolean.TRUE.equals(reseted)) {
												trace.info("Changement forcé réussie du mot de passe de l'utilisateur " + login);
												UserUtils.deleteCacheSession(eb, userId,
														false, r -> redirect(request, callback));
											} else {
												trace.info("Erreur lors du changement forcé du mot de passe de l'utilisateur " + login);
												error(request, resetCode);
											}
										});
									} else {
										userAuthAccount.changePassword(login, password, resultHandler);
									}
								} else {
									error(request, null);
								}
							}
						});
					}
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

	@Delete("/sessions")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void deletePermanentSessions(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
					UserUtils.deletePermanentSession(eb, user.getUserId(), sessionId,
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

	public void setUserAuthAccount(UserAuthAccount userAuthAccount) {
		this.userAuthAccount = userAuthAccount;
	}

	public void setEventStore(EventStore eventStore) {
		this.eventStore = eventStore;
	}

	public void setAuthorizedHostsLogin(JsonArray authorizedHostsLogin) {
		this.authorizedHostsLogin = authorizedHostsLogin;
	}

}
