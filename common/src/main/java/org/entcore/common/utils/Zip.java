/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.common.utils;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
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
		eb.send(address, j, handlerToAsyncHandler(handler));
	}

}
