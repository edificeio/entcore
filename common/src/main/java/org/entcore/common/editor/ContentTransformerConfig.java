package org.entcore.common.editor;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

import java.util.Optional;

import static java.util.Optional.empty;

public class ContentTransformerConfig {

	// TODO mest : centralize fetching config (present in blog, wiki...)
	public static Optional<JsonObject> getContentTransformerConfig(final Vertx vertx) {
		final LocalMap<Object, Object> server= vertx.sharedData().getLocalMap("server");
		final String rawConfiguration = (String) server.get("content-transformer");
		final Optional<JsonObject> contentTransformerConfig;
		if(rawConfiguration == null) {
			contentTransformerConfig = empty();
		} else {
			contentTransformerConfig = Optional.of(new JsonObject(rawConfiguration));
		}
		return contentTransformerConfig;
	}
}
