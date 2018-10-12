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

import fr.wseduc.cas.http.Response;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WrappedResponse implements Response {

	private static final Logger log = LoggerFactory.getLogger(WrappedResponse.class);
	private HttpServerResponse response;
	private boolean ended = false;

	public WrappedResponse(HttpServerResponse response) {
		this.response = response;
	}

	@Override
	public void setStatusCode(int i) {
		response.setStatusCode(i);
	}

	@Override
	public void setBody(String s) {
		log.debug(s);
		ended = true;
		response.end(s);
	}

	@Override
	public void putHeader(String s, String s2) {
		response.putHeader(s, s2);
	}

	@Override
	public void close() {
		if (!ended) {
			response.end();
		}
	}

}
