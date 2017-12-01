/*
 * Copyright © WebServices pour l'Éducation, 2015
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
