/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.auth;

import fr.wseduc.webutils.security.JWT;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.auth.controllers.AuthController;
import org.entcore.auth.controllers.ConfigurationController;
import org.entcore.auth.controllers.OpenIdConnectController;
import org.entcore.auth.controllers.SamlController;
import org.entcore.auth.security.AuthResourcesProvider;
import org.entcore.auth.security.SamlValidator;
import org.entcore.auth.services.impl.*;
import org.entcore.auth.users.DefaultUserAuthAccount;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo;
import org.opensaml.xml.ConfigurationException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Auth extends BaseServer {

	@Override
	public void start() throws Exception {
		final EventBus eb = getEventBus(vertx);
		super.start();
		setDefaultResourceFilter(new AuthResourcesProvider(new Neo(vertx, eb, null)));

		final UserAuthAccount userAuthAccount = new DefaultUserAuthAccount(vertx, config);
		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Auth.class.getSimpleName());

		AuthController authController = new AuthController();
		authController.setEventStore(eventStore);
		authController.setUserAuthAccount(userAuthAccount);
		addController(authController);

		final ConfigurationController configurationController = new ConfigurationController();
		configurationController.setConfigurationService(new DefaultConfigurationService());
		addController(configurationController);

		final String samlMetadataFolder = config.getString("saml-metadata-folder");
		if (samlMetadataFolder != null && !samlMetadataFolder.trim().isEmpty()) {
			vertx.fileSystem().readDir(samlMetadataFolder, new Handler<AsyncResult<List<String>>>() {
				@Override
				public void handle(AsyncResult<List<String>> event) {
					if (event.succeeded() && event.result().size() > 0) {
						try {
							SamlController samlController = new SamlController();
							JsonObject conf = new JsonObject()
									.put("saml-metadata-folder", samlMetadataFolder)
									.put("saml-private-key", config.getString("saml-private-key"))
									.put("saml-public-key", config.getString("saml-public-key"))
									.put("saml-issuer", config.getString("saml-issuer"))
									.put("saml-entng-idp-nq", config.getString("saml-entng-idp-nq"))
									.put("saml-slo-relayState", config.getString("saml-slo-relayState", "NULL"));
							vertx.deployVerticle(SamlValidator.class, new DeploymentOptions().setConfig(conf).setWorker(true));
							samlController.setEventStore(eventStore);
							samlController.setUserAuthAccount(userAuthAccount);
							samlController.setServiceProviderFactory(new DefaultServiceProviderFactory(
									config.getJsonObject("saml-services-providers")));
							samlController.setSignKey((String) vertx.sharedData().getLocalMap("server").get("signKey"));
							samlController.setSamlWayfParams(config.getJsonObject("saml-wayf"));
							samlController.setIgnoreCallBackPattern(config.getString("ignoreCallBackPattern"));
							addController(samlController);
							LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
							if (server != null) {
								String loginUri = config.getString("loginUri");
								String callbackParam = config.getString("callbackParam");
								if (loginUri != null && !loginUri.trim().isEmpty()) {
									server.putIfAbsent("loginUri", loginUri);
								}
								if (callbackParam != null && !callbackParam.trim().isEmpty()) {
									server.putIfAbsent("callbackParam", callbackParam);
								}
							}
						} catch (ConfigurationException e) {
							log.error("Saml loading error.", e);
						}
					}
				}
			});
		}
		final JsonObject openidFederate = config.getJsonObject("openid-federate");
		final JsonObject openidConnect = config.getJsonObject("openid-connect");
		final OpenIdConnectController openIdConnectController;
		if (openidFederate != null || openidConnect != null) {
			openIdConnectController = new OpenIdConnectController();
		} else {
			openIdConnectController = null;
		}
		if (openidConnect != null) {
			final String certsPath = openidConnect.getString("certs");
			if (isNotEmpty(certsPath)) {
				JWT.listCertificates(vertx, certsPath, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject certs) {
						openIdConnectController.setCertificates(certs);
					}
				});
			}
		}
		if (openidFederate != null) {
			openIdConnectController.setEventStore(eventStore);
			openIdConnectController.setUserAuthAccount(userAuthAccount);
			openIdConnectController.setSignKey((String) vertx.sharedData().getLocalMap("server").get("signKey"));
			openIdConnectController.setOpenIdConnectServiceProviderFactory(
					new DefaultOpenIdServiceProviderFactory(vertx, openidFederate.getJsonObject("domains")));
			openIdConnectController.setSubMapping(openidFederate.getBoolean("authorizeSubMapping", false));
			addController(openIdConnectController);

			final JsonArray authorizedHostsLogin = openidFederate.getJsonArray("authorizedHostsLogin");
			if (authorizedHostsLogin != null && authorizedHostsLogin.size() > 0) {
				authController.setAuthorizedHostsLogin(authorizedHostsLogin);
			}
		}
	}

}
