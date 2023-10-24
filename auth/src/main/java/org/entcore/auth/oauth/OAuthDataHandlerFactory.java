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

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonArray;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;
import jp.eisbahn.oauth2.server.models.Request;

import org.entcore.auth.security.SamlHelper;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.auth.services.impl.JwtVerifier;
import org.entcore.common.events.EventStore;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.redis.Redis;
import org.entcore.common.redis.RedisClient;

public class OAuthDataHandlerFactory implements DataHandlerFactory {

	private final Neo4j neo;
	private final MongoDb mongo;
	private final RedisClient redisClient;
	private final OpenIdConnectService openIdConnectService;
	private final boolean checkFederatedLogin;
	private final int pwMaxRetry;
	private final long pwBanDelay;
	private final EventStore eventStore;
	private final String passwordEventMinDate;
	private final int defaultSyncValue;
	private final JsonArray clientPWSupportSaml2;
	private final boolean otpDisabled;
	private SamlHelper samlHelper;
	private JwtVerifier jwtVerifier;

	public OAuthDataHandlerFactory(
			OpenIdConnectService openIdConnectService, boolean cfl, int pwMaxRetry, long pwBanDelay,
			String passwordEventMinDate, int defaultSyncValue, JsonArray clientPWSupportSaml2, EventStore eventStore,
			final boolean otpDisabled) {
		this.otpDisabled = otpDisabled;
		this.neo = Neo4j.getInstance();
		this.mongo = MongoDb.getInstance();
		this.openIdConnectService = openIdConnectService;
		this.checkFederatedLogin = cfl;
		this.redisClient = Redis.getClient();
		this.pwMaxRetry = pwMaxRetry;
		this.pwBanDelay = pwBanDelay;
		this.eventStore = eventStore;
		this.passwordEventMinDate = passwordEventMinDate;
		this.defaultSyncValue = defaultSyncValue;
		this.clientPWSupportSaml2 = clientPWSupportSaml2;
	}

	@Override
	public DataHandler create(Request request) {
		return new OAuthDataHandler(request, neo, mongo, redisClient, openIdConnectService, checkFederatedLogin,
				pwMaxRetry, pwBanDelay, passwordEventMinDate, defaultSyncValue, clientPWSupportSaml2, eventStore, samlHelper,
				jwtVerifier, otpDisabled);
	}

	public DataHandler create(JsonRequestAdapter request) {
		return new OAuthDataHandler(request, neo, mongo, redisClient, openIdConnectService, checkFederatedLogin,
				pwMaxRetry, pwBanDelay, passwordEventMinDate, defaultSyncValue, clientPWSupportSaml2, eventStore,
				samlHelper, jwtVerifier, otpDisabled);
	}

	public void setSamlHelper(SamlHelper samlHelper) {
		this.samlHelper = samlHelper;
	}

	public void setJwtVerifier(JwtVerifier jwtVerifier) {
		this.jwtVerifier = jwtVerifier;
	}

}
