/*
 * Copyright © "Open Digital Education", 2019
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.remote;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteClient {

	private final Map<String, HttpClient> clients;
	private final JsonObject remoteNodesConf;

	public RemoteClient(Vertx vertx, JsonObject remoteNodes) throws URISyntaxException {
		this.remoteNodesConf = remoteNodes;
		this.clients = new HashMap<>();
		for (String attr : remoteNodes.fieldNames()) {
			JsonObject j = remoteNodes.getJsonObject(attr);
			URI uri = new URI(j.getString("uri"));
			final HttpClientOptions options = new HttpClientOptions()
					.setDefaultHost(uri.getHost())
					.setDefaultPort(uri.getPort())
					.setMaxPoolSize(j.getInteger("poolSize", 5))
					.setKeepAlive(j.getBoolean("keepAlive", true));
			clients.put(attr, vertx.createHttpClient(options));
		}
	}

	public void getRemote(String uri, List<Future> futures) {
		for (Map.Entry<String, HttpClient> e : clients.entrySet()) {
			futures.add(getRemote(uri, e.getValue(), remoteNodesConf.getJsonObject(e.getKey())));
		}
	}

	private Future<RemoteClientResponse> getRemote(String uri, HttpClient client, JsonObject conf) {
		final Future<RemoteClientResponse> future = Future.future();
		final HttpClientRequest req = client.get(uri, httpClientResponse -> httpClientResponse.bodyHandler(buffer -> {
			RemoteClientResponse remoteClientResponse = new RemoteClientResponse(httpClientResponse.statusCode(), buffer.toString());
			if (httpClientResponse.statusCode() >= 200 && httpClientResponse.statusCode() < 300) {
				future.complete(remoteClientResponse);
			} else {
				future.fail(new RemoteClientException(remoteClientResponse));
			}
		}));
		JsonObject headers = conf.getJsonObject("headers");
		if (headers != null) {
			for (String attr : headers.fieldNames()) {
				req.putHeader(attr, headers.getString(attr));
			}
		}
		req.end();
		return future;
	}

}
