/*
 * Copyright © "Open Digital Education", 2018
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

package org.entcore.common.elasticsearch;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.Callable;

public class BulkRequestOld {

	private final HttpClientRequest request;
	private final Handler<HttpClientResponse> onBody;

	BulkRequestOld(HttpClientRequest request, final Handler<HttpClientResponse> onBody) {
		this.request = request;
		this.onBody = onBody;
	}

	public void index(JsonObject element, JsonObject metadata) {
		if (element == null) return;
		if (metadata == null) {
			metadata = new JsonObject();
			final Object id = element.remove("_id");
			if (id != null) {
				metadata.put("_id", id);
			}
		}
		request.write(new JsonObject().put("index", metadata)
				.encode() + "\n" + element.encode() + "\n");
	}

	public void end() {
		request.send().onSuccess(onBody);
	}

}
