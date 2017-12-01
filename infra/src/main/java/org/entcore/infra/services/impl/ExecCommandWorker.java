/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.infra.services.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.vertx.java.busmods.BusModBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static fr.wseduc.webutils.Utils.isEmpty;


public class ExecCommandWorker extends BusModBase implements Handler<Message<JsonObject>> {

	@Override
	public void start() {
		super.start();
		vertx.eventBus().localConsumer("exec.command", this);
	}

	@Override
	public void handle(Message<JsonObject> event) {
		String command = event.body().getString("command");
		if (isEmpty(command)) {
			sendError(event, "Invalid command");
			return;
		}
		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = "";
			while ((line = reader.readLine())!= null) {
				sb.append(line).append("\n");
			}
			sendOK(event, new JsonObject().put("result", sb.toString()));
		} catch (Exception e) {
			sendError(event, "Error executing command : " + command, e);
		}
	}

}
