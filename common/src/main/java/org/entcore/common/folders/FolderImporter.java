package org.entcore.common.folders;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.FileStats;

import io.vertx.core.CompositeFuture;
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
		public final String basePath;

		public Map<String, String>	oldIdsToNewIds = new HashMap<String, String>();
		public JsonArray						updatedDocs;

		public JsonArray 						errors = new JsonArray();

		public FolderImporterContext(String basePath)
		{
			super();
			this.basePath = basePath;
		}

		public void addError(String documentId, String fileId, String message, String details)
		{
			this.errors.add(
				new JsonObject()
					.put("docId", documentId)
					.put("fileId", fileId)
					.put("message", message)
					.put("details", details)
			);

			for(int i = this.updatedDocs.size(); i-- > 0;)
				if(DocumentHelper.getId(this.updatedDocs.getJsonObject(i)).equals(documentId))
				{
					this.updatedDocs.remove(i);
					break;
				}
		}

	}

	protected final FileSystem fs;
	protected final Storage storage;
	protected final boolean throwErrors;

	public FolderImporter(Storage storage, FileSystem fs)
	{
		this(storage, fs, true);
	}

	public FolderImporter(Storage storage, FileSystem fs, boolean throwErrors)
	{
		this.fs = fs;
		this.storage = storage;
		this.throwErrors = throwErrors;
	}

	private void bufferToStorage(FolderImporterContext context, JsonObject document, Buffer buff, boolean fileAlreadyExists, Future<Void> promise)
	{
		final String docId = DocumentHelper.getId(document);
		final String fileId = DocumentHelper.getFileId(document);
		final String contentType = DocumentHelper.getContentType(document);
		final String fileName = DocumentHelper.getFileName(document, fileId);

		Handler<JsonObject> hnd = new Handler<JsonObject>()
		{
			@Override
			public void handle(JsonObject status)
			{
				if(status.getString("status").equals("ok"))
				{
					final String storageId = DocumentHelper.getId(status);

					DocumentHelper.setFileId(document, storageId);
					context.oldIdsToNewIds.put(fileId, storageId);

					promise.complete();
				}
				else
				{
					String error = status.getString("message");
					context.addError(docId, fileId, "Failed to write the archived file", error);
					promise.fail(new RuntimeException(error));
				}
			}
		};

		// If the file already exists, duplicate it with a new id, else keep the old id
		if(fileAlreadyExists == true)
			this.storage.writeBuffer(buff, contentType, fileName, hnd);
		else
			this.storage.writeBuffer(fileId, buff, contentType, fileName, hnd);
	}

	private void importFile(FolderImporterContext context, JsonObject document, Future<Void> promise)
	{
		FolderImporter self = this;

		final String filePath = document.getString("localArchivePath");
		final String docId = DocumentHelper.getId(document);
		final String fileId = DocumentHelper.getFileId(document);

		// Start reading the file to import
		Future<Buffer> readFileFuture = Future.future();
		final String backupFilePath = context.basePath + File.separator + filePath;
		this.fs.readFile(backupFilePath, new Handler<AsyncResult<Buffer>>()
		{
			@Override
			public void handle(AsyncResult<Buffer> contents)
			{
				if(contents.succeeded() == true)
					readFileFuture.complete(contents.result());
				else
				{
					context.addError(docId, fileId, "Failed to open the archived file", contents.cause().getMessage());
					readFileFuture.fail(new RuntimeException(contents.cause()));
				}
			}
		});

		// Check whether the file already exists
		this.storage.fileStats(fileId, new Handler<AsyncResult<FileStats>>()
		{
			@Override
			public void handle(AsyncResult<FileStats> res)
			{
				// Wait until the file is read
				readFileFuture.setHandler(new Handler<AsyncResult<Buffer>>()
				{
					@Override
					public void handle(AsyncResult<Buffer> buff)
					{
						if(buff.succeeded() == true)
							self.bufferToStorage(context, document, buff.result(), res.succeeded(), promise);
						else
							promise.fail(buff.cause());
					}
				});
			}
		});
	}

	private CompositeFuture importDocuments(FolderImporterContext context)
	{
		ArrayList<Future> futures = new ArrayList<Future>(context.updatedDocs.size());

		for(int i =  context.updatedDocs.size(); i-- > 0;)
		{
			JsonObject fileDoc = context.updatedDocs.getJsonObject(i);

			Future<Void> future = Future.future();
			futures.add(future);
			
			if(DocumentHelper.getType(fileDoc).equals(FolderManager.FOLDER_TYPE) == false)
				this.importFile(context, fileDoc, future);
			else
				// Folders don't exist in the vertx filesystem, only in the mongo docs, so do nothing
				future.complete();
		}

		return CompositeFuture.join(futures);
	}

	public Future<FolderImporterContext> importFolders(FolderImporterContext context, JsonArray fileDocuments)
	{
		context.updatedDocs = fileDocuments;
		return this.importDocuments(context).map(context);
	}
}
