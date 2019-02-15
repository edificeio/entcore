package org.entcore.common.folders.impl;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.entcore.common.folders.FolderManager;
import org.entcore.common.utils.StringUtils;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DocumentHelper {
	public static JsonArray getOrCreateFavorties(JsonObject doc) {
		if (!doc.containsKey("favorites")) {
			doc.put("favorites", new JsonArray());
		}
		return doc.getJsonArray("favorites");
	}

	public static boolean idEquals(JsonObject doc1, JsonObject doc2) {
		String id1 = getId(doc1);
		String id2 = getId(doc2);
		return id1 != null && id1.equals(id2);
	}

	public static String getId(JsonObject doc) {
		return doc.getString("_id");
	}

	public static String getOwner(JsonObject doc) {
		return doc.getString("owner");
	}

	public static String getOwnerName(JsonObject doc) {
		return doc.getString("ownerName");
	}

	public static String getModifiedStr(JsonObject doc) {
		return doc.getString("modified");
	}

	public static Optional<Date> getModified(JsonObject doc) {
		try {
			return Optional.ofNullable(MongoDb.parseDate(doc.getString("modified")));
		} catch (ParseException e) {
			return Optional.empty();
		}
	}

	public static String getName(JsonObject doc) {
		return doc.getString("name");
	}

	public static String getName(JsonObject doc, String def) {
		return doc.getString("name", def);
	}

	public static void setParent(JsonObject doc, String newParent) {
		doc.put("eParent", newParent);
		doc.remove("eParentOld");
	}

	public static String getParent(JsonObject doc) {
		return doc.getString("eParent");
	}

	public static String getParentOld(JsonObject doc) {
		return doc.getString("eParentOld");
	}

	public static boolean isShared(JsonObject doc) {
		return doc.getBoolean("isShared", false);
	}

	public static boolean hasParent(JsonObject doc) {
		return !StringUtils.isEmpty(getParent(doc));
	}

	public static String getType(JsonObject doc) {
		return doc.getString("eType", "");
	}

	public static String getFileId(JsonObject doc) {
		return doc.getString("file", "");
	}

	public static String getFileName(JsonObject doc, String defaut) {
		JsonObject metadata = doc.getJsonObject("metadata");
		if (metadata != null) {
			return metadata.getString("filename", getName(doc, defaut));
		}
		return getName(doc, defaut);
	}

	public static boolean isFile(JsonObject doc) {
		return getType(doc).equals(FolderManager.FILE_TYPE);
	}

	public static boolean isFolder(JsonObject doc) {
		return getType(doc).equals(FolderManager.FOLDER_TYPE);
	}

	public static long getFileSize(JsonObject doc) {
		JsonObject metadata = doc.getJsonObject("metadata");
		if (metadata != null) {
			return metadata.getLong("size", 0l);
		}
		return 0;
	}

	public static long getFileSize(Collection<JsonObject> docs) {
		return docs.stream().map(o -> DocumentHelper.getFileSize(o)).reduce(0l, (a1, a2) -> a1 + a2);
	}

	public static long getFileSize(JsonArray docs) {
		return docs.stream().map(o -> (JsonObject) o)//
				.map(o -> DocumentHelper.getFileSize(o)).reduce(0l, (a1, a2) -> a1 + a2);
	}
}
