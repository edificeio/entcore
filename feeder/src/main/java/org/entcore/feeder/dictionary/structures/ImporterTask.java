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

package org.entcore.feeder.dictionary.structures;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.feeder.Feeder;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ImporterTask implements Handler<Long> {

	private final Vertx vertx;
	private final EventBus eb;
	private final boolean export;
	private final String feeder;
	private final long autoExportDelay;

	public ImporterTask(Vertx vertx, String feeder, boolean export, long autoExportDelay) {
		this.eb = vertx.eventBus();
		this.export = export;
		this.feeder = feeder;
		this.vertx = vertx;
		this.autoExportDelay = autoExportDelay;
	}

	@Override
	public void handle(Long event) {
		eb.send(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "import").put("feeder", feeder),
				new DeliveryOptions().setSendTimeout(5400000l), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && export) {
					vertx.setTimer(autoExportDelay, timerId ->
							eb.send(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "export")));
				}
			}
		}));
	}

}
