/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.http.HttpClientOptions;
import org.entcore.common.http.response.DefaultPages;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.Set;

import static fr.wseduc.webutils.Utils.isNotEmpty;


/**
 * This Filter only use HTTP to retrieve user's info.
 * It targets internal module and uses cookie to authorize user request
 */

public class HttpActionFilter extends AbstractActionFilter {

	private final HttpClient httpClient;
	private final Vertx vertx;

	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx,
			ResourcesProvider provider) {
		super(bindings, provider);
		this.vertx = vertx;
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(conf.getInteger("entcore.port", 8009))
				.setMaxPoolSize(16);
		this.httpClient = vertx.createHttpClient(options);
	}

	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx) {
		this(bindings, conf, vertx, null);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		request.pause();
		if (!(request instanceof SecureHttpServerRequest)) {
			handler.handle(false);
			return;
		}

		final SecureHttpServerRequest sreq = (SecureHttpServerRequest) request;
		if (isNotEmpty(sreq.getAttribute("remote_user"))) {
			httpClient.get("/auth/internal/userinfo", getResponseHandler(request, handler))
					.putHeader("Authorization", request.headers().get("Authorization"))
					.putHeader("Accept", "application/json; version=2.1")
					.end();
		} else if (isNotEmpty(sreq.getAttribute("client_id"))) {
			clientIsAuthorizedByScope(sreq, handler);
		} else {
			httpClient.get("/auth/oauth2/userinfo", getResponseHandler(request, handler))
					.putHeader("Cookie", request.headers().get("Cookie"))
					.putHeader("Accept", "application/json; version=2.1")
					.end();
		}
	}

	private Handler<HttpClientResponse> getResponseHandler(final HttpServerRequest request, final Handler<Boolean> handler) {
		return new Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer body) {
						request.resume();
						JsonObject session = new JsonObject(body.toString());
						if (response.statusCode() == 200) {
							((SecureHttpServerRequest) request).setSession(session);
							userIsAuthorized(request, session, handler);
						} else {
							UserAuthFilter.redirectLogin(vertx, request);
						}
					}
				});
			}
		};
	}

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
	}

}
