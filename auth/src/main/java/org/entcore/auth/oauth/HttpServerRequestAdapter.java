/* Copyright © "Open Digital Education", 2014
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
