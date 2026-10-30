package org.entcore.feeder.utils;

import io.vertx.core.json.JsonArray;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

/**
 * Safe removal of the AAF directories created through the "upload-aaf" feature (integration tests).
 * A directory is only removable when it directly sits under the import root, is not a symbolic link and
 * contains the {@link #UPLOAD_MARKER} file written at upload time : a directory supplied by an academy can
 * therefore never be erased, even if it is listed in the import directories file.
 */
public final class AAFUploadDirectories {
	/** Name of the file written in every uploaded directory, which alone makes it removable. */
	public static final String UPLOAD_MARKER = ".uploaded-by-it";
	/** Name of the file listing the sub directories to import. */
	public static final String IMPORT_DIRECTORIES_JSON = "importDirectories.json";
	private static final Pattern CLEANABLE_SUB_PATH = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

	private AAFUploadDirectories() {}

	/** Raised when a removal is refused, with the HTTP status that best describes the reason. */
	public static class RefusedException extends Exception {
		private final int statusCode;

		public RefusedException(final int statusCode, final String message) {
			super(message);
			this.statusCode = statusCode;
		}

		public int getStatusCode() {
			return statusCode;
		}
	}

	/** @return {@code true} iff the sub path is a single, plain directory name (no separator, no dot). */
	public static boolean isCleanableSubPath(final String subPath) {
		return subPath != null && CLEANABLE_SUB_PATH.matcher(subPath).matches();
	}

	/**
	 * Removes the uploaded directory {@code subPath} of {@code root}, or every uploaded directory of {@code root}
	 * when {@code subPath} is {@code null}, and drops them from the import directories file.
	 * Removing a directory which does not exist is not an error (the operation is idempotent).
	 * @param root The import root of a feeder
	 * @param subPath The directory to remove, or {@code null} for all the uploaded ones
	 * @return The names of the directories that have been removed
	 * @throws RefusedException If the sub path is not acceptable or is not a directory created by an upload
	 */
	public static List<String> clean(final Path root, final String subPath) throws RefusedException, IOException {
		if (!Files.isDirectory(root)) {
			return new ArrayList<>();
		}
		final Path realRoot = root.toRealPath();
		final List<String> removed = new ArrayList<>();
		if (subPath != null) {
			if (!isCleanableSubPath(subPath)) {
				throw new RefusedException(400, "subPath.not.acceptable");
			}
			if (deleteUploadedDirectory(realRoot, subPath)) {
				removed.add(subPath);
			}
			removeFromImportDirectories(realRoot, singletonList(subPath));
		} else {
			for (String name : listUploadedDirectories(realRoot)) {
				if (deleteUploadedDirectory(realRoot, name)) {
					removed.add(name);
				}
			}
			removeFromImportDirectories(realRoot, removed);
		}
		return removed;
	}

	/** @return The names of the directories directly under {@code realRoot} which have been created by an upload. */
	static List<String> listUploadedDirectories(final Path realRoot) throws IOException {
		final List<String> names = new ArrayList<>();
		try (DirectoryStream<Path> children = Files.newDirectoryStream(realRoot)) {
			for (Path child : children) {
				final String name = child.getFileName().toString();
				if (isCleanableSubPath(name) && isUploadedDirectory(child)) {
					names.add(name);
				}
			}
		}
		return names;
	}

	private static boolean isUploadedDirectory(final Path directory) {
		return !Files.isSymbolicLink(directory)
				&& Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
				&& Files.isRegularFile(directory.resolve(UPLOAD_MARKER), LinkOption.NOFOLLOW_LINKS);
	}

	private static boolean deleteUploadedDirectory(final Path realRoot, final String name) throws RefusedException, IOException {
		final Path target = realRoot.resolve(name).normalize();
		if (!realRoot.equals(target.getParent())) {
			throw new RefusedException(400, "subPath.not.acceptable");
		}
		if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
			return false;
		}
		if (!isUploadedDirectory(target)) {
			throw new RefusedException(403, "directory.not.uploaded");
		}
		deleteRecursively(target);
		return true;
	}

	/** Deletes a tree without ever following a symbolic link. */
	private static void deleteRecursively(final Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
				if (e != null) {
					throw e;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void removeFromImportDirectories(final Path realRoot, final List<String> names) throws IOException {
		final Path file = realRoot.resolve(IMPORT_DIRECTORIES_JSON);
		if (names.isEmpty() || !Files.isRegularFile(file)) {
			return;
		}
		final JsonArray directories = new JsonArray(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
		final JsonArray kept = new JsonArray();
		for (Object directory : directories) {
			if (!names.contains(directory)) {
				kept.add(directory);
			}
		}
		Files.write(file, kept.encode().getBytes(StandardCharsets.UTF_8));
	}
}
