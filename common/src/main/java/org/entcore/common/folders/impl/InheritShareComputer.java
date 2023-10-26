package org.entcore.common.folders.impl;

import java.util.*;

import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.entcore.common.utils.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class InheritShareComputer {
	static class InheritShareResult {
		public final Optional<JsonObject> parentRoot;
		public final JsonObject root;
		public final List<JsonObject> childrens;

		public InheritShareResult(Optional<JsonObject> parentRoot, JsonObject root, List<JsonObject> childrens) {
			super();
			this.parentRoot = parentRoot;
			this.root = root;
			this.childrens = childrens;
		}

		public List<JsonObject> getAll() {
			List<JsonObject> all = new ArrayList<>();
			all.add(this.root);
			all.addAll(childrens);
			return all;
		}
	}

	public static JsonArray concatShares(final JsonArray... shared){
		final JsonArray merged = new JsonArray();
		final Map<String, JsonObject> byUser = new HashMap<>();
		final Map<String, JsonObject> byGroup = new HashMap<>();
		for(final JsonArray current : shared){
			for(final Object share : current){
				if(share instanceof JsonObject){
					final JsonObject shareJson = (JsonObject) share;
					final String userId = shareJson.getString("userId");
					final String groupId = shareJson.getString("groupId");
					if(userId != null){
						final JsonObject currentShare = byUser.computeIfAbsent(userId, e -> {
							final JsonObject newShare = new JsonObject().mergeIn(shareJson);
							merged.add(newShare);
							return newShare;
						});
						currentShare.mergeIn(shareJson);
					}else if(groupId != null){
						final JsonObject currentShare = byGroup.computeIfAbsent(groupId, e -> {
							final JsonObject newShare = new JsonObject().mergeIn(shareJson);
							merged.add(newShare);
							return newShare;
						});
						currentShare.mergeIn(shareJson);
					}
				}
			}
		}
		return merged;
	}

	public static void mergeShared(JsonObject parentFolder, JsonObject current, boolean resetSharedIfNeeded) throws RuntimeException {
		if (!DocumentHelper.isFolder(parentFolder)) {
			throw new IllegalArgumentException("The parent is not a folder :" + parentFolder.getString("_id"));
		} else {
			JsonArray parentAncestors = parentFolder.getJsonArray("ancestors", new JsonArray());
			JsonArray parentShared = parentFolder.getJsonArray("inheritedShares", new JsonArray());
			JsonArray currentShared = current.getJsonArray("shared", new JsonArray());
			boolean isParentShared = DocumentHelper.isShared(parentFolder);
			//
			JsonArray inherit = new JsonArray();
			inherit.addAll(currentShared);
			inherit.addAll(parentShared);
			//
			JsonArray ancestors = new JsonArray();
			ancestors.addAll(parentAncestors);
			ancestors.add(DocumentHelper.getId(parentFolder));
			//
			current.put("ancestors", ancestors);
			current.put("inheritedShares", inherit);
			current.put("isShared", inherit.size() > 0);
			//if parent is not shared=> child is also not shared
			if(resetSharedIfNeeded && !isParentShared) {
				current.put("shared", new JsonArray());
				current.put("inheritedShares", new JsonArray());
				current.put("isShared", false);
			}
		}
	}

	public static void mergeShared(Optional<JsonObject> parentFolder, JsonObject current, boolean resetSharedIfNeeded) throws RuntimeException {
		if (parentFolder.isPresent()) {
			mergeShared(parentFolder.get(), current, resetSharedIfNeeded);
		} else {
			JsonArray shared = current.getJsonArray("shared", new JsonArray());
			current.put("shared", shared);
			current.put("inheritedShares", shared);
			current.put("isShared", shared.size() > 0);
			current.put("ancestors", new JsonArray());
		}
	}

	//
	private final QueryHelper query;

	public InheritShareComputer(QueryHelper query) {
		this.query = query;
	}

	private Future<List<JsonObject>> childrens(JsonObject root, boolean recursive) {
		if (recursive) {
			String rootId = DocumentHelper.getId(root);
			switch (DocumentHelper.getType(root)) {
			case FolderManager.FOLDER_TYPE:
				DocumentQueryBuilder parentFilter = query.queryBuilder().withId(rootId);
				return query.getChildrenRecursively(parentFilter, Optional.empty(), false);
			case FolderManager.FILE_TYPE:
			default:
				return Future.succeededFuture(new ArrayList<>());
			}
		} else {
			return Future.succeededFuture(new ArrayList<>());
		}
	}

	public Future<InheritShareResult> computeFromParentId(JsonObject root, boolean recursive,
														  Optional<String> parentRootId) {
		// get parent inheritshared
		Future<Optional<JsonObject>> rootParentFuture = this.rootParent(parentRootId);
		// get children recursively
		return rootParentFuture.compose(rootParentInheritShared -> {
			return this.childrens(root, recursive);
		}).map(childrens -> {
			// merge root (dont reset root's share=> root shares are setted by user)
			mergeShared(rootParentFuture.result(), root, false);
			// merge children
			mergeRecursive(root, childrens);
			// return result
			return new InheritShareResult(rootParentFuture.result(), root, childrens);
		});
	}

	public Future<InheritShareResult> computeFromParent(JsonObject root, boolean recursive,
														  Optional<JsonObject> parentRoot) {
		// get children recursively
		return this.childrens(root, recursive).map(childrens -> {
			// merge root (dont reset root's share=> root shares are setted by user)
			mergeShared(parentRoot, root, false);
			// merge children
			mergeRecursive(root, childrens);
			// return result
			return new InheritShareResult(parentRoot, root, childrens);
		});
	}

	public Future<InheritShareResult> compute(JsonObject root, boolean recursive) {
		// get parent inheritshared
		Future<Optional<JsonObject>> rootParentFuture = this.rootParent(root);
		// get children recursively
		return rootParentFuture.compose(rootParentInheritShared -> {
			return this.childrens(root, recursive);
		}).map(childrens -> {
			// merge root (dont reset root's share=> root shares are setted by user)
			mergeShared(rootParentFuture.result(), root, false);
			// merge children
			mergeRecursive(root, childrens);
			// return result
			return new InheritShareResult(rootParentFuture.result(), root, childrens);
		});
	}

	public Future<InheritShareResult> compute(String id, boolean recursive) {
		return this.query.findById(id).compose(root -> this.compute(root, recursive));
	}

	private void mergeRecursive(JsonObject parent, List<JsonObject> all) {
		String parentId = DocumentHelper.getId(parent);
		for (JsonObject current : all) {
			String eParent = DocumentHelper.getParent(current);
			if (parentId != null && parentId.equals(eParent)) {
				// reset shared of child if parent is not shared
				mergeShared(parent, current, true);
				// recurse after updating shared
				mergeRecursive(current, all);
			}
		}
	}

	private Future<Optional<JsonObject>> rootParent(Optional<String> parentId) {
		if (parentId.isPresent() && !StringUtils.isEmpty(parentId.get())) {
			return query.findById(parentId.get()).map(a -> Optional.ofNullable(a)).otherwise(Optional.empty());
		} else {
			return Future.succeededFuture(Optional.empty());
		}
	}

	private Future<Optional<JsonObject>> rootParent(JsonObject root) {
		String eParent = DocumentHelper.getParent(root);
		if (StringUtils.isEmpty(eParent)) {
			return Future.succeededFuture(Optional.empty());
		} else {
			return query.findById(eParent).map(a -> Optional.ofNullable(a)).otherwise(Optional.empty());
		}
	}

}
