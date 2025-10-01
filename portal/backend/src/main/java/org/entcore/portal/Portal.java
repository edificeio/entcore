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

package org.entcore.portal;

import io.vertx.core.Promise;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.broker.api.utils.BrokerProxyUtils;
import org.entcore.common.cache.CacheService;
import org.entcore.common.http.BaseServer;
import org.entcore.portal.controllers.PortalController;
import org.entcore.portal.listeners.I18nBrokerListenerImpl;
import org.entcore.common.events.EventBrokerListenerImpl;

public class Portal extends BaseServer {

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final Promise<Void> promise = Promise.promise();
		super.start(promise);
		promise.future()
				.onSuccess(x -> {
          final String assetPath = config.getString("assets-path", "../..");
          final AddressParameter parameter = new AddressParameter("application", "portal");
          final CacheService cacheService = CacheService.create(vertx);
          BrokerProxyUtils.addBrokerProxy(new I18nBrokerListenerImpl(vertx, assetPath, cacheService), vertx, parameter);
          BrokerProxyUtils.addBrokerProxy(new EventBrokerListenerImpl(), vertx);
          addController(new PortalController());
        })
				.onFailure(ex -> log.error("Error when start Infra server super classes", ex));
	}

}
