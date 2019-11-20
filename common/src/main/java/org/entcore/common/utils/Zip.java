/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.utils;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.zip.Deflater;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Zip {

	private EventBus eb;
	private String address;
	private static final Logger log = LoggerFactory.getLogger(Zip.class);

	private Zip() {}

	private static class ZipHolder {
		private static final Zip instance = new Zip();
	}

	public static Zip getInstance() {
		return ZipHolder.instance;
	}

	public void init(EventBus eb, String address) {
		this.eb = eb;
		this.address = address;
	}

	public void zipFolder(String path, String zipPath, boolean deletePath, Handler<Message<JsonObject>> handler) {
		zipFolder(path, zipPath, deletePath, Deflater.DEFAULT_COMPRESSION, handler);
	}

	public void zipFolder(String path, String zipPath, boolean deletePath, int level, Handler<Message<JsonObject>> handler) {
		JsonObject j = new JsonObject()
				.put("path", path)
				.put("zipFile", zipPath)
				.put("deletePath", deletePath)
				.put("level", level);
		eb.send(address, j, new DeliveryOptions().setSendTimeout(900000l), handlerToAsyncHandler(handler));
	}

}
