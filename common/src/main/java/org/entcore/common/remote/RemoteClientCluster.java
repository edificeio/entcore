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
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteClientCluster {

	private final Map<String, RemoteClient> clients;
	private final JsonObject remoteNodesConf;

	public RemoteClientCluster(Vertx vertx, JsonObject remoteNodes) throws URISyntaxException {
		this.remoteNodesConf = remoteNodes;
		this.clients = new HashMap<>();
		for (String attr : remoteNodes.fieldNames())
			clients.put(attr, new RemoteClient(vertx, remoteNodes.getJsonObject(attr)));
	}

	public void getRemote(String uri, List<Future> futures) {
		for (Map.Entry<String, RemoteClient> e : clients.entrySet()) {
			futures.add(getRemote(uri, e.getValue()));
		}
	}

	private Future<RemoteClientResponse> getRemote(String uri, RemoteClient client) {
		final Promise<RemoteClientResponse> future = Promise.promise();
		client.request(HttpMethod.GET, uri)
				.flatMap(HttpClientRequest::send)
				.onSuccess(httpClientResponse -> httpClientResponse.bodyHandler(buffer -> {
					RemoteClientResponse remoteClientResponse = new RemoteClientResponse(httpClientResponse.statusCode(), buffer.toString());
					if (httpClientResponse.statusCode() >= 200 && httpClientResponse.statusCode() < 300) {
						future.complete(remoteClientResponse);
					} else {
						future.fail(new RemoteClientException(remoteClientResponse));
					}
				}));
		return future.future();
	}

}
