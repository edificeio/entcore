/*
 * Copyright © "Open Digital Education", 2017
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
