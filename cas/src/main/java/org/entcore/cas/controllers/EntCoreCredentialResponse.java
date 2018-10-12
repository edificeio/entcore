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

package org.entcore.cas.controllers;

import fr.wseduc.cas.endpoint.CredentialResponse;
import fr.wseduc.cas.entities.LoginTicket;
import fr.wseduc.cas.http.Request;
import fr.wseduc.cas.http.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class EntCoreCredentialResponse extends CredentialResponse {

	@Override
	public void loginRequestorResponse(Request request, LoginTicket loginTicket,
			String service, boolean renew, boolean gateway, String method) {
		Response response = request.getResponse();
		try {
			response.putHeader("Location", "/auth/login?callback=" +
					URLEncoder.encode("/cas/login?" + serializeParams(request), "UTF-8"));
			response.setStatusCode(302);
		} catch (UnsupportedEncodingException e) {
			response.setStatusCode(500);
			response.setBody(e.getMessage());
		} finally {
			response.close();
		}
	}

	protected String serializeParams(Request request) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : request.getParameterMap().entrySet()) {
			sb.append("&").append(entry.getKey()).append("=").append(
					URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		return sb.length() > 0 ? sb.substring(1) : "";
	}

	@Override
	public void loggedIn(Request request) {
		Response response = request.getResponse();
		response.putHeader("Location", "/");
		response.setStatusCode(302);
		response.close();
	}

}
