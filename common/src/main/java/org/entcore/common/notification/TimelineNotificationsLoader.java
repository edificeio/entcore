/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.common.notification;

import java.io.File;
import java.util.List;

import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.common.utils.MapFactory;
import org.entcore.common.utils.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TimelineNotificationsLoader {

	private final Vertx vertx;
	private final static String notifyDir = FileResolver.absolutePath("view/notify");
	private static final Logger log = LoggerFactory.getLogger(TimelineNotificationsLoader.class);
	private AsyncMap<String, String> sharedMap;
	private final static String sharedMapName = "notificationsMap";
	private static TimelineNotificationsLoader instance = null;

	public static enum Frequencies {
		NEVER,
		IMMEDIATE,
		DAILY,
		WEEKLY;

		public static String defaultFrequency(){
			return Frequencies.WEEKLY.name();
		}
	}
	public static enum Restrictions {
		INTERNAL,
		EXTERNAL,
		NONE,
		HIDDEN;

		public static String defaultRestriction(){
			return Restrictions.NONE.name();
		}
	}

	public static TimelineNotificationsLoader getInstance(Vertx vertx){
		if(instance == null){
			instance = new TimelineNotificationsLoader(vertx);
		}
		return instance;
	}

	private TimelineNotificationsLoader(Vertx vertx){
		this.vertx = vertx;
		MapFactory.getClusterMap(sharedMapName, vertx, new Handler<AsyncMap<String, String>>() {
			@Override
			public void handle(AsyncMap<String, String> map) {
				sharedMap = map;
				scanNotifications();
			}
		});
	}

	public void getNotification(String name, Handler<JsonObject> handler){
		sharedMap.get(name.toLowerCase(), new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> stringResult) {
				if(stringResult.failed() || stringResult.result() == null) {
					handler.handle(new JsonObject());
				} else {
					handler.handle(new JsonObject(stringResult.result()));
				}
			}
		});
	}

	private void registerNotification(String fullName, JsonObject notification){
		log.info("Registering notification : " + fullName);
		sharedMap.put(fullName, notification.encode(), ar -> {
			if (ar.failed()) {
				log.error("Error registering notification : " + fullName, ar.cause());
			}
		});
	}

	private void scanNotifications(){
		final Handler<AsyncResult<List<String>>> notifyDirHandler = new Handler<AsyncResult<List<String>>>(){
			public void handle(AsyncResult<List<String>> ar) {
				if(ar.succeeded()) {
					for(final String path : ar.result()) {
						if(path == null)
							continue;
						vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
							public void handle(AsyncResult<FileProps> properties) {
								if(properties.result().isRegularFile() && path.endsWith(".html")){
									processNotification(path);
								} else if(properties.result().isDirectory() && !path.endsWith("i18n")){
									processTypeFolder(path);
								}
							}
						});
					}
				} else {
					log.warn("Error while reading notifications directory " + notifyDir + " contents.", ar.cause());
				}
			}
		};

		vertx.fileSystem().exists(notifyDir, new Handler<AsyncResult<Boolean>>() {
			public void handle(AsyncResult<Boolean> ar) {
				if (ar.succeeded() && ar.result()) {
					vertx.fileSystem().readDir(notifyDir, notifyDirHandler);
				} else {
					log.warn("Notifications directory " + notifyDir + " doesn't exist.", ar.cause());
				}
			}
		});
	}

	private void processNotification(String path){
		final String appName = Config.getConf().getString("app-name");
		if(appName == null || appName.trim().isEmpty()){
			log.error("Invalid application name while registering notification at path : " + path);
			return;
		}
		processNotification(path, appName);
	}

	private void processNotification(final String path, final String type){
		final File pathFile = new File(path);
		final String notificationName = pathFile.getName().substring(0, pathFile.getName().lastIndexOf('.'));
		final String propsFilePath = path.substring(0, path.lastIndexOf(".")) + ".json";
		//final String templatePath = pathFile.getAbsolutePath().substring(pathFile.getAbsolutePath().indexOf("notify/"));

		vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {
			public void handle(AsyncResult<Buffer> templateAsync) {
				if(templateAsync.failed()){
					log.error("Cannot read template at path : " + path);
					return;
				}

				final String fullName = (type + "." + notificationName).toLowerCase();

				//Default values
				final JsonObject notificationJson = new JsonObject()
						.put("type", type.toUpperCase())
						.put("event-type", notificationName.toUpperCase())
						.put("app-name", Config.getConf().getString("app-name"))
						.put("app-address", Config.getConf().getString("app-address", "/"))
						.put("template", templateAsync.result().toString())
						.put("defaultFrequency", Frequencies.defaultFrequency())
						.put("restriction", Restrictions.defaultRestriction())
						.put("push-notif", false);

				vertx.fileSystem().exists(propsFilePath, new Handler<AsyncResult<Boolean>>() {
					public void handle(AsyncResult<Boolean> ar) {
						if(ar.succeeded()){
							if(ar.result()){
								vertx.fileSystem().readFile(propsFilePath, new Handler<AsyncResult<Buffer>>() {
									public void handle(AsyncResult<Buffer> ar) {
										if(ar.succeeded()){
											JsonObject props = new JsonObject(ar.result().toString("UTF-8"));

											// Overrides
											registerNotification(fullName, notificationJson
												.put("defaultFrequency", props.getString("default-frequency", notificationJson.getString("defaultFrequency")))
												.put("restriction", props.getString("restrict", notificationJson.getString("restriction")))
												.put("push-notif", props.getBoolean("push-notif", false))
											);
										} else {
											registerNotification(fullName, notificationJson);
										}
									}
								});
							} else {
								registerNotification(fullName, notificationJson);
							}
						}
					}
				});
			}
		});

	}

	private void processTypeFolder(final String path){
		final String appName = new File(path).getName();
		vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
			public void handle(AsyncResult<List<String>> ar) {
				if (ar.succeeded()) {
					for(final String item : ar.result()) {
						if(item == null)
							continue;
						vertx.fileSystem().props(item, new Handler<AsyncResult<FileProps>>() {
							public void handle(AsyncResult<FileProps> properties) {
								if(properties.result().isRegularFile() && item.endsWith(".html")){
									processNotification(item, appName);
								}
							}
						});
					}
				} else {
					log.warn("Error while reading notifications application subdirectory " + path + " contents.", ar.cause());
				}
			}
		});
	}

}
