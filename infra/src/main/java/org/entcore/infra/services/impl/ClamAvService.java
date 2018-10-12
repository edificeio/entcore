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
