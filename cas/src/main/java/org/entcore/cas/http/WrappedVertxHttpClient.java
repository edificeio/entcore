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
