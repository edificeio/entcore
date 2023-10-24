package org.entcore.common.folders.impl;

import static org.entcore.common.folders.impl.QueryHelper.isOk;
import static org.entcore.common.folders.impl.QueryHelper.toErrorStr;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import io.vertx.core.Promise;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class StorageHelper {
	public static Set<String> getThumbnailsFilesIds(JsonObject jsonDocument) {
		Set<String> thumbIds = Stream.of(jsonDocument.getJsonObject("thumbnails", null))
				.filter(thumbs -> thumbs != null)//
				.flatMap(thumbs -> thumbs.stream()
						.map(entry -> entry.getValue() != null ? entry.getValue().toString() : null))//
				.filter(file -> !StringUtils.isEmpty(file)).collect(Collectors.toSet());
		return thumbIds;
	}

	public static void replaceAll(JsonObject jsonDocument, Map<String, String> oldFileIdForNewFileId) {
		replaceAll(jsonDocument, oldFileIdForNewFileId, false);
	}

	/**
	 * Map all files ID in a Document to another "new" ID.
	 * Useful when cloning a document.
	 * @param jsonDocument
	 * @param oldFileIdForNewFileId Map of (old,new) IDs
	 * @param skipMissingThumbnails When truthy, drop thumbnail files that are not in the map.
	 */
	public static void replaceAll(
			JsonObject jsonDocument, Map<String, String> oldFileIdForNewFileId,
			boolean skipMissingThumbnails
			) {
		// replace file id
		Set<String> fileIds = getFileId(jsonDocument);
		for (String fileId : fileIds) {
			if (!oldFileIdForNewFileId.containsKey(fileId)) {
				throw new IllegalStateException("Could not found newFileId of the file:" + fileId);
			}
			setFileId(jsonDocument, oldFileIdForNewFileId.get(fileId));
		}
		// replace or drop thumb values
		fileIds = getThumbnailsFilesIds(jsonDocument);
		for (String fileId : fileIds) {
			if (!oldFileIdForNewFileId.containsKey(fileId)) {
				if(skipMissingThumbnails) {
					dropThumbnailFileId(jsonDocument, fileId);
				} else {
					throw new IllegalStateException("Could not found newFileId of the file:" + fileId);
				}
			} else {
				replaceThumbnailFileId(jsonDocument, fileId, oldFileIdForNewFileId.get(fileId));
			}
		}
	}

	static Set<String> getFileId(JsonObject jsonDocument) {
		Set<String> filesIds = Stream.of(jsonDocument.getString("file", null))
				.filter(file -> !StringUtils.isEmpty(file)).collect(Collectors.toSet());
		return filesIds;
	}

	static void setFileId(JsonObject jsonDocument, String fileId) {
		jsonDocument.put("file", fileId);
	}

	static void replaceThumbnailFileId(JsonObject jsonDocument, String fileId, String newFileId) {
		JsonObject meta = jsonDocument.getJsonObject("thumbnails", new JsonObject());
		for (String key : meta.fieldNames()) {
			String value = meta.getString(key);
			if (value != null && newFileId != null && value.equals(fileId)) {
				meta.put(key, newFileId);
			}
		}
		jsonDocument.put("thumbnails", meta);
	}

	static void dropThumbnailFileId(JsonObject jsonDocument, String fileId) {
		JsonObject meta = jsonDocument.getJsonObject("thumbnails", new JsonObject());
		for (String key : meta.fieldNames()) {
			String value = meta.getString(key);
			if (value != null && value.equals(fileId)) {
				meta.remove(key);
				break;
			}
		}
		jsonDocument.put("thumbnails", meta);
	}

	public static List<String> getListOfFileIds(JsonObject jsonDocument) {
		List<String> listOfFilesIds = new ArrayList<>(getFileId(jsonDocument));
		listOfFilesIds.addAll(getThumbnailsFilesIds(jsonDocument));
		return listOfFilesIds;
	}

	public static List<String> getListOfFileIds(Collection<JsonObject> files) {
		List<String> listOfFilesIds = files.stream().flatMap(f -> getListOfFileIds(f).stream())
				.collect(Collectors.toList());
		return listOfFilesIds;
	}

	static Future<Map<String, String>> copyFileInStorage(Storage storage, Collection<JsonObject> originals,
			boolean throwErrors) {
		// only files are duplicated in storage
		Set<JsonObject> files = originals.stream().filter(o -> DocumentHelper.isFile(o)).collect(Collectors.toSet());
		if (files.isEmpty()) {
			return Future.succeededFuture(new HashMap<>());
		}
		@SuppressWarnings("rawtypes")
		List<Future> copyFutures = new ArrayList<>();
		Set<String> fileIds = new HashSet<>(StorageHelper.getListOfFileIds(files));
		for (String fId : fileIds) {
			Promise<Entry<String, String>> future = Promise.promise();
			storage.copyFile(fId, res -> {
				if (isOk(res)) {
					future.complete(new AbstractMap.SimpleEntry<String, String>(fId, res.getString("_id")));
				} else {
					if (throwErrors) {
						future.fail(toErrorStr(res));
					} else {
						// if file not exists=> no errors
						String message = res.getString("message", "");
						if (message.contains("java.nio.file.NoSuchFileException")) {
							future.complete(new AbstractMap.SimpleEntry<String, String>(fId, fId));
						} else {
							future.fail(toErrorStr(res));
						}
					}
				}
			});
			copyFutures.add(future.future());
		}
		return CompositeFuture.all(copyFutures).map(copyStorageEvent -> {
			@SuppressWarnings("unchecked")
			Map<String, String> oldFileIdForNewFileId = copyStorageEvent.list().stream()
					.map(pair -> (Entry<String, String>) pair)
					.collect(Collectors.toMap(pair -> pair.getKey(), pair -> pair.getValue()));
			return oldFileIdForNewFileId;
		});
	}

	/**
	 * Copy main file and any thumbnail of a document.
	 * @param storage where to copy
	 * @param fileIds a list of file ids
	 * @return a Map of <ID of original file, ID of copied file>
	 *
	 */
	static public Future<Map<String, String>> copyFiles(Storage storage, final List<String> fileIds) {
		@SuppressWarnings("rawtypes")
		final List<Future> copyFutures = new ArrayList<Future>();

		for (String fId : fileIds) {
			Promise<Pair<String, String>> promise = Promise.promise();

			storage.copyFile(fId, res -> {
				if (isOk(res)) {
					promise.complete(Pair.of(fId, res.getString("_id")));
				} else {
					promise.complete(null);
				}
			});

			copyFutures.add(promise.future());
		}

		return CompositeFuture
		.all(copyFutures)
		.map(unused -> {
			@SuppressWarnings("unchecked")
			Map<String, String> oldFileIdForNewFileId = copyFutures.stream()
				.map(future -> future == null ? null : (Pair<String, String>) future.result())
				.filter( pair -> pair != null)
				.collect(Collectors.toMap(pair -> pair.getKey(), pair -> pair.getValue()));
			return oldFileIdForNewFileId;
		});
	}
}