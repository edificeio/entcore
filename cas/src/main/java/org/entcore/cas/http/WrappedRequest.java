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

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.http.Request;
import fr.wseduc.cas.http.Response;

public class WrappedRequest implements Request {

	private final HttpServerRequest request;
	private final Response response;

	public WrappedRequest(HttpServerRequest request) {
		this.request = request;
		this.response = new WrappedResponse(request.response());
	}

	@Override
	public String getParameter(String s) {
		return request.params().get(s);
	}

	@Override
	public Map<String, String> getParameterMap() {
		Map<String, String> params = new HashMap<>();
		for (Map.Entry<String, String> e: request.params().entries()) {
			params.put(e.getKey(), e.getValue());
		}
		return params;
	}

	@Override
	public String getHeader(String name) {
		return request.headers().get(name);
	}

	@Override
	public Response getResponse() {
		return response;
	}

	@Override
	public void getFormAttributesMap(final Handler<Map<String, String>> handler) {
		request.endHandler(new io.vertx.core.Handler<Void>() {
			@Override
			public void handle(Void event) {
				Map<String, String> params = new HashMap<>();
				for (Map.Entry<String, String> e: request.formAttributes().entries()) {
					params.put(e.getKey(), e.getValue());
				}
				handler.handle(params);
			}
		});
	}

	@Override
	public void getBody(final Handler<String> handler, final String encoding) {
		request.bodyHandler(new io.vertx.core.Handler<Buffer>(){
			@Override
			public void handle(Buffer event) {
				handler.handle(event != null ? event.toString(encoding != null ? encoding : "UTF-8") : null);
			}
		});
	}

	public HttpServerRequest getServerRequest() {
		return request;
	}

}
