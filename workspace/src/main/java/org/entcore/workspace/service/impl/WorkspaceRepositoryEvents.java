package org.entcore.workspace.service.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.FileUtils;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.dao.RackDao;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class WorkspaceRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceRepositoryEvents.class);
	private final MongoDb mongo = MongoDb.getInstance();
	private final EventBus eb;
	private final String gridfsAddress;

	public WorkspaceRepositoryEvents(EventBus eb, String gridfsAddress) {
		this.eb = eb;
		this.gridfsAddress = gridfsAddress;
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		for (Object o : groups) {
			if (!(o instanceof JsonObject)) continue;
			final JsonObject j = (JsonObject) o;
			final JsonObject query = MongoQueryBuilder.build(
				QueryBuilder.start("shared.groupId").is(j.getString("group")));
			JsonArray userShare = new JsonArray();
			for (Object u : j.getArray("users")) {
				JsonObject share = new JsonObject()
						.putString("userId", u.toString())
						.putBoolean("org-entcore-workspace-service-WorkspaceService|copyDocuments", true)
						.putBoolean("org-entcore-workspace-service-WorkspaceService|getDocument", true);
				userShare.addObject(share);
			}
			JsonObject update = new JsonObject()
					.putObject("$addToSet",
							new JsonObject().putObject("shared",
									new JsonObject().putArray("$each", userShare)));
			mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, update, false, true,
					new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if (!"ok".equals(event.body().getString("status"))) {
						log.error("Error updating documents with group " +
								j.getString("group") + " : " + event.body().encode());
					} else {
						log.info("Documents with group " + j.getString("group") +
								" updated : " + event.body().getInteger("number"));
					}
				}
			});
		}
	}

	@Override
	public void deleteUsers(JsonArray users) {
		String [] userIds = new String[users.size()];
		for (int i = 0; i < users.size(); i++) {
			JsonObject j = users.get(i);
			userIds[i] = j.getString("id");
		}
		final JsonObject queryDocuments = MongoQueryBuilder.build(QueryBuilder.start("owner").in(userIds));
		deleteFiles(queryDocuments, DocumentDao.DOCUMENTS_COLLECTION);
		final JsonObject queryRacks = MongoQueryBuilder.build(QueryBuilder.start("to").in(userIds));
		deleteFiles(queryRacks, RackDao.RACKS_COLLECTION);
	}

	private void deleteFiles(final JsonObject query, final String collection) {
		mongo.find(collection, query, null,
				new JsonObject().putNumber("file", 1), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				String status = res.body().getString("status");
				JsonArray results = res.body().getArray("results");
				if ("ok".equals(status) && results != null && results.size() > 0) {
					for (Object o : results) {
						if (!(o instanceof JsonObject)) continue;
						final String file = ((JsonObject) o).getString("file");
						FileUtils.gridfsRemoveFile(file, eb, gridfsAddress,
								new Handler<JsonObject>() {

									@Override
									public void handle(JsonObject event) {
										if (event == null) {
											log.error("Error deleting file " + file);
										} else if (!"ok".equals(event.getString("status"))) {
											log.error("Error deleting file " + file + " : " + event.encode());
										}
									}
								});
					}
					mongo.delete(collection, query, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if (!"ok".equals(event.body().getString("status"))) {
								log.error("Error deleting documents : " + event.body().encode());
							} else {
								log.info("Documents deleted : " + event.body().getInteger("number"));
							}
						}
					});
				}
			}
		});
	}

}
