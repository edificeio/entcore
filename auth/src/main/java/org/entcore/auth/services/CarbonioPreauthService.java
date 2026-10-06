package org.entcore.auth.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CarbonioPreauthService {
	private static final Logger log = LoggerFactory.getLogger(CarbonioPreauthService.class);

	private static final String HMAC_ALGORITHM = "HmacSHA1";
	/** Path of the communication API endpoint returning the unread Inbox count. */
	private static final String UNREAD_PATH = "/unread/carbonio";

	private final String carbonioRedirectUrl;
	private final String carbonioDomainKey;
	private final HttpClient httpClient;
	private final String unreadUrl;
	private final String unreadAuthorizationHeader;
	private final long unreadTimeout;

	/**
	 * @param communicationUrl Base URL of the communication API, null or empty to disable the unread count.
	 */
	public CarbonioPreauthService(String carbonioRedirectUrl, String carbonioDomainKey, HttpClient httpClient,
								  String communicationUrl, String communicationUsername, String communicationPassword,
								  long unreadTimeout) {
		this.carbonioRedirectUrl = carbonioRedirectUrl;
		this.carbonioDomainKey = carbonioDomainKey;
		this.httpClient = httpClient;
		this.unreadTimeout = unreadTimeout;
		if (communicationUrl != null && !communicationUrl.isEmpty()) {
			this.unreadUrl = communicationUrl.replaceFirst("/+$", "") + UNREAD_PATH;
			this.unreadAuthorizationHeader = "Basic " + Base64.getEncoder()
					.encodeToString((communicationUsername + ":" + communicationPassword).getBytes(StandardCharsets.UTF_8));
		} else {
			log.warn("Carbonio unread count disabled: communication API URL is not configured");
			this.unreadUrl = null;
			this.unreadAuthorizationHeader = null;
		}
	}

	public Either<String, String> generatePreauthUrl(String account, String redirectUrl) {
		long timestamp = System.currentTimeMillis();

		Map<String, String> params = new HashMap<>();
		params.put("account", account);
		params.put("by", "name");
		params.put("timestamp", String.valueOf(timestamp));
		params.put("expires", "0");

		Either<String, String> preauthResult = computePreAuth(params);
		return preauthResult.isRight()
				? new Either.Right<>(buildUrl(params, preauthResult.right().getValue(), redirectUrl))
				: new Either.Left<>(preauthResult.left().getValue());
	}

	private Either<String, String> computePreAuth(Map<String, String> params) {
		try {
			String preAuthString = new TreeSet<>(params.keySet()).stream()
					.map(params::get)
					.collect(Collectors.joining("|"));

			String hmac = calculateHmac(preAuthString, carbonioDomainKey);
			return new Either.Right<>(hmac);
		} catch (Exception e) {
			log.error("Failed to compute preauth HMAC", e);
			return new Either.Left<>("Failed to compute preauth: " + e.getMessage());
		}
	}

	private String calculateHmac(String data, String key) throws Exception {
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(secretKeySpec);
		byte[] rawHmac = mac.doFinal(data.getBytes());
		return bytesToHex(rawHmac);
	}

	private String bytesToHex(byte[] bytes) {
		return IntStream.range(0, bytes.length)
				.mapToObj(i -> String.format("%02x", bytes[i]))
				.collect(Collectors.joining());
	}

	private String buildUrl(Map<String, String> params, String computedPreAuth, String redirectUrl) {
		if (redirectUrl != null && !redirectUrl.isEmpty()) {
			
			String encodedRedirectUrl = "";
			try {
				encodedRedirectUrl = URLEncoder.encode(redirectUrl, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("Error encoding redirect URL Carbonio Preauth Redirect URL: " + redirectUrl, e);
				encodedRedirectUrl = redirectUrl;
			}

			return String.format("%s/service/preauth?account=%s&by=%s&timestamp=%s&expires=%s&preauth=%s&redirectURL=%s",
					carbonioRedirectUrl,
					params.get("account"),
					params.get("by"),
					params.get("timestamp"),
					params.get("expires"),
					computedPreAuth,
					encodedRedirectUrl);
		}
		return String.format("%s/service/preauth?account=%s&by=%s&timestamp=%s&expires=%s&preauth=%s",
				carbonioRedirectUrl,
				params.get("account"),
				params.get("by"),
				params.get("timestamp"),
				params.get("expires"),
				computedPreAuth
		);
	}

	/**
	 * Returns the unread Inbox message count of the Carbonio account linked to an ENT user.
	 * A user without a Carbonio account (HTTP 404), or a disabled unread count, gives 0.
	 *
	 * @param userId The ENT user id.
	 * @return A Future with the unread count, failed if the communication API is unreachable or answers an error.
	 */
	public Future<Integer> getUnreadCount(String userId) {
		if (unreadUrl == null) {
			return Future.succeededFuture(0);
		}
		final RequestOptions options = new RequestOptions()
				.setMethod(HttpMethod.POST)
				.setAbsoluteURI(unreadUrl)
				.setTimeout(unreadTimeout)
				.putHeader(HttpHeaders.AUTHORIZATION, unreadAuthorizationHeader)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.putHeader(HttpHeaders.ACCEPT, "application/json");
		final String body = new JsonObject().put("userId", userId).encode();

		return httpClient.request(options)
				.compose(request -> request.send(body))
				.compose(response -> response.body().compose(buffer -> parseUnreadResponse(response, buffer.toString(), userId)));
	}

	private Future<Integer> parseUnreadResponse(HttpClientResponse response, String body, String userId) {
		switch (response.statusCode()) {
			case 200:
				try {
					return Future.succeededFuture(new JsonObject(body).getInteger("unread", 0));
				} catch (Exception e) {
					return Future.failedFuture("Invalid Carbonio unread response: " + body);
				}
			case 404:
				log.debug("No Carbonio account for user " + userId);
				return Future.succeededFuture(0);
			default:
				return Future.failedFuture("Unexpected Carbonio unread response status " + response.statusCode() + ": " + body);
		}
	}
}
