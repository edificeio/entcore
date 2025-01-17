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

	public static String prepareNameForSearch(String name){
		if(StringUtils.isEmpty(name)){
			return name;
		}
		final String stripped = StringUtils.stripAccentsToLowerCase(name);
		return stripped.replaceAll("_", " ");
	}

	private static JsonObject initBase(JsonObject doc, String owner, String ownerName, String name, String application)
	{
		if(doc == null)
		{
			doc = new JsonObject();

			// Set some attributes only on new files, otherwise keep the old ones
			doc.putNull("alt");
			doc.putNull("eParent");
			doc.put("ancestors", new JsonArray());
			doc.put("inheritedShares", new JsonArray());
			doc.put("isShared", false);
			doc.putNull("legend");
			doc.put("shared", new JsonArray());
		}

		String now = MongoDb.formatDate(new Date());

		// Set creation data to the current user and time
		doc.put("created", now);
		doc.put("modified", now);
		doc.put("owner", owner);
		doc.put("ownerName", ownerName);

		if(name == null)
			name = DocumentHelper.getName(doc, "");

		doc.put("name", name);
		doc.put("nameSearch", prepareNameForSearch(name));

		if(application != null)
			doc.put("application", application);

		return doc;
	}

	public static JsonObject initFile(JsonObject doc, String owner, String ownerName)
	{
		return DocumentHelper.initFile(doc, owner, ownerName, null, null);
	}

	public static JsonObject initFile(JsonObject doc, String owner, String ownerName, String name)
	{
		return DocumentHelper.initFile(doc, owner, ownerName, name, null);
	}

	public static JsonObject initFile(JsonObject doc, String owner, String ownerName, String name, String application)
	{
		doc = DocumentHelper.initBase(doc, owner, ownerName, name, application);

		doc.put("eType", FolderManager.FILE_TYPE);

		return doc;
	}

	public static JsonObject initFolder(JsonObject doc, String owner, String ownerName)
	{
		return DocumentHelper.initFolder(doc, owner, ownerName, null, null);
	}

	public static JsonObject initFolder(JsonObject doc, String owner, String ownerName, String name)
	{
		return DocumentHelper.initFolder(doc, owner, ownerName, name, null);
	}

	public static JsonObject initFolder(JsonObject doc, String owner, String ownerName, String name, String application)
	{
		doc = DocumentHelper.initBase(doc, owner, ownerName, name, application);

		doc.put("eType", FolderManager.FOLDER_TYPE);

		// Create the metadata if needed
		JsonObject metadata = DocumentHelper.getMetadata(doc);

		return doc;
	}

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

	public static JsonObject setModified(JsonObject doc, Date date) {
		Object modified = doc.getValue("modified");
		Object created = doc.getValue("created");
		// Check that the dates are in string format
		// we also check for nullity to keep the same behaviour as in vertx 3
		if((modified == null || modified instanceof String) &&
			(created == null || created instanceof String)) {
			String now = MongoDb.formatDate(date != null ? date : new Date());

			doc.put("modified", now);

			if(created == null) {
				doc.put("created", now);
			}
		} else {
			// It appears that those dates were in fact objects
			JsonObject now = MongoDb.now();

			doc.put("modified", now);

			if(created == null) {
				doc.put("created", now);
			}
		}

		return doc;
	}

	public static String getName(JsonObject doc) {
		return doc.getString("name");
	}

	public static String getName(JsonObject doc, String def) {
		return doc.getString("name", def);
	}

	public static JsonObject setName(JsonObject doc, String name)
	{
		doc.put("name", name);
		doc.put("nameSearch", prepareNameForSearch(name));
		return doc;
	}

	public static String getTitle(JsonObject doc) {
		return doc.getString("title");
	}

	public static String getTitle(JsonObject doc, String def) {
		return doc.getString("title", def);
	}

	public static JsonObject setTitle(JsonObject doc, String title)
	{
		doc.put("title", title);
		return doc;
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

			if(DocumentHelper.isFolder(doc) == false)
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

	public static void setFileSize(JsonObject doc, long size) {
		final JsonObject metadata = doc.getJsonObject("metadata", new JsonObject());
		metadata.put("size", size);
		doc.put("metadata", metadata);
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

	public static String getAppProperty(JsonObject doc, String propName)
	{
		return doc.getString(propName);
	}

	public static JsonObject setAppProperty(JsonObject doc, String propName, String value)
	{
		doc.put(propName, value);
		return doc;
	}

	public static JsonObject clearComments(JsonObject doc)
	{
		return DocumentHelper.clearComments(doc, false);
	}

	public static JsonObject clearComments(JsonObject doc, boolean deep)
	{
		if(doc.getValue("comments") != null)
			doc.put("comments", new JsonArray());

		if(deep == true)
		{
			for(Map.Entry<String, Object> entry : doc.getMap().entrySet())
			{
				Object v = entry.getValue();
				if(v instanceof JsonObject)
					DocumentHelper.clearComments((JsonObject)v, deep);
				else if(v instanceof JsonArray)
				{
					JsonArray a = (JsonArray) v;
					for(int i = a.size(); i-->0;)
					{
						Object vv = a.getValue(i);
						if(vv instanceof JsonObject)
							DocumentHelper.clearComments((JsonObject)vv, deep);
					}
				}
			}
		}
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
