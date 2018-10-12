/*
 * Copyright © "Open Digital Education", 2015
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

import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.opensaml.saml2.core.Assertion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultServiceProviderFactory implements SamlServiceProviderFactory {

	private static final Logger logger = LoggerFactory.getLogger(DefaultServiceProviderFactory.class);
	private final Map<String, SamlServiceProvider> services = new HashMap<>();

	public DefaultServiceProviderFactory(JsonObject confSP) {
		if (confSP != null) {
			for (String attr : confSP.fieldNames()) {
				try {
					services.put(attr, (SamlServiceProvider) Class.forName(confSP.getString(attr)).newInstance());
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
					logger.error("Error loading saml service provider.", e);
				}
			}
		}
	}

	@Override
	public SamlServiceProvider serviceProvider(Assertion assertion) {
		if (assertion == null || assertion.getSubject() == null || assertion.getSubject().getNameID() == null) {
			return null;
		}
		return services.get(assertion.getSubject().getNameID().getNameQualifier());
	}

}
