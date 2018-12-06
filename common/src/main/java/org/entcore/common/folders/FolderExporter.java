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
				String name = DocumentHelper.getName(founded.get(), "undefined");
				buildPath(rows, founded.get(), paths);
				paths.add(name);
			}
		}
	}

	private void buildMapping(List<JsonObject> rows, FolderExporterContext context) {
		for (JsonObject row : rows) {
			context.namesByIds.put(row.getString("_id"), row.getString("name", "undefined"));
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

	private CompositeFuture mkdirs(FolderExporterContext context) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = new ArrayList<>();
		{
			Future<Void> future = Future.future();
			fs.mkdirs(context.basePath, future.completer());
			futures.add(future);
		}
		for (String path : context.folders) {
			Future<Void> future = Future.future();
			futures.add(future);
			fs.mkdirs(path, future.completer());
		}
		return CompositeFuture.all(futures);
	}
	private String cleanName(JsonObject doc) {
		String name = DocumentHelper.getName(doc, "undefined");
		return name.replace(File.separatorChar, ' ').replace('\\', ' ');
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
				nameByFileId.put(fileId, name);
			}
			//
			String[] ids = nameByFileId.fieldNames().stream().toArray(String[]::new);
			storage.writeToFileSystem(ids, folderPath, nameByFileId, res -> {
				if ("ok".equals(res.getString("status"))) {
					future.complete(res);
				} else if (throwErrors) {
					future.fail(res.getString("error"));
				} else {
					future.complete(new JsonObject());
					log.error("Failed to export file : "
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
