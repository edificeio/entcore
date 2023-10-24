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

package org.entcore.auth;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.webutils.security.JWT;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import jp.eisbahn.oauth2.server.data.DataHandler;

import org.entcore.auth.controllers.*;
import org.entcore.auth.controllers.AuthController.AuthEvent;
import org.entcore.auth.oauth.HttpServerRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.oauth.OAuthDataHandlerFactory;
import org.entcore.auth.security.AuthResourcesProvider;
import org.entcore.auth.security.SamlHelper;
import org.entcore.auth.security.SamlValidator;
import org.entcore.auth.services.MfaService;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.auth.services.SafeRedirectionService;
import org.entcore.auth.services.impl.*;
import org.entcore.auth.users.AuthRepositoryEvents;
import org.entcore.auth.users.DefaultUserAuthAccount;
import org.entcore.auth.users.NewDeviceWarningTask;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.datavalidation.utils.UserValidationFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.sms.SmsSenderFactory;
import org.opensaml.xml.ConfigurationException;

import java.util.List;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Auth extends BaseServer {

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final EventBus eb = getEventBus(vertx);
		super.start(startPromise);
		setDefaultResourceFilter(new AuthResourcesProvider(new Neo(vertx, eb, null)));
		final String JWT_PERIOD_CRON = "jwt-bearer-authorization-periodic";
		final String JWT_PERIOD = "jwt-bearer-authorization";

		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Auth.class.getSimpleName());
		final UserAuthAccount userAuthAccount = new DefaultUserAuthAccount(vertx, config, eventStore);
		SafeRedirectionService.getInstance().init(vertx, config.getJsonObject("safeRedirect", new JsonObject()));

		SmsSenderFactory.getInstance().init(vertx, config);
		UserValidationFactory.getFactory().setEventStore(eventStore, AuthEvent.SMS.name());
		final MfaService mfaService = new DefaultMfaService(vertx, config).setEventStore(eventStore);

		final JsonObject oic = config.getJsonObject("openid-connect");
		final OpenIdConnectService openIdConnectService = (oic != null)
				? new DefaultOpendIdConnectService(oic.getString("iss"), vertx, oic.getString("keys"))
				: null;
		final boolean checkFederatedLogin = config.getBoolean("check-federated-login", false);
		final OAuthDataHandlerFactory oauthDataFactory = new OAuthDataHandlerFactory(
				openIdConnectService, checkFederatedLogin, config.getInteger("maxRetry", 5), config.getLong("banDelay", 900000L),
				config.getString("password-event-min-date"), config.getInteger("password-event-sync-default-value", 0),
				config.getJsonArray("oauth2-pw-client-enable-saml2"), eventStore,
				config.getBoolean("otp-disabled", false));

		AuthController authController = new AuthController();
		authController.setEventStore(eventStore);
		authController.setUserAuthAccount(userAuthAccount);
		authController.setOauthDataFactory(oauthDataFactory);
		authController.setCheckFederatedLogin(checkFederatedLogin);
		authController.setMfaService(mfaService);
		addController(authController);

		final ConfigurationController configurationController = new ConfigurationController();
		configurationController.setConfigurationService(new DefaultConfigurationService());
		addController(configurationController);
		final JwtVerifier jwtVerifier;
		if (getOrElse(config.getBoolean(JWT_PERIOD), true)) {
			jwtVerifier = new JwtVerifier(vertx);
			oauthDataFactory.setJwtVerifier(jwtVerifier);
		} else {
			jwtVerifier = null;
		}
		final String samlMetadataFolder = config.getString("saml-metadata-folder");
		if (samlMetadataFolder != null && !samlMetadataFolder.trim().isEmpty()) {
			vertx.fileSystem().readDir(samlMetadataFolder, new Handler<AsyncResult<List<String>>>() {
				@Override
				public void handle(AsyncResult<List<String>> event) {
					if (event.succeeded() && event.result().size() > 0) {
						try {
							final String signKey = (String) vertx.sharedData().getLocalMap("server").get("signKey");
							final SamlHelper samlHelper = new SamlHelper(vertx,
									new DefaultServiceProviderFactory(config.getJsonObject("saml-services-providers")),
									signKey,
									config.getString("custom-token-encrypt-key", UUID.randomUUID().toString())
							);
							oauthDataFactory.setSamlHelper(samlHelper);

							SamlController samlController = new SamlController();
							JsonObject conf = config;

							vertx.deployVerticle(SamlValidator.class,
									new DeploymentOptions().setConfig(conf).setWorker(true));
							samlController.setEventStore(eventStore);
							samlController.setUserAuthAccount(userAuthAccount);
							samlController.setSamlHelper(samlHelper);
							samlController.setSignKey(signKey);
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
								final JsonObject authLocations = config.getJsonObject("authLocations");
								if (authLocations != null && authLocations.size() > 0) {
									server.putIfAbsent("authLocations", authLocations.encode());
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
			addController(openIdConnectController);
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
			openIdConnectController.setActivationThemes(config.getJsonObject("activation-themes", new JsonObject()));

			final JsonArray authorizedHostsLogin = openidFederate.getJsonArray("authorizedHostsLogin");
			if (authorizedHostsLogin != null && authorizedHostsLogin.size() > 0) {
				authController.setAuthorizedHostsLogin(authorizedHostsLogin);
			}
		}

		final JsonObject NDWConf = config.getJsonObject("new-device-warning");
		NewDeviceWarningTask NDWTask = null;
		if(NDWConf != null)
		{
			String cron = NDWConf.getString("cron");
			if(cron != null)
			{
				EmailFactory emailFactory = new EmailFactory(vertx, config);
				boolean warnADMC = NDWConf.getBoolean("warn-admc", false);
				boolean warnADML = NDWConf.getBoolean("warn-adml", false);
				boolean warnUsers = NDWConf.getBoolean("warn-users", false);
				int scoreThreshold = NDWConf.getInteger("score-threshold", 2).intValue();
				int batchLimit = NDWConf.getInteger("batch-limit", 4000).intValue();
				String processInterval = NDWConf.getString("process-interval");
				NDWTask = new NewDeviceWarningTask(vertx, config, emailFactory.getSender(), config.getString("email"),
													warnADMC, warnADML, warnUsers, scoreThreshold, batchLimit, processInterval);
				new CronTrigger(vertx, cron).schedule(NDWTask);
			}
		}

		addController(new TestController(NDWTask));

		CanopeCasClient canopeController = new CanopeCasClient(authController);
		addController(canopeController);

		setRepositoryEvents(new AuthRepositoryEvents(NDWTask));

		addController(new RedirectController());

		if (jwtVerifier != null) {
			DataHandler data = oauthDataFactory.create(new HttpServerRequestAdapter(null));
			((OAuthDataHandler) data).getClientsByGrantType(vertx, jwtVerifier);
			vertx.setPeriodic((config.containsKey(JWT_PERIOD_CRON)
					&& (config.getLong(JWT_PERIOD_CRON) != null)) ? config.getLong(JWT_PERIOD_CRON) : 60000,
					new Handler<Long>() {
				@Override
				public void handle(Long event) {
					((OAuthDataHandler) data).getClientsByGrantType(vertx, jwtVerifier);
				}
			});
		}
	}

}
