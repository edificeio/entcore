/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.Feed;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.ResultMessage;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.net.URI;

public class AafFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(AafFeeder.class);
	private final Vertx vertx;
	private final String path;
	private final HttpClient httpClient;

	public AafFeeder(Vertx vertx, String path, String neo4jAafExtensionURI) {
		this.vertx = vertx;
		this.path = path;
		if (neo4jAafExtensionURI != null) {
			URI uri = URI.create(neo4jAafExtensionURI);
			httpClient = vertx.createHttpClient().setHost(uri.getHost())
					.setPort(uri.getPort()).setKeepAlive(false);
		} else {
			httpClient = null;
		}
	}

	@Override
	public void launch(final Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		if (importer.isFirstImport() && httpClient != null) {
			HttpClientRequest r = httpClient.post("/aaf/import", new Handler<HttpClientResponse>() {
				@Override
				public void handle(HttpClientResponse response) {
					if (response.statusCode() == 200) {
						log.info("Indexing...");
						importer.reinitTransaction();
						importer.profileConstraints();
						importer.functionConstraints();
						importer.structureConstraints();
						importer.fieldOfStudyConstraints();
						importer.moduleConstraints();
						importer.userConstraints();
						importer.classConstraints();
						importer.groupConstraints();
						importer.persist(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								if (handler != null) {
									handler.handle(message);
								}
							}
						});
					} else {
						if (handler != null) {
							handler.handle(new ResultMessage().error(response.statusMessage()));
						}
					}
				}
			});
			r.putHeader("Content-Type", "application/json");
			r.end(new JsonObject().putString("path", path).encode());
		} else {
			new StructureImportProcessing(path,vertx).start(handler);
		}
	}

}
