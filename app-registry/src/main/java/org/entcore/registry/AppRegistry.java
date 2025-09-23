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

package org.entcore.registry;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Promise;
import org.entcore.common.appregistry.AppRegistryEventsHandler;
import org.entcore.common.http.BaseServer;
import org.entcore.registry.controllers.*;
import org.entcore.registry.filters.AppRegistryFilter;
import org.entcore.registry.services.impl.NopAppRegistryEventService;

import java.util.ArrayList;
import java.util.List;

public class AppRegistry extends BaseServer {

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final Promise<Void> promise = Promise.promise();
		super.start(promise);
		promise.future().compose(init -> initAppRegistry()).onComplete(startPromise);
	}

	public Future<Void> initAppRegistry() {
    final List<Future<?>> futures = new ArrayList<>();
    futures.add(addController(new AppRegistryController()));

    futures.add(addController(new ExternalApplicationController(config.getInteger("massAuthorizeBatchSize", 1000))));
    futures.add(addController(new WidgetController()));
		try {
      futures.add(addController(new LibraryController(vertx, config())));
		} catch (Exception e) {
			return Future.failedFuture(e);
		}
		JsonObject eduMalinConf = config.getJsonObject("edumalin-widget-config");
		if(eduMalinConf != null)
      futures.add(addController(new EdumalinWidgetController()));

		JsonObject webGerestEnabled = config.getJsonObject("webGerest-config");
		if(webGerestEnabled != null) {
      futures.add(addController(new WebGerestController()));
		}
		JsonObject screenTimeEnabled = config.getJsonObject("screen-time-config");
		if(screenTimeEnabled != null) {
      futures.add(addController(new ScreenTimeController()));
		}

		setDefaultResourceFilter(new AppRegistryFilter());
		new AppRegistryEventsHandler(vertx, new NopAppRegistryEventService());
		vertx.eventBus().publish("app-registry.loaded", new JsonObject());
		return Future.all(futures).mapEmpty();
	}

}
