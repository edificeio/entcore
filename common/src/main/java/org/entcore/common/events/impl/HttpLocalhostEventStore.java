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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;

public class HttpLocalhostEventStore extends GenericEventStore {

	private final HttpClient httpClient;

	public HttpLocalhostEventStore(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	protected void storeEvent(final JsonObject event, final Handler<Either<String, Void>> handler) {
		HttpClientRequest req = httpClient.post("/infra/event/localhost/store", new Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				if (response.statusCode() == 200) {
					handler.handle(new Either.Right<String, Void>(null));
				} else if (response.statusCode() == 403) {
					handler.handle(new Either.Left<String, Void>(
							"Error : " + response.statusMessage() + ", Event : " + event.encode()));
				} else {
					response.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer b) {
							if (b.length() > 0) {
								JsonObject body = new JsonObject(b.toString());
								handler.handle(new Either.Left<String, Void>(
										"Error : " + body.getString("error") + ", Event : " + event.encode()));
							} else {
								handler.handle(new Either.Left<String, Void>(
										"Error : " + response.statusMessage() + ", Event : " + event.encode()));
							}
						}
					});
				}
			}
		});
		req.end(event.encode());
	}

}
