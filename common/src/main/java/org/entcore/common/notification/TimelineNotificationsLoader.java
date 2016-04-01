package org.entcore.common.notification;

import java.io.File;
import java.util.Map;

import org.entcore.common.utils.Config;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;

public class TimelineNotificationsLoader {

	private final Vertx vertx;
	private final static String notifyDir = "./view/notify";
	private static final Logger log = LoggerFactory.getLogger(TimelineNotificationsLoader.class);
	private final Map<String, String> sharedMap;
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
		NONE;

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
		scanNotifications();
		ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		Boolean cluster = (Boolean) server.get("cluster");
		if (Boolean.TRUE.equals(cluster)) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			sharedMap = cm.getSyncMap(sharedMapName);
		} else {
			sharedMap = vertx.sharedData().getMap(sharedMapName);
		}
	}

	public JsonObject getNotification(String name){
		String stringResult = sharedMap.get(name.toLowerCase());
		if(stringResult == null)
			return new JsonObject();
		return new JsonObject(stringResult);
	}

	private void registerNotification(String fullName, JsonObject notification){
		log.info("Registering notification : " + fullName);
		sharedMap.put(fullName, notification.encode());
	}

	private void scanNotifications(){
		final Handler<AsyncResult<String[]>> notifyDirHandler = new Handler<AsyncResult<String[]>>(){
			public void handle(AsyncResult<String[]> ar) {
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
						.putString("type", type.toUpperCase())
						.putString("event-type", notificationName.toUpperCase())
						.putString("app-name", Config.getConf().getString("app-name"))
						.putString("app-address", Config.getConf().getString("host", "") + Config.getConf().getString("app-address", "/"))
						.putString("template", templateAsync.result().toString())
						.putString("defaultFrequency", Frequencies.defaultFrequency())
						.putString("restriction", Restrictions.defaultRestriction());

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
												.putString("defaultFrequency", props.getString("default-frequency", notificationJson.getString("defaultFrequency")))
												.putString("restriction", props.getString("restrict", notificationJson.getString("restriction")))
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
		vertx.fileSystem().readDir(path, new Handler<AsyncResult<String[]>>() {
			public void handle(AsyncResult<String[]> ar) {
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
