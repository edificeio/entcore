package org.entcore.conversation.util;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Comparison layer of the referential POC (step 6) on the directory bus reads of conversation: after a successful
 * directory answer, a sample of the reads is replayed in background against referential
 * ({@code POST <url>/api/directory-bus/<action>}, same body) and the gaps are logged one JSON line each on
 * "referential.acl.diff", without any value. The directory answer is always the one served. Disabled without the
 * "referential-acl" configuration ({"url": ..., "sample-rate": 0..1}).
 */
public final class ReferentialAcl {

	private static final Logger log = LoggerFactory.getLogger(ReferentialAcl.class);
	private static final Logger diffLog = LoggerFactory.getLogger("referential.acl.diff");
	private static ReferentialAcl instance;

	private final HttpClient client;
	private final String url;
	private final double sampleRate;

	private ReferentialAcl(Vertx vertx, JsonObject config) {
		this.client = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(true));
		this.url = config.getString("url").replaceAll("/+$", "");
		this.sampleRate = config.getDouble("sample-rate", 1.0);
	}

	/** Enables the comparison if {@code config} has a "referential-acl" block with an url. */
	public static void init(Vertx vertx, JsonObject config) {
		final JsonObject acl = config.getJsonObject("referential-acl");
		if (acl != null && acl.getString("url") != null) {
			instance = new ReferentialAcl(vertx, acl);
			log.info("Referential ACL enabled, sample rate " + instance.sampleRate);
		}
	}

	/** Replays in background the bus read {@code request} against referential and logs the gaps with {@code neo4j}. */
	public static void compare(JsonObject request, JsonArray neo4j) {
		final ReferentialAcl acl = instance;
		final String action = request.getString("action");
		if (acl == null || !AclComparator.isCompared(action) || Math.random() >= acl.sampleRate) {
			return;
		}
		final JsonObject body = request.copy();
		body.remove("action");
		final String correlationId = UUID.randomUUID().toString();
		acl.client.request(new RequestOptions().setMethod(HttpMethod.POST)
						.setAbsoluteURI(acl.url + "/api/directory-bus/" + action)
						.putHeader("Content-Type", "application/json"))
				.compose(req -> req.send(body.encode()))
				.compose(resp -> resp.statusCode() == 200 ? resp.body()
						: io.vertx.core.Future.<Buffer>failedFuture("status " + resp.statusCode()))
				.onSuccess(buffer -> log(action, correlationId,
						AclComparator.compare(action, neo4j, new JsonArray(buffer))))
				.onFailure(e -> log.warn("Referential ACL " + action + " not compared: " + e.getMessage()));
	}

	private static void log(String action, String correlationId, List<JsonObject> diffs) {
		for (JsonObject diff : diffs) {
			diffLog.info(diff.copy().put("pilot", "conversation").put("endpoint", action)
					.put("correlationId", correlationId).encode());
		}
	}
}
