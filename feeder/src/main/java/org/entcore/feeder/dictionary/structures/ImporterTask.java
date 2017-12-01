/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.feeder.Feeder;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ImporterTask implements Handler<Long> {

	private final EventBus eb;
	private final boolean export;
	private final String feeder;

	public ImporterTask(EventBus eb, String feeder, boolean export) {
		this.eb = eb;
		this.export = export;
		this.feeder = feeder;
	}

	@Override
	public void handle(Long event) {
		eb.send(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "import").put("feeder", feeder), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && export) {
					eb.send(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "export"));
				}
			}
		}));
	}

}
