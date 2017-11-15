/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.common.storage.impl;

import org.entcore.common.storage.AntivirusClient;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class HttpAntivirusClient implements AntivirusClient {

	private static final Logger log = LoggerFactory.getLogger(HttpAntivirusClient.class);
	private HttpClient httpClient;
	private String credential;

	public HttpAntivirusClient(Vertx vertx, String host, String cretential) {
		this.httpClient = vertx.createHttpClient()
				.setHost(host)
				.setPort(8001)
				.setMaxPoolSize(16)
				.setConnectTimeout(10000)
				.setKeepAlive(true);
		this.credential = cretential;
	}

	@Override
	public void scan(final String path) {
		HttpClientRequest req = httpClient.post("/infra/antivirus/scan", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				if (resp.statusCode() != 200) {
					log.error("Error when call scan file : " + path);
				}
			}
		});
		req.putHeader("Content-Type", "application/json");
		req.putHeader("Authorization", "Basic " + credential);
		req.end(new JsonObject().putString("file", path).encode());
	}

}
