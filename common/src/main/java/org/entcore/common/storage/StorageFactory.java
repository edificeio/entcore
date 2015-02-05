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

package org.entcore.common.storage;

import fr.wseduc.webutils.Server;
import org.entcore.common.storage.impl.GridfsStorage;
import org.entcore.common.storage.impl.SwiftStorage;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;

public class StorageFactory {

	private final Vertx vertx;
	private final JsonObject config;

	public StorageFactory(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.config = config;
	}

	public Storage getStorage() {
		JsonObject swift = config.getObject("swift");
		Storage storage = null;
		if (swift != null) {
			String uri = swift.getString("uri");
			String container = swift.getString("container");
			String username = swift.getString("user");
			String password = swift.getString("key");
			try {
				storage = new SwiftStorage(vertx, new URI(uri), container, username, password);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} else {
			String gridfsAddress = config.getString("gridfs-address", "wse.gridfs.persistor");
			storage = new GridfsStorage(vertx, Server.getEventBus(vertx), gridfsAddress);
		}
		return storage;
	}

}
