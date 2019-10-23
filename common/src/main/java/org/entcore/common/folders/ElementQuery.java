package org.entcore.common.folders;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElementQuery {
	public static enum ElementSort {
		Asc, Desc
	}

	private String id;
	private String type;
	private Integer skip;
	private Integer limit;
	private boolean shared;
	private boolean directShared;
	private Boolean trash;
	private boolean favorites;
	private String parentId;
	private String ancestorId;
	private String application;
	private String searchByName;
	private Boolean hierarchical;
	private Collection<String> ids;
	private Set<String> projection;
	private Set<String> visibilitiesOr;
	private Set<String> visibilitiesIn;
	private Set<String> visibilitiesNotIn;
	private List<String> fullTextSearch;
	private Set<String> ownerIds;
	private String actionNotExists;
	private String actionExists;
	private Boolean hasBeenShared;
	private String notApplication;
	private String trasherId;
	private Boolean noParent;
	private List<Map.Entry<String, ElementSort>> sort;
	private Map<String, Object> params = new HashMap<String, Object>();

	public boolean isDirectShared() {
		return directShared;
	}

	public void setDirectShared(boolean directShared) {
		this.directShared = directShared;
	}

	public Set<String> getVisibilitiesOr() {
		return visibilitiesOr;
	}

	public void setVisibilitiesOr(Set<String> visibilitiesOr) {
		this.visibilitiesOr = visibilitiesOr;
	}

	public Set<String> getVisibilitiesNotIn() {
		return visibilitiesNotIn;
	}

	public void setVisibilitiesNotIn(Set<String> notVisibilities) {
		this.visibilitiesNotIn = notVisibilities;
	}

	public Boolean getNoParent() {
		return noParent;
	}

	public void setNoParent(Boolean noParent) {
		this.noParent = noParent;
	}

	public static Set<String> defaultProjection() {
		return Stream.of("_id", "name", "eType", "file", "deleted", //
				"thumbnails", "eParent", "shared", "inheritedShares", //
				"metadata", "owner", "ownerName", "created", "modified").collect(Collectors.toSet());
	}

	public String getTrasherId() {
		return trasherId;
	}

	public void setTrasherId(String trasherId) {
		this.trasherId = trasherId;
	}

	public Boolean getHasBeenShared() {
		return hasBeenShared;
	}

	public String getNotApplication() {
		return notApplication;
	}

	public void setHasBeenShared(Boolean hasBeenShared) {
		this.hasBeenShared = hasBeenShared;
	}

	public void setNotApplication(String notApplication) {
		this.notApplication = notApplication;
	}

	public String getActionNotExists() {
		return actionNotExists;
	}

	public void setActionNotExists(String actionNotExists) {
		this.actionNotExists = actionNotExists;
	}

	public void setActionExists(String actionExists) {
		this.actionExists = actionExists;
	}

	public String getActionExists() {
		return actionExists;
	}

	public Set<String> getOwnerIds() {
		return ownerIds;
	}

	public void setOwnerIds(Set<String> ownerIds) {
		this.ownerIds = ownerIds;
	}

	public boolean isFavorites() {
		return favorites;
	}

	public boolean isShared() {
		return shared;
	}

	public boolean getFavorites() {
		return favorites;
	}

	public void setFavorites(boolean favorites) {
		this.favorites = favorites;
	}

	public ElementQuery(boolean includeShared) {
		this.shared = includeShared;
	}

	public boolean getShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public Collection<String> getIds() {
		return ids;
	}

	public void setIds(Collection<String> ids) {
		this.ids = ids;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getLimit() {
		return limit;
	}

	public Integer getSkip() {
		return skip;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public void setSkip(Integer skip) {
		this.skip = skip;
	}

	public void addSort(String name, ElementSort sort) {
		if (this.sort == null) {
			this.sort = new ArrayList<>();
		}
		this.sort.add(new AbstractMap.SimpleEntry<String, ElementSort>(name, sort));
	}

	public void addProjection(String name) {
		if (this.projection == null) {
			this.projection = new HashSet<>();
		}
		this.projection.add(name);
	}

	public Set<String> getProjection() {
		return projection;
	}

	public List<Map.Entry<String, ElementSort>> getSort() {
		return sort;
	}

	public void setSort(List<Map.Entry<String, ElementSort>> sort) {
		this.sort = sort;
	}

	public List<String> getFullTextSearch() {
		return fullTextSearch;
	}

	public void setFullTextSearch(List<String> fullTextSearch) {
		this.fullTextSearch = fullTextSearch;
	}

	public void setProjection(Set<String> projection) {
		this.projection = projection;
	}

	public Set<String> getVisibilitiesIn() {
		return visibilitiesIn;
	}

	public void setVisibilitiesIn(Set<String> visibilities) {
		this.visibilitiesIn = visibilities;
	}

	public String getApplication() {
		return application;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public void setSearchByName(String searchByName) {
		this.searchByName = searchByName;
	}

	public String getSearchByName() {
		return searchByName;
	}

	public void setFolder(String folder) {
		this.searchByName = folder;
	}

	public Boolean getHierarchical() {
		return hierarchical;
	}

	public void setHierarchical(Boolean hierarchical) {
		this.hierarchical = hierarchical;
	}

	public Boolean getTrash() {
		return trash;
	}

	public void setTrash(Boolean trash) {
		this.trash = trash;
	}

	public String getId() {
		return id;
	}

	public String getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(String ancestorId) {
		this.ancestorId = ancestorId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
}
