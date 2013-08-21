package edu.one.core.infra.http.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import edu.one.core.infra.http.Renders;

public class OAuth2Client {

	private final URI uri;
	private final String clientId;
	private final String secret;
	private final String authorizeUrn;
	private final String tokenUrn;
	private final String redirectUri;
	private final HttpClient httpClient;

	public OAuth2Client(URI uri, String clientId, String secret, String authorizeUrn,
			String tokenUrn, String redirectUri, Vertx vertx, int poolSize) {
		this.httpClient = vertx.createHttpClient()
				.setHost(uri.getHost())
				.setPort(uri.getPort())
				.setMaxPoolSize(poolSize)
				.setKeepAlive(false);
		this.uri = uri;
		this.clientId = clientId;
		this.secret = secret;
		this.authorizeUrn = authorizeUrn;
		this.tokenUrn = tokenUrn;
		this.redirectUri = redirectUri;
	}

	public OAuth2Client(URI uri, String clientId, String secret, String redirectUri, Vertx vertx) {
		this(uri, clientId, secret, "/oauth2/auth", "/oauth2/token", redirectUri, vertx, 16);
	}

	public void authorizeRedirect(HttpServerRequest request, String state, String scope) {
		String rUri = "";
		try {
			rUri = URLEncoder.encode(redirectUri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String params = "response_type=code&client_id=" + clientId + "&state=" + state +
				"&redirect_uri=" + rUri + "&scope=" + scope;

		Renders.redirect(request, uri.toString(), authorizeUrn + "?" + params);
	}

	public void authorizationCodeToken(HttpServerRequest request, String state,
			Handler<JsonObject> handler) {
		String s = request.params().get("state");
		String code = request.params().get("code");
		String error = request.params().get("error");
		JsonObject r = new JsonObject();
		if (state != null && !state.equals(s)) {
			handler.handle(r.putString("error", "invalid_state"));
			return;
		}
		if (error != null) {
			handler.handle(r.putString("error", error));
			return;
		}
		try {
			getAccessToken(code, handler);
		} catch (UnsupportedEncodingException e) {
			handler.handle(r.putString("error", e.getMessage()));
		}
	}

	public void getAccessToken(String code,
			final Handler<JsonObject> handler) throws UnsupportedEncodingException {
		HttpClientRequest req = httpClient.post(tokenUrn, new Handler<HttpClientResponse>() {

			@Override
			public void handle(final HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer r) {
						JsonObject j = new JsonObject(r.toString("UTF-8"));
						if (response.statusCode() == 200) {
							JsonObject json = new JsonObject()
							.putString("status", "ok")
							.putObject("token", j);
							handler.handle(json);
						} else {
							handler.handle(j.putNumber("statusCode", response.statusCode()));
						}
					}
				});
			}
		});
		req.headers()
			.add("Authorization", "Basic " + Base64.encodeBytes(
					(clientId + ":" + secret).getBytes("UTF-8")))
			.add("Content-Type", "application/x-www-form-urlencoded")
			.add("Accept", "application/json; charset=UTF-8");
		String body = "grant_type=authorization_code&code=" + code +
				"&redirect_uri=" + redirectUri;
		req.end(body, "UTF-8");
	}

	public void getProtectedResource(String path, String accessToken,
			Handler<HttpClientResponse> handler) {
		getProtectedResource(path, accessToken, "application/json; charset=UTF-8", handler);
	}

	public void getProtectedResource(String path, String accessToken, String acceptMimeType,
			Handler<HttpClientResponse> handler) {
		HttpClientRequest req = httpClient.get(path, handler);
		req.headers()
		.add("Authorization", "Bearer " + accessToken)
		.add("Accept", acceptMimeType);
		req.end();
	}

}
