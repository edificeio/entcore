/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.auth;

import static org.entcore.auth.oauth.OAuthAuthorizationResponse.code;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidRequest;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.invalidScope;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.serverError;
import static org.entcore.auth.oauth.OAuthAuthorizationResponse.unauthorizedClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.auth.adapter.ResponseAdapterFactory;
import org.entcore.auth.adapter.UserInfoAdapter;
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
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import org.entcore.auth.oauth.HttpServerRequestAdapter;
import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.oauth.OAuthDataHandlerFactory;
import org.entcore.auth.users.DefaultUserAuthAccount;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.SecuredAction;

public class AuthController extends Controller {

	private final DataHandlerFactory oauthDataFactory;
	private final Token token;
	private final ProtectedResource protectedResource;
	private final UserAuthAccount userAuthAccount;
	private final TracerHelper trace;
	private static final String USERINFO_SCOPE = "userinfo";

	public AuthController(Vertx vertx, Container container, RouteMatcher rm, TracerHelper trace,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		Neo neo = new Neo(eb, log);
		this.trace = trace;
		this.oauthDataFactory = new OAuthDataHandlerFactory(
				neo,
				new MongoDb(eb, container.config()
						.getString("mongo.address", "wse.mongodb.persistor")));
		GrantHandlerProvider grantHandlerProvider = new DefaultGrantHandlerProvider();
		ClientCredentialFetcher clientCredentialFetcher = new ClientCredentialFetcherImpl();
		this.token = new Token();
		this.token.setDataHandlerFactory(oauthDataFactory);
		this.token.setGrantHandlerProvider(grantHandlerProvider);
		this.token.setClientCredentialFetcher(clientCredentialFetcher);
		AccessTokenFetcherProvider accessTokenFetcherProvider =
				new DefaultAccessTokenFetcherProvider();
		this.protectedResource = new ProtectedResource();
		this.protectedResource.setDataHandlerFactory(oauthDataFactory);
		this.protectedResource.setAccessTokenFetcherProvider(accessTokenFetcherProvider);
		this.userAuthAccount = new DefaultUserAuthAccount(vertx, container);
	}

	public void authorize(final HttpServerRequest request) {
		final String responseType = request.params().get("response_type");
		final String clientId = request.params().get("client_id");
		final String redirectUri = request.params().get("redirect_uri");
		final String scope = request.params().get("scope");
		final String state = request.params().get("state");
		if ("code".equals(responseType) && clientId != null && !clientId.trim().isEmpty()) {
			if (USERINFO_SCOPE.equals(scope)) {
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

	private void viewLogin(HttpServerRequest request, String error, String callBack) {
		JsonObject context = new JsonObject();
		if (callBack != null && !callBack.trim().isEmpty()) {
			try {
				context.putString("callBack", URLEncoder.encode(callBack, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (error != null && !error.trim().isEmpty()) {
			context.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate(error, request.headers().get("Accept-Language"))));
		}
		renderView(request, context, "login.html", null);
	}

	public void login(HttpServerRequest request) {
		viewLogin(request, null, request.params().get("callBack"));
	}

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
				String password = request.formAttributes().get("password");
				data.getUserId(login, password, new Handler<String>() {

					@Override
					public void handle(String userId) {
						String c = callBack.toString();
						if (userId != null && !userId.trim().isEmpty()) {
							trace.info("Connexion de l'utilisateur " + login);
							createSession(userId, request, c);
						} else {
							trace.info("Erreur de connexion pour l'utilisateur " + login);
							viewLogin(request, "auth.error.authenticationFailed", c);
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
							CookieHelper.getInstance().setSigned("oneSessionId", sessionId,
									container.config().getLong("cookie_timeout", 1800L),
									request.response());
							redirect(request, callBack, "");
						} else {
							viewLogin(request, "auth.error.authenticationFailed", callBack);
						}
					}
				});
	}

	public void logout(final HttpServerRequest request) {
		String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		String c = request.params().get("callback");
		final StringBuilder callback = new StringBuilder();
		if (c != null && !c.trim().isEmpty()) {
			try {
				callback.append(URLDecoder.decode(c, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
				callback.append(container.config()
						.getObject("authenticationServer").getString("logoutCallback", "/auth/login"));
			}
		} else {
			callback.append(container.config()
					.getObject("authenticationServer").getString("logoutCallback", "/auth/login"));
		}

		if (sessionId != null && !sessionId.trim().isEmpty()) {
			UserUtils.deleteSession(eb, sessionId, new org.vertx.java.core.Handler<Boolean>() {

				@Override
				public void handle(Boolean deleted) {
					if (Boolean.TRUE.equals(deleted)) {
						CookieHelper.set("oneSessionId", "", 0l, request.response());
					}
					redirect(request, callback.toString(), "");
				}
			});
		} else {
			redirect(request, callback.toString(), "");
		}
	}

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

	public void activeAccount(HttpServerRequest request) {
		JsonObject json = new JsonObject();
		if (request.params().contains("activationCode")) {
			json.putString("activationCode", request.params().get("activationCode"));
		}
		if (request.params().contains("login")) {
			json.putString("login", request.params().get("login"));
		}
		if (container.config().getBoolean("cgu", true)) {
			json.putBoolean("cgu", true);
		}
		renderView(request, json);
	}

	public void activeAccountSubmit(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String login = request.formAttributes().get("login");
				final String activationCode = request.formAttributes().get("activationCode");
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
					renderView(request, error);
				} else if (login == null || activationCode == null|| password == null ||
						login.trim().isEmpty() || activationCode.trim().isEmpty() ||
						password.trim().isEmpty() || !password.equals(confirmPassword)) {
					trace.info("Echec de l'activation du compte utilisateur " + login);
					JsonObject error = new JsonObject()
					.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate("auth.activation.invalid.argument", request.headers().get("Accept-Language"))));
					if (activationCode != null) {
						error.putString("activationCode", activationCode);
					}
					if (login != null) {
						error.putString("login", login);
					}
					if (container.config().getBoolean("cgu", true)) {
						error.putBoolean("cgu", true);
					}
					renderView(request, error);
				} else {
					userAuthAccount.activateAccount(login, activationCode, password,
							new org.vertx.java.core.Handler<Either<String, String>>() {

						@Override
						public void handle(Either<String, String> activated) {
							if (activated.isRight() && activated.right().getValue() != null) {
								trace.info("Activation du compte utilisateur " + login);
								if (container.config().getBoolean("activationAutoLogin", false)) {
									createSession(activated.right().getValue(), request,
											container.config().getString("host"));
								} else {
									redirect(request, "/auth/login");
								}
							} else {
								trace.info("Echec de l'activation : compte utilisateur " + login +
										" introuvable ou déjà activé.");
								JsonObject error = new JsonObject()
								.putObject("error", new JsonObject()
								.putString("message", I18n.getInstance().translate("activation.error", request.headers().get("Accept-Language"))));
								error.putString("activationCode", activationCode);
								renderView(request, error);
							}
						}
					});
				}
			}
		});
	}

	public void forgotPassword(HttpServerRequest request) {
		renderView(request);
	}

	public void forgotPasswordSubmit(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				String login = request.formAttributes().get("login");
				final I18n i18n = I18n.getInstance();
				final String language = request.headers().get("Accept-Language");
				if (login != null && !login.trim().isEmpty()) {
					userAuthAccount.forgotPassword(request, login,
							new org.vertx.java.core.Handler<Boolean>() {

						@Override
						public void handle(Boolean sent) {
							if (Boolean.TRUE.equals(sent)) {
								String message = i18n.translate("auth.resetCodeSent", language);
								renderView(request, new JsonObject()
								.putString("message", message), "message.html", null);
							} else {
								error(request, i18n, language);
							}
						}
					});
				} else {
					error(request, i18n, language);
				}
			}

			private void error(final HttpServerRequest request,
					final I18n i18n, final String language) {
				String message = i18n.translate("forgot.error", language);
				JsonObject error = new JsonObject()
				.putObject("error", new JsonObject()
				.putString("message", message));
				renderView(request, error);
			}
		});
	}

	@SecuredAction( value = "auth.send.reset.password", type = ActionType.RESOURCE)
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

	@SecuredAction( value = "auth.block.user", type = ActionType.RESOURCE)
	public void blockUser(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new org.vertx.java.core.Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				String userId = request.params().get("userId");
				boolean block = json.getBoolean("block", true);
				userAuthAccount.blockUser(userId, block, new org.vertx.java.core.Handler<Boolean>() {
					@Override
					public void handle(Boolean r) {
						if (Boolean.TRUE.equals(r)) {
							request.response().end();
						} else {
							badRequest(request);
						}
					}
				});
			}
		});
	}

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
					.putString("resetCode", request.params().get("resetCode")), "reset.html", null);
				}
			}
		});
	}

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
						password.trim().isEmpty() || !password.equals(confirmPassword)) {
					trace.info("Erreur lors de la réinitialisation "
							+ "du mot de passe de l'utilisateur " + login);
					JsonObject error = new JsonObject()
					.putObject("error", new JsonObject()
					.putString("message", I18n.getInstance().translate("auth.reset.invalid.argument", request.headers().get("Accept-Language"))));
					if (resetCode != null) {
						error.putString("resetCode", resetCode);
					}
					resetPasswordView(request, error);
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
				.putString("message", I18n.getInstance().translate("reset.error", request.headers().get("Accept-Language"))));
				if (resetCode != null) {
					error.putString("resetCode", resetCode);
				}
				resetPasswordView(request, error);
			}
		});
	}

	public void cgu(final HttpServerRequest request) {
		renderView(request);
	}

}
