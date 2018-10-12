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

import io.vertx.core.http.HttpServerRequest;

public class OAuthAuthorizationResponse {

	public static void code(HttpServerRequest request,
			String redirectUri, String code, String state) {
		String params = "code=" + code;
		if (state != null && !state.trim().isEmpty()) {
			params += "&state=" + state;
		}
		redirect(request, redirectUri, params);
	}

	public static void invalidRequest(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "invalid_request", state);
	}

	public static void unauthorizedClient(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "unauthorized_client", state);
	}

	public static void accessDenied(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "access_denied", state);
	}

	public static void unsupportedResponseType(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "unsupported_response_type", state);
	}

	public static void invalidScope(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "invalid_scope", state);
	}

	public static void serverError(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "server_error", state);
	}

	public static void temporarilyUnavailable(HttpServerRequest request, String redirectUri, String state) {
		error(request, redirectUri, "temporarily_unavailable", state);
	}

	private static void error(HttpServerRequest request, String redirectUri, String error, String state) {
		String params = "error=" + error;
		if (state != null && !state.trim().isEmpty()) {
			params += "&state=" + state;
		}
		redirect(request, redirectUri, params);
	}

	private static void redirect(HttpServerRequest request, String redirectUri, String params) {
//		String p;
//		try {
//			p = URLEncoder.encode(params, "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			p = params;
//		}
		request.response().setStatusCode(302);
		request.response().putHeader("Location", redirectUri + "?" + params);
		request.response().end();
	}

}
