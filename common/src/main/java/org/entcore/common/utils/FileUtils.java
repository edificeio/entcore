/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.common.utils;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


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

	public static void unzip(final String zipFilename, final String destDirname, final Handler<Either<String, Void>> handler)
	{
		Thread t = new Thread(new Runnable()
		{
			private final String zip  = zipFilename;
			private final String dest = destDirname;
			private final Handler hnd = handler;

			@Override
			public void run()
			{
				System.out.println();
				System.out.println();
				System.out.println("THREAD");
				System.out.println();
				System.out.println();
				try
				{
					FileUtils.unzip(zip, dest);
					hnd.handle(new Either.Right<String, Void>(null));
				}
				catch(IOException e)
				{
					hnd.handle(new Either.Left<String, Void>(e.getMessage()));
				}
			}
		});

		t.start();
	}

	public static String getParentPath(String path) {
		return Paths.get(path).getParent().toString();
	}

	public static String getFilename(String name) {
		return StringUtils.substringAfterLast(name, File.separator);
	}

	public static String getPathWithoutFilename(String name) {
		return StringUtils.substringBeforeLast(name, File.separator);
	}

	public static String stripExtension(String name) {
		return StringUtils.substringBeforeLast(name, ".");
	}

	public static Optional<String> getFileExtension(String name) {
		String ext = StringUtils.substringAfter(name, ".");
		return Optional.ofNullable(StringUtils.isEmpty(ext) ? null : ext);
	}

}
