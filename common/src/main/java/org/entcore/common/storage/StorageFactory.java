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
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.storage.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import org.entcore.common.validation.ExtensionValidator;
import org.entcore.common.validation.FileValidator;
import org.entcore.common.validation.QuotaFileSizeValidation;

import java.net.URI;
import java.net.URISyntaxException;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class StorageFactory {

	private final Vertx vertx;
	private JsonObject swift;
	private JsonObject fs;
	private String gridfsAddress;

	public StorageFactory(Vertx vertx) {
		this(vertx, null);
	}

	public StorageFactory(Vertx vertx, JsonObject config) {
		this(vertx, config, null);
	}

	public StorageFactory(Vertx vertx, JsonObject config, AbstractApplicationStorage applicationStorage) {
		this.vertx = vertx;
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		String s = (String) server.get("swift");
		if (s != null) {
			this.swift = new JsonObject(s);
		}
		s = (String) server.get("file-system");
		if (s != null) {
			this.fs = new JsonObject(s);
		}
		this.gridfsAddress = (String) server.get("gridfsAddress");
		if (config != null && config.getJsonObject("swift") != null) {
			this.swift = config.getJsonObject("swift");
		} else if (config != null && config.getJsonObject("file-system") != null) {
			this.fs = config.getJsonObject("file-system");
		} else if (config != null && config.getString("gridfs-address") != null) {
			this.gridfsAddress = config.getString("gridfs-address");
		}

		if (applicationStorage != null) {
			applicationStorage.setVertx(vertx);
			vertx.eventBus().localConsumer("storage", applicationStorage);
		}
	}

	public Storage getStorage() {
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
		} else if (fs != null) {
			storage = new FileStorage(vertx, fs.getString("path"), fs.getBoolean("flat", false));
			JsonObject antivirus = fs.getJsonObject("antivirus");
			if (antivirus != null) {
				final String h = antivirus.getString("host");
				final String c = antivirus.getString("credential");
				if (isNotEmpty(h) && isNotEmpty(c)) {
					AntivirusClient av = new HttpAntivirusClient(vertx, h, c);
					((FileStorage) storage).setAntivirus(av);
				}
			}
			FileValidator fileValidator = new QuotaFileSizeValidation();
			JsonArray blockedExtensions = fs.getJsonArray("blockedExtensions");
			if (blockedExtensions != null && blockedExtensions.size() > 0) {
				fileValidator.setNext(new ExtensionValidator(blockedExtensions));
			}
			((FileStorage) storage).setValidator(fileValidator);
		} else {
			storage = new GridfsStorage(vertx, Server.getEventBus(vertx), gridfsAddress);
		}
		return storage;
	}

}
