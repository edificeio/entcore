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
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.communication.controllers.CommunicationController;
import org.entcore.communication.filters.CommunicationFilter;
import org.entcore.communication.services.impl.DefaultCommunicationService;

public class Communication extends BaseServer {

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		super.start(startPromise);
    TimelineHelper helper = new TimelineHelper(vertx, vertx.eventBus(), config);
		CommunicationController communicationController = new CommunicationController();

		communicationController.setCommunicationService(new DefaultCommunicationService(helper, config.getJsonArray("discoverVisibleExpectedProfile", new JsonArray())));

		addController(communicationController);
		setDefaultResourceFilter(new CommunicationFilter());
	}

}
