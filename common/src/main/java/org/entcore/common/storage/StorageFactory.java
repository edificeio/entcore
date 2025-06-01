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
import fr.wseduc.webutils.collections.SharedDataHelper;

import static fr.wseduc.webutils.Utils.isNotEmpty;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.MessagingClientFactoryProvider;
import org.entcore.common.storage.impl.AbstractApplicationStorage;
import org.entcore.common.storage.impl.FileStorage;
import org.entcore.common.storage.impl.GridfsStorage;
import org.entcore.common.storage.impl.HttpAntivirusClient;
import org.entcore.common.storage.impl.S3FallbackStorage;
import org.entcore.common.storage.impl.S3FallbackS3FSStorage;
import org.entcore.common.storage.impl.S3FallbackS3S3Storage;
import org.entcore.common.storage.impl.StorageFileAnalyzer;

import static org.entcore.common.storage.impl.StorageFileAnalyzer.Configuration.DEFAULT_CONTENT;
import org.entcore.common.storage.impl.S3Storage;
import org.entcore.common.validation.FileValidator;

import java.net.URI;
import java.net.URISyntaxException;

public class StorageFactory {

	private final Vertx vertx;
	private IMessagingClient messagingClient;
	private JsonObject fs;
	private JsonObject s3;
	private String gridfsAddress;
	private StorageFileAnalyzer.Configuration storageFileAnalyzerConfiguration;

	public StorageFactory(Vertx vertx, Promise<StorageFactory> initPromise) {
		this(vertx, null, initPromise);
	}

	public StorageFactory(Vertx vertx, JsonObject config, Promise<StorageFactory> initPromise) {
		this(vertx, config, null, initPromise);
	}

	public StorageFactory(Vertx vertx, JsonObject config, AbstractApplicationStorage applicationStorage, Promise<StorageFactory> initPromise) {
		this.vertx = vertx;
		final SharedDataHelper sharedDataHelper = SharedDataHelper.getInstance();
		sharedDataHelper.init(vertx);
		sharedDataHelper.<String, String>getMulti("server", "s3", "file-system", "gridfsAddress").onSuccess(server -> {
			String s = (String) server.get("s3");
			if (s != null) {
				this.s3 = new JsonObject(s);
			}
			s = (String) server.get("file-system");
			if (s != null) {
				this.fs = new JsonObject(s);
			}
			this.gridfsAddress = (String) server.get("gridfsAddress");
			if (config != null && config.getJsonObject("s3") != null) {
				this.s3 = config.getJsonObject("s3");
			} else if (config != null && config.getJsonObject("file-system") != null) {
				this.fs = config.getJsonObject("file-system");
			} else if (config != null && config.getString("gridfs-address") != null) {
				this.gridfsAddress = config.getString("gridfs-address");
			}

			if (applicationStorage != null) {
				applicationStorage.setVertx(vertx);
				vertx.eventBus().consumer("storage", applicationStorage);
			}

			if(config == null) {
				this.messagingClient = IMessagingClient.noop;
				this.storageFileAnalyzerConfiguration = new StorageFileAnalyzer.Configuration();
			} else {
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
			initPromise.complete(this);
		}).onFailure(ex -> initPromise.fail(ex));
	}

	public static Future<StorageFactory> build(Vertx vertx) {
		return build(vertx, null, null);
	}

	public static Future<StorageFactory> build(Vertx vertx, JsonObject config) {
		return build(vertx, config, null);
	}

	public static Future<StorageFactory> build(Vertx vertx, JsonObject config, AbstractApplicationStorage applicationStorage) {
		final Promise<StorageFactory> promise = Promise.promise();
		new StorageFactory(vertx, config, applicationStorage, promise);
		return promise.future();
	}

	public Storage getStorage() {
		Storage storage = null;
		if (s3 != null) {
			String uri = s3.getString("uri");
			String accessKey = s3.getString("accessKey");
			String secretKey = s3.getString("secretKey");
			String region = s3.getString("region");
			String bucket = s3.getString("bucket");
			String ssec = s3.getString("ssec");
			boolean keepAlive = s3.getBoolean("keepAlive", false);
			int timeout = s3.getInteger("timeout", 10000);
			int threshold = s3.getInteger("threshold", 100);
			long openDelay = s3.getLong("openDelay", 10000l);
			try {
				storage = new S3Storage(vertx, new URI(uri), accessKey, secretKey, region, bucket, ssec, keepAlive, timeout, threshold, openDelay);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			JsonObject antivirus = s3.getJsonObject("antivirus");
			if (antivirus != null) {
				final String host = antivirus.getString("host");
				final int port = antivirus.getInteger("port", 8080);
				final String credential = antivirus.getString("credential");
				if (isNotEmpty(host) && isNotEmpty(credential)) {
					AntivirusClient av = new HttpAntivirusClient(vertx, host, credential, port);
					((S3Storage) storage).setAntivirus(av);
				}
			}

			((S3Storage) storage).setValidator(FileValidator.createNullable(s3));

			JsonObject s3fallbacks3s3 = s3.getJsonObject("s3fallbacks3s3");
			if (s3fallbacks3s3 != null) {
					S3FallbackS3S3Storage s3FallbackS3FSStorage = new S3FallbackS3S3Storage(vertx, s3, s3fallbacks3s3);
					((S3Storage) storage).setFallbackStorage(s3FallbackS3FSStorage);
			}
		} else if (fs != null) {
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
			((FileStorage) storage).setValidator(FileValidator.createNullable(fs));

			JsonObject s3fallback = fs.getJsonObject("s3fallback");
			JsonObject s3fallbacks3fs = fs.getJsonObject("s3fallbacks3fs");
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
			else if (s3fallbacks3fs != null) {
				final String uri = s3fallbacks3fs.getString("uri");
				final String bucket = s3fallbacks3fs.getString("bucket");
				final String region = s3fallbacks3fs.getString("region");
				final String accessKey = s3fallbacks3fs.getString("accessKey");
				final String secretKey = s3fallbacks3fs.getString("secretKey");
				final String ssecKey = s3fallbacks3fs.getString("ssec");
				final int bucketMaxAge = s3fallbacks3fs.getInteger("bucketMaxAge", 2);

				if (isNotEmpty(uri) && isNotEmpty(bucket) && isNotEmpty(region) && isNotEmpty(accessKey) && isNotEmpty(secretKey)) {
					S3FallbackS3FSStorage s3FallbackS3FSStorage = new S3FallbackS3FSStorage(
							vertx, uri, bucket, region, accessKey, secretKey, ssecKey, bucketMaxAge);
					((FileStorage) storage).setFallbackStorage(s3FallbackS3FSStorage);
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
