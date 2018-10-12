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

import io.vertx.core.json.JsonObject;

import jp.eisbahn.oauth2.server.models.Request;

import static fr.wseduc.webutils.Utils.getOrElse;

public class JsonRequestAdapter implements Request {

	private final JsonObject request;

	public JsonRequestAdapter(JsonObject request) {
		this.request = request;
	}

	@Override
	public String getHeader(String name) {
        return getOrElse(request.getJsonObject("headers").getString(name), request.getJsonObject("headers").getString(name.toLowerCase()));
	}

	@Override
	public String getParameter(String name) {
		return request.getJsonObject("params").getString(name);
	}

	@Override
	public Map<String, String> getParameterMap() {
		Map<String, String> params = new HashMap<>();
		for (String attr: request.getJsonObject("params").fieldNames()) {
			Object v = request.getJsonObject("params").getValue("attr");
			if (v instanceof String) {
				params.put(attr, (String) v);
			}
		}
		return params;
	}

}
