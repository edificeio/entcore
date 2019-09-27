package org.entcore.common.service.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.folders.FolderExporter;
import org.entcore.common.folders.FolderExporter.FolderExporterContext;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.ResourceUtils;
import org.entcore.common.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRepositoryEvents implements RepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(AbstractRepositoryEvents.class);
	protected final Vertx vertx;
	protected final String title;
	protected final FolderExporter exporter;
	protected final MongoDb mongo = MongoDb.getInstance();

	protected AbstractRepositoryEvents(Vertx vertx) {
		this.vertx = vertx;
		String app = Server.getPathPrefix(Config.getConf()).substring(1);
		this.title = String.valueOf(app.charAt(0)).toUpperCase() + app.substring(1);
		if (vertx != null) {
			this.exporter = new FolderExporter(new StorageFactory(vertx).getStorage(), vertx.fileSystem());
		} else {
			this.exporter = null;
		}
	}

	protected void createExportDirectory(String exportPath, String locale, final Handler<String> handler) {
		this.vertx.eventBus().send("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), json -> {
			if (json.succeeded()) {
				final String path = exportPath + File.separator +
						StringUtils.stripAccents(((JsonObject)json.result().body()).getString(title.toLowerCase()));
				vertx.fileSystem().mkdir(path, event -> {
					if (event.succeeded()) {
						handler.handle(path);
					} else {
						log.error(title + " : Could not create folder " + exportPath + " - " + event.cause());
						handler.handle(null);
					}
				});
			} else {
				log.error(title + " : Could not create folder " + exportPath + " - " + json.cause());
				handler.handle(null);
			}
		});
	}

	protected void exportDocumentsDependancies(JsonArray prevResults, String exportPath, Handler<Boolean> handler) {
		if (!prevResults.isEmpty()) {
			String res = prevResults.encode();
			JsonArray documentsIds = new JsonArray(ResourceUtils.extractIds(res));
			if (!documentsIds.isEmpty()) {
				QueryBuilder findDocsbyId = QueryBuilder.start("_id").in(documentsIds);
				JsonObject query = MongoQueryBuilder.build(findDocsbyId);
				mongo.find("documents", query, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonArray results = event.body().getJsonArray("results");
						if ("ok".equals(event.body().getString("status")) && results != null && !results.isEmpty()) {
							List<JsonObject> list = new ArrayList<>();
							results.forEach(elem -> {
								JsonObject doc = ((JsonObject) elem);
								String filename = doc.getString("name");
								String fileId = doc.getString("_id");
								int dot = filename.lastIndexOf('.');
								filename = dot > -1 ? filename.substring(0, dot) + "_" + fileId + filename.substring(dot)
										: filename + "_" + fileId;
								doc.put("name",filename);
								doc.getJsonObject("metadata").put("filename",filename);
								list.add(doc);
							});
							String exportPathTmp = exportPath + "_tmp";
							String exportPathFinal = exportPath + File.separator + "Documents";
							exporter.export(new FolderExporterContext(exportPathTmp), list).setHandler(res -> {
								if (res.failed()) {
									log.error(title + " : Failed to export document to " + exportPathTmp + " - "
											+ res.cause());
								}
								// We still move the tmp folder if it failed
								vertx.fileSystem().move(exportPathTmp, exportPathFinal, resMove -> {
									if (resMove.succeeded()) {
										log.info(title + " : Documents successfully exported from " + exportPathTmp
												+ " to " + exportPathFinal);
										handler.handle(true);
									} else {
										log.error(title + " : Failed to export document from " + exportPathTmp
												+ " to " + exportPathFinal + " - " + resMove.cause());
										handler.handle(true);
									}
								});
							});
						} else {
							log.error(title + " : Failed to export document: " + event.body().getString("message"));
							handler.handle(true);
						}
					}
				});
			} else {
				handler.handle(true);
			}
		} else {
			handler.handle(true);
		}
	}

}
