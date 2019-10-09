package org.entcore.common.folders.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

	public static JsonObject setId(JsonObject doc, String id) {
		doc.put("_id", id);
		return doc;
	}

	public static JsonObject removeId(JsonObject doc)
	{
		doc.remove("_id");
		return doc;
	}

	public static String getOwner(JsonObject doc) {
		return doc.getString("owner");
	}

	public static String getExternalId(JsonObject doc){
		return doc.getString("externalId");
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

	public static JsonObject setParent(JsonObject doc, String newParent) {
		doc.put("eParent", newParent);
		doc.remove("eParentOld");
		return doc;
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

	public static JsonObject setShared(JsonObject doc, boolean isShared)
	{
		doc.put("isShared", isShared);
		return doc;
	}

	public static JsonObject removeShares(JsonObject doc)
	{
		doc.put("shared", new JsonArray());
		doc.put("inheritedShares", new JsonArray());
		return doc;
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

	public static JsonObject setFileId(JsonObject doc, String fileId) {
		doc.put("file", fileId);
		return doc;
	}

	public static JsonObject getMetadata(JsonObject doc)
	{
		JsonObject metadata = doc.getJsonObject("metadata");
		if (metadata == null)
		{
			metadata = new JsonObject();
			doc.put("metadata", metadata);
		}

		return metadata;
	}

	public static JsonObject mergeMetadata(JsonObject src, JsonObject dest)
	{
		JsonObject oldM = DocumentHelper.getMetadata(src);
		JsonObject newM = DocumentHelper.getMetadata(dest);

		Map<String, Object> data = oldM.getMap();
		for(Map.Entry<String, Object> entry : data.entrySet())
			newM.put(entry.getKey(), entry.getValue());

		return dest;
	}

	public static String getFileName(JsonObject doc, String defaut) {
		JsonObject metadata = doc.getJsonObject("metadata");
		if (metadata != null) {
			return metadata.getString("filename", getName(doc, defaut));
		}
		return getName(doc, defaut);
	}

	public static JsonObject setFileName(JsonObject doc, String fileName)
	{
		JsonObject metadata = DocumentHelper.getMetadata(doc);
		metadata.put("filename", fileName);

		return doc;
	}

	public static String getContentType(JsonObject doc) {
		JsonObject metadata = doc.getJsonObject("metadata");
		if (metadata != null) {
			return metadata.getString("content-type", "");
		}
		return "";
	}

	public static JsonObject setContentType(JsonObject doc, String contentType)
	{
		JsonObject metadata = DocumentHelper.getMetadata(doc);
		metadata.put("content-type", contentType);

		return doc;
	}

	public static boolean isFile(JsonObject doc) {
		return getType(doc).equals(FolderManager.FILE_TYPE);
	}

	public static JsonArray getAncestors(JsonObject doc) {
		return doc.getJsonArray("ancestors", new JsonArray());
	}

	public static List<String> getAncestorsAsList(JsonObject doc) {
		List<String> ancestors = new ArrayList<>();
		for (Object o : getAncestors(doc)) {
			if (o instanceof String)
				ancestors.add((String) o);
		}
		return ancestors;
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

	public static JsonObject getThumbnails(JsonObject doc)
	{
		JsonObject thumbs = doc.getJsonObject("thumbnails");
		return thumbs == null ? new JsonObject() : thumbs;
	}

	public static JsonObject setThumbnails(JsonObject doc, JsonObject thumbnails)
	{
		doc.put("thumbnails", thumbnails == null ? new JsonObject() : thumbnails);
		return doc;
	}

	public static boolean getProtected(JsonObject doc)
	{
		return doc.getBoolean("protected");
	}

	public static JsonObject setProtected(JsonObject doc, boolean isProtected)
	{
		doc.put("protected", isProtected);
		return doc;
	}

	public static boolean isImage(JsonObject doc)
	{
		if (doc == null)
			return false;
		JsonObject metadata = doc.getJsonObject("metadata");
		return metadata != null && (
			"image/jpeg".equals(metadata.getString("content-type"))
			|| "image/gif".equals(metadata.getString("content-type"))
			|| "image/png".equals(metadata.getString("content-type"))
			|| "image/tiff".equals(metadata.getString("content-type"))
		);
	}
}
