package edu.one.core.common.user;

import java.util.List;

public class UserInfos {

	public static class Action {
		private String name;
		private String displayName;
		private String type;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

	public static class Application {
		private String name;
		private String address;
		private String icon;
		private String target;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

	}

	private String userId;
	private String firstName;
	private String lastName;
	private String username;
	private String classId;
	private String schoolName;
	private String uai;
	private String level;
	private String type;
	private String login;
	private List<Action> authorizedActions;
	private List<Application> apps;
	private List<String> profilGroupsIds;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<Action> getAuthorizedActions() {
		return authorizedActions;
	}

	public void setAuthorizedActions(List<Action> authorizedActions) {
		this.authorizedActions = authorizedActions;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getClassId() {
		return classId;
	}

	public void setClassId(String classId) {
		this.classId = classId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public void setSchoolName(String schoolName) {
		this.schoolName = schoolName;
	}

	public String getUai() {
		return uai;
	}

	public void setUai(String uai) {
		this.uai = uai;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<Application> getApps() {
		return apps;
	}

	public void setApps(List<Application> apps) {
		this.apps = apps;
	}

	public List<String> getProfilGroupsIds() {
		return profilGroupsIds;
	}

	public void setProfilGroupsIds(List<String> profilGroupsIds) {
		this.profilGroupsIds = profilGroupsIds;
	}

}
