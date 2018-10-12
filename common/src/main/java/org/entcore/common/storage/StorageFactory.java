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
