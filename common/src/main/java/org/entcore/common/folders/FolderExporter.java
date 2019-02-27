package org.entcore.common.folders;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FolderExporter {
	private static final Logger log = LoggerFactory.getLogger(FolderExporter.class);

	public static class FolderExporterContext {
		public final String basePath;
		public Map<String, String> namesByIds = new HashMap<>();
		public Map<String, List<JsonObject>> docByFolders = new HashMap<>();
		public Set<String> folders = new HashSet<>();
		public JsonArray errors = new JsonArray();

		public FolderExporterContext(String basePath) {
			super();
			this.basePath = basePath;
		}

	}

	protected final FileSystem fs;
	protected final Storage storage;
	protected final boolean throwErrors;

	public FolderExporter(Storage storage, FileSystem fs) {
		this(storage, fs, true);
	}

	public FolderExporter(Storage storage, FileSystem fs, boolean throwErrors) {
		this.fs = fs;
		this.storage = storage;
		this.throwErrors = throwErrors;
	}

	private void buildPath(List<JsonObject> rows, JsonObject current, List<String> paths) {
		String parent = DocumentHelper.getParent(current);
		if (!StringUtils.isEmpty(parent)) {
			Optional<JsonObject> founded = rows.stream().filter(r -> parent.equals(DocumentHelper.getId(r)))
					.findFirst();
			if (founded.isPresent()) {
				String name = cleanName(founded.get());
				buildPath(rows, founded.get(), paths);
				paths.add(name);
			}
		}
	}

	private void buildMapping(List<JsonObject> rows, FolderExporterContext context) {
		for (JsonObject row : rows) {
			context.namesByIds.put(row.getString("_id"), cleanName(row));
		}

		for (JsonObject row : rows) {
			if (DocumentHelper.isFile(row)) {
				List<String> paths = new ArrayList<>();
				buildPath(rows, row, paths);
				//
				String folderPath = paths.stream().reduce("", (t, u) -> t + File.separator + u);
				String fullFolderPath = Paths.get(context.basePath, folderPath).normalize().toString();

				context.folders.add(fullFolderPath);
				context.docByFolders.putIfAbsent(fullFolderPath, new ArrayList<>());
				context.docByFolders.get(fullFolderPath).add(row);
			}
		}
	}

	private Future<Void> mkdirs(FolderExporterContext context) {
		Set<String> uniqFolders = new HashSet<String>();
		for (String f1 : context.folders) {
			boolean ignore = false;
			for (String f2 : context.folders) {
				// if one folder f2 include this one => ignore f1
				if (!f1.equals(f2) && f2.contains(f1)) {
					ignore = true;
				}
			}
			if (!ignore) {
				uniqFolders.add(f1);
			}
		}
		//
		Future<Void> futureRoot = Future.future();
		fs.mkdirs(context.basePath, futureRoot.completer());
		return futureRoot.compose(resRoot -> {
			log.debug("Folder Root creation succeed: " + "/" + context.basePath);
			@SuppressWarnings("rawtypes")
			List<Future> futures = new ArrayList<>();
			for (String path : uniqFolders) {
				Future<Void> future = Future.future();
				fs.mkdirs(path, res -> {
					log.debug("Folder creation result: " + "/" + res.succeeded() + "/" + path);
					future.completer().handle(res);
				});
				futures.add(future);
			}
			return CompositeFuture.all(futures).map(res -> null);
		});
	}

	private static String cleanName(JsonObject doc) {
		String name = DocumentHelper.getFileName(doc, "undefined");
		return name.replaceAll("/", "_").replaceAll("\\\\", "_").trim();
	}

	private CompositeFuture copyFiles(FolderExporterContext context) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = new ArrayList<>();
		for (String folderPath : context.docByFolders.keySet()) {
			Future<JsonObject> future = Future.future();
			futures.add(future);
			List<JsonObject> docs = context.docByFolders.get(folderPath);
			//
			JsonObject nameByFileId = new JsonObject();
			Map<String, Integer> nameCount = new HashMap<>();
			for (JsonObject doc : docs) {
				String fileId = DocumentHelper.getFileId(doc);
				String name = cleanName(doc);
				Integer count = nameCount.merge(name, 1, Integer::sum) - 1;
				// if name already exists ... add suffix
				if (count > 0) {
					if (name.contains(".")) {
						name = name.substring(0, name.indexOf(".")) + "_" + count + name.substring(name.indexOf("."));
					} else {
						name = name + "_" + count;
					}
				}
				name = StringUtils.replaceForbiddenCharacters(name);
				nameByFileId.put(fileId, name);
				context.namesByIds.put(fileId,name);
			}
			//
			String[] ids = nameByFileId.fieldNames().stream().toArray(String[]::new);
			storage.writeToFileSystem(ids, folderPath, nameByFileId, res -> {
				if ("ok".equals(res.getString("status"))) {
					future.complete(res);
				} else if (throwErrors) {
					future.fail(res.getString("error"));
				} else {
					context.errors.addAll(res.getJsonArray("errors"));
					future.complete();
					log.error("Failed to export file : " + folderPath + " - " + nameByFileId + "- "
							+ new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(ids)).encode() + " - "
							+ res.encode());
				}
			});
		}
		return CompositeFuture.all(futures);
	}

	public Future<FolderExporterContext> export(FolderExporterContext context, List<JsonObject> rows) {
		this.buildMapping(rows, context);
		return this.mkdirs(context).compose(res -> {
			return this.copyFiles(context);
		}).map(context);
	}
}
