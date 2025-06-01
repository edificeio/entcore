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

package org.entcore.common.datavalidation.utils;

import org.entcore.common.datavalidation.UserValidationService;
import org.entcore.common.datavalidation.impl.DefaultUserValidationService;
import org.entcore.common.datavalidation.metrics.DataValidationMetricsFactory;
import org.entcore.common.events.EventStore;

import fr.wseduc.webutils.collections.SharedDataHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.security.InvalidKeyException;

public class UserValidationFactory {

	private static final Logger log = LoggerFactory.getLogger(UserValidationFactory.class);
	private Vertx vertx;
	private JsonObject config;
	private JsonObject moduleConfig;
    private EventStore eventStore;
    private String eventType;
	/** When truthy, deactivates the email address or mobile phone validation right after login (on web and mobile app). */
	public boolean deactivateValidationAfterLogin = false;
	public boolean activateValidationRelative = false;
	private UserValidationService handler;

	public UserValidationFactory() {
	}

	private static class UserValidationFactoryHolder {
		private static final UserValidationFactory instance = new UserValidationFactory();
	}

	public static UserValidationFactory getFactory() {
		return UserValidationFactoryHolder.instance;
	}

	public UserValidationFactory init(Vertx vertx, JsonObject moduleConfig, Promise<UserValidationFactory> initPromise) {
		this.vertx = vertx;
		this.moduleConfig = moduleConfig;
		DataValidationMetricsFactory.init(vertx, moduleConfig);
		config = moduleConfig.getJsonObject("emailValidationConfig");
		if (config == null ) {
			final SharedDataHelper sharedDataHelper = SharedDataHelper.getInstance();
			sharedDataHelper.init(vertx);
			sharedDataHelper.<String, String>get("server", "emailValidationConfig").onSuccess(s -> {
				config = (s != null) ? new JsonObject(s) : new JsonObject();
				initInternal(initPromise);
			}).onFailure(ex ->  {
				log.error("Error when init UserValidationFactory from async map server", ex);
				initPromise.fail(ex);
			});
		} else {
			initInternal(initPromise);
		}
		return this;
	}

	private void initInternal(Promise<UserValidationFactory> initPromise)  {
		try {
			// The encryptKey parameter must be defined correctly.
			String encryptKey = config.getString("encryptKey", null);
			if( encryptKey != null
						&& (encryptKey.length()!=16 && encryptKey.length()!=24 && encryptKey.length()!=32) ) {
				// An AES key has to be 16, 24 or 32 bytes long.
				throw new InvalidKeyException("The \"encryptKey\" parameter must be 16, 24 or 32 bytes long.");
			}

			final Boolean emailValidationActive = config.getBoolean("active", true);
			final boolean emailValidationRelativeActive = config.getBoolean("emailValidationRelativeActive", false);
			deactivateValidationAfterLogin = Boolean.FALSE.equals(emailValidationActive);
			activateValidationRelative = Boolean.TRUE.equals(emailValidationRelativeActive);
			initPromise.complete(this);
		} catch (InvalidKeyException e) {
			log.error("Error with key format when init UserValidationFactory", e);
			initPromise.fail(e);
		}
	}

	public static Future<UserValidationFactory> build(Vertx vertx, JsonObject config) {
		final Promise<UserValidationFactory> promise = Promise.promise();
		final UserValidationFactory userValidationFactory = getFactory();
		userValidationFactory.init(vertx, config, promise);
		return promise.future();
	}

	public UserValidationFactory setEventStore(EventStore eventStore, String eventType) {
		this.eventStore = eventStore;
        this.eventType = eventType;
        return this;
	}

	public static UserValidationService getInstance() {
		return getFactory().getService();
	}

	public UserValidationService getService() {
		if (handler == null ) {
			handler = new DefaultUserValidationService(vertx, moduleConfig, config).setEventStore(eventStore, eventType);
		}
		return handler;
	}
}
