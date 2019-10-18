package org.entcore.common.folders;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.FileStats;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.service.impl.AbstractRepositoryEvents;
import org.entcore.common.service.impl.MongoDbRepositoryEvents;

import io.vertx.core.CompositeFuture;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.Future;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FolderImporter
{
	private static final Logger log = LoggerFactory.getLogger(FolderImporter.class);

	public static class FolderImporterContext
	{
		public final String										basePath;
		public final String										userId;
		public final String										userName;
		public final boolean									doCommitToDocumentsCollection;
		public String													importIntoFolderId = null;

		public Map<String, String>						oldIdsToNewIds = new HashMap<String, String>();
		public Map<String, String>						fileOldIdsToNewIds = new HashMap<String, String>();

		public Map<String, List<JsonObject>>	oldIdsToChildren = new HashMap<String, List<JsonObject>>();

		public JsonArray											updatedDocs = new JsonArray();
		public JsonArray											customDocs = new JsonArray();

		public JsonArray 											errors = new JsonArray();

		public FolderImporterContext(String basePath, String userId, String userName)
		{
			this(basePath, userId, userName, true, null);
		}

		public FolderImporterContext(String basePath, String userId, String userName, boolean doCommitToDocumentsCollection)
		{
			this(basePath, userId, userName, doCommitToDocumentsCollection, null);
		}

		public FolderImporterContext(String basePath, String userId, String userName, boolean doCommitToDocumentsCollection, String importIntoFolder)
		{
			this.basePath = basePath;
			this.userId = userId;
			this.userName = userName;
			this.doCommitToDocumentsCollection = doCommitToDocumentsCollection;

			if(importIntoFolder != null)
			{
				this.importIntoFolderId = UUID.randomUUID().toString();

				JsonObject folder = DocumentHelper.initFolder(null, userId, userName, importIntoFolder, "media-library");
				DocumentHelper.setId(folder, this.importIntoFolderId);

				this.customDocs.add(folder);
			}
		}

		public synchronized void addError(String documentId, String fileId, String message, String details)
		{
			this.errors.add(
				new JsonObject()
					.put("docId", documentId)
					.put("fileId", fileId)
					.put("message", message)
					.put("details", details)
			);

			if(this.updatedDocs != null)
			{
				for(int i = this.updatedDocs.size(); i-- > 0;)
					if(DocumentHelper.getId(this.updatedDocs.getJsonObject(i)).equals(documentId))
					{
						this.updatedDocs.remove(i);
						break;
					}
			}
		}

		public synchronized void addUpdatedDoc(JsonObject doc)
		{
			this.updatedDocs.add(doc);
		}

		public synchronized JsonArray getFinalisedDocs()
		{
			return this.updatedDocs.addAll(this.customDocs);
		}

		public synchronized void addFileLink(String oldId, String newId)
		{
			this.fileOldIdsToNewIds.put(oldId, newId);
		}

	}

	protected final FileSystem fs;
	protected final EventBus eb;
	protected final boolean throwErrors;
	protected final Pattern uuidPattern = Pattern.compile(StringUtils.UUID_REGEX);

	public FolderImporter(FileSystem fs, EventBus eb)
	{
		this(fs, eb, true);
	}

	public FolderImporter(FileSystem fs, EventBus eb, boolean throwErrors)
	{
		this.fs = fs;
		this.eb = eb;
		this.throwErrors = throwErrors;
	}

	private JsonObject sanitiseDocData(FolderImporterContext context, JsonObject docData)
	{
		docData.put("owner", context.userId);
		docData.put("ownerName", context.userName);
		docData.remove("localArchivePath");

		// Imported files are set to private
		DocumentHelper.setShared(DocumentHelper.removeShares(docData), false);

		return docData;
	}

	private Future<JsonObject> commitToMongo(FolderImporterContext context, Future beforeCommit)
	{
		FolderImporter self = this;
		Future<JsonObject> promise = Future.future();

		beforeCommit.setHandler(new Handler<AsyncResult<FolderImporterContext>>()
		{
			@Override
			public void handle(AsyncResult<FolderImporterContext> mappedContext)
			{
				if(context.doCommitToDocumentsCollection == false)
				{
					int nbFiles = context.fileOldIdsToNewIds.size();
					int nbFilesDup = 0;

					for(Map.Entry<String, String> entry : context.fileOldIdsToNewIds.entrySet())
						if(entry.getValue() != entry.getKey())
							++nbFilesDup;

					JsonObject rapport =
						new JsonObject()
							.put("resourcesNumber", Integer.toString(nbFiles))
							.put("duplicatesNumber", Integer.toString(nbFilesDup))
							.put("errorsNumber", Integer.toString(context.errors.size()));

					promise.complete(rapport);

					return;
				}

				JsonArray updatedDocs = context.getFinalisedDocs();
				final int nbErrors = (mappedContext.succeeded() == false) ? context.errors.size() : 0;

				List<JsonObject> importList = new ArrayList<JsonObject>(updatedDocs.size());

				for(int i = updatedDocs.size(); i-- > 0;)
					importList.add(self.sanitiseDocData(context, updatedDocs.getJsonObject(i)));

				MongoDbRepositoryEvents.importDocuments("documents", importList, "", new Handler<JsonObject>()
				{
					@Override
					public void handle(JsonObject result)
					{
						JsonObject rapport = result.getJsonObject("rapport");
						rapport.put("errorsNumber", Integer.toString(Integer.parseInt(rapport.getString("errorsNumber")) + nbErrors));

						JsonObject idsMap = result.getJsonObject("idsMap");
						context.oldIdsToNewIds = new HashMap<String, String>();

						for(String oldId : idsMap.fieldNames())
							context.oldIdsToNewIds.put(oldId, idsMap.getString(oldId));

						promise.complete(rapport);
					}
				});
			}
		});

		return promise;
	}

	private void bufferToStorage(FolderImporterContext context, JsonObject document, Buffer buff, Future<Void> promise)
	{
		final String docId = DocumentHelper.getId(document);
		final String fileId = DocumentHelper.getFileId(document);
		String contentType = DocumentHelper.getContentType(document);
		final String fileName = DocumentHelper.getFileName(document, fileId);

		if(contentType == null || contentType.trim().equals(""))
		{
			byte fileBytes[] = buff.getBytes();

			try
			{
				InputStream is = new BufferedInputStream(new ByteArrayInputStream(fileBytes));
				contentType = URLConnection.guessContentTypeFromStream(is);
			}
			catch(IOException e)
			{
				context.addError(docId, fileId, "Failed to read file MIME type", e.getMessage());
				promise.fail(e);
				return;
			}

			DocumentHelper.setContentType(document, contentType);
		}

		FolderImporter self = this;
		JsonObject importParams = new JsonObject()
																	.put("action", "importDocument")
																	.put("buffer", buff.getBytes())
																	.put("oldFileId", fileId)
																	.put("fileName", fileName)
																	.put("contentType", contentType);

		this.eb.send("org.entcore.workspace", importParams, new Handler<AsyncResult<Message<JsonObject>>>()
		{
			@Override
			public void handle(AsyncResult<Message<JsonObject>> message)
			{
				if(message.succeeded() == false)
				{
					String error = message.cause().getMessage();
					context.addError(docId, fileId, "Failed to import file", error);
					promise.fail(new RuntimeException(message.cause()));
					return;
				}

				JsonObject writtenFile = message.result().body();
				if(writtenFile.getString("status").equals("ok"))
				{
					final String storageId = DocumentHelper.getId(writtenFile);

					context.addFileLink(fileId, storageId);
					DocumentHelper.setFileId(document, storageId);

					JsonObject thumbnailsObj = DocumentHelper.setThumbnails(new JsonObject(), DocumentHelper.getThumbnails(document));
					JsonObject params = new JsonObject()
																.put("action", "createThumbnails")
																.put("fileDocument", writtenFile)
																.put("thumbnails", thumbnailsObj);

					self.eb.send("org.entcore.workspace", params, new Handler<AsyncResult<Message<JsonObject>>>()
					{
						@Override
						public void handle(AsyncResult<Message<JsonObject>> thumbMessage)
						{
							if(thumbMessage.succeeded() == false)
							{
								String error = thumbMessage.cause().getMessage();
								context.addError(docId, fileId, "Failed to generate thumbnails", error);
								promise.fail(new RuntimeException(thumbMessage.cause()));
							}
							else
							{
								JsonObject body = thumbMessage.result().body();
					
								if(body.getString("status").equals("ok") == true)
								{
									DocumentHelper.mergeMetadata(body, document);

									DocumentHelper.setThumbnails(document, DocumentHelper.getThumbnails(body.getJsonObject("result")));
									promise.complete();
								}
								else
								{
									String error = body.getString("message");
									context.addError(docId, fileId, "Failed to generate thumbnails", error);
									promise.fail(new RuntimeException(error));
								}
							}
						}
					});
				}
				else
				{
					String error = writtenFile.getString("message");
					context.addError(docId, fileId, "Failed to write the archived file", error);
					promise.fail(new RuntimeException(error));
				}
			}
		});
	}

	private void importFile(FolderImporterContext context, JsonObject document, String filePath, Future<Void> promise)
	{
		FolderImporter self = this;

		final String docId = DocumentHelper.getId(document);
		final String fileId = DocumentHelper.getFileId(document);

		this.fs.readFile(filePath, new Handler<AsyncResult<Buffer>>()
		{
			@Override
			public void handle(AsyncResult<Buffer> buff)
			{
				if(buff.succeeded() == true)
					self.bufferToStorage(context, document, buff.result(), promise);
				else
				{
					context.addError(docId, fileId, "Failed to open the archived file", buff.cause().getMessage());
					promise.fail(buff.cause());
				}
			}
		});
	}

	private void removeParent(FolderImporterContext context, List<JsonObject> orphans, Set<JsonObject> processed)
	{
		for(JsonObject child : orphans)
		{
			String id = DocumentHelper.getId(child);

			DocumentHelper.setParent(child, null);

			List<JsonObject> orphanOwnChildren = context.oldIdsToChildren.get(id);
			if(orphanOwnChildren != null && processed.add(child) == true)
				this.removeParent(context, orphanOwnChildren, processed);
		}
	}

	private void moveOrphansToRoot(FolderImporterContext context)
	{
		Map<String, String> allDocs = context.oldIdsToNewIds;
		Map<String, List<JsonObject>> parents = context.oldIdsToChildren;

		Set<JsonObject> processedOrphans = new HashSet<JsonObject>();

		for(String parentId : parents.keySet())
			if(allDocs.containsKey(parentId) == false)
				this.removeParent(context, parents.get(parentId), processedOrphans);
	}

	private CompositeFuture importDocuments(FolderImporterContext context)
	{
		ArrayList<Future> futures = new ArrayList<Future>(context.updatedDocs.size());

		for(int i =  context.updatedDocs.size(); i-- > 0;)
		{
			JsonObject fileDoc = context.updatedDocs.getJsonObject(i);

			String fileDocId = DocumentHelper.getId(fileDoc);
			context.oldIdsToNewIds.put(fileDocId, fileDocId);

			Future<Void> future = Future.future();
			futures.add(future);

			if(DocumentHelper.isFolder(fileDoc) == false)
				this.importFile(context, fileDoc, context.basePath + File.separator + fileDoc.getString("localArchivePath"), future);
			else
				// Folders don't exist in the vertx filesystem, only in the mongo docs, so do nothing
				future.complete();

			// Build the parent mapping
			if(DocumentHelper.hasParent(fileDoc) == true)
			{
				String parentId = DocumentHelper.getParent(fileDoc);

				if(context.oldIdsToChildren.containsKey(parentId) == false)
				{
					LinkedList<JsonObject> children =	new LinkedList<JsonObject>();

					children.add(fileDoc);
					context.oldIdsToChildren.put(parentId, children);
				}
				else
					context.oldIdsToChildren.get(parentId).add(fileDoc);
			}
		}

		this.moveOrphansToRoot(context);

		return CompositeFuture.join(futures);
	}

	/**
		* Imports files from a workspace export style folder.
		* @param context				A fresh FolderImporterContext
		* @param fileDocuments	The contents of the main workspace export file
		*/
	public void importFoldersWorkspaceFormat(FolderImporterContext context, JsonArray fileDocuments, Handler<JsonObject> handler)
	{
		context.updatedDocs = fileDocuments;
		Future<JsonObject> doImport = this.commitToMongo(context, this.importDocuments(context).map(context));

		doImport.setHandler(new Handler<AsyncResult<JsonObject>>()
		{
			@Override
			public void handle(AsyncResult<JsonObject> res)
			{
				handler.handle(res.result());
			}
		});
	}

	private Future importFlatFiles(FolderImporterContext context)
	{
		Future<FolderImporterContext> importDone = Future.future().map(context);
		FolderImporter self = this;

		Pattern fileId = Pattern.compile(StringUtils.UUID_REGEX);

		this.fs.readDir(context.basePath, new Handler<AsyncResult<List<String>>>()
		{
			@Override
			public void handle(AsyncResult<List<String>> result)
			{
				if(result.succeeded() == false)
				{
					context.addError(null, null, "Failed to read document folder", result.cause().getMessage());
					throw new RuntimeException(result.cause());
				}
				else
				{
					List<String> filesInDir = result.result();

					LinkedList<Future> futures = new LinkedList<Future>();

					for(String filePath : filesInDir)
					{
						Future<Void> future = Future.future();
						futures.add(future);

						String fileTrunc = FileUtils.getFilename(filePath);
						Matcher m = fileId.matcher(fileTrunc);
						if(m.find() == false)
						{
							String error = "Filename " + fileTrunc + "does not contain the file id";
							context.addError(null, null, error, null);
							future.fail(new RuntimeException(error));

							continue;
						}

						String fileName;

						// File name format should be <FILE NAME>_<FILE ID><EXTENSION> or <FILE ID>_<FILE NAME><EXTENSION>
						if(m.start() > 0)
							fileName = fileTrunc.substring(0, m.start() - 1) + fileTrunc.substring(m.end());
						else
							fileName = fileTrunc.substring(m.end() + 1);

						String fileId = m.group();

						// Sanitise file data
						JsonObject fileDocument = DocumentHelper.initFile(null, context.userId, context.userName, fileName, "media-library");
						DocumentHelper.setFileName(fileDocument, fileName);
						DocumentHelper.setId(fileDocument, fileId);

						if(context.importIntoFolderId != null)
							DocumentHelper.setParent(fileDocument, context.importIntoFolderId);
						else
							DocumentHelper.setProtected(fileDocument, true);

						context.addUpdatedDoc(fileDocument);
						self.importFile(context, fileDocument, filePath, future);
					}

					CompositeFuture.join(futures).setHandler(new Handler<AsyncResult<CompositeFuture>>()
					{
						@Override
						public void handle(AsyncResult<CompositeFuture> res)
						{
							if(res.succeeded() == true)
								importDone.complete();
							else
								importDone.fail(res.cause());
						}
					});
				}
			}
		});

		return importDone;
	}

	/**
		* Imports files from a generic resource folder with all files directly inside it.
		* @param context				A fresh FolderImporterContext
		*/
	public void importFoldersFlatFormat(FolderImporterContext context, Handler<JsonObject> handler)
	{
		Future<JsonObject> doImport = this.commitToMongo(context, this.importFlatFiles(context).map(context));
		doImport.setHandler(new Handler<AsyncResult<JsonObject>>()
		{
			@Override
			public void handle(AsyncResult<JsonObject> res)
			{
				handler.handle(res.result());
			}
		});
	}

	/**
		* Change old file ids in all data documents to the new ids from the import
		* @param context				A FolderImporterContext that has been used for an import
		* @param dataToUpdate		The list of data objects to update
		*/
	public void applyFileIdsChange(FolderImporterContext context, List<JsonObject> dataToUpdate)
	{
		for(JsonObject data : dataToUpdate)
			AbstractRepositoryEvents.applyIdsChange(data, context.doCommitToDocumentsCollection == false ? context.fileOldIdsToNewIds : context.oldIdsToNewIds);
	}
}
