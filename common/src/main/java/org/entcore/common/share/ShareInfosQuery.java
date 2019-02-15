package org.entcore.common.share;

import java.util.ArrayList;
import java.util.List;

public class ShareInfosQuery {
	//GROUP FILTERS
	public static final String GROUP_FILTER_STUDENT = "Student";
	public static final String GROUP_FILTER_RELATIVE = "Relative";
	public static final String GROUP_FILTER_PERSONNEL = "Personnel";
	public static final String GROUP_FILTER_TEACHER = "Teacher";
	public static final String GROUP_FILTER_GUEST = "Guest";
	public static final String GROUP_FILTER_ADMINLOCAL = "AdminLocal";
	//USER PROFILES
	public static final String USER_PROFILE_TEACHER = "Teacher";
	public static final String USER_PROFILE_STUDENT = "Student";
	public static final String USER_PROFILE_RELATIVE = "Relative";
	public static final String USER_PROFILE_GUEST = "Guest";
	public static final String USER_PROFILE_PERSONNEL = "Personnel";
	//
	private final String search;
	private List<String> onlyUsersWithProfiles = new ArrayList<>();
	private List<String> onlyGroupsWithFilters = new ArrayList<>();

	public ShareInfosQuery(String search) {
		super();
		this.search = search;
	}

	public List<String> getOnlyUsersWithProfiles() {
		return onlyUsersWithProfiles;
	}

	public void setOnlyUsersWithProfiles(List<String> onlyUsersWithProfiles) {
		this.onlyUsersWithProfiles = onlyUsersWithProfiles;
	}

	public List<String> getOnlyGroupsWithFilters() {
		return onlyGroupsWithFilters;
	}

	public void setOnlyGroupsWithFilters(List<String> onlyGroupsWithFilters) {
		this.onlyGroupsWithFilters = onlyGroupsWithFilters;
	}

	public String getSearch() {
		return search;
	}

}
