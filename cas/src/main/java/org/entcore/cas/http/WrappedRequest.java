/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
