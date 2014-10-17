/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

	private String serializeParams(Request request) throws UnsupportedEncodingException {
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
