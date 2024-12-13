package org.entcore.common.service.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Server;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.bson.conversions.Bson;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractRepositoryEvents implements RepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(AbstractRepositoryEvents.class);
	protected final Vertx vertx;
	protected final FileSystem fs;
	protected final EventBus eb;
	protected final String title;
	protected final FolderExporter exporter;
	protected final MongoDb mongo = MongoDb.getInstance();
	private static final Pattern uuidPattern = Pattern.compile(StringUtils.UUID_REGEX);

	protected AbstractRepositoryEvents(Vertx vertx)
	{
		this.vertx = vertx;
		String app = Server.getPathPrefix(Config.getConf()).substring(1);
		this.title = String.valueOf(app.charAt(0)).toUpperCase() + app.substring(1);

		if (vertx != null)
		{
			this.fs = vertx.fileSystem();
			this.eb = vertx.eventBus();
			this.exporter = new FolderExporter(new StorageFactory(vertx).getStorage(), this.fs);
		}
		else
		{
			this.fs = null;
			this.eb = null;
			this.exporter = null;
		}
	}

	protected void createExportDirectory(String exportPath, String locale, final Handler<String> handler) {
		this.vertx.eventBus().request("portal", new JsonObject().put("action","getI18n").put("acceptLanguage",locale), json -> {
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
				Bson findDocsbyId = Filters.eq("_id", documentsIds);
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
							exporter.export(new FolderExporterContext(exportPathTmp), list).onComplete(res -> {
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


	public static void applyIdsChange(JsonObject docFragment, Map<String, String> oldIdsToNewIds)
	{
		if(docFragment == null)
			return;

		for(String field : docFragment.fieldNames())
		{
			Object val = docFragment.getValue(field);

			if(val instanceof JsonObject)
				applyIdsChange((JsonObject)val, oldIdsToNewIds);
			else if(val instanceof JsonArray)
				applyIdsChange((JsonArray)val, oldIdsToNewIds);
			else if(val instanceof String)
			{
				docFragment.put(field, applyIdsChange((String)val, oldIdsToNewIds));
			}
		}
	}

	public static void applyIdsChange(JsonArray docFragment, Map<String, String> oldIdsToNewIds)
	{
		if(docFragment == null)
			return;

		for(int i = docFragment.size(); i-- > 0;)
		{
			Object val = docFragment.getValue(i);

			if(val instanceof JsonObject)
				applyIdsChange((JsonObject)val, oldIdsToNewIds);
			else if(val instanceof JsonArray)
				applyIdsChange((JsonArray)val, oldIdsToNewIds);
			else if(val instanceof String)
			{
				docFragment.set(i, applyIdsChange((String)val, oldIdsToNewIds));
			}
		}
	}

	public static String applyIdsChange(String docFragment, Map<String, String> oldIdsToNewIds)
	{
		if(docFragment == null)
			return null;

		String result = "";
		Matcher m = AbstractRepositoryEvents.uuidPattern.matcher(docFragment);
		int oldEnd = 0;

		do
		{
			if(m.find() == true)
			{
				result += docFragment.substring(oldEnd, m.start());
				oldEnd = m.end();

				String oldId = m.group();
				String newId = oldIdsToNewIds.get(oldId);
				if(newId != null)
					result += newId;
				else
					result += oldId;
			}
			else
				result += docFragment.substring(oldEnd);
		}
		while(m.hitEnd() == false);

		return result;
	}
	
	protected Future<String> getDuplicateSuffix(String locale)
	{
		// Get the latest translation from portal
		JsonObject rq =
			new JsonObject()
				.put("action", "getI18n")
				.put("acceptLanguage", locale)
				.put("label", "duplicate.suffix");

		Promise<String> promise = Promise.promise();

		this.eb.request("portal", rq, new Handler<AsyncResult<Message<JsonObject>>>()
		{
			@Override
			public void handle(AsyncResult<Message<JsonObject>> msg)
			{
				String defaultLabel = " — Copie";

				if(msg.succeeded() == false)
					promise.complete(defaultLabel);
				else
				{
					String lbl = msg.result().body().getString("label");
					promise.complete(lbl == null ? defaultLabel : lbl);
				}
			}
		});

		return promise.future();
	}

}
