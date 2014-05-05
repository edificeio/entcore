/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.http.HttpServerRequest;

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
