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

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ClamAvService extends AbstractAntivirusService {

	@Override
	public void parseScanReport(String path, final Handler<AsyncResult<List<InfectedFile>>> handler) {
		vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {
			@Override
			public void handle(AsyncResult<Buffer> event) {
				if (event.succeeded()) {
					List<InfectedFile> infectedFiles = parse(event.result().toString());
					handler.handle(new DefaultAsyncResult<>(infectedFiles));
				} else {
					handler.handle(new DefaultAsyncResult<List<InfectedFile>>(event.cause()));
				}
			}
		});
	}

	protected List<InfectedFile> parse(String report) {
		Scanner s = new Scanner(report);
		List<InfectedFile> infectedFiles = new LinkedList<>();
		while (s.hasNextLine()) {
			String line = s.nextLine();
			if (line.contains("FOUND")) {
				String [] item = line.split("\\s+");
				infectedFiles.add(new InfectedFile(item[0].replace(":",""), item[1]));
			}
		}
		s.close();
		return infectedFiles;
	}

	@Override
	public void scan(String file) {
		String command = "clamdscan -m --fdpass " + file;
		JsonObject m = new JsonObject().put("command", command);
		vertx.eventBus().send("exec.command", m, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					List<InfectedFile> infectedFiles = parse(event.body().getString("result"));
					if (infectedFiles.size() > 0) {
						launchReplace(infectedFiles);
					}
				}
			}
		}));
	}

}
