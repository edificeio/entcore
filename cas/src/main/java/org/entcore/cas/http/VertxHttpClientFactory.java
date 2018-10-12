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

import fr.wseduc.cas.http.HttpClient;
import fr.wseduc.cas.http.HttpClientFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;

public class VertxHttpClientFactory implements HttpClientFactory {

	private final Vertx vertx;

	public VertxHttpClientFactory(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public HttpClient create(String host, int port, boolean ssl) {
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(host)
				.setDefaultPort(port)
				.setSsl(ssl)
				.setKeepAlive(false);
		io.vertx.core.http.HttpClient httpClient = vertx.createHttpClient(options);
		return new WrappedVertxHttpClient(httpClient);

	}
}
