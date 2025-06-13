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

package org.entcore.communication;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.communication.controllers.CommunicationController;
import org.entcore.communication.filters.CommunicationFilter;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.services.impl.BrokerSwitchCommunicationService;
import org.entcore.communication.services.impl.BrokerSwitchType;
import org.entcore.communication.services.impl.DefaultCommunicationService;

import java.util.Set;
import java.util.stream.Collectors;

public class Communication extends BaseServer {

	private Logger log = LoggerFactory.getLogger(Communication.class);

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		super.start(startPromise);
    TimelineHelper helper = new TimelineHelper(vertx, vertx.eventBus(), config);
		CommunicationController communicationController = new CommunicationController();

		final DefaultCommunicationService communicationService = new DefaultCommunicationService(helper, config.getJsonArray("discoverVisibleExpectedProfile", new JsonArray()));
		final CommunicationService brokerSwitchCommunicationService = getSwitchservice(communicationService, vertx, config);
		communicationController.setCommunicationService(brokerSwitchCommunicationService);

		addController(communicationController);
		setDefaultResourceFilter(new CommunicationFilter());
	}

	private CommunicationService getSwitchservice(final CommunicationService communicationService,
																														final Vertx vertx,
																														final JsonObject config) {
		final JsonObject brokerConfig = config.getJsonObject("migration", new JsonObject());
		if (brokerConfig.getBoolean("enabled", false)) {
			log.info("Using service switch for communication service");
			final BrokerSwitchType switchType = BrokerSwitchType.valueOf(
					brokerConfig.getString("switch-type", BrokerSwitchType.READ_LEGACY_WRITE_LEGACY.name()));
			log.info("Switch type: " + switchType);
			final Set<String> availableReadActions = brokerConfig.getJsonArray("available-read-actions", new JsonArray())
				.stream()
				.map(o -> (String)o)
				.collect(Collectors.toSet());
			return new BrokerSwitchCommunicationService(communicationService, switchType, availableReadActions, vertx.eventBus());
		} else {
			log.info("Legacy communication service only");
			return communicationService;
		}
	}

}
