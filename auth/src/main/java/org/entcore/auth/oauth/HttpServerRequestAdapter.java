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

package org.entcore.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.http.HttpServerRequest;

import jp.eisbahn.oauth2.server.models.Request;

public class HttpServerRequestAdapter implements Request {

	private final HttpServerRequest request;

	public HttpServerRequestAdapter(HttpServerRequest request) {
		this.request = request;
	}

	@Override
	public String getParameter(String name) {
		return request.formAttributes().get(name);
	}

	@Override
	public Map<String, String> getParameterMap() {
		Map<String, String> params = new HashMap<>();
		for (Entry<String, String> e: request.formAttributes().entries()) {
			params.put(e.getKey(), e.getValue());
		}
		return params;
	}

	@Override
	public String getHeader(String name) {
		return request.headers().get(name);
	}

}
