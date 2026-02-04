package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.auth.services.CarbonioPreauthService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.List;
import java.util.Map;

public class CarbonioPreauthController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(CarbonioPreauthController.class);
	private static final String CARBONIO_AUTH_COOKIE_NAME = "ZM_AUTH_TOKEN";

	CarbonioPreauthService carbonioPreauthService;
	String carbonioBaseUrl;
	String carbonioRedirectUrl;
	String carbonioDomainKey;
	HttpClient httpClient;

	/**
	 * Initializes the Carbonio pre-authentication controller.
	 * Reads configuration values for Carbonio base URL, redirect URL, and domain key.
	 * Creates the Carbonio pre-auth service and HTTP client for making requests.
	 * 
	 * @param vertx The Vert.x instance for async operations
	 * @param config The configuration object containing Carbonio settings
	 * @param rm The route matcher for registering HTTP routes
	 * @param securedActions Map of secured actions for authorization
	 */
	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		carbonioBaseUrl = config.getString("carbonio-base-url");
		carbonioRedirectUrl = config.getString("carbonio-redirect-url");
		carbonioDomainKey = config.getString("carbonio-domain-key");

		carbonioPreauthService = new CarbonioPreauthService(carbonioRedirectUrl, carbonioDomainKey);

		httpClient = vertx.createHttpClient(new HttpClientOptions());
	}

	/**
	 * Handles Carbonio pre-authentication for web clients.
	 * Generates a pre-authentication URL and redirects the user to Carbonio.
	 * The user session must be active to perform this operation.
	 * 
	 * @param request The HTTP request containing the user session
	 * @return HTTP 302 redirect to Carbonio pre-auth URL on success,
	 *         HTTP 401 if user not found in session,
	 *         HTTP 400 if pre-auth URL generation fails
	 */
	@Get("/carbonio/preauth")
	@SecuredAction("carbonio.preauth")
	public void preauth(HttpServerRequest request) {
		this.getPreauthUrl(request, ar -> {
			if (ar.succeeded()) {
				//Redirect to Carbonio preauth URL
				request.response().setStatusCode(302);
				request.response().putHeader("Location", ar.result());
				request.response().end();
			} else {
				// Error already handled and response sent in getPreauthUrl
				log.debug("Preauth URL generation failed: " + ar.cause().getMessage());
				request.response().setStatusCode(400);
				request.response().end();
			}
		});
	}

	/**
	 * Handles Carbonio pre-authentication for mobile clients.
	 * Generates a pre-authentication URL, fetches the authentication token from Carbonio,
	 * and returns it to the mobile client for direct use.
	 * The user session must be active to perform this operation.
	 * 
	 * @param request The HTTP request containing the user session
	 * @return HTTP 200 with the ZM_AUTH_TOKEN cookie value on success,
	 *         HTTP 401 if user not found in session,
	 *         HTTP 400 if pre-auth URL generation or token fetch fails
	 */
	@Get("/carbonio/token")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getToken(HttpServerRequest request) {
		this.getPreauthUrl(request, ar -> {
			if (ar.succeeded()) {
				// Fetch token from preauth URL
				fetchTokenFromUrl(ar.result())
					.onSuccess(token -> {				
						request.response().setStatusCode(200);
						request.response().end(token);
					})
					.onFailure(err -> {
						log.error("Failed to fetch Carbonio token: " + err);
						badRequest(request, "Failed to fetch Carbonio token: " + err);
					});
			} else {
				// Error already handled and response sent in getPreauthUrl
				log.debug("Preauth URL generation failed for token endpoint: " + ar.cause().getMessage());
				request.response().setStatusCode(400);
				request.response().end();
			}
		});
	}

	/**
	 * Generates the Carbonio pre-authentication URL for the authenticated user.
	 * Retrieves user information from the session, constructs the user alias,
	 * and delegates to the CarbonioPreauthService to generate the signed URL.
	 * 
	 * @param request The HTTP request containing the user session
	 * @param handler The handler called with the pre-auth URL on success,
	 *                or a failed result with an error message
	 *                Returns 401 if user not found in session
	 *                Returns 400 if URL generation fails
	 */
	private void getPreauthUrl(HttpServerRequest request, Handler<AsyncResult<String>> handler) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos == null) {
				log.warn("Carbonio preauth failed: user not found in session");
				unauthorized(request, "User not found");
				handler.handle(Future.failedFuture("User not found"));
			} else {
				final String userAlias = getUserAlias(userInfos);

				Either<String, String> result = carbonioPreauthService.generatePreauthUrl(userAlias);
				if (result.isLeft()) {
					log.error("Failed to generate Carbonio preauth URL for user " + userAlias + ": " + result.left().getValue());
					badRequest(request, result.left().getValue());
					handler.handle(Future.failedFuture(result.left().getValue()));
					return;
				}

				String carbonioPreAuthUrl = result.right().getValue();
				log.info("Carbonio preauth redirect for user: " + userInfos.getUserId());
				handler.handle(Future.succeededFuture(carbonioPreAuthUrl));
			}
		});
	}

	/**
	 * Constructs a Carbonio user alias from user information.
	 * The alias is formatted as userId@domain, where the domain is extracted
	 * from the configured Carbonio base URL.
	 * 
	 * @param userInfos The user information containing the user ID
	 * @return The user alias in the format userId@domain (e.g., "user123@mail.carbonio.com")
	 */
	private String getUserAlias(UserInfos userInfos) {
		final String domain = carbonioBaseUrl
				.replaceFirst("https?://", "")
				.replaceFirst("/.*$", "");

		return String.format("%s@%s", userInfos.getUserId(), domain);
	}

	/**
	 * Fetches the Carbonio authentication token from the pre-auth URL.
	 * Performs an HTTP GET request to the Carbonio pre-auth URL and extracts
	 * the ZM_AUTH_TOKEN from the Set-Cookie response headers.
	 * Expects a 302 redirect response with cookies.
	 * 
	 * @param url The Carbonio pre-authentication URL to query
	 * @return A Future containing the extracted auth token value on success,
	 *         or a failed Future if no token found or request fails
	 */
	private Future<String> fetchTokenFromUrl(String url) {
		Promise <String> promise = Promise.promise();
		httpClient.request(new RequestOptions().setAbsoluteURI(url)
			.setMethod(HttpMethod.GET))
			.flatMap(HttpClientRequest::send)
			.onSuccess(response -> {
				if(response.statusCode() == 302) {
					// Retrieve cookies
					List<String> cookies = response.headers().getAll("Set-Cookie");
					if (cookies != null && !cookies.isEmpty()) {
						for(String cookie : cookies) {
							if(cookie.contains(CARBONIO_AUTH_COOKIE_NAME)) {
								log.debug("Found " + CARBONIO_AUTH_COOKIE_NAME + " cookie in Carbonio response");
								// Retrieve Carbonio auth token from cookie
								String token = cookie.split(";")[0].split("=")[1];
								promise.complete(token);
								return;
							}
						}
						log.warn("Carbonio preauth : no " + CARBONIO_AUTH_COOKIE_NAME + " found in response");
						promise.fail("No " + CARBONIO_AUTH_COOKIE_NAME + " found in Carbonio response");
					} else {
						log.warn("Carbonio preauth : no cookies found in response");
						promise.fail("No cookies found in Carbonio response");
					}
				} else {
					log.warn("Carbonio preauth : unexpected response status " + response.statusCode() + " for URL: " + url);
					promise.fail("Unexpected response status: " + response.statusCode());
				}
		})
		.onFailure(err -> {
			log.error("Carbonio preauth failed to create HTTP request to URL: " + url, err);
			promise.fail("Request creation failed: " + err.getMessage());
		});
		return promise.future();
	}
}
