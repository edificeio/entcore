package org.entcore.common.folders.impl;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.Deflater;

import org.entcore.common.folders.FolderExporter;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.Zip;

import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

class FolderExporterZip extends FolderExporter {
	static class ZipContext extends FolderExporterContext {
		final String zipFullPath;
		final String zipName;
		final String baseName;
		final String rootBase;

		public ZipContext(String rootBase, String basePath, String baseName) {
			super(basePath);
			this.rootBase = rootBase;
			this.baseName = baseName;
			this.zipName = this.baseName + ".zip";
			// basePath is the root folder => so up and set filename
			this.zipFullPath = Paths.get(this.basePath).resolve("..").resolve(this.zipName).normalize().toString();
		}
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

	public FolderExporterZip(Storage storage, FileSystem fs) {
		super(storage, fs);
	}

	public FolderExporterZip(Storage storage, FileSystem fs, boolean throwErrors) {
		super(storage, fs, throwErrors);
	}

	public Future<ZipContext> exportToZip(Optional<JsonObject> root, List<JsonObject> rows) {
		UUID uuid = UUID.randomUUID();
		String baseName = root.isPresent() ? root.get().getString("name", "archive") : "archive";
		String rootBase = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString()).normalize().toString();
		String basePath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString(), baseName).normalize()
				.toString();
		ZipContext context = new ZipContext(rootBase, basePath, baseName);
		return this.export(context, rows).compose(res -> {
			return this.createZip(context);
		}).map(res -> context);
	}

	public Future<Void> sendZip(HttpServerRequest req, ZipContext context) {
		Future<Void> future = Future.future();
		final HttpServerResponse resp = req.response();
		resp.putHeader("Content-Disposition", "attachment; filename=\"" + context.zipName + "\"");
		resp.putHeader("Content-Type", "application/octet-stream");
		resp.putHeader("Content-Description", "File Transfer");
		resp.putHeader("Content-Transfer-Encoding", "binary");
		resp.sendFile(context.zipFullPath, future.completer());
		return future;
	}

	public Future<ZipContext> exportAndSendZip(JsonObject root, List<JsonObject> rows, HttpServerRequest req, boolean clean) {
		return this.exportToZip(Optional.ofNullable(root), rows).compose(res -> {
			return this.sendZip(req, res).map((r)->res);
		}).compose(res->{
			if(clean){
				return removeZip(res);
			}else{
				return Future.succeededFuture(res);
			}
		});
	}

	public Future<ZipContext> exportAndSendZip(List<JsonObject> rows, HttpServerRequest req, boolean clean) {
		return this.exportToZip(Optional.empty(), rows).compose(res -> {
			return this.sendZip(req, res).map((r)->res);
		}).compose(res->{
			if(clean){
				return removeZip(res);
			}else{
				return Future.succeededFuture(res);
			}
		});
	}

	public Future<ZipContext> removeZip(ZipContext context){
		Future<ZipContext> future = Future.future();
		this.fs.deleteRecursive(context.rootBase,true, resRmDir->{
			future.complete();
		});
		return future;
	}
}
