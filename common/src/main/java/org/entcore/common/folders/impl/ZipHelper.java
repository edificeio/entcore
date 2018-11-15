package org.entcore.common.folders.impl;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.Deflater;

import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.Zip;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

class ZipHelper {
	static class ZipContext {
		String basePath;
		String baseName;
		String zipFullPath;
		String zipName;
		Map<String, String> namesByIds = new HashMap<>();
		Map<String, List<JsonObject>> docByFolders = new HashMap<>();
		Set<String> folders = new HashSet<>();
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

	private void buildMapping(List<JsonObject> rows, ZipContext context) {
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

	private CompositeFuture mkdirs(ZipContext context) {
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

	private CompositeFuture copyFiles(ZipContext context) {
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
				String name = DocumentHelper.getName(doc, "undefined");
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
				} else {
					future.fail(res.getString("error"));
				}
			});
		}
		return CompositeFuture.all(futures);
	}

	private Future<JsonObject> createZip(ZipContext context) {
		Future<JsonObject> future = Future.future();
		Zip.getInstance().zipFolder(context.basePath, context.zipFullPath, true, Deflater.NO_COMPRESSION, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				future.complete(res.body());
			} else {
				future.fail(res.body().getString("message"));
			}
		});
		return future;
	}

	private final FileSystem fs;
	private final Storage storage;

	public ZipHelper(Storage storage, FileSystem fs) {
		this.fs = fs;
		this.storage = storage;
	}

	public Future<ZipContext> build(Optional<JsonObject> root, List<JsonObject> rows) {
		ZipContext context = new ZipContext();
		UUID uuid = UUID.randomUUID();
		context.baseName = root.isPresent() ? root.get().getString("name", "archive") : "archive";
		context.basePath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString(), context.baseName)
				.normalize().toString();
		context.zipName = context.baseName + ".zip";
		context.zipFullPath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString(), context.zipName)
				.normalize().toString();
		this.buildMapping(rows, context);
		return this.mkdirs(context).compose(res -> {
			return this.copyFiles(context);
		}).compose(res -> {
			return this.createZip(context);
		}).map(res -> context);
	}

	public Future<Void> send(HttpServerRequest req, ZipContext context) {
		Future<Void> future = Future.future();
		final HttpServerResponse resp = req.response();
		resp.putHeader("Content-Disposition", "attachment; filename=\"" + context.zipName + "\"");
		resp.putHeader("Content-Type", "application/octet-stream");
		resp.putHeader("Content-Description", "File Transfer");
		resp.putHeader("Content-Transfer-Encoding", "binary");
		resp.sendFile(context.zipFullPath, future.completer());
		return future;
	}

	public Future<Void> buildAndSend(JsonObject root, List<JsonObject> rows, HttpServerRequest req) {
		return this.build(Optional.ofNullable(root), rows).compose(res -> {
			return this.send(req, res);
		});
	}

	public Future<Void> buildAndSend(List<JsonObject> rows, HttpServerRequest req) {
		return this.build(Optional.empty(), rows).compose(res -> {
			return this.send(req, res);
		});
	}
}
