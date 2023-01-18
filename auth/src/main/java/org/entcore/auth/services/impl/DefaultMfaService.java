package org.entcore.auth.services.impl;

import org.entcore.auth.services.MfaService;
import org.entcore.common.utils.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DefaultMfaService implements MfaService {
    static Logger logger = LoggerFactory.getLogger(DefaultMfaService.class);

    public DefaultMfaService(Vertx vertx, JsonObject config) {
        final JsonArray types = config.getJsonArray("types", new JsonArray());
    }

    public Future<JsonObject> tryCode(final String key) {
        return Future.failedFuture("not implemented yet");
    }

	/**
	 * 
	 * @return
	 */
	public Future<JsonArray> getProtectedURLs() {
        return Future.failedFuture("not implemented yet");
    }



}