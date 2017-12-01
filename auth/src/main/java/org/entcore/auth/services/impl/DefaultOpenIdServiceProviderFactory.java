/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.oauth.OpenIdConnectClient;
import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.auth.services.OpenIdServiceProviderFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class DefaultOpenIdServiceProviderFactory implements OpenIdServiceProviderFactory {

	private static final Logger log = LoggerFactory.getLogger(DefaultOpenIdServiceProviderFactory.class);
	private final Map<String, OpenIdConnectServiceProvider> services = new HashMap<>();
	private final Map<String, OpenIdConnectClient> openIdConnectClients = new HashMap<>();

	public DefaultOpenIdServiceProviderFactory(Vertx vertx, JsonObject domains) {
		for (String domain : domains.fieldNames()) {
			JsonObject c = domains.getJsonObject(domain);
			OpenIdConnectServiceProvider provider;
			if ("France-Connect".equals(c.getString("provider"))) {
				provider = new FranceConnectServiceProvider(c.getString("iss"));
			} else {
				provider = new DefaultOpendIdConnectService(c.getString("iss"));
			}
			provider.setSetFederated(c.getBoolean("set-federated", true));
			services.put(domain, provider);
			try {
				OpenIdConnectClient oic = new OpenIdConnectClient(
						new URI(c.getString("uri")),
						c.getString("clientId"),
						c.getString("secret"),
						c.getString("authorizeUrn"),
						c.getString("tokenUrn"),
						c.getString("redirectUri"),
						vertx,
						16,
						c.getString("certsUri")
				);
				oic.setUserInfoUrn(c.getString("userInfoUrn"));
				oic.setLogoutUri(c.getString("logoutUri"));
				oic.setBasic(c.getBoolean("basic-to-get-token", true));
				openIdConnectClients.put(domain, oic);
			} catch (URISyntaxException e) {
				log.error("Invalid openid server uri", e);
			}
		}
	}

	@Override
	public OpenIdConnectServiceProvider serviceProvider(HttpServerRequest request) {
		OpenIdConnectServiceProvider provider = services.get(Renders.getHost(request));
		if (provider == null) {
			Renders.forbidden(request, "invalid.federate.domain");
			return null;
		}
		return provider;
	}

	@Override
	public OpenIdConnectClient openIdClient(HttpServerRequest request) {
		OpenIdConnectClient oic = openIdConnectClients.get(Renders.getHost(request));
		if (oic == null) {
			Renders.forbidden(request, "invalid.federate.domain");
			return null;
		}
		return oic;
	}

}
