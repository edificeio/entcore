/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.utils;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;


public final class FileUtils {

	private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

	private FileUtils(){}

	public static void deleteImportPath(final Vertx vertx, final String path) {
		deleteImportPath(vertx, path, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.failed()) {
					log.error("Error deleting import : " + path, event.cause());
				}
			}
		});
	}

	public static void deleteImportPath(final Vertx vertx, final String path, final Handler<AsyncResult<Void>> handler) {
		vertx.fileSystem().exists(path, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> event) {
				if (event.succeeded()) {
					if (Boolean.TRUE.equals(event.result())) {
						vertx.fileSystem().deleteRecursive(path, true, handler);
					} else {
						handler.handle(new DefaultAsyncResult<>((Void) null));
					}
				} else {
					handler.handle(new DefaultAsyncResult<Void>(event.cause()));
				}
			}
		});
	}

	private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {
		final Path path = Paths.get(zipFilename);
		final URI uri = URI.create("jar:file:" + path.toUri().getPath());

		final Map<String, String> env = new HashMap<>();
		if (create) {
			env.put("create", "true");
		}
		return FileSystems.newFileSystem(uri, env);
	}

	public static void unzip(String zipFilename, String destDirname) throws IOException {
		final Path destDir = Paths.get(destDirname);
		if(Files.notExists(destDir)){
			Files.createDirectories(destDir);
		}

		try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)) {
			final Path root = zipFileSystem.getPath("/");

			Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					final Path destFile = Paths.get(destDir.toString(), file.toString());
					Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
					if(Files.notExists(dirToCreate)){
						Files.createDirectory(dirToCreate);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static String getParentPath(String path) {
		return Paths.get(path).getParent().toString();
	}

}
