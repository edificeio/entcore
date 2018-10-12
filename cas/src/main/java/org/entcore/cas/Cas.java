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

package org.entcore.cas;

import fr.wseduc.cas.endpoint.CredentialResponse;
import org.entcore.cas.controllers.*;
import org.entcore.cas.data.EntCoreDataHandlerFactory;
import org.entcore.cas.http.VertxHttpClientFactory;
import org.entcore.common.http.BaseServer;

import fr.wseduc.cas.endpoint.CasValidator;
import fr.wseduc.cas.endpoint.Credential;
import fr.wseduc.cas.endpoint.SamlValidator;
import io.vertx.core.Handler;

import static fr.wseduc.webutils.Utils.isNotEmpty;


public class Cas extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

		EntCoreDataHandlerFactory dataHandlerFactory = new EntCoreDataHandlerFactory(getEventBus(vertx), config);

		final ConfigurationController configurationController = new ConfigurationController();
		configurationController.setRegisteredServices(dataHandlerFactory.getServices());
		addController(configurationController);
		configurationController.loadPatterns();

		CredentialResponse credentialResponse;
		if (isNotEmpty(config.getString("external-login-uri")) && isNotEmpty(config.getString("host"))) {
			credentialResponse = new ExternalCredentialResponse(config.getString("external-login-uri"), config.getString("host"));
		} else {
			credentialResponse = new EntCoreCredentialResponse();
		}
		Credential credential = new Credential();
		credential.setDataHandlerFactory(dataHandlerFactory);
		credential.setCredentialResponse(credentialResponse);
		credential.setHttpClientFactory(new VertxHttpClientFactory(vertx));
		CredentialController credentialController = new CredentialController();
		credentialController.setCredential(credential);
		addController(credentialController);

		CasValidator casValidator = new CasValidator();
		casValidator.setDataHandlerFactory(dataHandlerFactory);
		ValidatorController validatorController = new ValidatorController();
		validatorController.setValidator(casValidator);
		addController(validatorController);

		SamlValidator samlValidator = new SamlValidator();
		samlValidator.setDataHandlerFactory(dataHandlerFactory);
		SamlValidatorController samlvalidatorController = new SamlValidatorController();
		samlvalidatorController.setValidator(samlValidator);
		addController(samlvalidatorController);

		vertx.setPeriodic(config.getLong("refreshPatterns", 3600l * 1000l), new Handler<Long>() {
			@Override
			public void handle(Long event) {
				configurationController.loadPatterns();
			}
		});

	}

}
