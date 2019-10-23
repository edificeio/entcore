/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.conversation.service.impl;

import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.storage.Storage;
import org.entcore.common.service.impl.SqlRepositoryEvents;
import io.vertx.core.Vertx;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.wseduc.webutils.Either;
import org.entcore.common.utils.StringUtils;

public class ConversationRepositoryEvents extends SqlRepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(ConversationRepositoryEvents.class);
	private final Sql sql = Sql.getInstance();
	private final Storage storage;
	private final long timeout;

	public ConversationRepositoryEvents(Storage storage, long timeout, Vertx vertx) {
		super(vertx);
		this.storage = storage;
		this.timeout = timeout;
		this.mainResourceName = "messages";
	}

	private void exportAttachments(String exportPath, JsonArray query, Handler<Boolean> handler) {
		sql.transaction(query, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					JsonArray ja = event.body().getJsonArray("results");
					if (ja != null && !ja.isEmpty()) {
						JsonArray results = ja.getJsonObject(0).getJsonArray("results");
						if (results != null && !results.isEmpty()) {
							List<String> ids = new ArrayList<>();
							Map<String, Object> filenamesByIds = new HashMap<>();
							for (int i = 0; i < results.size(); i++) {
								JsonArray result = results.getJsonArray(i);
								String id = result.getString(0);
								String filename = result.getString(3);
								int dot = filename.lastIndexOf('.');
								filename = dot > -1 ? filename.substring(0, dot) + "_" + id + filename.substring(dot)
								: filename + "_" + id;
								filename = StringUtils.replaceForbiddenCharacters(filename);
								ids.add(id);
								filenamesByIds.put(id, filename);
							}
							JsonObject aliases = new JsonObject(filenamesByIds);
							final String exportPathTmp = exportPath + File.separator + "_tmp2";
							final String exportPathFinal = exportPath + File.separator + "Attachments";
							vertx.fileSystem().mkdir(exportPathTmp, new Handler<AsyncResult<Void>>() {
								@Override
								public void handle(AsyncResult<Void> event) {
									if (event.succeeded()) {
										storage.writeToFileSystem(ids.stream().toArray(String[]::new), exportPathTmp,
												aliases, new Handler<JsonObject>() {
													@Override
													public void handle(JsonObject event) {
														if (!"ok".equals(event.getString("status"))) {
															log.error(title
																	+ " : Failed to export one or more attachments to "
																	+ exportPathTmp + " - "
																	+ event.getJsonArray("errors").toString());
														}
														vertx.fileSystem().move(exportPathTmp, exportPathFinal,
																resMove -> {
																	if (resMove.succeeded()) {
																		log.info(title
																				+ " : Attachments (if any) successfully exported from "
																				+ exportPathTmp + " to "
																				+ exportPathFinal);
																		handler.handle(true);
																	} else {
																		log.error(title
																				+ " : Failed to export attachments from "
																				+ exportPathTmp + " to "
																				+ exportPathFinal + " - "
																				+ resMove.cause());
																		handler.handle(true);
																	}
																});
													}
												});
									} else {
										log.error(title + " : Could not create folder " + exportPathTmp + " - "
												+ event.cause());
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
				} else {
					handler.handle(true);
				}
			}
		});
	}

	@Override
	public void exportResources(JsonArray resourcesIds, String exportId, String userId, JsonArray groups, String exportPath,
			String locale, String host, Handler<Boolean> handler) {


			final HashMap<String, JsonArray> queries = new HashMap<String, JsonArray>();

			final String attachmentTable = "conversation.attachments", foldersTable = "conversation.folders",
					messagesTable = "conversation.messages", usermessagesTable = "conversation.usermessages",
					usermessagesattachmentsTable = "conversation.usermessagesattachments";

			JsonArray userIdParam = new JsonArray().add(userId);
			JsonArray resourcesIdsAndUserIdParams = userIdParam;
			String resourcesList = "";

			if(resourcesIds != null)
			{
				resourcesIdsAndUserIdParams = new JsonArray().addAll(resourcesIds).add(userId);
				resourcesList = Sql.listPrepared(resourcesIds);
			}

			String queryAttachments = "SELECT DISTINCT att.* " +
																"FROM " + attachmentTable + " att " +
																"LEFT JOIN " + usermessagesattachmentsTable + " userAtt ON att.id = userAtt.attachment_id " +
																"WHERE " +
																	(resourcesIds != null ? ("userAtt.message_id IN " + resourcesList + " AND ") : "") +
																	"userAtt.user_id = ?";
			JsonArray attachments = new SqlStatementsBuilder().prepared(queryAttachments, resourcesIdsAndUserIdParams).build();
			queries.put(attachmentTable, attachments);

			String queryFolders = "SELECT DISTINCT fol.* " +
														"FROM " + foldersTable + " fol " +
														"WHERE fol.user_id = ?";
			queries.put(foldersTable, new SqlStatementsBuilder().prepared(queryFolders, userIdParam).build());

			String queryMessages =  "SELECT DISTINCT mess.* " +
															"FROM " + messagesTable + " mess " +
															"LEFT JOIN " + usermessagesTable + " umess ON mess.id = umess.message_id " +
															"WHERE " +
																(resourcesIds != null ? ("mess.id IN " + resourcesList + " AND ") : "") +
																"umess.user_id = ?";
			queries.put(messagesTable, new SqlStatementsBuilder().prepared(queryMessages, resourcesIdsAndUserIdParams).build());

			String queryUserMessages =  "SELECT DISTINCT umess.* " +
																	"FROM " + usermessagesTable + " umess " +
																	"WHERE " +
																		(resourcesIds != null ? ("umess.message_id IN " + resourcesList + " AND ") : "") +
																	  "umess.user_id = ?";
			queries.put(usermessagesTable, new SqlStatementsBuilder().prepared(queryUserMessages, resourcesIdsAndUserIdParams).build());

			String queryUserMessagesAttachments = "SELECT DISTINCT umessatt.* " +
																						"FROM " + usermessagesattachmentsTable + " umessatt " +
																						"WHERE " +
																							(resourcesIds != null ? ("umessatt.message_id IN " + resourcesList + " AND ") : "") +
																							"umessatt.user_id = ?";
			queries.put(usermessagesattachmentsTable,
					new SqlStatementsBuilder().prepared(queryUserMessagesAttachments, resourcesIdsAndUserIdParams).build());

			AtomicBoolean exported = new AtomicBoolean(false);

			createExportDirectory(exportPath, locale, new Handler<String>() {
				@Override
				public void handle(String path) {
					if (path != null) {
						exportAttachments(path, attachments, new Handler<Boolean>() {
							@Override
							public void handle(Boolean event) {
								exportTables(queries, new JsonArray(), path, exported, handler);
							}
						});
					} else {
						handler.handle(exported.get());
					}
				}
			});
	}

	@Override
	public void importResources(String importId, String userId, String userLogin, String username, String importPath, String locale,
		Handler<JsonObject> handler)
	{
		fs.readFile(importPath + File.separator + "conversation.usermessages", result -> {
			if (result.failed()) {
				log.error(title
						+ " : Failed to read table conversation.usermessages in archive.");
				handler.handle(new JsonObject().put("status", "error"));
			} else {

				JsonObject results = result.result().toJsonObject();
				JsonArray ja = results.getJsonArray("results");
				if (!ja.isEmpty()) {
					exportUserId.put(userId,ja.getJsonArray(0).getString(results.getJsonArray("fields").getList().indexOf("user_id")));
				}

				List<String> tables = new ArrayList<>(Arrays.asList("messages", "attachments", "folders",
						"usermessages", "usermessagesattachments"));
				String postgresRandomUuid = "md5(random()::text || clock_timestamp()::text)::uuid";
				Map<String,String> tablesWithId = new HashMap<>();
				tablesWithId.put("messages", postgresRandomUuid);
				tablesWithId.put("attachments", postgresRandomUuid);
				tablesWithId.put("folders", postgresRandomUuid);

				importTables(importPath, "conversation", tables, tablesWithId, userId, username, new SqlStatementsBuilder(), handler);
			}
		});
	}

	private final Map<String,String> exportUserId = new HashMap<>();

	@Override
	public JsonArray transformResults(JsonArray fields, JsonArray results, String userId, String username, SqlStatementsBuilder builder, String table) {

		// Dirty hack..
		if ("messages".equals(table) && exportUserId.get(userId) != null) {
			results.forEach(res -> {
				JsonArray l =(JsonArray)res;
				for (int i = 0; i < l.size(); i++) {
					if (l.getValue(i) instanceof String) {
						String s = (String)l.getValue(i);
						String field = fields.getString(i);
						if ("from".equals(field) || "to".equals(field)
								|| "cc".equals(field) || "cci".equals(field)) {
							l.getList().set(i, s.replace(exportUserId.get(userId), userId));
						}
						if ("displayNames".equals(field)) {
						    l.getList().set(i, s.replaceAll(exportUserId.get(userId)+"\\$[^\\$]+?\\$",userId+"\\$"+username+"\\$"));
                        }
					}
				}
			});
		}
		//

		List<String> list = Arrays.asList("folders", "usermessages", "usermessagesattachments");

		if (list.contains(table)) {
			int index = fields.getList().indexOf("user_id");
			results.forEach(res -> {
				((JsonArray)res).getList().set(index,userId);
			});
		}

		// Re-orders items to avoid breaking foreign key constraint
		if ("folders".equals(table) || "messages".equals(table)) {
			final int indexId = fields.getList().indexOf("id");
			final int parentId = fields.getList().indexOf("parent_id");
			label: for (int i = 0; i < results.size();) {
				String parent = results.getJsonArray(i).getString(parentId);
				if (parent != null) {
					for (int j = i; j < results.size(); j++) {
						String id = results.getJsonArray(j).getString(indexId);
						if (id.equals(parent)) {
							JsonArray tmp = results.getJsonArray(i);
							results.getList().set(i, results.getJsonArray(j));
							results.getList().set(j, tmp);
							continue label;
						}
					}
				}
				i++;
			}
		}

		return results;
	}

	@Override
	protected void importDocumentsDependancies(String importPath, String userId, String userName, JsonArray statements,
											 Handler<JsonArray> handler) {
		super.importDocumentsDependancies(importPath, userId, userName, statements, newStatements -> {
			final String filePath = importPath + File.separator + "Attachments";
			fs.exists(filePath, exist -> {
				if (exist.succeeded() && exist.result().booleanValue()) {
					FolderImporter.FolderImporterContext ctx = new FolderImporter.FolderImporterContext(filePath, userId, userName, false);
					fileImporter.importFoldersFlatFormat(ctx, rapport -> {
						fileImporter.applyFileIdsChange(ctx, newStatements.getList());
						handler.handle(newStatements);
					});
				} else {
					handler.handle(newStatements);
				}
			});
		});
	}

	@Override
	public void removeShareGroups(JsonArray oldGroups) {
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$ $' || ? || '$ ') - (? || '$ $' || ? || '$' || ?) " +
			"WHERE \"to\" @> (?)::jsonb";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$ $' || ? || '$ ') - (? || '$ $' || ? || '$' || ?) " +
			"WHERE \"cc\" @> (?)::jsonb";

		for (Object o : groups) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject group = (JsonObject) o;

			JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

			params.add(group.getString("group", ""));
			params.add(new fr.wseduc.webutils.collections.JsonArray().add(group.getString("groupName", "")).toString());
			params.add(group.getString("group", ""));
			params.add(group.getString("groupName", ""));
			params.add(group.getString("group", ""));
			params.add(group.getString("groupName", ""));
			params.add(group.getString("groupName", ""));
			params.add(new fr.wseduc.webutils.collections.JsonArray().add(group.getString("group", "")).toString());

			builder.prepared(setTO, params);
			builder.prepared(setCC, params);
		}
		sql.transaction(builder.build(), new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error updating delete groups in conversation : " + event.body().encode());
				}
			}
		});
	}

	@Override
	public void deleteUsers(JsonArray users) {
		JsonArray userIds = new fr.wseduc.webutils.collections.JsonArray();
		for (Object o : users) {
			if (!(o instanceof JsonObject)) continue;
			userIds.add(((JsonObject) o).getString("id"));
		}

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String deleteFolder =
			"DELETE FROM conversation.folders f " +
			"WHERE f.user_id IN " + Sql.listPrepared(userIds.getList());
		builder.prepared(deleteFolder, userIds);

		String deleteUserMessages =
			"DELETE FROM conversation.usermessages um " +
			"WHERE um.user_id IN " + Sql.listPrepared(userIds.getList());
		builder.prepared(deleteUserMessages, userIds);

		String setFrom =
			"UPDATE conversation.messages " +
			"SET " +
			"\"from\" = '', " +
			"\"fromName\" = ?, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"from\" = ?";

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"to\" @> (?)::jsonb";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"cc\" @> (?)::jsonb";

		for (Object o : users) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject user = (JsonObject) o;
			JsonArray paramsToCc = new fr.wseduc.webutils.collections.JsonArray();
			JsonArray paramsFrom = new fr.wseduc.webutils.collections.JsonArray();

			paramsToCc.add(user.getString("id", ""));
			paramsToCc.add(new fr.wseduc.webutils.collections.JsonArray().add(user.getString("displayName", "")).toString());
			paramsToCc.add(user.getString("id", ""));
			paramsToCc.add(user.getString("displayName", ""));
			paramsToCc.add(new fr.wseduc.webutils.collections.JsonArray().add(user.getString("id", "")).toString());

			paramsFrom.add(user.getString("displayName", ""));
			paramsFrom.add(user.getString("id", ""));
			paramsFrom.add(user.getString("displayName", ""));
			paramsFrom.add(user.getString("id", ""));

			builder.prepared(setTO, paramsToCc);
			builder.prepared(setCC, paramsToCc);
			builder.prepared(setFrom, paramsFrom);
		}
		sql.transaction(builder.build(), new DeliveryOptions().setSendTimeout(timeout),
				SqlResult.validResultsHandler(event -> {
			if(event.isLeft()){
				log.error("Error deleting conversation data : " + event.left().getValue());
			}
		}));
	}

}
