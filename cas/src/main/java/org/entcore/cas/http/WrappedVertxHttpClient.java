/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.cas.http;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.http.ClientResponse;
import fr.wseduc.cas.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class WrappedVertxHttpClient implements HttpClient {

	private io.vertx.core.http.HttpClient httpClient;

	public WrappedVertxHttpClient(io.vertx.core.http.HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public void get(String uri, final Handler<ClientResponse> handler) {
		HttpClientRequest req = httpClient.get(uri, new io.vertx.core.Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				handler.handle(new ClientResponse() {
					@Override
					public int getStatusCode() {
						return response.statusCode();
					}
				});
				httpClient.close();
			}
		});
		req.end();
	}

	@Override
	public void post(String uri, String body, final  Handler<ClientResponse> handler) {
		HttpClientRequest req = httpClient.post(uri, new io.vertx.core.Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				handler.handle(new ClientResponse() {
					@Override
					public int getStatusCode() {
						return response.statusCode();
					}
				});
				httpClient.close();
			}
		});
		req.end(body);
	}

}
