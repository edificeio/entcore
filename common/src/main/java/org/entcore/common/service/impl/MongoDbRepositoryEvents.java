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

package org.entcore.common.service.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoDbAPI;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.*;
import org.bson.conversions.Bson;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.folders.impl.DocumentHelper;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;

import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.folders.FolderImporter.FolderImporterContext;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MongoDbRepositoryEvents extends AbstractRepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(MongoDbRepositoryEvents.class);
	protected final String managerRight;
	protected final String revisionsCollection;
	protected final String revisionIdAttribute;
	private final FolderImporter fileImporter;
	protected final Map<String, String> collectionNameToImportPrefixMap = new LinkedHashMap<String, String>();

	public MongoDbRepositoryEvents() {
		this(null, null, null, null);
	}

	public MongoDbRepositoryEvents(String managerRight) {
		this(null, managerRight, null, null);
	}

	public MongoDbRepositoryEvents(Vertx vertx) {
		this(vertx, null, null, null);
	}

	public MongoDbRepositoryEvents(Vertx vertx, String managerRight, String revisionsCollection,
								   String revisionIdAttribute) {
		super(vertx);
		this.managerRight = managerRight;
		this.revisionsCollection = revisionsCollection;
		this.revisionIdAttribute = revisionIdAttribute;
		this.fileImporter = vertx == null ? null : new FolderImporter(vertx, vertx.fileSystem(), vertx.eventBus());
	}

	@Override
	public void deleteGroups(final JsonArray groups, final Handler<List<ResourceChanges>> handler) {
		log.info("[deleteGroups] start...");
		if(groups == null) {
			handler.handle(new ArrayList<>());
			return;
		}

		for(int i = groups.size(); i-- > 0;) {
			if(groups.hasNull(i))
				groups.remove(i);
			else if (groups.getJsonObject(i) != null && groups.getJsonObject(i).getString("group") == null)
				groups.remove(i);
		}
		if(groups.size() == 0){
			handler.handle(new ArrayList<>());
			return;
		}

		final String[] groupIds = new String[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			JsonObject j = groups.getJsonObject(i);
			groupIds[i] = j.getString("group");
		}
		final long timestamp = System.currentTimeMillis();
		final JsonObject matcher = MongoQueryBuilder.build(Filters.in("shared.groupId", groupIds));
		final MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		modifier.set("_deleteGroupsKey", timestamp);
		modifier.pull("shared", MongoQueryBuilder.build(Filters.in("groupId", groupIds)));

		final String collection = MongoDbConf.getInstance().getCollection();
		if (collection == null || collection.trim().isEmpty()) {
			log.error("Error deleting groups : invalid collection " + collection + " in class " + this.getClass().getName());
			handler.handle(new ArrayList<>());
			return;
		}
		mongo.update(collection, matcher, modifier.build(), false, true, MongoDbAPI.WriteConcern.MAJORITY, (event)-> {
			if (!"ok".equals(event.body().getString("status"))) {
				log.error("Error deleting groups in collection " + collection +
						" : " + event.body().getString("message"));
			}
			// find deleted resources
			final Bson findByKey = Filters.eq("_deleteGroupsKey", timestamp);
			final JsonObject query = MongoQueryBuilder.build(findByKey);
			mongo.find(collection, query, eventFind -> {
				final JsonArray results = eventFind.body().getJsonArray("results");
				final List<ResourceChanges> list = new ArrayList<>();
				if ("ok".equals(eventFind.body().getString("status")) && results != null && !results.isEmpty()) {
					log.info("[deleteGroups] resource to delete count="+results.size());
					results.forEach(elem -> {
						if(elem instanceof  JsonObject){
							final JsonObject jsonElem = (JsonObject) elem;
							final String id = jsonElem.getString("_id");
							list.add(new ResourceChanges(id, false));
						}
					});
				} else {
					log.error("[deleteGroups] Could not found deleted resources:"+ eventFind.body());
				}
				handler.handle(list);
			});
		});
	}

	@Override
	public void deleteUsers(final JsonArray users, final Handler<List<ResourceChanges>> handler) {
		log.info("[deleteUsers] start...");
		if(users == null) {
			handler.handle(new ArrayList<>());
			return;
		}
		for(int i = users.size(); i-- > 0;) {
			if(users.hasNull(i))
				users.remove(i);
			else if (users.getJsonObject(i) != null && users.getJsonObject(i).getString("id") == null)
				users.remove(i);
		}
		if(users.size() == 0){
			handler.handle(new ArrayList<>());
			return;
		}

		final String[] userIds = new String[users.size()];
		for (int i = 0; i < users.size(); i++) {
			JsonObject j = users.getJsonObject(i);
			userIds[i] = j.getString("id");
		}

		final long timestamp = System.currentTimeMillis();
		final JsonObject criteriaShared = MongoQueryBuilder.build(Filters.in("shared.userId", userIds));
		final MongoUpdateBuilder modifierShared = new MongoUpdateBuilder();
		modifierShared.set("_deleteUsersKey", timestamp);
		modifierShared.pull("shared", MongoQueryBuilder.build(Filters.in("userId", userIds)));
		final String collection = MongoDbConf.getInstance().getCollection();
		if (collection == null || collection.trim().isEmpty()) {
			log.error("Error deleting users : invalid collection " + collection + " in class " + this.getClass().getName());
			handler.handle(new ArrayList<>());
			return;
		}
		mongo.update(collection, criteriaShared, modifierShared.build(),false, true, MongoDbAPI.WriteConcern.MAJORITY, (eventShared) -> {
			if (!"ok".equals(eventShared.body().getString("status"))) {
				log.error("Error deleting users shared in collection " + collection  +
						" : " + eventShared.body().getString("message"));
			}
			Bson findByAuthor = Filters.in("author.userId", userIds);
			Bson findByOwner = Filters.in("owner.userId", userIds);
			Bson findByAuthorOrOwner = Filters.or(findByAuthor, findByOwner);
			final JsonObject criteria = MongoQueryBuilder.build(findByAuthorOrOwner);
			final MongoUpdateBuilder modifier = new MongoUpdateBuilder();
			modifier.set("owner.deleted", true);
			modifier.set("_deleteUsersKey", timestamp);
			mongo.update(collection, criteria, modifier.build(), false, true, MongoDbAPI.WriteConcern.MAJORITY, (eventOwner) -> {
				if (!"ok".equals(eventOwner.body().getString("status"))) {
					log.error("Error deleting users shared in collection " + collection +
							" : " + eventOwner.body().getString("message"));
				}
				// find updated resources
				final Bson findByKey = Filters.eq("_deleteUsersKey", timestamp);
				final JsonObject query = MongoQueryBuilder.build(findByKey);
				mongo.find(collection, query, eventFind -> {
					final JsonArray results = eventFind.body().getJsonArray("results");
					final List<ResourceChanges> list = new ArrayList<>();
					if ("ok".equals(eventFind.body().getString("status")) && results != null && !results.isEmpty()) {
						log.info("[deleteUsers] resource to update count="+results.size());
						results.forEach(elem -> {
							if(elem instanceof  JsonObject){
								final JsonObject jsonElem = (JsonObject) elem;
								final String id = jsonElem.getString("_id");
								list.add(new ResourceChanges(id, false));
							}
						});
					} else {
						log.error("[deleteUsers] Could not found updated resources:"+ eventFind.body());
					}
					// If the app as a manager right, we will delete objects that do not have
					// - managers
					// - owner
					if (managerRight != null && !managerRight.trim().isEmpty()) {
						removeObjects(collection, toDelete -> {
							toDelete.forEach(toDel -> {
								// do not trigger an update and a delete for the same ID
								list.removeIf(element -> element.id.equals(toDel.id));
								list.add(toDel);
							});
							// trigger update and delete
							handler.handle(list);
						});
					}else{
						// trigger update
						handler.handle(list);
					}
				});
			});
		});
	}

	protected void removeObjects(final String collection) {
		this.removeObjects(collection, e -> {});
	}

	/**
	 * Remove all the objects of the specified collection that :
	 * <ul>
	 *   <li>do not have managers</li>
	 *   <li>do not have an owner</li>
	 * </ul>
	 * @param collection The name of the collection of the resources to delete
	 * @param handler List of all the objects that were deleted
	 */
	protected void removeObjects(final String collection, final Handler<List<ResourceChanges>> handler) {
		JsonObject matcher = MongoQueryBuilder.build(
				Filters.and(Filters.ne("shared." + managerRight, true),
						Filters.eq("owner.deleted", true)));

		JsonObject projection = new JsonObject().put("_id", 1);

		// Get ids of objects who have no manager and no owner (owner has just been deleted, or has been deleted previously)
		mongo.find(collection, matcher, null, projection, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("results");
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error when finding objects who have no manager and no owner : " +
							event.body().getString("message"));
					// call handler
					handler.handle(new ArrayList<>());
				} else if (res == null || res.size() == 0) {
					log.info("There are no objects without manager and without owner : no objects to delete");
					// call handler
					handler.handle(new ArrayList<>());
				} else {
					final String[] objectIds = new String[res.size()];
					for (int i = 0; i < res.size(); i++) {
						JsonObject j = res.getJsonObject(i);
						objectIds[i] = j.getString("_id");
					}
					JsonObject matcher = MongoQueryBuilder.build(Filters.in("_id", objectIds));
					mongo.delete(collection, matcher, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if (!"ok".equals(event.body().getString("status"))) {
								log.error("Error deleting objects in collection " + collection +
										" : " + event.body().getString("message"));
							} else if (revisionsCollection != null && !revisionsCollection.trim().isEmpty() &&
									revisionIdAttribute != null && !revisionIdAttribute.trim().isEmpty()) {
								JsonObject criteria = MongoQueryBuilder.build(
										Filters.in(revisionIdAttribute, objectIds));
								mongo.delete(revisionsCollection, criteria, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (!"ok".equals(event.body().getString("status"))) {
											log.error("Error deleting revisions objects in collection " +
													revisionsCollection + " : " + event.body().getString("message"));
										}
									}
								});
							}
							// trigger delete
							final List<ResourceChanges> list = new ArrayList<>();
							log.info("[deleteUsers] resource to delete count="+objectIds.length);
							for(final String id : objectIds){
								list.add(new ResourceChanges(id, true));
							}
							handler.handle(list);
						}
					});
				}
			}
		});
	}

	protected void exportFiles(final JsonArray results, String exportPath, Set<String> usedFileName,
							   final AtomicBoolean exported, final Handler<Boolean> handler) {
		if (results.isEmpty()) {
			exported.set(true);
			log.info(title + " exported successfully to : " + exportPath);
			handler.handle(exported.get());
		} else {
			JsonObject resources = results.getJsonObject(0);
			String fileId = resources.getString("_id");
			String fileName = resources.getString("title");
			if (fileName == null) {
				fileName = resources.getString("name");
			}
			fileName = StringUtils.replaceForbiddenCharacters(fileName);
			if (!usedFileName.add(fileName)) {
				fileName += "_" + fileId;
			}
			final String filePath = exportPath + File.separator + fileName;
			vertx.fileSystem().writeFile(filePath, resources.toBuffer(), new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						results.remove(0);
						exportFiles(results, exportPath, usedFileName, exported, handler);
					} else {
						log.error(title + " : Could not write file " + filePath, event.cause());
						handler.handle(exported.get());
					}
				}
			});
		}
	}

	/**
	 * Override this method to filter the resources to export
	 * @param resources The resources to filter
	 * @return The filtered resources
	 */
	protected JsonArray exportResourcesFilter(final JsonArray resources, String exportId, String userId){
		return resources;
	}

	@Override
	public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId,
								JsonArray g, String exportPath, String locale, String host, Handler<Boolean> handler) {
		Bson findByAuthor = Filters.eq("author.userId", userId);
		Bson findByOwner = Filters.eq("owner.userId", userId);
		Bson findByAuthorOrOwner = Filters.or(findByAuthor, findByOwner);

		Bson findByShared = Filters.or(
				Filters.eq("shared.userId", userId),
				Filters.in("shared.groupId", g)
		);
		Bson findByAuthorOrOwnerOrShared = exportSharedResources == false ? findByAuthorOrOwner : Filters.or(
				findByAuthorOrOwner,
				findByShared
		);

		JsonObject query;

		if(resourcesIds == null)
			query = MongoQueryBuilder.build(findByAuthorOrOwnerOrShared);
		else {
			Bson limitToResources = Filters.and(findByAuthorOrOwnerOrShared,
					Filters.in("_id", resourcesIds)
			);
			query = MongoQueryBuilder.build(limitToResources);
		}

		final AtomicBoolean exported = new AtomicBoolean(false);
		final String collection = MongoDbConf.getInstance().getCollection();

		mongo.find(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray results = exportResourcesFilter(event.body().getJsonArray("results"), exportId, userId);

				if ("ok".equals(event.body().getString("status")) && results != null) {
					for(int i = results.size(); i-->0;)
						DocumentHelper.clearComments(results.getJsonObject(i), true);

					createExportDirectory(exportPath, locale, new Handler<String>() {
						@Override
						public void handle(String path) {
							if (path != null) {
								Handler<Boolean> finish = new Handler<Boolean>() {
									@Override
									public void handle(Boolean bool) {
										if (bool) {
											exportFiles(results, path, new HashSet<String>(), exported, handler);
										} else {
											// Should never happen, export doesn't fail if docs export fail.
											handler.handle(exported.get());
										}
									}
								};

								if(exportDocuments == true)
									exportDocumentsDependancies(results, path, finish);
								else
									finish.handle(Boolean.TRUE);
							} else {
								handler.handle(exported.get());
							}
						}
					});
				} else {
					log.error(title + " : Could not proceed query " + query.encode(),
							event.body().getString("message"));
					handler.handle(exported.get());
				}
			}
		});
	}

	protected JsonObject revertExportChanges(JsonObject document, String importPrefix) {
		String title = DocumentHelper.getTitle(document);
		String name = DocumentHelper.getName(document);
		String headline = DocumentHelper.getAppProperty(document, "headline");

		if(title != null && title.startsWith(importPrefix) == true)
			DocumentHelper.setTitle(document, title.substring(importPrefix.length()));

		if(name != null && name.startsWith(importPrefix) == true)
			DocumentHelper.setName(document, name.substring(importPrefix.length()));

		if(headline != null && headline.startsWith(importPrefix) == true)
			DocumentHelper.setAppProperty(document, "headline", headline.substring(importPrefix.length()));

		// Set the modified date to now
		DocumentHelper.setModified(document, null);

		return document;
	}

	protected static void transformDocumentDuplicate(JsonObject document, String collectionName, String duplicateSuffix, boolean hintUpdateDuplicateName) {
		// Override this method to apply custom transformations to an object


		if(hintUpdateDuplicateName == true) {
			String title = DocumentHelper.getTitle(document);
			String name = DocumentHelper.getName(document);
			String headline = DocumentHelper.getAppProperty(document, "headline");

			if(title != null)
				DocumentHelper.setTitle(document, title + duplicateSuffix);

			if(name != null)
				DocumentHelper.setName(document, name + duplicateSuffix);

			if(headline != null)
				DocumentHelper.setAppProperty(document, "headline", headline + duplicateSuffix);
		}

		DocumentHelper.setModified(document, null);
	}

	protected JsonObject transformDocumentBeforeImport(JsonObject document, String collectionName,
													   String importId, String userId, String userLogin, String userName) {
		// Override this method to apply custom transformations to an object
		return document;
	}

	protected JsonObject sanitiseDocument(JsonObject document, String collectionName, String collectionPrefix,
										  String importId, String userId, String userLogin, String userName) {
		MongoDbCrudService.setUserMetadata(document, userId, userName);
		MongoDbShareService.removeShareMetadata(document);
		DocumentHelper.clearComments(document);

		this.revertExportChanges(document, collectionPrefix);
		document = this.transformDocumentBeforeImport(document, collectionName, importId, userId, userLogin, userName);

		return document;
	}

	protected boolean filterMongoDocumentFile(String filePath, Buffer fileContents) {
		return true;
	}

	protected Future<Map<String, JsonObject>> readAllDocumentsFromDir(String dirPath, String userId, String userName) {
		Promise<Map<String, JsonObject>> promise = Promise.promise();

		if(this.fileImporter == null)
			promise.fail("Cannot import documents without a file importer instance");

		MongoDbRepositoryEvents self = this;
		this.fs.readDir(dirPath, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> result) {
				if(result.succeeded() == false)
					promise.fail(result.cause());
				else {
					List<String> filesInDir = result.result();
					int nbFiles = filesInDir.size();

					ArrayList<JsonObject> mongoDocs = new ArrayList<JsonObject>(nbFiles);
					ArrayList<String> mongoDocsFileNames = new ArrayList<String>(nbFiles);
					AtomicInteger unprocessed = new AtomicInteger(nbFiles);
					AtomicInteger nbErrors = new AtomicInteger(0);

					for(int i = 0; i < nbFiles; ++i) {
						mongoDocs.add(null);
						mongoDocsFileNames.add(null);
					}

					List<FolderImporterContext> contexts = Collections.synchronizedList(new LinkedList<FolderImporterContext>());

					Handler finaliseRead = new Handler<Void>() {
						@Override
						public void handle(Void result) {
							for(FolderImporterContext importedCtx : contexts)
								self.fileImporter.applyFileIdsChange(importedCtx, mongoDocs);

							Map<String, JsonObject> fileMap = new HashMap<String, JsonObject>();
							for(int i = mongoDocs.size(); i-- > 0;)
								fileMap.put(mongoDocsFileNames.get(i), mongoDocs.get(i));

							promise.complete(fileMap);
						}
					};

					for(String filePath : filesInDir) {
						self.fs.props(filePath, new Handler<AsyncResult<FileProps>>() {
							@Override
							public void handle(AsyncResult<FileProps> propsResult) {
								if(propsResult.succeeded() == false)
									promise.fail(propsResult.cause());
								else {
									if(propsResult.result().isDirectory() == true) {
										FolderImporterContext ctx = new FolderImporterContext(filePath, userId, userName);
										self.fileImporter.importFoldersFlatFormat(ctx, new Handler<JsonObject>() {
											@Override
											public void handle(JsonObject rapport) {
												int ix = unprocessed.decrementAndGet();

												nbErrors.addAndGet(Integer.parseInt(rapport.getString("errorsNumber", "1")));
												contexts.add(ctx);

												if(ix == 0)
													finaliseRead.handle(null);
											}
										});
									} else {
										self.fs.readFile(filePath, new Handler<AsyncResult<Buffer>>() {
											@Override
											public void handle(AsyncResult<Buffer> fileResult) {
												if(fileResult.succeeded() == false)
													promise.fail(fileResult.cause());
												else {
													int ix = unprocessed.decrementAndGet();

													if(filterMongoDocumentFile(filePath, fileResult.result()) == true) {
														mongoDocs.set(ix, fileResult.result().toJsonObject());
														mongoDocsFileNames.set(ix, FileUtils.getFilename(filePath));
													}

													if(ix == 0)
														finaliseRead.handle(null);
												}
											}
										});
									}
								}
							}
						});
					}
				}
			}
		});

		return promise.future();
	}

	/**
	 * Try to import {@code documents} in {@code collection} by following these steps :
	 * <ol>
	 *     <li>Filter out empty documents</li>
	 *     <li>Find existing documents in the collection with the same ids</li>
	 *     <li>For each document that already exists, generate a new id (hopefully this one will be unique), propagate
	 *     this new id to related resources, folders, etc. and adapt the duplicata data to reflect that it is a duplicata</li>
	 *     <li>Save the resources</li>
	 *     <li>Register errors, duplicates and oks and call {@code handler} with the report</li>
	 * </ol>
	 * @param collection Name of the collection in which the {@code documents} should be imported
	 * @param documents The documents to import
	 * @param duplicateSuffix Suffix to add to the fields {@code title}, {@code name} or {@code headline} of a resource
	 *                           that we tried to import but which was detected as a duplicata of an already existing
	 *                           owned resource iff {@code hintUpdateDuplicateName} is {@code true}
	 * @param hintUpdateDuplicateName {@code true} iff we want to add a suffix to duplicated resources
	 * @param handler Downstream processes which will receive a report containing the following fields :
	 *              <ul>
	 *                <li>{@code rapport}
	 *                	<ul>
	 *                		<li>{@code resourcesNumber}: number of resources imported</li>
	 *                		<li>{@code duplicatesNumber}: number of resources which where marked as duplicated</li>
	 *                		<li>{@code errorsNumber}: number of resources which could not be imported</li>
	 *                	</ul>
	 *                </li>
	 *                <li>{@code collection}: name of the collection in which the resources were imported</li>
	 *                <li>{@code idsMap}: association between the ids of the resources that were to be imported and the id with which they were indeed imported
	 *                <ul>
	 *                	<li>=> the key and the value are the same except for duplicates</li>
	 *                	<li>=> if one wishes to know imported ids, it suffices to take only the values of {@code idsMap}</li>
	 *                </ul>
	 *                </li>
	 *              </ul>
	 */
	public static void importDocuments(String collection, List<JsonObject> documents, String duplicateSuffix, boolean hintUpdateDuplicateName,
									   Handler<JsonObject> handler) {
		if(documents.size() == 0) {
			JsonObject rapport =
					new JsonObject()
							.put("rapport",
									new JsonObject()
											.put("resourcesNumber", Integer.toString(0))
											.put("duplicatesNumber", Integer.toString(0))
											.put("errorsNumber", Integer.toString(0))
							)
							.put("collection", collection)
							.put("idsMap", new JsonObject());

			handler.handle(rapport);
			return;
		}

		MongoDb mongo =  MongoDb.getInstance();

		JsonArray savePayload = new JsonArray();
		JsonArray idsToImport = new JsonArray();
		Map<String, Integer> idToIxMap = new HashMap<String, Integer>();
		Map<String, String> oldIdsToNewIds = new HashMap<String, String>();

		for(int i = 0, skipped = 0, l = documents.size(); i < l; ++i) {
			JsonObject d = documents.get(i);

			if(d == null) {
				++skipped;
				continue;
			}

			String docId = DocumentHelper.getId(d);

			savePayload.add(d);
			idsToImport.add(docId);
			idToIxMap.put(docId, i - skipped);
			oldIdsToNewIds.put(docId, docId);
		}

		Bson lookForExisting = Filters.in("_id", idsToImport);
		// HINT: a little optimisation could be made here if we only fetched the resources id because that's all
		// we're going to use
		mongo.find(collection, MongoQueryBuilder.build(lookForExisting), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> searchMsg) {
				JsonObject body = searchMsg.body();

				if(body.getString("status").equals("ok")) {
					JsonArray foundDocs = body.getJsonArray("results");
					int nbDuplicates = 0;

					for(int i = foundDocs.size(); i-- > 0;) {
						String foundId = DocumentHelper.getId(foundDocs.getJsonObject(i));

						// Find already-existing resources
						Integer mapIx = idToIxMap.get(foundId);
						if(mapIx != null) {
							String newId = UUID.randomUUID().toString();
							oldIdsToNewIds.put(foundId, newId);

							// Create a duplicate
							JsonObject dupDoc = savePayload.getJsonObject(mapIx);
							DocumentHelper.setId(dupDoc, newId);
							transformDocumentDuplicate(dupDoc, collection, duplicateSuffix, hintUpdateDuplicateName);

							++nbDuplicates;
						}
					}

					// Update documents that are inside duplicate folders to link them to the new folder
					for(int i = 0, l = savePayload.size(); i < l; ++i) {
						JsonObject d = savePayload.getJsonObject(i);

						if(d == null)
							continue;

						String parentId = DocumentHelper.getParent(d);
						if(oldIdsToNewIds.containsKey(parentId))
							DocumentHelper.setParent(d, oldIdsToNewIds.get(parentId));
					}

					// Apply ids change from the collection
					if(oldIdsToNewIds.size() > 0)
						applyIdsChange(savePayload, oldIdsToNewIds);

					final int totalDuplicates = nbDuplicates;
					mongo.insert(collection, savePayload, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> saveMsg) {
							JsonObject body = saveMsg.body();
							int nbErrors = -1;

							if(body.getString("status").equals("ok"))
								nbErrors = 0;
							else {
								JsonObject mongoError = new JsonObject(body.getString("message"));

								nbErrors = savePayload.size() - mongoError.getInteger("n");
							}

							Map<String, Object> convertedMap = new HashMap<String, Object>();
							convertedMap.putAll(oldIdsToNewIds);

							JsonObject rapport =
									new JsonObject()
											.put("rapport",
													new JsonObject()
															.put("resourcesNumber", Integer.toString(savePayload.size() - nbErrors))
															.put("duplicatesNumber", Integer.toString(totalDuplicates))
															.put("errorsNumber", Integer.toString(nbErrors))
											)
											.put("collection", collection)
											.put("idsMap", new JsonObject(convertedMap));

							handler.handle(rapport);
						}
					});
				} else {
					// If the find fails, don't import anything
					JsonObject rapport =
							new JsonObject()
									.put("rapport",
											new JsonObject()
													.put("resourcesNumber", Integer.toString(0))
													.put("duplicatesNumber", Integer.toString(0))
													.put("errorsNumber", Integer.toString(savePayload.size()))
									)
									.put("collection", collection)
									.put("idsMap", new JsonObject());

					handler.handle(rapport);
				}
			}
		});
	}

	/**
	 *
	 * @param importId Identifier of the current import process
	 * @param userId Id of the user requesting the import
	 * @param userLogin Login of the user requesting the import
	 * @param userName Name of the user requesting the import
	 * @param importPath Complete path on the fs to the root of the resources to import
	 *                      (e.g. /opt/data/1688042705171_91c22b66-ba1b-4fde-a3fe-95219cc18d4a/Blog to import blog data
	 *                   of the archive unzipped in /opt/data/1688042705171_91c22b66-ba1b-4fde-a3fe-95219cc18d4a)
	 * @param locale
	 * @param host
	 * @param forceImportAsDuplication
	 * @param handler
	 */
	@Override
	public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
								String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {
		MongoDbRepositoryEvents self = this;

		final JsonObject duplicateSuffixWrapper = new JsonObject();

		Handler readDirsHandler = new Handler<Map<String, JsonObject>>() {
			@Override
			public void handle(Map<String, JsonObject> docs) {
				Map<String, String> prefixMap = self.collectionNameToImportPrefixMap;

				// Single collection case, aka the generic mongoDB apps
				if(prefixMap.size() == 0) {
					//prefixMap = new HashMap<String, String>();
					prefixMap.put(MongoDbConf.getInstance().getCollection(), "");
				}

				List<Future> collFutures = new LinkedList<Future>();
				List<Future> collFuturesChain = new LinkedList<Future>();

				/**
				 Collections depending on others are expected to be bigger than the collections they depend on.
				 e.g. Forum posts depend on their thread and there are more forum posts than there are forum threads.

				 Thus, it is worth the extra memory avoiding to loop multiple times on the depending collections for id changes.
				 */
				Map<String, String> previousIdsMapCombined = new HashMap<String, String>();
				// Should be Map<String, Map<String, String>> but casting to JsonObject fails...
				Map<String, Map<String, Object>> previousIdsMapByCollection = new HashMap<String, Map<String, Object>>();

				String mainResourceName = "";

				for(Map.Entry<String, String> prefix : prefixMap.entrySet()) {
					if(mainResourceName.equals("") == true)
						mainResourceName = prefix.getKey();
					ArrayList<JsonObject> collectionDocs = new ArrayList<JsonObject>(docs.size());

					// Sort documents depending on their collection
					for(Map.Entry<String, JsonObject> entry : docs.entrySet()) {
						if(entry == null || entry.getKey() == null || entry.getValue() == null)
							continue;

						if(entry.getKey().startsWith(prefix.getValue()) == true) {
							JsonObject finalDoc = self.sanitiseDocument(entry.getValue(), prefix.getKey(), prefix.getValue(), importId, userId, userLogin, userName);
							if(finalDoc != null)
								collectionDocs.add(finalDoc);
						}
					}

					if(collectionDocs.size() != 0) {
						Promise<JsonObject> collDone = Promise.promise();
						Promise<JsonObject> collChain = Promise.promise();

						// Import collections one by one because we might need to apply id changes
						Handler importNextCollection = new Handler<AsyncResult<JsonObject>>() {
							@Override
							public void handle(AsyncResult<JsonObject> previousResult) {
								if(previousResult != null) {
									// Replace old ids with new ids
									for(int i = collectionDocs.size(); i-- > 0;)
										AbstractRepositoryEvents.applyIdsChange(collectionDocs.get(i), previousIdsMapCombined);
								}
								// The heuristic is to rename only the master element and not its dependants, since they should be confined to it
								boolean hint = previousResult == null;

								MongoDbRepositoryEvents.importDocuments(prefix.getKey(), collectionDocs, duplicateSuffixWrapper.getString("str"), hint,
										new Handler<JsonObject>() {
											@Override
											public void handle(JsonObject result) {
												JsonObject previousIdsMap = result.getJsonObject("idsMap");

												Map<String, Object> collectionIdsMap = new HashMap<String, Object>();

												// Fill the ids maps
												for(String key : previousIdsMap.getMap().keySet()) {
													String newId = previousIdsMap.getString(key);
													collectionIdsMap.put(key, newId);
													previousIdsMapCombined.put(key, newId);
												}

												previousIdsMapByCollection.put(prefix.getKey(), collectionIdsMap);

												collChain.complete(result);
												collDone.complete(result);
											}
										});
							}
						};

						// Start importing collections
						if(collFutures.size() == 0)
							importNextCollection.handle(null);
						else
							collFuturesChain.get(collFuturesChain.size() - 1).onComplete(importNextCollection);

						collFuturesChain.add(collChain.future());
						collFutures.add(collDone.future());
					}
				}

				final String mainResourceNameFinal = mainResourceName;

				// Fuse reports into a final one
				CompositeFuture.join(collFutures).onComplete(new Handler<AsyncResult<CompositeFuture>>() {
					@Override
					public void handle(AsyncResult<CompositeFuture> result) {
						if(result.succeeded()) {
							List<JsonObject> rapports = result.result().list();

							int nbResources = 0;
							int nbDuplicates = 0;
							int nbErrors = 0;

							Map<String, Integer> dupsPerCollection = new HashMap<String, Integer>();

							for(JsonObject rapWrapper : rapports) {
								JsonObject rap = rapWrapper.getJsonObject("rapport");
								Integer dups = Integer.parseInt(rap.getString("duplicatesNumber"));

								nbResources += Integer.parseInt(rap.getString("resourcesNumber"));
								nbDuplicates += dups;
								nbErrors += Integer.parseInt(rap.getString("errorsNumber"));

								dupsPerCollection.put(rapWrapper.getString("collection"), dups);
							}

							JsonObject idsMapObj = new JsonObject();
							for(Map.Entry<String, Map<String, Object>> entry : previousIdsMapByCollection.entrySet())
								idsMapObj.put(entry.getKey(), new JsonObject(entry.getValue()));

							JsonObject finalRapport =
								new JsonObject()
									.put("resourcesNumber", Integer.toString(nbResources))
									.put("duplicatesNumber", Integer.toString(nbDuplicates))
									.put("errorsNumber", Integer.toString(nbErrors))
									.put("resourcesIdsMap", idsMapObj)
									.put("duplicatesNumberMap", dupsPerCollection)
									.put("mainResourceName", mainResourceNameFinal)
										.put("mainRepository", MongoDbConf.getInstance().getCollection());

							handler.handle(finalRapport);
						}
						// Can't fail
					}
				});

			};
		};

		Future<Map<String, JsonObject>> readDirs = this.readAllDocumentsFromDir(importPath, userId, userName);
		Future<String> dupSuffix = this.getDuplicateSuffix(locale);

		CompositeFuture.join(readDirs, dupSuffix).onComplete(new Handler<AsyncResult<CompositeFuture>>() {
			@Override
			public void handle(AsyncResult<CompositeFuture> ftr) {
				duplicateSuffixWrapper.put("str", dupSuffix.result());

				if(readDirs.succeeded() == true)
					readDirsHandler.handle(readDirs.result());
				else {
					// Error in readDirs
					handler.handle(new JsonObject()
							.put("resourcesNumber", Integer.toString(0))
							.put("duplicatesNumber", Integer.toString(0))
							.put("errorsNumber", Integer.toString(1))
							.put("resourcesIdsMap", new JsonObject())
							.put("duplicatesNumberMap", new JsonObject())
							.put("mainResourceName", "")
					);
				}
			}
		});
	}

	@Override
	public Optional<String> getMainRepositoryName(){
		return Optional.ofNullable(MongoDbConf.getInstance().getCollection());
	}
}
