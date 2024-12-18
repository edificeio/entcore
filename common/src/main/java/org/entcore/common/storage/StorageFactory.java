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
import static fr.wseduc.webutils.Utils.isNotEmpty;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.MessagingClientFactoryProvider;
import org.entcore.common.storage.impl.AbstractApplicationStorage;
import org.entcore.common.storage.impl.FileStorage;
import org.entcore.common.storage.impl.GridfsStorage;
import org.entcore.common.storage.impl.HttpAntivirusClient;
import org.entcore.common.storage.impl.S3FallbackStorage;
import org.entcore.common.storage.impl.StorageFileAnalyzer;
import static org.entcore.common.storage.impl.StorageFileAnalyzer.Configuration.DEFAULT_CONTENT;
import org.entcore.common.validation.ExtensionValidator;
import org.entcore.common.validation.FileValidator;
import org.entcore.common.validation.QuotaFileSizeValidation;


public class StorageFactory {

	private final Vertx vertx;
	private final IMessagingClient messagingClient;
	private JsonObject fs;
	private String gridfsAddress;
	private final StorageFileAnalyzer.Configuration storageFileAnalyzerConfiguration;

	public StorageFactory(Vertx vertx) {
		this(vertx, null);
	}

	public StorageFactory(Vertx vertx, JsonObject config) {
		this(vertx, config, null);
	}

	public StorageFactory(Vertx vertx, JsonObject config, AbstractApplicationStorage applicationStorage) {
		this.vertx = vertx;
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		String s = (String) server.get("file-system");
		if (s != null) {
			this.fs = new JsonObject(s);
		}
		this.gridfsAddress = (String) server.get("gridfsAddress");
		if (config != null && config.getJsonObject("file-system") != null) {
			this.fs = config.getJsonObject("file-system");
		} else if (config != null && config.getString("gridfs-address") != null) {
			this.gridfsAddress = config.getString("gridfs-address");
		}

		if (applicationStorage != null) {
			applicationStorage.setVertx(vertx);
			vertx.eventBus().localConsumer("storage", applicationStorage);
		}

		if(config == null) {
			this.messagingClient = IMessagingClient.noop;
			this.storageFileAnalyzerConfiguration = new StorageFileAnalyzer.Configuration();
		} else {
			final IMessagingClient messagingClient;
			final JsonObject fileAnalyzerConfiguration = config.getJsonObject("fileAnalyzer");
			if (fileAnalyzerConfiguration != null && fileAnalyzerConfiguration.getBoolean("enabled", false)) {
				MessagingClientFactoryProvider.init(vertx);
				this.messagingClient = MessagingClientFactoryProvider.getFactory(fileAnalyzerConfiguration.getJsonObject("messaging")).create();
				if(this.messagingClient.canListen()) {
					this.storageFileAnalyzerConfiguration = new StorageFileAnalyzer.Configuration(
							fileAnalyzerConfiguration.getJsonArray("mime-types", new JsonArray()).getList(),
							fileAnalyzerConfiguration.getInteger("max-size", -1),
							fileAnalyzerConfiguration.getString("replacement-content", DEFAULT_CONTENT)
					);
				} else {
					this.storageFileAnalyzerConfiguration = new StorageFileAnalyzer.Configuration();
				}
			} else {
				this.messagingClient = IMessagingClient.noop;
				this.storageFileAnalyzerConfiguration = new StorageFileAnalyzer.Configuration();
			}
		}
	}

	public Storage getStorage() {
		Storage storage = null;
		if (fs != null) {
			if (fs.containsKey("paths")) {
				storage = new FileStorage(vertx, fs.getJsonArray("paths"), fs.getBoolean("flat", false), messagingClient, storageFileAnalyzerConfiguration);
			} else {
				storage = new FileStorage(vertx, fs.getString("path"), fs.getBoolean("flat", false), messagingClient, storageFileAnalyzerConfiguration);
			}
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

			JsonObject s3fallback = fs.getJsonObject("s3fallback");
			if (s3fallback != null) {
				final String host = s3fallback.getString("host");
				final String name = s3fallback.getString("name");
				final boolean multiBuckets = s3fallback.getBoolean("multi-buckets", true);
				final int nbStorageFolder = s3fallback.getInteger("nb-storage-folder", 1);
				final String region = s3fallback.getString("region");
				final String accessKey = s3fallback.getString("access-key");
				final String secretKey = s3fallback.getString("secret-key");
				if (isNotEmpty(host) && isNotEmpty(name) && isNotEmpty(region) && isNotEmpty(accessKey) && isNotEmpty(secretKey)) {
					S3FallbackStorage s3FallbackStorage = new S3FallbackStorage(
							vertx, host, name, multiBuckets, nbStorageFolder, region, accessKey, secretKey);
					((FileStorage) storage).setFallbackStorage(s3FallbackStorage);
				}
			}
		} else {
			storage = new GridfsStorage(vertx, Server.getEventBus(vertx), gridfsAddress);
		}
		return storage;
	}

	public IMessagingClient getMessagingClient() {
		return messagingClient;
	}
}
