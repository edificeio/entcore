/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.auth.controllers;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.code;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidRequest;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidScope;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.serverError;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.unauthorizedClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
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

import org.entcore.auth.adapter.ResponseAdapterFactory;
import org.entcore.auth.adapter.UserInfoAdapter;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.auth.services.impl.DefaultOpendIdConnectService;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.security.ActionType;
import jp.eisbahn.oauth2.server.async.Handler;
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

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;
import org.vertx.java.platform.Container;
import org.entcore.auth.oauth.HttpServerRequestAdapter;
import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.oauth.OAuthDataHandlerFactory;
import org.entcore.auth.users.UserAuthAccount;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;

import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;

import fr.wseduc.security.SecuredAction;

public class AuthController extends BaseController {

	private DataHandlerFactory oauthDataFactory;
	private Token token;
	private ProtectedResource protectedResource;
	private UserAuthAccount userAuthAccount;
	private static final Tracer trace = TracerFactory.getTracer("auth");
	private EventStore eventStore;
	private Map<Object, Object> invalidEmails;
	private JsonArray authorizedHostsLogin;
	public enum AuthEvent { ACTIVATION, LOGIN, SMS }
	private Pattern passwordPattern;
	private String smsProvider;
	private boolean slo;
	private List<String> internalAddress;


	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		JsonObject oic = container.config().getObject("openid-connect");
		OpenIdConnectService openIdConnectService = (oic != null) ? new DefaultOpendIdConnectService(
				oic.getString("iss"), vertx, oic.getString("keys")) : null;
		oauthDataFactory = new OAuthDataHandlerFactory(Neo4j.getInstance(), MongoDb.getInstance(), openIdConnectService,
				container.config().getBoolean("check-federated-login", false));
		GrantHandlerProvider grantHandlerProvider = new DefaultGrantHandlerProvider();
		ClientCredentialFetcher clientCredentialFetcher = new ClientCredentialFetcherImpl();
		token = new Token();
		token.setDataHandlerFactory(oauthDataFactory);
		token.setGrantHandlerProvider(grantHandlerProvider);
		token.setClientCredentialFetcher(clientCredentialFetcher);
		AccessTokenFetcherProvider accessTokenFetcherProvider =
				new DefaultAccessTokenFetcherProvider();
		protectedResource = new ProtectedResource();
		protectedResource.setDataHandlerFactory(oauthDataFactory);
		protectedResource.setAccessTokenFetcherProvider(accessTokenFetcherProvider);
		passwordPattern = Pattern.compile(container.config().getString("passwordRegex", ".{8}.*"));
		ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		if(server != null && server.get("smsProvider") != null)
			smsProvider = (String) server.get("smsProvider");
		slo = container.config().getBoolean("slo", false);
		if (server != null) {
			Boolean cluster = (Boolean) server.get("cluster");
			if (Boolean.TRUE.equals(cluster)) {
				ClusterManager cm = ((VertxInternal) vertx).clusterManager();
				invalidEmails = cm.getSyncMap("invalidEmails");
			} else {
				invalidEmails = vertx.sharedData().getMap("invalidEmails");
			}
		} else {
			invalidEmails = new HashMap<>();
		}
		internalAddress = container.config().getArray("internalAddress", new JsonArray().add("localhost").add("127.0.0.1")).toList();
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
							UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {

								@Override
								public void handle(UserInfos user) {
									if (user != null && user.getUserId() != null) {
										((OAuthDataHandler) data).createOrUpdateAuthInfo(
												clientId, user.getUserId(), scope, redirectUri,
												new Handler<AuthInfo>() {

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
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
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
				context.putString("callBack", URLEncoder.encode(callBack, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (error != null && !error.trim().isEmpty()) {
			context.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
		}
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.putBoolean("notLoggedIn", user == null);
				renderView(request, context, "login.html", null);
			}
		});
	}

	@Get("/context")
	public void context(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		context.putString("callBack", container.config().getObject("authenticationServer").getString("loginCallback"));
		context.putBoolean("cgu", container.config().getBoolean("cgu", true));
		context.putString("passwordRegex", passwordPattern.toString());
		context.putObject("mandatory", container.config().getObject("mandatory", new JsonObject()));
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
				context.putString("callBack", URLEncoder.encode(callBack, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (error != null && !error.trim().isEmpty()) {
			context.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate(error, getHost(request), I18n.acceptLanguage(request))));
		}
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.putBoolean("notLoggedIn", user == null);
				renderView(request, context, "login.html", null);
			}
		});
	}

	@Get("/login")
	public void login(HttpServerRequest request) {
		final String host = getHost(request);
		if (authorizedHostsLogin != null && isNotEmpty(host) && !authorizedHostsLogin.contains(host)) {
			redirect(request, pathPrefix + "/openid/login");
		} else {
			viewLogin(request, null, request.params().get("callBack"));
		}
	}

	@Post("/login")
	public void loginSubmit(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			public void handle() {
				String c = request.formAttributes().get("callBack");
				final StringBuilder callBack = new StringBuilder();
				if (c != null && !c.trim().isEmpty()) {
					try {
						callBack.append(URLDecoder.decode(c,"UTF-8"));
					} catch (UnsupportedEncodingException ex) {
						log.error(ex.getMessage(), ex);
						callBack.append(container.config()
								.getObject("authenticationServer").getString("loginCallback"));
					}
				} else {
					callBack.append(container.config()
							.getObject("authenticationServer").getString("loginCallback"));
				}
				DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(request));
				final String login = request.formAttributes().get("email");
				final String password = request.formAttributes().get("password");
				data.getUserId(login, password, new Handler<String>() {

					@Override
					public void handle(final String userId) {
						final String c = callBack.toString();
						if (userId != null && !userId.trim().isEmpty()) {
							trace.info("Connexion de l'utilisateur " + login);
							userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
									new org.vertx.java.core.Handler<Boolean>() {
								public void handle(Boolean ok) {
									if(!ok){
										trace.error("[Auth](loginSubmit) Error while storing last known domain for user " + userId);
									}
								}
							});
							eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login);
							createSession(userId, request, c);
						} else {
							userAuthAccount.matchActivationCode(login, password, new org.vertx.java.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean passIsActivationCode) {
									if(passIsActivationCode){
										trace.info("Code d'activation entré pour l'utilisateur " + login);
										final JsonObject json = new JsonObject();
										json.putString("activationCode", password);
										json.putString("login", login);
										if (container.config().getBoolean("cgu", true)) {
											json.putBoolean("cgu", true);
										}
										UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
											@Override
											public void handle(UserInfos user) {
												json.putBoolean("notLoggedIn", user == null);
												renderView(request, json, "activation.html", null);
											}
										});

									} else {
										userAuthAccount.matchResetCode(login, password, new org.vertx.java.core.Handler<Boolean>() {
											@Override
											public void handle(Boolean passIsResetCode) {
												if(passIsResetCode){
													redirect(request, "/auth/reset/"+password+"?login="+login);
												} else {
													trace.info("Erreur de connexion pour l'utilisateur " + login);
													loginResult(request, "auth.error.authenticationFailed", c);
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

	private void createSession(String userId, final HttpServerRequest request, final String callBack) {
		UserUtils.createSession(eb, userId,
				new org.vertx.java.core.Handler<String>() {

					@Override
					public void handle(String sessionId) {
						if (sessionId != null && !sessionId.trim().isEmpty()) {
							boolean rememberMe = "true".equals(request.formAttributes().get("rememberMe"));
							long timeout = rememberMe ? 3600l * 24 * 365 : container.config()
									.getLong("cookie_timeout", Long.MIN_VALUE);
							CookieHelper.getInstance().setSigned("oneSessionId", sessionId, timeout, request);
							CookieHelper.set("authenticated", "true", timeout, request);
							redirect(request, callBack.matches("https?://[0-9a-zA-Z\\.\\-_]+/auth/login/?(\\?.*)?") ?
									callBack.replaceFirst("/auth/login", "") : callBack, "");
						} else {
							loginResult(request, "auth.error.authenticationFailed", callBack);
						}
					}
				});
	}

	@Get("/logout")
	public void logout(final HttpServerRequest request) {
		final String c = request.params().get("callback");
		if (slo) {
			UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
				@Override
				public void handle(UserInfos event) {
					if (event != null && Boolean.TRUE.equals(event.getFederated())) {
						if (container.config().containsField("openid-federate")) {
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
						logoutCallback(request, c1, container, eb);
					}
				}
			});
		} else {
			logoutCallback(request, c, container, eb);
		}
	}

	public static void logoutCallback(final HttpServerRequest request, String c, Container container, EventBus eb) {
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
				callback.append(container.config()
						.getObject("authenticationServer").getString("logoutCallback", "/"));
			}
		} else {
			callback.append(container.config()
					.getObject("authenticationServer").getString("logoutCallback", "/"));
		}

		if (sessionId != null && !sessionId.trim().isEmpty()) {
			UserUtils.deleteSession(eb, sessionId, new org.vertx.java.core.Handler<Boolean>() {

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
		UserUtils.getSession(eb, request, new org.vertx.java.core.Handler<JsonObject>() {

			@Override
			public void handle(JsonObject infos) {
				if (infos != null) {
					JsonObject info;
					UserInfoAdapter adapter = ResponseAdapterFactory.getUserInfoAdapter(request);
					if (request instanceof SecureHttpServerRequest) {
						SecureHttpServerRequest sr = (SecureHttpServerRequest) request;
						String clientId = sr.getAttribute("client_id");
						info = adapter.getInfo(infos, clientId);
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

	@Get("/internal/userinfo")
	@SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
	public void internalUserInfo(final HttpServerRequest request) {
		if (!(request instanceof SecureHttpServerRequest) || !internalAddress.contains(getIp(request))) {
			forbidden(request);
			return;
		}
		UserUtils.getSessionByUserId(eb, ((SecureHttpServerRequest)request).getAttribute("remote_user"), new org.vertx.java.core.Handler<JsonObject>() {

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
				new Handler<Try<OAuthError,ProtectedResource.Response>>() {

			@Override
			public void handle(Try<OAuthError, ProtectedResource.Response> resp) {
				ProtectedResource.Response response;
				try {
					response = resp.get();
					JsonObject r = new JsonObject()
					.putString("status", "ok")
					.putString("client_id", response.getClientId())
					.putString("remote_user", response.getRemoteUser())
					.putString("scope", response.getScope());
					message.reply(r);
				} catch (OAuthError e) {
					message.reply(new JsonObject().putString("error", e.getType()));
				}
			}
		});
	}

	@Get("/activation")
	public void activeAccount(final HttpServerRequest request) {
		final JsonObject json = new JsonObject();
		if (request.params().contains("activationCode")) {
			json.putString("activationCode", request.params().get("activationCode"));
		}
		if (request.params().contains("login")) {
			json.putString("login", request.params().get("login"));
		}
		if (container.config().getBoolean("cgu", true)) {
			json.putBoolean("cgu", true);
		}
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				json.putBoolean("notLoggedIn", user == null);
				renderView(request, json);
			}
		});
	}

	@Post("/activation")
	public void activeAccountSubmit(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String login = request.formAttributes().get("login");
				final String activationCode = request.formAttributes().get("activationCode");
				final String email = request.formAttributes().get("mail");
				final String phone = request.formAttributes().get("phone");
				String password = request.formAttributes().get("password");
				String confirmPassword = request.formAttributes().get("confirmPassword");
				if (container.config().getBoolean("cgu", true) &&
						!"true".equals(request.formAttributes().get("acceptCGU"))) {
					trace.info("Invalid cgu " + login);
					JsonObject error = new JsonObject()
							.putObject("error", new JsonObject()
							.putString("message", "invalid.cgu"))
							.putBoolean("cgu", true);
					if (activationCode != null) {
						error.putString("activationCode", activationCode);
					}
					if (login != null) {
						error.putString("login", login);
					}
					renderJson(request, error);
				} else if (
					login == null || activationCode == null|| password == null ||
					login.trim().isEmpty() || activationCode.trim().isEmpty() ||
					password.trim().isEmpty() || !password.equals(confirmPassword) ||
					!passwordPattern.matcher(password).matches() ||
					(container.config().getObject("mandatory", new JsonObject()).getBoolean("mail", false)
					  && (email == null || email.trim().isEmpty() || invalidEmails.containsKey(email))) ||
					(container.config().getObject("mandatory", new JsonObject()).getBoolean("phone", false)
					  && (phone == null || phone.trim().isEmpty())) ||
					(email != null && !email.trim().isEmpty() && !StringValidation.isEmail(email)) ||
					(phone != null && !phone.trim().isEmpty() && !StringValidation.isPhone(phone))
				) {
					trace.info("Echec de l'activation du compte utilisateur " + login);
					JsonObject error = new JsonObject()
					.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate("auth.activation.invalid.argument", getHost(request), I18n.acceptLanguage(request))));
					if (activationCode != null) {
						error.putString("activationCode", activationCode);
					}
					if (login != null) {
						error.putString("login", login);
					}
					if (container.config().getBoolean("cgu", true)) {
						error.putBoolean("cgu", true);
					}
					renderJson(request, error);
				} else {
					userAuthAccount.activateAccount(login, activationCode, password, email, phone, request,
							new org.vertx.java.core.Handler<Either<String, String>>() {

						@Override
						public void handle(Either<String, String> activated) {
							if (activated.isRight() && activated.right().getValue() != null) {
								final String userId = activated.right().getValue();
								trace.info("Activation du compte utilisateur " + login);
								eventStore.createAndStoreEvent(AuthEvent.ACTIVATION.name(), login);
								if (container.config().getBoolean("activationAutoLogin", false)) {
									trace.info("Connexion de l'utilisateur " + login);
									userAuthAccount.storeDomain(userId, Renders.getHost(request), Renders.getScheme(request),
											new org.vertx.java.core.Handler<Boolean>() {
										public void handle(Boolean ok) {
											if(!ok){
												trace.error("[Auth](loginSubmit) Error while storing last known domain for user " + userId);
											}
										}
									});
									eventStore.createAndStoreEvent(AuthEvent.LOGIN.name(), login);
									createSession(userId, request,
											getScheme(request) + "://" + getHost(request));
								} else {
									redirect(request, "/auth/login");
								}
							} else {
								trace.info("Echec de l'activation : compte utilisateur " + login +
										" introuvable ou déjà activé.");
								JsonObject error = new JsonObject()
								.putObject("error", new JsonObject()
								.putString("message", I18n.getInstance().translate("activation.error", getHost(request), I18n.acceptLanguage(request))));
								error.putString("activationCode", activationCode);
								renderJson(request, error);
							}
						}
					});
				}
			}
		});
	}

	@Get("/forgot")
	public void forgotPassword(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.putBoolean("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	@Get("/upgrade")
	public void upgrade(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.putBoolean("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	@Post("/forgot-id")
	public void forgetId(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, new org.vertx.java.core.Handler<JsonObject>() {
			public void handle(JsonObject data) {
				final String mail = data.getString("mail");
				final String service = data.getString("service");
				if(mail == null || mail.trim().isEmpty()){
					badRequest(request);
					return;
				}

				userAuthAccount.findByMail(mail, new org.vertx.java.core.Handler<Either<String,JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> result) {
						//No user with that email, or more than one found.
						if(result.isLeft()){
							badRequest(request, result.left().getValue());
							return;
						}
						if(result.right().getValue().size() == 0){
							badRequest(request, "no.match");
							return;
						}

						final String id = result.right().getValue().getString("login", "");
						final String mobile = result.right().getValue().getString("mobile", "");

						//Force mail
						if("mail".equals(service)){
							userAuthAccount.sendForgottenIdMail(request, id, mail, new org.vertx.java.core.Handler<Either<String,JsonObject>>() {
								public void handle(Either<String, JsonObject> event) {
									if(event.isLeft()){
										badRequest(request, event.left().getValue());
										return;
									}
									if(smsProvider != null && !smsProvider.isEmpty()){
										final String obfuscatedMobile = StringValidation.obfuscateMobile(mobile);
										renderJson(request, new JsonObject().putString("mobile", obfuscatedMobile));
									} else {
										renderJson(request, new JsonObject());
									}
								}
							});
						} else if("mobile".equals(service) && !mobile.isEmpty() && smsProvider != null && !smsProvider.isEmpty()){
							eventStore.createAndStoreEvent(AuthEvent.SMS.name(), id);
							userAuthAccount.sendForgottenIdSms(request, id, mobile, DefaultResponseHandler.defaultResponseHandler(request));
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
		userAuthAccount.findByLogin(request.params().get("login"), null, new org.vertx.java.core.Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> result) {
				if(result.isLeft()){
					badRequest(request, result.left().getValue());
					return;
				}
				if(result.right().getValue().size() == 0){
					badRequest(request, "no.match");
					return;
				}

				final String mail = result.right().getValue().getString("email", "");
				final String mobile = result.right().getValue().getString("mobile", "");

				boolean mailCheck = mail != null && !mail.trim().isEmpty();
				boolean mobileCheck = mobile != null && !mobile.trim().isEmpty();

				if(!mailCheck && !mobileCheck){
					badRequest(request, "no.match");
					return;
				}

				final String obfuscatedMail = StringValidation.obfuscateMail(mail);
				final String obfuscatedMobile = StringValidation.obfuscateMobile(mobile);

				if(smsProvider != null && !smsProvider.isEmpty())
					renderJson(request, new JsonObject().putString("mobile", obfuscatedMobile).putString("mail", obfuscatedMail));
				else
					renderJson(request, new JsonObject().putString("mail", obfuscatedMail));
			}
		});
	}

	@Post("/forgot-password")
	public void forgotPasswordSubmit(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new org.vertx.java.core.Handler<JsonObject>() {
			public void handle(JsonObject data) {
				final String login = data.getString("login");
				final String service = data.getString("service");
				final String resetCode = StringValidation.generateRandomCode(8);
				if(login == null || login.trim().isEmpty() || service == null || service.trim().isEmpty()){
					badRequest(request, "invalid.login");
					return;
				}

				userAuthAccount.findByLogin(login, resetCode, new org.vertx.java.core.Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> result) {
						if(result.isLeft()){
							badRequest(request, result.left().getValue());
							return;
						}
						if(result.right().getValue().size() == 0){
							badRequest(request, "no.match");
							return;
						}

						final String mail = result.right().getValue().getString("email", "");
						final String mobile = result.right().getValue().getString("mobile", "");

						if("mail".equals(service)){
							userAuthAccount.sendResetPasswordMail(request, mail, resetCode,
									DefaultResponseHandler.defaultResponseHandler(request));
						} else if("mobile".equals(service) && smsProvider != null && !smsProvider.isEmpty()){
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
	@SecuredAction( value = "", type = ActionType.RESOURCE)
	@IgnoreCsrf
	public void sendResetPassword(final HttpServerRequest request) {
		String login = request.formAttributes().get("login");
		String email = request.formAttributes().get("email");
		if (login == null || login.trim().isEmpty() || !StringValidation.isEmail(email)) {
			badRequest(request);
			return;
		}
		userAuthAccount.sendResetCode(request, login, email, new org.vertx.java.core.Handler<Boolean>() {
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

	@Put("/block/:userId")
	@SecuredAction( value = "", type = ActionType.RESOURCE)
	public void blockUser(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new org.vertx.java.core.Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				final String userId = request.params().get("userId");
				boolean block = json.getBoolean("block", true);
				userAuthAccount.blockUser(userId, block, new org.vertx.java.core.Handler<Boolean>() {
					@Override
					public void handle(Boolean r) {
						if (Boolean.TRUE.equals(r)) {
							request.response().end();
							UserUtils.deletePermanentSession(eb, userId, null, new org.vertx.java.core.Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (!event) {
										log.error("Error delete permanent session with userId : " + userId);
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

	@Get("/reset/:resetCode")
	public void resetPassword(final HttpServerRequest request) {
		resetPasswordView(request, null);
	}

	private void resetPasswordView(final HttpServerRequest request, final JsonObject p) {
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				JsonObject params = p;
				if (params == null) {
					params = new JsonObject();
				}
				if (user != null && "password".equals(request.params().get("resetCode"))) {
					renderView(request, params
					.putString("login", user.getLogin())
					.putString("callback", "/userbook/mon-compte"),
					"changePassword.html", null);
				} else {
					renderView(request, params
					.putBoolean("notLoggedIn", user == null)
					.putString("login", request.params().get("login"))
					.putString("resetCode", request.params().get("resetCode")), "reset.html", null);
				}
			}
		});
	}

	@Post("/reset")
	public void resetPasswordSubmit(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String login = request.formAttributes().get("login");
				final String resetCode = request.formAttributes().get("resetCode");
				final String oldPassword = request.formAttributes().get("oldPassword");
				final String password = request.formAttributes().get("password");
				String confirmPassword = request.formAttributes().get("confirmPassword");
				final String callback = Utils.getOrElse(
						request.formAttributes().get("callback"), "/auth/login", false);
				if (login == null || ((resetCode == null || resetCode.trim().isEmpty()) &&
						(oldPassword == null || oldPassword.trim().isEmpty())) ||
						password == null || login.trim().isEmpty() ||
						password.trim().isEmpty() || !password.equals(confirmPassword) ||
						!passwordPattern.matcher(password).matches()) {
					trace.info("Erreur lors de la réinitialisation "
							+ "du mot de passe de l'utilisateur " + login);
					JsonObject error = new JsonObject()
					.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate("auth.reset.invalid.argument", getHost(request), I18n.acceptLanguage(request))));
					if (resetCode != null) {
						error.putString("resetCode", resetCode);
					}
					renderJson(request, error);
				} else {
					final org.vertx.java.core.Handler<Boolean> resultHandler =
							new org.vertx.java.core.Handler<Boolean>() {

						@Override
						public void handle(Boolean reseted) {
							if (Boolean.TRUE.equals(reseted)) {
                trace.info("Réinitialisation réussie du mot de passe de l'utilisateur " + login);
								redirect(request, callback);
							} else {
                trace.info("Erreur lors de la réinitialisation "
                    + "du mot de passe de l'utilisateur " + login);
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
									userAuthAccount.changePassword(login, password, resultHandler);
								} else {
									error(request, null);
								}
							}
						});
					}
				}
			}

			private void error(final HttpServerRequest request,
					final String resetCode) {
				JsonObject error = new JsonObject()
				.putObject("error", new JsonObject()
				.putString("message", I18n.getInstance().translate("reset.error", getHost(request), I18n.acceptLanguage(request))));
				if (resetCode != null) {
					error.putString("resetCode", resetCode);
				}
				renderJson(request, error);
			}
		});
	}

	@Get("/cgu")
	public void cgu(final HttpServerRequest request) {
		final JsonObject context = new JsonObject();
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				context.putBoolean("notLoggedIn", user == null);
				renderView(request, context);
			}
		});
	}

	@Delete("/sessions")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void deletePermanentSessions(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new org.vertx.java.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
					UserUtils.deletePermanentSession(eb, user.getUserId(), sessionId,
							new org.vertx.java.core.Handler<Boolean>() {
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
