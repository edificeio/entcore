/*
 * Copyright © WebServices pour l'Éducation, 2018
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
 */

package org.entcore.common.elasticsearch;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticSearch {

	private static class ElasticSearchClient {

		private final int index;
		private final HttpClient client;
		private AtomicInteger errorsCount = new AtomicInteger(0);

		private ElasticSearchClient(int index, HttpClient client) {
			this.index = index;
			this.client = client;
		}

		private boolean checkError() {
			return errorsCount.incrementAndGet() > 3;
		}

		private void checkSuccess() {
			if (errorsCount.get() > 0) {
				errorsCount.set(0);
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger(ElasticSearch.class);
	private ElasticSearchClient[] clients;
	private final CopyOnWriteArrayList<Integer> availableNodes = new CopyOnWriteArrayList<>();
	private final Random rnd = new Random();
	private String defaultIndex;
	private Vertx vertx;

	private ElasticSearch() {}

	private static class ElasticSearchHolder {
		private static final ElasticSearch instance = new ElasticSearch();
	}

	public static ElasticSearch getInstance() {
		return ElasticSearchHolder.instance;
	}

	public void init(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		JsonArray serverUris = config.getJsonArray("server-uris");
		String serverUri = config.getString("server-uri");
		if (serverUris == null && serverUri != null) {
			serverUris = new fr.wseduc.webutils.collections.JsonArray().add(serverUri);
		}

		if (serverUris != null) {
			try {
				URI[] uris = new URI[serverUris.size()];
				for (int i = 0; i < serverUris.size(); i++) {
					uris[i] = new URI(serverUris.getString(i));
				}
				init(uris, vertx,
						config.getInteger("poolSize", 16),
						config.getBoolean("keepAlive", true),
						config);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.error("Invalid ElasticSearch URI");
		}
	}

	public void init(URI[] uris, Vertx vertx, int poolSize,
						 boolean keepAlive, JsonObject elasticsearchConfig) {
		defaultIndex = elasticsearchConfig.getString("index");
		clients = new ElasticSearchClient[uris.length];
		for (int i = 0; i < uris.length; i++) {
			HttpClientOptions httpClientOptions = new HttpClientOptions()
					.setKeepAlive(keepAlive)
					.setMaxPoolSize(poolSize)
					.setDefaultHost(uris[i].getHost())
					.setDefaultPort(uris[i].getPort())
					.setConnectTimeout(20000);
			clients[i] = new ElasticSearchClient(i, vertx.createHttpClient(httpClientOptions));
			availableNodes.addIfAbsent(i);
		}
	}

	public void search(String type, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
		postInternal(defaultIndex + "/" + type + "/_search", 200, query, handler);
	}

	public void post(String type, JsonObject object, Handler<AsyncResult<JsonObject>> handler) {
		postInternal(defaultIndex + "/" + type, 201, object, handler);
	}

	private void postInternal(String path, int expectedStatus, JsonObject payload, Handler<AsyncResult<JsonObject>> handler) {
		final ElasticSearchClient esc = getClient();
		final HttpClientRequest req = esc.client.post(path, event -> {
			if (event.statusCode() == expectedStatus) {
				event.bodyHandler(respBody -> handler.handle(new DefaultAsyncResult<>(new JsonObject(respBody))));
			} else {
				handler.handle(new DefaultAsyncResult<>(new ElasticSearchException(event.statusMessage())));
			}
			esc.checkSuccess();
		});
		req.exceptionHandler(e -> checkDisableClientAfterError(esc, e));
		req.putHeader("Content-Type", "application/json");
		req.putHeader("Accept", "application/json; charset=UTF-8");
		req.end(payload.encode());
	}

	public BulkRequest bulk(String type, Handler<AsyncResult<JsonObject>> handler) {
		final ElasticSearchClient esc = getClient();
		final HttpClientRequest req = esc.client.post(defaultIndex + "/" + type + "/_bulk", event -> {
			if (event.statusCode() == 200) {
				event.bodyHandler(respBody -> handler.handle(new DefaultAsyncResult<>(new JsonObject(respBody))));
			} else {
				handler.handle(new DefaultAsyncResult<>(new ElasticSearchException(event.statusMessage())));
			}
			esc.checkSuccess();
		});
		req.exceptionHandler(e -> checkDisableClientAfterError(esc, e));
		req.putHeader("Content-Type", "application/x-ndjson");
		req.putHeader("Accept", "application/json; charset=UTF-8");
		req.setChunked(true);
		return new BulkRequest(req);
	}

	private void checkDisableClientAfterError(ElasticSearchClient esc, Throwable e) {
		log.error("Error with ElasticSearchClient : " + esc.index, e);
		if (esc.checkError()) {
			availableNodes.remove(Integer.valueOf(esc.index));
			vertx.setTimer(60000L, h -> availableNodes.addIfAbsent(esc.index));
		}
	}

	private ElasticSearchClient getClient() {
		return clients[rnd.nextInt(availableNodes.size())];
	}

}
