/*
 * Copyright © "Open Digital Education", 2016
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
